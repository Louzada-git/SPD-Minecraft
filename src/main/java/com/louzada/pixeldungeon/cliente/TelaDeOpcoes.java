package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.rede.PacoteOpcaoEscolhida;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// A janelinha de opções (o "WndOptions" do Shattered): o título (com o item), o texto e um botão por opção.
// Com muitas opções (o palpite da pedra de intuição), os botões ficam em duas colunas. Fechar é desistir.
public class TelaDeOpcoes extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int LARGURA = 240;
    private static final int ALTURA_DO_BOTAO = 18;

    private Component texto;
    private ItemStack icone;
    private List<Component> opcoes;
    private List<FormattedCharSequence> linhas;
    private boolean escolheu = false;
    private int esquerda;
    private int topo;
    private int altura;

    public TelaDeOpcoes(Component titulo, Component texto, ItemStack icone, List<Component> opcoes) {
        super(titulo);
        this.texto = texto;
        this.icone = icone;
        this.opcoes = opcoes;
    }

    private boolean duasColunas() {
        return opcoes.size() > 5;
    }

    @Override
    protected void init() {
        linhas = font.split(texto, LARGURA - 16);
        int fileiras = duasColunas() ? (opcoes.size() + 1) / 2 : opcoes.size();
        altura = 32 + linhas.size() * 10 + fileiras * (ALTURA_DO_BOTAO + 2) + 6;
        esquerda = (width - LARGURA) / 2;
        topo = (height - altura) / 2;
        int y = topo + 30 + linhas.size() * 10;
        int larguraDoBotao = duasColunas() ? (LARGURA - 20) / 2 : LARGURA - 16;
        for (int i = 0; i < opcoes.size(); i++) {
            int indice = i;
            int coluna = duasColunas() ? i % 2 : 0;
            int fileira = duasColunas() ? i / 2 : i;
            addRenderableWidget(Button.builder(opcoes.get(i), botao -> escolher(indice))
                    .bounds(esquerda + 8 + coluna * (larguraDoBotao + 4), y + fileira * (ALTURA_DO_BOTAO + 2), larguraDoBotao, ALTURA_DO_BOTAO).build());
        }
    }

    private void escolher(int indice) {
        escolheu = true;
        PacketDistributor.sendToServer(new PacoteOpcaoEscolhida(indice));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Fechou sem escolher: desistiu
    @Override
    public void removed() {
        if (!escolheu) {
            PacketDistributor.sendToServer(new PacoteOpcaoEscolhida(-1));
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        int larguraDoTitulo = font.width(title) + (icone.isEmpty() ? 0 : 20);
        int xDoTitulo = esquerda + (LARGURA - larguraDoTitulo) / 2;
        if (!icone.isEmpty()) {
            desenho.renderItem(icone, xDoTitulo, topo + 6);
            xDoTitulo += 20;
        }
        desenho.drawString(font, title, xDoTitulo, topo + 10, COR_DO_TITULO);
        for (int i = 0; i < linhas.size(); i++) {
            desenho.drawString(font, linhas.get(i), esquerda + 8, topo + 28 + i * 10, COR_DO_TEXTO);
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + altura + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + altura, COR_DO_FUNDO);
    }
}
