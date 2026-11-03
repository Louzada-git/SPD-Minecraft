package com.louzada.pixeldungeon.cliente;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// A barra de ferramentas do Shattered (a da interface completa), no canto de baixo à direita:
//   os três botões (procurar, esperar e mochila) e, à esquerda deles, as casinhas de atalho.
// No Shattered são até 6 casinhas; aqui são as 9 da barra de itens do Minecraft.
public class BarraDeFerramentas {
    public static final int ALTURA = 26;
    public static final int CASINHAS = 9;
    // Largura de cada casinha na imagem: a primeira tem a borda da esquerda, a última a da direita
    private static final int LARGURA_DA_PRIMEIRA = 20;
    private static final int LARGURA_DO_MEIO = 18;
    private static final int LARGURA_DA_ULTIMA = 19;
    // Os três botões juntos
    private static final int LARGURA_DOS_BOTOES = 24 + 20 + 20;
    public static final int LARGURA = LARGURA_DOS_BOTOES + LARGURA_DA_PRIMEIRA + LARGURA_DO_MEIO * (CASINHAS - 2) + LARGURA_DA_ULTIMA;

    public enum Botao {
        MOCHILA, ESPERAR, PROCURAR
    }

    // Onde começa a casinha (a imagem dela) contando da direita da barra
    public static int xDaCasinha(int direita, int casinha) {
        int x = direita - LARGURA_DOS_BOTOES;
        for (int i = CASINHAS - 1; i >= casinha; i--) {
            x -= largura(i);
        }
        return x;
    }

    // Onde fica o desenho do item dentro da casinha
    public static int xDoItem(int direita, int casinha) {
        return xDaCasinha(direita, casinha) + (casinha == 0 ? 3 : 1);
    }

    public static int yDoItem(int topo) {
        return topo + 6;
    }

    private static int largura(int casinha) {
        return casinha == 0 ? LARGURA_DA_PRIMEIRA : casinha == CASINHAS - 1 ? LARGURA_DA_ULTIMA : LARGURA_DO_MEIO;
    }

    // Os botões (x de cada um, da direita pra esquerda: mochila, esperar, procurar)
    public static int xDoBotao(int direita, Botao botao) {
        return switch (botao) {
            case MOCHILA -> direita - 24;
            case ESPERAR -> direita - 44;
            case PROCURAR -> direita - 64;
        };
    }

    public static int larguraDoBotao(Botao botao) {
        return botao == Botao.MOCHILA ? 24 : 20;
    }

    @Nullable
    public static Botao botaoEm(int direita, int topo, double mouseX, double mouseY) {
        for (Botao botao : Botao.values()) {
            if (DesenhoDoShattered.dentro(mouseX, mouseY, xDoBotao(direita, botao), topo, larguraDoBotao(botao), ALTURA)) {
                return botao;
            }
        }
        return null;
    }

    public static boolean dentro(int direita, int topo, double mouseX, double mouseY) {
        return DesenhoDoShattered.dentro(mouseX, mouseY, direita - LARGURA, topo, LARGURA, ALTURA);
    }

