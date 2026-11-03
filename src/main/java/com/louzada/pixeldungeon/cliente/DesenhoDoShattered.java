package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// Desenha pedaços das imagens da interface do Shattered (a barra de status, a barra de ferramentas, as molduras...).
// Cada imagem é uma folha com vários desenhos; os números são os mesmos do código do Shattered.
public class DesenhoDoShattered {
    // Barra de status: o quadro do herói, a vida e a experiência
    public static final Folha STATUS = new Folha("status_pane", 128, 128);
    // Barra de ferramentas: os botões (mochila, esperar, procurar) e as casinhas de atalho
    public static final Folha FERRAMENTAS = new Folha("toolbar", 256, 32);
    // Molduras das janelas e dos painéis
    public static final Folha MOLDURAS = new Folha("chrome", 128, 64);
    // Ícones soltos
    public static final Folha FOME = new Folha("fome", 16, 16);
    public static final Folha FAMINTO = new Folha("faminto", 16, 16);
    public static final Folha TALENTO = new Folha("talento", 13, 13);
    public static final Folha MOEDA = new Folha("moeda", 7, 7);
    // Os desenhos das casinhas vazias de equipamento
    public static final Folha ESPACO_ARMADURA = new Folha("espaco_armadura", 16, 16);
    public static final Folha ESPACO_ARTEFATO = new Folha("espaco_artefato", 16, 16);

    // Cor dos títulos do Shattered (amarelo) e dos textos claros
    public static final int AMARELO = 0xFFFFFF44;
    public static final int AMARELO_CLARO = 0xFFFFFFAA;
    public static final int BRANCO = 0xFFFFFFFF;

    // Fundo das casinhas do inventário (cinza escuro meio transparente) e das casinhas equipadas (cinza claro)
    public static final int FUNDO_DA_CASINHA = 0x9953564D;
    public static final int FUNDO_EQUIPADO = 0x9991938C;

    // Uma imagem da interface, com o tamanho dela (pra achar os pedaços)
    public record Folha(ResourceLocation textura, int largura, int altura) {
        Folha(String nome, int largura, int altura) {
            this(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/shattered/" + nome + ".png"), largura, altura);
        }

        // A folha inteira (pros ícones soltos)
        public void desenhar(GuiGraphics desenho, int x, int y) {
            desenhar(desenho, x, y, 0, 0, largura, altura);
        }

        // Um pedaço da folha, do mesmo tamanho
        public void desenhar(GuiGraphics desenho, int x, int y, int u, int v, int l, int a) {
            esticado(desenho, x, y, l, a, u, v, l, a);
        }

        // Um pedaço da folha, esticado (ou encolhido) pra caber em larguraNaTela x alturaNaTela
        public void esticado(GuiGraphics desenho, int x, int y, int larguraNaTela, int alturaNaTela, int u, int v, int l, int a) {
            if (larguraNaTela <= 0 || alturaNaTela <= 0 || l <= 0 || a <= 0) {
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            desenho.blit(textura, x, y, larguraNaTela, alturaNaTela, u, v, l, a, largura, altura);
            RenderSystem.disableBlend();
        }

        // Moldura de "nove pedaços" (igual ao NinePatch do Shattered): os cantos ficam do mesmo tamanho,
        // as bordas e o meio esticam pra moldura ficar do tamanho pedido
        public void moldura(GuiGraphics desenho, int x, int y, int larguraNaTela, int alturaNaTela, int u, int v, int l, int a, int esquerda,
                int cima, int direita, int baixo) {
            int[] xs = {x, x + esquerda, x + larguraNaTela - direita};
            int[] larguras = {esquerda, larguraNaTela - esquerda - direita, direita};
            int[] us = {u, u + esquerda, u + l - direita};
            int[] largurasNaFolha = {esquerda, l - esquerda - direita, direita};
            int[] ys = {y, y + cima, y + alturaNaTela - baixo};
            int[] alturas = {cima, alturaNaTela - cima - baixo, baixo};
            int[] vs = {v, v + cima, v + a - baixo};
            int[] alturasNaFolha = {cima, a - cima - baixo, baixo};
            for (int linha = 0; linha < 3; linha++) {
                for (int coluna = 0; coluna < 3; coluna++) {
                    esticado(desenho, xs[coluna], ys[linha], larguras[coluna], alturas[linha], us[coluna], vs[linha], largurasNaFolha[coluna],
                            alturasNaFolha[linha]);
                }
            }
        }
    }

    // As molduras do Shattered (Chrome.java)
    // Janela: a moldura das janelas
    public static void janela(GuiGraphics desenho, int x, int y, int largura, int altura) {
        MOLDURAS.moldura(desenho, x, y, largura, altura, 0, 0, 20, 20, 6, 6, 6, 6);
    }

    // Painel escuro meio transparente (o fundo do inventário do Shattered)
    public static void painel(GuiGraphics desenho, int x, int y, int largura, int altura) {
        MOLDURAS.moldura(desenho, x, y, largura, altura, 29, 9, 9, 9, 4, 4, 4, 4);
    }

    // Etiqueta (as plaquinhas do lado direito da tela do Shattered)
    public static void etiqueta(GuiGraphics desenho, int x, int y, int largura, int altura) {
        MOLDURAS.moldura(desenho, x, y, largura, altura, 22, 18, 16, 14, 3, 3, 3, 3);
    }

    // Está dentro do retângulo?
    public static boolean dentro(double x, double y, int esquerda, int topo, int largura, int altura) {
        return x >= esquerda && x < esquerda + largura && y >= topo && y < topo + altura;
    }
}
