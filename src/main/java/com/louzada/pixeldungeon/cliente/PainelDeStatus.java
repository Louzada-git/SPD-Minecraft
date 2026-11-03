package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

// A barra de status grande do Shattered (a da interface completa):
//   à esquerda, o rosto do herói (a skin do jogador) e o nível embaixo;
//   à direita, a vida (vermelha, com o escudo em cinza) e a experiência (amarela);
//   em cima delas, os efeitos ativos, a fome e o aviso de pontos de talento.
public class PainelDeStatus {
    public static final int LARGURA = 160;
    public static final int ALTURA = 39;
    // As barras de vida e de experiência têm 128 de largura, igual no Shattered
    private static final int LARGURA_DA_BARRA = 128;
    // Fileira dos efeitos (ícones de 16, um do lado do outro)
    private static final int X_DOS_EFEITOS = 31;
    private static final int LARGURA_DOS_EFEITOS = LARGURA - X_DOS_EFEITOS - 1;
    private static final int ICONE = 16;
    // O rosto pisca em amarelo por 10 segundos quando aparecem pontos de talento novos
    private static final long TEMPO_PISCANDO = 10_000;
    private static final float PISCADAS_POR_SEGUNDO = (float) (Math.PI * 1.5);
    private static int ultimosPontos = 0;
    private static long piscarAte = 0;

    // Um ícone da fileira de cima: um efeito, a fome ou o aviso de talento
    private record Icone(int x, int y, @Nullable MobEffectInstance efeito, @Nullable DesenhoDoShattered.Folha imagem, List<Component> dica) {
    }

    public static void desenhar(GuiGraphics desenho, LocalPlayer jogador, int x, int y) {
        Minecraft jogo = Minecraft.getInstance();
        Font fonte = jogo.font;

        // Fundo: o quadro do herói, o do nível e o das barras (o meio estica até 160 de largura)
        DesenhoDoShattered.STATUS.moldura(desenho, x, y, LARGURA, ALTURA, 0, 64, 41, 39, 33, 0, 4, 0);

        // O rosto do herói: a skin do jogador
        PlayerFaceRenderer.draw(desenho, jogador.getSkin(), x + 7, y + 7, 16);
        int tinta = tintaDoRosto(jogador);
        if (tinta != 0) {
            desenho.fill(x + 7, y + 7, x + 23, y + 23, tinta);
        }

        // Vida: primeiro o escudo (cinza), depois a vida (vermelha) por cima
        float maximo = Math.max(1.0F, jogador.getMaxHealth());
        float vida = Math.max(0.0F, jogador.getHealth()) / maximo;
        float escudo = jogador.getAbsorptionAmount() / maximo;
        if (vida + escudo > 1.0F) {
            float excesso = vida + escudo;
            vida /= excesso;
            escudo /= excesso;
        }
        DesenhoDoShattered.STATUS.desenhar(desenho, x + 30, y + 19, 0, 112, Math.round(LARGURA_DA_BARRA * (vida + escudo)), 9);
        DesenhoDoShattered.STATUS.desenhar(desenho, x + 30, y + 19, 0, 103, Math.round(LARGURA_DA_BARRA * vida), 9);
        String textoDaVida = textoDaVida(jogador);
        desenho.drawString(fonte, textoDaVida, x + 30 + (LARGURA_DA_BARRA - fonte.width(textoDaVida)) / 2, y + 20, 0x99FFFFFF, false);

        // Experiência
        int xp = xp(jogador);
        int xpParaSubir = RegrasDoHeroi.xpParaSubir(EstadoNoCliente.nivel());
        DesenhoDoShattered.STATUS.desenhar(desenho, x + 30, y + 30, 0, 121, Math.round(LARGURA_DA_BARRA * Mth.clamp((float) xp / xpParaSubir, 0, 1)), 7);
        String textoDaXp = xp + "/" + xpParaSubir;
        desenho.drawString(fonte, textoDaXp, x + 30 + (LARGURA_DA_BARRA - fonte.width(textoDaXp)) / 2, y + 30, 0x99FFFFAA, false);

        // Nível, embaixo do rosto
        String nivel = "lv. " + EstadoNoCliente.nivel();
        if (fonte.width(nivel) > 26) {
            nivel = "lv." + EstadoNoCliente.nivel();
        }
        desenho.drawString(fonte, nivel, x + 1 + (28 - fonte.width(nivel) + 1) / 2, y + 30, DesenhoDoShattered.AMARELO_CLARO, false);

        // Efeitos, fome e talentos
        for (Icone icone : icones(jogador, x, y)) {
            if (icone.efeito() != null) {
                desenharEfeito(desenho, icone.efeito(), icone.x(), icone.y());
            } else if (icone.imagem() != null) {
                DesenhoDoShattered.Folha imagem = icone.imagem();
                imagem.desenhar(desenho, icone.x() + (ICONE - imagem.largura()) / 2, icone.y() + (ICONE - imagem.altura()) / 2);
            }
        }
        // Do lado do aviso de talento, a tecla da tela de talentos
        if (EstadoNoCliente.pontosLivres() > 0) {
            Component tecla = TeclasDoMod.TALENTOS.getTranslatedKeyMessage();
            desenho.drawString(fonte, tecla, x + X_DOS_EFEITOS + LARGURA_DOS_EFEITOS - fonte.width(tecla), y + 4, DesenhoDoShattered.AMARELO, true);
        }
    }

