package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.rede.PacoteAprimorarTalento;
import com.louzada.pixeldungeon.rede.PacotePedirCompartilhar;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

// Tela de talentos (tecla K), no estilo do Shattered:
//   à esquerda, os talentos de cada tier (uma linha por tier) com os pontos de cada um embaixo;
//   à direita, o que o talento escolhido faz e o botão "Aprimorar".
// O tier 2 fica apagado até o nível 7.
public class TelaDeTalentos extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int COR_APAGADA = 0xFF9A9A9A;
    private static final int LARGURA = 440;
    private static final int ALTURA = 196;
    // A coluna dos talentos (à esquerda) e a da descrição (à direita)
    private static final int LARGURA_DOS_TALENTOS = 228;
    private static final int X_DA_DESCRICAO = 240;
    private static final int TAMANHO_DO_ICONE = 32;
    private static final int ESPACO_DO_ICONE = 44;
    // Cada tier ocupa uma faixa: o título, os ícones e os pontos
    private static final int ALTURA_DO_TIER = 66;

    private ClasseDoHeroi classe;
    private List<List<Talento>> tiers = List.of();
    private Talento escolhido;
    private Button botaoAprimorar;
    private int esquerda;
    private int topo;

    public TelaDeTalentos() {
        super(Component.translatable("tela.pixeldungeon.talentos"));
    }

    // Já abre com esse talento escolhido (usado nas fotos de teste)
    public TelaDeTalentos(Talento escolhido) {
        this();
        this.escolhido = escolhido;
    }

    @Override
    protected void init() {
        esquerda = (width - LARGURA) / 2;
        topo = (height - ALTURA) / 2;
        classe = EstadoNoCliente.classe();
        tiers = new ArrayList<>();
        if (classe != null) {
            for (int tier = 1; tier <= Talento.TIERS; tier++) {
                tiers.add(Talento.daClasse(classe, tier));
            }
            if (escolhido == null && !tiers.get(0).isEmpty()) {
                escolhido = tiers.get(0).get(0);
            }
        }
        botaoAprimorar = addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.aprimorar"), botao -> aprimorar())
                .bounds(esquerda + X_DA_DESCRICAO + (LARGURA - X_DA_DESCRICAO - 100) / 2, topo + ALTURA - 26, 100, 20)
                .build());
        // Com quem compartilhar o que identificar (poções e pergaminhos)
        addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.compartilhar.botao"),
                botao -> PacketDistributor.sendToServer(new PacotePedirCompartilhar()))
                .bounds(esquerda + (LARGURA_DOS_TALENTOS - 90) / 2 + 6, topo + ALTURA - 26, 90, 20)
                .build());
    }

    private void aprimorar() {
        if (escolhido != null) {
            PacketDistributor.sendToServer(new PacoteAprimorarTalento(escolhido.id()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Onde fica cada coisa ----

    private int topoDoTier(int tier) {
        return topo + 24 + (tier - 1) * ALTURA_DO_TIER;
    }

    private int xDoIcone(int tier, int posicao) {
        int quantos = tiers.get(tier - 1).size();
        int inicio = esquerda + 6 + (LARGURA_DOS_TALENTOS - quantos * ESPACO_DO_ICONE) / 2;
        return inicio + posicao * ESPACO_DO_ICONE + (ESPACO_DO_ICONE - TAMANHO_DO_ICONE) / 2;
    }

    private int yDosIcones(int tier) {
        return topoDoTier(tier) + 14;
    }

    private static boolean liberado(int tier) {
        return EstadoNoCliente.nivel() >= Talento.nivelDoTier(tier);
    }

    // ---- Clique nos talentos ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        for (int tier = 1; tier <= tiers.size(); tier++) {
            List<Talento> talentos = tiers.get(tier - 1);
            for (int i = 0; i < talentos.size(); i++) {
                int x = xDoIcone(tier, i);
                int y = yDosIcones(tier);
                if (mouseX >= x && mouseX < x + TAMANHO_DO_ICONE && mouseY >= y && mouseY < y + TAMANHO_DO_ICONE) {
                    escolhido = talentos.get(i);
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    // ---- Desenho ----

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        botaoAprimorar.active = escolhido != null && EstadoNoCliente.pontosLivres(escolhido.tier()) > 0
                && EstadoNoCliente.pontos(escolhido) < Talento.PONTOS_MAXIMOS;
        botaoAprimorar.visible = classe != null;

        super.render(desenho, mouseX, mouseY, parcial);

        desenho.drawCenteredString(font, title, esquerda + LARGURA / 2, topo + 8, COR_DO_TITULO);
        if (classe == null) {
            desenho.drawCenteredString(font, Component.translatable("tela.pixeldungeon.sem_heroi"), esquerda + LARGURA / 2, topo + ALTURA / 2, COR_APAGADA);
            return;
        }
        for (int tier = 1; tier <= tiers.size(); tier++) {
            desenharTier(desenho, tier);
        }
        // Linha separando os talentos da descrição
        desenho.fill(esquerda + X_DA_DESCRICAO - 6, topo + 22, esquerda + X_DA_DESCRICAO - 5, topo + ALTURA - 32, COR_DA_BORDA);
        desenharDescricao(desenho);
    }

    private void desenharTier(GuiGraphics desenho, int tier) {
        int centro = esquerda + 6 + LARGURA_DOS_TALENTOS / 2;
        boolean liberado = liberado(tier);
        int livres = EstadoNoCliente.pontosLivres(tier);
        Component titulo = liberado ? Component.translatable("tela.pixeldungeon.tier_e_pontos", tier, livres)
                : Component.translatable("tela.pixeldungeon.tier_bloqueado", tier, Talento.nivelDoTier(tier));
        desenho.drawCenteredString(font, titulo, centro, topoDoTier(tier), liberado && livres > 0 ? COR_DO_TITULO : COR_APAGADA);

        List<Talento> talentos = tiers.get(tier - 1);
        for (int i = 0; i < talentos.size(); i++) {
            Talento talento = talentos.get(i);
            int x = xDoIcone(tier, i);
            int y = yDosIcones(tier);
            if (talento == escolhido) {
                desenho.renderOutline(x - 2, y - 2, TAMANHO_DO_ICONE + 4, TAMANHO_DO_ICONE + 4, COR_DO_TITULO);
            }
            desenho.blit(iconeDe(talento), x, y, TAMANHO_DO_ICONE, TAMANHO_DO_ICONE, 0, 0, 16, 16, 16, 16);
            int pontos = EstadoNoCliente.pontos(talento);
            if (pontos == 0) {
                // Talento sem pontos fica mais escuro, igual ao Shattered (e mais ainda num tier bloqueado)
                desenho.fill(x, y, x + TAMANHO_DO_ICONE, y + TAMANHO_DO_ICONE, liberado ? 0x88000000 : 0xBB000000);
            }
            for (int ponto = 0; ponto < Talento.PONTOS_MAXIMOS; ponto++) {
                int px = x + 7 + ponto * 11;
                int py = y + TAMANHO_DO_ICONE + 4;
                desenho.fill(px, py, px + 8, py + 4, ponto < pontos ? COR_DO_TITULO : 0xFF45484B);
            }
        }
    }

    // O talento escolhido: nome, o que faz e (se for o caso) o aviso de que ainda não faz nada
    private void desenharDescricao(GuiGraphics desenho) {
        if (escolhido == null) {
            return;
        }
        int x = esquerda + X_DA_DESCRICAO;
        int largura = LARGURA - X_DA_DESCRICAO - 8;
        int y = topo + 24;
        desenho.drawCenteredString(font, Component.translatable(escolhido.chaveDoNome()), x + largura / 2, y, COR_DO_TITULO);
        y += 13;
        y = escrever(desenho, TextoDoShattered.destacado(escolhido.chaveDaDescricao()), x, y, largura, COR_DO_TEXTO);
        if (escolhido.chaveDoPendente() != null) {
            y += 4;
            escrever(desenho, Component.translatable(escolhido.chaveDoPendente()).withStyle(ChatFormatting.ITALIC), x, y, largura, COR_APAGADA);
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + ALTURA + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + ALTURA, COR_DO_FUNDO);
    }

    private int escrever(GuiGraphics desenho, Component texto, int x, int y, int largura, int cor) {
        for (FormattedCharSequence linha : font.split(texto, largura)) {
            desenho.drawString(font, linha, x, y, cor);
            y += font.lineHeight + 1;
        }
        return y;
    }

    private static ResourceLocation iconeDe(Talento talento) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/talentos/" + talento.id() + ".png");
    }
}
