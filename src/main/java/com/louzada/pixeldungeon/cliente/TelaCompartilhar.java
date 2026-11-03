package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.rede.PacoteDefinirCompartilhar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

// Tela de compartilhar identificação: com quem vão as poções e pergaminhos que o jogador descobrir.
//   todos: todo mundo na run descobre junto
//   ninguém: cada um descobre sozinho
//   escolhidos: só os jogadores marcados na lista
public class TelaCompartilhar extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_APAGADA = 0xFF9A9A9A;
    private static final int LARGURA = 220;
    private static final int ALTURA = 200;

    private String modo;
    private List<String> jogadores;
    private Set<String> escolhidos;
    private int esquerda;
    private int topo;

    public TelaCompartilhar(String modo, List<String> jogadores, List<String> escolhidos) {
        super(Component.translatable("tela.pixeldungeon.compartilhar"));
        this.modo = modo;
        this.jogadores = jogadores;
        this.escolhidos = new HashSet<>(escolhidos);
    }

    @Override
    protected void init() {
        esquerda = (width - LARGURA) / 2;
        topo = (height - ALTURA) / 2;
        clearWidgets();
        int y = topo + 26;
        for (String opcao : List.of(DadosDoHeroi.COMPARTILHAR_COM_TODOS, DadosDoHeroi.COMPARTILHAR_COM_NINGUEM, DadosDoHeroi.COMPARTILHAR_COM_ESCOLHIDOS)) {
            Component texto = Component.translatable("tela.pixeldungeon.compartilhar." + opcao);
            Button botao = addRenderableWidget(Button.builder(opcao.equals(modo) ? Component.literal("> ").append(texto).append(" <") : texto,
                    clique -> {
                        modo = opcao;
                        init();
                    }).bounds(esquerda + 20, y, LARGURA - 40, 18).build());
            botao.active = !opcao.equals(modo);
            y += 22;
        }
        // Os jogadores (só dá pra marcar no modo "escolhidos")
        y += 6;
        for (String nome : jogadores) {
            boolean marcado = escolhidos.contains(nome);
            Button botao = addRenderableWidget(Button.builder(Component.literal((marcado ? "[x] " : "[ ] ") + nome), clique -> {
                if (!escolhidos.remove(nome)) {
                    escolhidos.add(nome);
                }
                init();
            }).bounds(esquerda + 30, y, LARGURA - 60, 16).build());
            botao.active = DadosDoHeroi.COMPARTILHAR_COM_ESCOLHIDOS.equals(modo);
            y += 18;
            if (y > topo + ALTURA - 40) {
                break;
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.salvar"), clique -> salvar())
                .bounds(esquerda + LARGURA / 2 - 40, topo + ALTURA - 26, 80, 20).build());
    }

    private void salvar() {
        PacketDistributor.sendToServer(new PacoteDefinirCompartilhar(modo, new ArrayList<>(escolhidos)));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        desenho.drawCenteredString(font, title, esquerda + LARGURA / 2, topo + 10, COR_DO_TITULO);
        if (jogadores.isEmpty()) {
            desenho.drawCenteredString(font, Component.translatable("tela.pixeldungeon.compartilhar.sozinho"), esquerda + LARGURA / 2, topo + 100,
                    COR_APAGADA);
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + ALTURA + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + ALTURA, COR_DO_FUNDO);
    }
}
