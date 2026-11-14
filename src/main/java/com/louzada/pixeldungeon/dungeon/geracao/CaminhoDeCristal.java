package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O caminho de cristal (o "CrystalPathRoom" do Shattered): um corredor com 6 salinhas atrás de portas de cristal, 3 de cada
// lado. O andar ganha 3 chaves de cristal, então dá pra abrir 3 das 6. Cada salinha tem uma poção ou um pergaminho: as do
// meio, os mais comuns; as da porta, os do meio; as do fundo (que só se abrem depois de abrir a da frente), os mais raros.
// Uma das poções é a de experiência, ou um dos pergaminhos é o de transmutação. A porta é comum (e fica no meio da parede).
public class CaminhoDeCristal extends ComodoEspecial {

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
        // As salinhas, da mais perto para a mais longe da porta
        SubSala[] salas = new SubSala[6];
        Ponto porta = new Ponto(entrada());
        Ponto premio1;
        Ponto premio2;
        byte cristal = PlantaDoAndar.PORTA_DE_CRISTAL;
        if (porta.x == esquerda || porta.x == direita) {
            Pincel.desenharDentro(planta, this, porta, largura() > 8 ? 5 : 3, PlantaDoAndar.CHAO);
            int l1 = largura() >= 9 ? 2 : 1;
            int l2 = largura() % 2 == 0 ? 2 : 1;
            int a = altura() >= 9 ? 2 : 1;
            if (porta.x == esquerda) {
                salas[0] = SubSala.em(esquerda + 1, porta.y - a - 1, l1 - 1, a - 1);
                Pincel.definir(planta, salas[0].esquerda, salas[0].baixo + 1, cristal);
                salas[1] = SubSala.em(esquerda + 1, porta.y + 2, l1 - 1, a - 1);
                Pincel.definir(planta, salas[1].esquerda, salas[1].cima - 1, cristal);
                salas[2] = SubSala.em(salas[1].direita + 2, porta.y - a - 1, l1 - 1, a - 1);
                Pincel.definir(planta, salas[2].esquerda, salas[2].baixo + 1, cristal);
                salas[3] = SubSala.em(salas[1].direita + 2, porta.y + 2, l1 - 1, a - 1);
                Pincel.definir(planta, salas[3].esquerda, salas[3].cima - 1, cristal);
                salas[4] = SubSala.em(salas[3].direita + 2, porta.y - a - 1, l2 - 1, a);
                Pincel.definir(planta, salas[4].esquerda - 1, salas[4].baixo - 1, cristal);
                salas[5] = SubSala.em(salas[3].direita + 2, porta.y + 1, l2 - 1, a);
                Pincel.definir(planta, salas[5].esquerda - 1, salas[5].cima + 1, cristal);
                premio1 = new Ponto(salas[4].esquerda, salas[4].baixo);
                premio2 = new Ponto(salas[5].esquerda, salas[5].cima);
            } else {
                salas[0] = SubSala.em(direita - l1, porta.y - a - 1, l1 - 1, a - 1);
                Pincel.definir(planta, salas[0].direita, salas[0].baixo + 1, cristal);
                salas[1] = SubSala.em(direita - l1, porta.y + 2, l1 - 1, a - 1);
                Pincel.definir(planta, salas[1].direita, salas[1].cima - 1, cristal);
                salas[2] = SubSala.em(salas[1].esquerda - l1 - 1, porta.y - a - 1, l1 - 1, a - 1);
                Pincel.definir(planta, salas[2].direita, salas[2].baixo + 1, cristal);
                salas[3] = SubSala.em(salas[1].esquerda - l1 - 1, porta.y + 2, l1 - 1, a - 1);
                Pincel.definir(planta, salas[3].direita, salas[3].cima - 1, cristal);
                salas[4] = SubSala.em(salas[3].esquerda - l2 - 1, porta.y - a - 1, l2 - 1, a);
                Pincel.definir(planta, salas[4].direita + 1, salas[4].baixo - 1, cristal);
                salas[5] = SubSala.em(salas[3].esquerda - l2 - 1, porta.y + 1, l2 - 1, a);
                Pincel.definir(planta, salas[5].direita + 1, salas[5].cima + 1, cristal);
                premio1 = new Ponto(salas[4].direita, salas[4].baixo);
                premio2 = new Ponto(salas[5].direita, salas[5].cima);
            }
        } else {
            Pincel.desenharDentro(planta, this, porta, altura() > 8 ? 5 : 3, PlantaDoAndar.CHAO);
            int l = largura() >= 9 ? 2 : 1;
            int a1 = altura() >= 9 ? 2 : 1;
            int a2 = altura() % 2 == 0 ? 2 : 1;
            if (porta.y == cima) {
                salas[0] = SubSala.em(porta.x - l - 1, cima + 1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[0].direita + 1, salas[0].cima, cristal);
                salas[1] = SubSala.em(porta.x + 2, cima + 1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[1].esquerda - 1, salas[1].cima, cristal);
                salas[2] = SubSala.em(porta.x - l - 1, salas[1].baixo + 2, l - 1, a1 - 1);
                Pincel.definir(planta, salas[2].direita + 1, salas[2].cima, cristal);
                salas[3] = SubSala.em(porta.x + 2, salas[1].baixo + 2, l - 1, a1 - 1);
                Pincel.definir(planta, salas[3].esquerda - 1, salas[3].cima, cristal);
                salas[4] = SubSala.em(porta.x - l - 1, salas[3].baixo + 2, l, a2 - 1);
                Pincel.definir(planta, salas[4].direita - 1, salas[4].cima - 1, cristal);
                salas[5] = SubSala.em(porta.x + 1, salas[3].baixo + 2, l, a2 - 1);
                Pincel.definir(planta, salas[5].esquerda + 1, salas[5].cima - 1, cristal);
                premio1 = new Ponto(salas[4].direita, salas[4].cima);
                premio2 = new Ponto(salas[5].esquerda, salas[5].cima);
            } else {
                salas[0] = SubSala.em(porta.x - l - 1, baixo - a1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[0].direita + 1, salas[0].baixo, cristal);
                salas[1] = SubSala.em(porta.x + 2, baixo - a1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[1].esquerda - 1, salas[1].baixo, cristal);
                salas[2] = SubSala.em(porta.x - l - 1, salas[1].cima - a1 - 1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[2].direita + 1, salas[2].baixo, cristal);
                salas[3] = SubSala.em(porta.x + 2, salas[1].cima - a1 - 1, l - 1, a1 - 1);
                Pincel.definir(planta, salas[3].esquerda - 1, salas[3].baixo, cristal);
                salas[4] = SubSala.em(porta.x - l - 1, salas[3].cima - a2 - 1, l, a2 - 1);
                Pincel.definir(planta, salas[4].direita - 1, salas[4].baixo + 1, cristal);
                salas[5] = SubSala.em(porta.x + 1, salas[3].cima - a2 - 1, l, a2 - 1);
                Pincel.definir(planta, salas[5].esquerda + 1, salas[5].baixo + 1, cristal);
                premio1 = new Ponto(salas[4].direita, salas[4].baixo);
                premio2 = new Ponto(salas[5].esquerda, salas[5].baixo);
            }
        }
        for (SubSala sala : salas) {
            sala.preencher(planta, PlantaDoAndar.CHAO_ESPECIAL);
        }
        Pincel.definir(planta, premio1, PlantaDoAndar.PEDESTAL);
        Pincel.definir(planta, premio2, PlantaDoAndar.PEDESTAL);