    // Desenha a barra. Na tela do inventário, os itens das casinhas são desenhados pela própria tela
    // (dá pra pegar e soltar), então aqui vão só as casinhas.
    public static void desenhar(GuiGraphics desenho, LocalPlayer jogador, int direita, int topo, boolean comItens, double mouseX, double mouseY) {
        Minecraft jogo = Minecraft.getInstance();
        Font fonte = jogo.font;
        Botao sobOMouse = botaoEm(direita, topo, mouseX, mouseY);

        // Botões: o fundo de pedra e o ícone no meio
        desenharBotao(desenho, xDoBotao(direita, Botao.MOCHILA), topo, 0, 24, 160, sobOMouse == Botao.MOCHILA);
        desenharBotao(desenho, xDoBotao(direita, Botao.ESPERAR), topo, 24, 20, 176, sobOMouse == Botao.ESPERAR);
        desenharBotao(desenho, xDoBotao(direita, Botao.PROCURAR), topo, 44, 20, 192, sobOMouse == Botao.PROCURAR);

        // Casinhas de atalho
        int selecionada = jogador.getInventory().selected;
        for (int casinha = 0; casinha < CASINHAS; casinha++) {
            int x = xDaCasinha(direita, casinha);
            int u = casinha == 0 ? 86 : casinha == CASINHAS - 1 ? 106 : 88;
            DesenhoDoShattered.FERRAMENTAS.desenhar(desenho, x, topo + 2, u, 0, largura(casinha), 24);
            int xDoItem = xDoItem(direita, casinha);
            if (casinha == selecionada) {
                // A casinha do item na mão fica mais clara, com uma borda amarela
                desenho.fill(xDoItem, topo + 4, xDoItem + 16, topo + 24, 0x50FFFFFF);
                desenho.renderOutline(xDoItem - 1, topo + 3, 18, 22, 0xC0FFFF44);
            }
            if (comItens) {
                ItemStack pilha = jogador.getInventory().getItem(casinha);
                desenho.renderItem(jogador, pilha, xDoItem, yDoItem(topo), casinha + 1);
                desenho.renderItemDecorations(fonte, pilha, xDoItem, yDoItem(topo));
            }
        }

        if (comItens) {
            // O item da mão esquerda, numa casinha solta do lado (só quando tem alguma coisa)
            ItemStack naOutraMao = jogador.getOffhandItem();
            if (!naOutraMao.isEmpty()) {
                int x = direita - LARGURA - 4 - 22;
                DesenhoDoShattered.FERRAMENTAS.desenhar(desenho, x, topo + 2, 64, 0, 22, 24);
                desenho.renderItem(jogador, naOutraMao, x + 3, yDoItem(topo), 0);
                desenho.renderItemDecorations(fonte, naOutraMao, x + 3, yDoItem(topo));
            }
        }

        // Habilidade da arma da Duelista: uma etiqueta em cima da barra, igual aos botões de ação do Shattered
        if (EstadoNoCliente.classe() == ClasseDoHeroi.DUELISTA) {
            int cargas = EstadoNoCliente.cargasDaHabilidade();
            StringBuilder pontos = new StringBuilder();
            for (int i = 0; i < EstadoNoCliente.cargasMaximasDaHabilidade(); i++) {
                pontos.append(i < cargas ? '◆' : '◇');
            }
            Component texto = Component.translatable("hud.pixeldungeon.habilidade", TeclasDoMod.HABILIDADE.getTranslatedKeyMessage(), pontos.toString());
            if (EstadoNoCliente.golpesSeguidos() > 0) {
                texto = texto.copy().append(Component.literal(" x" + EstadoNoCliente.golpesSeguidos()).withStyle(style -> style.withColor(0xFF8800)));
            }
            int largura = fonte.width(texto) + 8;
            int y = topo - 16;
            DesenhoDoShattered.etiqueta(desenho, direita - largura, y, largura, 16);
            desenho.drawString(fonte, texto, direita - largura + 4, y + 4, cargas > 0 ? DesenhoDoShattered.AMARELO : 0xFFAAAAAA, true);
        }
    }

    private static void desenharBotao(GuiGraphics desenho, int x, int topo, int u, int largura, int uDoIcone, boolean claro) {
        DesenhoDoShattered.FERRAMENTAS.desenhar(desenho, x, topo, u, 0, largura, ALTURA);
        DesenhoDoShattered.FERRAMENTAS.desenhar(desenho, x + (largura - 16) / 2, topo + 5, uDoIcone, 0, 16, 16);
        if (claro) {
            desenho.fill(x + 1, topo + 1, x + largura - 1, topo + ALTURA - 1, 0x30FFFFFF);
        }
    }
}
