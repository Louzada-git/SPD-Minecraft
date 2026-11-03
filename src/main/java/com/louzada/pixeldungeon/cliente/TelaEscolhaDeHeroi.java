package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.rede.PacoteEscolherClasse;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

// A tela "Escolha Seu Herói", no estilo do Shattered:
//   à esquerda: título, os 6 heróis, nome, descrição e o botão Começar
//   à direita: a arte do herói escolhido
public class TelaEscolhaDeHeroi extends Screen {
    // Cores do Shattered
    private static final int COR_DO_FUNDO = 0xFF2D2F31;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    // Tamanho da arte grande (os arquivos têm 800x450)
    private static final int ARTE_LARGURA = 800;
    private static final int ARTE_ALTURA = 450;
    // Botões dos heróis: o boneco tem 12x15 pixels e é desenhado 2x maior
    private static final int BONECO_LARGURA = 12;
    private static final int BONECO_ALTURA = 15;
    private static final int ESCALA_DO_BONECO = 2;
    private static final int ESPACO_DO_BOTAO = 32;
    private static final int ALTURA_DO_BOTAO = 40;

    private boolean runEmAndamento;
    private ClasseDoHeroi escolhida = ClasseDoHeroi.GUERREIRO;
    private boolean jaEscolheu = false;

    // Posições calculadas no init()
    private int larguraDoPainel;
    private int centroDoPainel;
    private int topoDosBotoes;
    private int inicioDosBotoes;

    public TelaEscolhaDeHeroi(boolean runEmAndamento) {
        super(Component.translatable("tela.pixeldungeon.escolha_heroi"));
        this.runEmAndamento = runEmAndamento;
    }