        // As mais comuns nas salinhas 2 e 3, depois 0 e 1, e as mais raras no fundo (ver SaqueDoAndar.caminhoDeCristal)
        boolean troca = Dado.ate(2) == 1;
        Ponto[] lugares = { salas[troca ? 2 : 3].meio(), salas[troca ? 3 : 2].meio(), salas[troca ? 0 : 1].meio(), salas[troca ? 1 : 0].meio(),
                troca ? premio1 : premio2, troca ? premio2 : premio1 };
        for (int i = 0; i < lugares.length; i++) {
            planta.adicionarPremio(lugares[i].x, lugares[i].y, Premio.values()[Premio.CAMINHO_1.ordinal() + i], false);
        }
        for (int i = 0; i < 3; i++) {
            planta.pedir(PlantaDoAndar.ItemPedido.CHAVE_DE_CRISTAL);
        }
        entrada().definir(Porta.Tipo.COMUM);
    }

    // A porta só pode ficar no meio da parede
    @Override
    public boolean podeLigar(Ponto p) {
        if (!super.podeLigar(p)) {
            return false;
        }
        return Math.abs(p.x - (direita - (largura() - 1) / 2f)) < 1f || Math.abs(p.y - (baixo - (altura() - 1) / 2f)) < 1f;
    }

    @Override
    public boolean podeTerGrama(Ponto p) {
        return false;
    }

    @Override
    public boolean podeTerAgua(Ponto p) {
        return false;
    }

    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