    // A dica de onde o mouse está (na tela do inventário), ou null
    @Nullable
    public static List<Component> dica(LocalPlayer jogador, int x, int y, double mouseX, double mouseY) {
        for (Icone icone : icones(jogador, x, y)) {
            if (DesenhoDoShattered.dentro(mouseX, mouseY, icone.x(), icone.y(), ICONE, ICONE)) {
                return icone.dica();
            }
        }
        if (DesenhoDoShattered.dentro(mouseX, mouseY, x + 30, y + 19, LARGURA_DA_BARRA, 9)) {
            return List.of(Component.translatable("hud.pixeldungeon.vida", textoDaVida(jogador)));
        }
        if (DesenhoDoShattered.dentro(mouseX, mouseY, x + 30, y + 30, LARGURA_DA_BARRA, 7)) {
            return List.of(Component.translatable("hud.pixeldungeon.experiencia", xp(jogador), RegrasDoHeroi.xpParaSubir(EstadoNoCliente.nivel())));
        }
        if (DesenhoDoShattered.dentro(mouseX, mouseY, x, y, 30, ALTURA)) {
            return dicaDoHeroi(jogador);
        }
        return null;
    }

    // Clicar no rosto (ou no painel do herói) abre os talentos, igual ao Shattered
    public static boolean noRosto(int x, int y, double mouseX, double mouseY) {
        return DesenhoDoShattered.dentro(mouseX, mouseY, x, y, 30, ALTURA);
    }

    public static List<Component> dicaDoHeroi(LocalPlayer jogador) {
        List<Component> linhas = new ArrayList<>();
        ClasseDoHeroi classe = EstadoNoCliente.classe();
        MutableComponent titulo = jogador.getDisplayName().copy();
        if (classe != null) {
            titulo.append(Component.literal(" - ")).append(Component.translatable(classe.chaveDoNome()));
        }
        linhas.add(titulo.withStyle(ChatFormatting.YELLOW));
        linhas.add(Component.translatable("hud.pixeldungeon.ver_talentos", TeclasDoMod.TALENTOS.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));
        return linhas;
    }

    private static String textoDaVida(LocalPlayer jogador) {
        int vida = Mth.ceil(Math.max(0.0F, jogador.getHealth()));
        int maximo = Mth.ceil(jogador.getMaxHealth());
        int escudo = Mth.ceil(jogador.getAbsorptionAmount());
        return escudo > 0 ? vida + "+" + escudo + "/" + maximo : vida + "/" + maximo;
    }

    // A experiência do nível atual (o servidor guarda ela no total de experiência do Minecraft)
    private static int xp(LocalPlayer jogador) {
        return Math.max(0, jogador.totalExperience);
    }

    // O rosto fica escuro se o herói morreu, pisca em vermelho com pouca vida e em amarelo com talentos novos
    private static int tintaDoRosto(LocalPlayer jogador) {
        long agora = System.currentTimeMillis();
        int pontos = EstadoNoCliente.pontosLivres();
        if (pontos > ultimosPontos) {
            piscarAte = agora + TEMPO_PISCANDO;
        }
        ultimosPontos = pontos;

        float parteDaVida = jogador.getHealth() / Math.max(1.0F, jogador.getMaxHealth());
        if (jogador.isDeadOrDying()) {
            return 0x80000000;
        }
        if (parteDaVida < 0.334F) {
            // Quanto menos vida, mais rápido pisca
            float fase = (agora % 100_000) / 1000.0F * 5.0F * (0.4F - parteDaVida);
            float onda = (Mth.sin(fase * (float) Math.PI * 2) + 1) / 2;
            int vermelho = (int) Mth.lerp(onda, 0x66, 0xCC);
            return 0x80000000 | (vermelho << 16);
        }
        if (agora < piscarAte) {
            float segundos = (piscarAte - agora) / 1000.0F;
            int alfa = (int) (Math.abs(Mth.cos(segundos * PISCADAS_POR_SEGUNDO)) / 2 * 255);
            return (alfa << 24) | 0xFFFF00;
        }
        return 0;
    }

