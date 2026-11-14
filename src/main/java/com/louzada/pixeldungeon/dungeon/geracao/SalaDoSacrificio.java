package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala do sacrifício (o "SacrificeRoom" do Shattered): um abismo com um caminho até um altar com o fogo de sacrifício,
// cercado de brasas e de 4 estátuas. Quem morre perto do fogo (marcado por ele) é sacrificado; depois de sacrifícios
// suficientes (6 + 4 x andar de experiência, com os inimigos valendo 2 ou 3 vezes a deles), o fogo apaga e deixa uma arma
// da região seguinte, amaldiçoada mas +1. A porta é só uma passagem.
public class SalaDoSacrificio extends ComodoEspecial {

    @Override
    public int minLargura() {
        return 7;
    }

    @Override
    public int minAltura() {
        return 7;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ABISMO);
        Ponto c = centro();
        Porta porta = entrada();
        if (porta.x == esquerda || porta.x == direita) {
            if (porta.y == c.y) {
                c.y += Dado.ate(2) == 0 ? -1 : +1;
            }
            Ponto p = Pincel.desenharDentro(planta, this, porta, Math.abs(porta.x - c.x) - 2, PlantaDoAndar.CHAO_ESPECIAL);
            for (; p.y != c.y; p.y += p.y < c.y ? +1 : -1) {
                Pincel.definir(planta, p, PlantaDoAndar.CHAO_ESPECIAL);
            }
        } else {
            if (porta.x == c.x) {
                c.x += Dado.ate(2) == 0 ? -1 : +1;
            }
            Ponto p = Pincel.desenharDentro(planta, this, porta, Math.abs(porta.y - c.y) - 2, PlantaDoAndar.CHAO_ESPECIAL);
            for (; p.x != c.x; p.x += p.x < c.x ? +1 : -1) {
                Pincel.definir(planta, p, PlantaDoAndar.CHAO_ESPECIAL);
            }
        }
        // 4 estátuas pra se proteger de quem ataca de longe
        if (c.x - 2 > esquerda) {
            Pincel.definir(planta, c.x - 2, c.y, PlantaDoAndar.ESTATUA);
        }
        if (c.y - 2 > cima) {
            Pincel.definir(planta, c.x, c.y - 2, PlantaDoAndar.ESTATUA);
        }
        if (c.x + 2 < direita) {
            Pincel.definir(planta, c.x + 2, c.y, PlantaDoAndar.ESTATUA);
        }
        if (c.y + 2 < baixo) {
            Pincel.definir(planta, c.x, c.y + 2, PlantaDoAndar.ESTATUA);
        }
        Pincel.preencher(planta, c.x - 1, c.y - 1, 3, 3, PlantaDoAndar.BRASAS);
        Pincel.definir(planta, c, PlantaDoAndar.PEDESTAL);
        planta.definirFogoDeSacrificio(c.x, c.y);
        porta.definir(Porta.Tipo.VAZIA);
    }

    @Override
    public boolean podeTerGrama(Ponto p) {
        return false;
    }
}
