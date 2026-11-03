package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// Desenha na planta (o "Painter" do Shattered): preencher retângulos, linhas, elipses...
public final class Pincel {
    private Pincel() {
    }

    public static void definir(PlantaDoAndar planta, int x, int y, byte tipo) {
        planta.definir(x, y, tipo);
    }

    public static void definir(PlantaDoAndar planta, Ponto p, byte tipo) {
        planta.definir(p.x, p.y, tipo);
    }

    public static void preencher(PlantaDoAndar planta, int x, int y, int largura, int altura, byte tipo) {
        for (int qy = y; qy < y + altura; qy++) {
            for (int qx = x; qx < x + largura; qx++) {
                planta.definir(qx, qy, tipo);
            }
        }
    }

    // O retângulo inteiro (num cômodo, inclui as paredes)
    public static void preencher(PlantaDoAndar planta, Retangulo ret, byte tipo) {
        preencher(planta, ret.esquerda, ret.cima, ret.largura(), ret.altura(), tipo);
    }

    // O retângulo, deixando "margem" quadradinhos de fora em cada lado
    public static void preencher(PlantaDoAndar planta, Retangulo ret, int margem, byte tipo) {
        preencher(planta, ret.esquerda + margem, ret.cima + margem, ret.largura() - margem * 2, ret.altura() - margem * 2, tipo);
    }

    // Uma linha reta de um ponto até outro
    public static void linha(PlantaDoAndar planta, Ponto de, Ponto ate, byte tipo) {
        float x = de.x;
        float y = de.y;
        float dx = ate.x - de.x;
        float dy = ate.y - de.y;
        boolean andaPeloX = Math.abs(dx) >= Math.abs(dy);
        if (andaPeloX) {
            dy /= Math.abs(dx);
            dx /= Math.abs(dx);
        } else {
            dx /= Math.abs(dy);
            dy /= Math.abs(dy);
        }
        planta.definir(Math.round(x), Math.round(y), tipo);
        while ((andaPeloX && ate.x != x) || (!andaPeloX && ate.y != y)) {
            x += dx;
            y += dy;
            planta.definir(Math.round(x), Math.round(y), tipo);
        }
    }

    public static void elipse(PlantaDoAndar planta, Retangulo ret, int margem, byte tipo) {
        elipse(planta, ret.esquerda + margem, ret.cima + margem, ret.largura() - margem * 2, ret.altura() - margem * 2, tipo);
    }

    // Uma elipse que cabe no retângulo (cada linha testa o meio do quadradinho)
    public static void elipse(PlantaDoAndar planta, int x, int y, int largura, int altura, byte tipo) {
        double raioV = altura / 2f;
        double raioH = largura / 2f;
        for (int i = 0; i < altura; i++) {
            double linhaY = -raioV + 0.5 + i;
            double larguraDaLinha = 2.0 * Math.sqrt((raioH * raioH) * (1.0 - (linhaY * linhaY) / (raioV * raioV)));
            if (largura % 2 == 0) {
                larguraDaLinha = Math.round(larguraDaLinha / 2.0) * 2.0;
            } else {
                larguraDaLinha = Math.floor(larguraDaLinha / 2.0) * 2.0;
                larguraDaLinha++;
            }
            int inicio = x + (largura - (int) larguraDaLinha) / 2;
            preencher(planta, inicio, y + i, (int) larguraDaLinha, 1, tipo);
        }
    }

    // A partir de uma porta do cômodo, anda "n" quadradinhos pra dentro desenhando
    public static Ponto desenharDentro(PlantaDoAndar planta, Comodo comodo, Ponto de, int n, byte tipo) {
        Ponto passo = new Ponto();
        if (de.x == comodo.esquerda) {
            passo.definir(+1, 0);
        } else if (de.x == comodo.direita) {
            passo.definir(-1, 0);
        } else if (de.y == comodo.cima) {
            passo.definir(0, +1);
        } else if (de.y == comodo.baixo) {
            passo.definir(0, -1);
        }
        Ponto p = new Ponto(de).deslocar(passo.x, passo.y);
        for (int i = 0; i < n; i++) {
            planta.definir(p.x, p.y, tipo);
            p.deslocar(passo.x, passo.y);
        }
        return p;
    }
}