    @Override
    protected void init() {
        larguraDoPainel = Math.max(210, Math.min(260, width * 2 / 5));
        centroDoPainel = larguraDoPainel / 2;
        topoDosBotoes = 34;
        inicioDosBotoes = centroDoPainel - (ClasseDoHeroi.values().length * ESPACO_DO_BOTAO) / 2;

        addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.comecar"), botao -> comecar())
                .bounds(centroDoPainel - 50, height - 32, 100, 20)
                .build());
    }

    private void comecar() {
        jaEscolheu = true;
        PacketDistributor.sendToServer(new PacoteEscolherClasse(escolhida.id()));
        onClose();
    }

    @Override
    public void onClose() {
        // Se fechou sem escolher, avisa como abrir de novo
        if (!jaEscolheu && minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("mensagem.pixeldungeon.reabrir_menu"), false);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Clique nos heróis ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        ClasseDoHeroi[] classes = ClasseDoHeroi.values();
        for (int i = 0; i < classes.length; i++) {
            int x = inicioDosBotoes + i * ESPACO_DO_BOTAO;
            if (mouseX >= x && mouseX < x + ESPACO_DO_BOTAO && mouseY >= topoDosBotoes && mouseY < topoDosBotoes + ALTURA_DO_BOTAO) {
                escolhida = classes[i];
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    // ---- Desenho ----

    // Fundo: cor do Shattered + a arte do herói escolhido ocupando a direita
    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        desenho.fill(0, 0, width, height, COR_DO_FUNDO);

        int larguraLivre = width - larguraDoPainel;
        if (larguraLivre > 0) {
            // A arte ocupa a altura toda; se for larga demais, corta o lado direito
            int alturaDaArte = height;
            int larguraDaArte = alturaDaArte * ARTE_LARGURA / ARTE_ALTURA;
            int larguraDesenhada = Math.min(larguraDaArte, larguraLivre);
            int pedacoDaTextura = larguraDesenhada * ARTE_LARGURA / larguraDaArte;
            desenho.blit(arteDe(escolhida), larguraDoPainel, 0, larguraDesenhada, alturaDaArte,
                    0, 0, pedacoDaTextura, ARTE_ALTURA, ARTE_LARGURA, ARTE_ALTURA);
        }
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);

        // Título
        desenho.drawCenteredString(font, title, centroDoPainel, 14, COR_DO_TITULO);

        // Os 6 heróis
        ClasseDoHeroi[] classes = ClasseDoHeroi.values();
        for (int i = 0; i < classes.length; i++) {
            int x = inicioDosBotoes + i * ESPACO_DO_BOTAO;
            int xDoBoneco = x + (ESPACO_DO_BOTAO - BONECO_LARGURA * ESCALA_DO_BONECO) / 2;
            int yDoBoneco = topoDosBotoes + (ALTURA_DO_BOTAO - BONECO_ALTURA * ESCALA_DO_BONECO) / 2;
            desenho.blit(iconeDe(classes[i]), xDoBoneco, yDoBoneco,
                    BONECO_LARGURA * ESCALA_DO_BONECO, BONECO_ALTURA * ESCALA_DO_BONECO,
                    0, 0, BONECO_LARGURA, BONECO_ALTURA, BONECO_LARGURA, BONECO_ALTURA);

            boolean emCima = mouseX >= x && mouseX < x + ESPACO_DO_BOTAO && mouseY >= topoDosBotoes && mouseY < topoDosBotoes + ALTURA_DO_BOTAO;
            if (classes[i] == escolhida) {
                // Moldura amarela em volta do escolhido
                desenho.renderOutline(x + 1, topoDosBotoes, ESPACO_DO_BOTAO - 2, ALTURA_DO_BOTAO, COR_DO_TITULO);
            } else {
                // Os outros ficam mais escuros (menos se o mouse estiver em cima)
                desenho.fill(x + 1, topoDosBotoes, x + ESPACO_DO_BOTAO - 1, topoDosBotoes + ALTURA_DO_BOTAO, emCima ? 0x55000000 : 0x99000000);
            }
        }

        // Nome do herói
        int y = topoDosBotoes + ALTURA_DO_BOTAO + 10;
        desenho.drawCenteredString(font, Component.translatable(escolhida.chaveDoNome()), centroDoPainel, y, COR_DO_TITULO);
        y += 16;

        // Descrição (palavras entre _ ficam amarelas, igual ao Shattered)
        int larguraDoTexto = larguraDoPainel - 24;
        y = escreverCentralizado(desenho, destacar(Language.getInstance().getOrDefault(escolhida.chaveDaDescricao())), y, larguraDoTexto, COR_DO_TEXTO);
        y += 8;

        // O que a classe recebe agora
        Component kit = Component.translatable(escolhida.chaveDoKit()).withStyle(ChatFormatting.GRAY);
        y = escreverCentralizado(desenho, kit, y, larguraDoTexto, COR_DO_TEXTO);

        if (runEmAndamento) {
            y += 8;
            escreverCentralizado(desenho, Component.translatable("tela.pixeldungeon.run_em_andamento").withStyle(ChatFormatting.GRAY), y, larguraDoTexto, COR_DO_TEXTO);
        }
    }

    // Escreve o texto quebrando linhas e centralizando no painel; devolve a altura onde parou
    private int escreverCentralizado(GuiGraphics desenho, Component texto, int y, int largura, int cor) {
        List<FormattedCharSequence> linhas = font.split(texto, largura);
        for (FormattedCharSequence linha : linhas) {
            desenho.drawString(font, linha, centroDoPainel - font.width(linha) / 2, y, cor);
            y += font.lineHeight + 1;
        }
        return y;
    }

    // "O Guerreiro tem um _selo quebrado_" -> "selo quebrado" em amarelo
    private static Component destacar(String texto) {
        MutableComponent resultado = Component.empty();
        String[] partes = texto.split("_", -1);
        for (int i = 0; i < partes.length; i++) {
            MutableComponent parte = Component.literal(partes[i]);
            if (i % 2 == 1) {
                parte = parte.withStyle(ChatFormatting.YELLOW);
            }
            resultado.append(parte);
        }
        return resultado;
    }

    private static ResourceLocation arteDe(ClasseDoHeroi classe) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/herois/" + classe.id() + ".png");
    }

    private static ResourceLocation iconeDe(ClasseDoHeroi classe) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/herois/" + classe.id() + "_icone.png");
    }
}