    // Onde fica cada ícone da fileira de cima (o aviso de talento fica no fim, do lado da tecla)
    private static List<Icone> icones(LocalPlayer jogador, int x, int y) {
        List<Icone> icones = new ArrayList<>();
        List<MobEffectInstance> efeitos = jogador.getActiveEffects().stream().filter(MobEffectInstance::showIcon)
                .sorted(Comparator.comparing(efeito -> efeito.getEffect().value().getDescriptionId())).toList();
        int fome = jogador.getFoodData().getFoodLevel();
        DesenhoDoShattered.Folha iconeDaFome = fome <= 0 ? DesenhoDoShattered.FAMINTO : fome <= 7 ? DesenhoDoShattered.FOME : null;
        int quantos = efeitos.size() + (iconeDaFome != null ? 1 : 0);

        // Espaço que sobra pros ícones (o aviso de talento ocupa o fim da fileira)
        int largura = LARGURA_DOS_EFEITOS;
        if (EstadoNoCliente.pontosLivres() > 0) {
            Font fonte = Minecraft.getInstance().font;
            int larguraDoAviso = ICONE + 1 + fonte.width(TeclasDoMod.TALENTOS.getTranslatedKeyMessage());
            largura -= larguraDoAviso + 2;
            icones.add(new Icone(x + X_DOS_EFEITOS + LARGURA_DOS_EFEITOS - larguraDoAviso, y, null, DesenhoDoShattered.TALENTO,
                    List.of(Component.translatable("hud.pixeldungeon.pontos_de_talento", EstadoNoCliente.pontosLivres(),
                            TeclasDoMod.TALENTOS.getTranslatedKeyMessage()).withStyle(ChatFormatting.YELLOW))));
        }
        // Igual ao Shattered: com ícones demais, eles ficam um pouco por cima dos outros
        int passo = ICONE + 1;
        if (quantos > 1 && (quantos - 1) * passo + ICONE > largura) {
            passo = Math.max(4, (largura - ICONE) / (quantos - 1));
        }
        int posicao = 0;
        if (iconeDaFome != null) {
            String chave = iconeDaFome == DesenhoDoShattered.FAMINTO ? "morrendo_de_fome" : "com_fome";
            icones.add(new Icone(x + X_DOS_EFEITOS + posicao * passo, y, null, iconeDaFome,
                    List.of(Component.translatable("hud.pixeldungeon." + chave).withStyle(ChatFormatting.YELLOW),
                            Component.translatable("hud.pixeldungeon." + chave + ".descricao").withStyle(ChatFormatting.GRAY))));
            posicao++;
        }
        float ticksPorSegundo = jogador.level().tickRateManager().tickrate();
        for (MobEffectInstance efeito : efeitos) {
            MutableComponent nome = efeito.getEffect().value().getDisplayName().copy();
            if (efeito.getAmplifier() > 0) {
                nome.append(" ").append(Component.translatable("enchantment.level." + (efeito.getAmplifier() + 1)));
            }
            List<Component> dica = new ArrayList<>();
            dica.add(nome.withStyle(ChatFormatting.YELLOW));
            if (!efeito.isInfiniteDuration()) {
                dica.add(MobEffectUtil.formatDuration(efeito, 1.0F, ticksPorSegundo).copy().withStyle(ChatFormatting.GRAY));
            }
            icones.add(new Icone(x + X_DOS_EFEITOS + posicao * passo, y, efeito, null, dica));
            posicao++;
        }
        return icones;
    }

    // O ícone do efeito (as texturas dos efeitos têm 18x18, com 1 de borda), piscando quando está acabando
    private static void desenharEfeito(GuiGraphics desenho, MobEffectInstance efeito, int x, int y) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(efeito.getEffect());
        float alfa = 1.0F;
        if (!efeito.isAmbient() && efeito.endsWithin(200)) {
            int resta = efeito.getDuration();
            alfa = Mth.clamp(resta / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                    + Mth.cos(resta * (float) Math.PI / 5.0F) * Mth.clamp((10 - resta / 20) / 10.0F * 0.25F, 0.0F, 0.25F);
        }
        RenderSystem.enableBlend();
        desenho.setColor(1.0F, 1.0F, 1.0F, alfa);
        desenho.blit(x - 1, y - 1, 0, 18, 18, sprite);
        desenho.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
