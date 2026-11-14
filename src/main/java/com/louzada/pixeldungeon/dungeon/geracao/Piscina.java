package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A piscina (o "PoolRoom" do Shattered): a sala é toda água, com 3 piranhas, e do lado oposto à porta um baú num pedestal.
// Uma faixa de chão especial do lado da porta. O andar ganha uma poção de invisibilidade (as piranhas não veem quem está invisível).
// O prêmio (ver SaqueDoAndar): 1 em 3 vezes, um dos itens que o andar ia ter; senão, uma arma, arma de arremesso ou armadura
// da região seguinte, nunca amaldiçoada, às vezes +1.
public class Piscina extends ComodoEspecial {
    private static final int PIRANHAS = 3;

    @Override
    public int minLargura() {
        return 6;
    }

    @Override
    public int minAltura() {
        return 6;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.AGUA);
        Porta porta = entrada();
        porta.definir(Porta.Tipo.COMUM);
        int x = -1;
        int y = -1;
        if (porta.x == esquerda) {
            x = direita - 1;
            y = cima + altura() / 2;
            Pincel.preencher(planta, esquerda + 1, cima + 1, 1, altura() - 2, PlantaDoAndar.CHAO_ESPECIAL);
        } else if (porta.x == direita) {
            x = esquerda + 1;
            y = cima + altura() / 2;
            Pincel.preencher(planta, direita - 1, cima + 1, 1, altura() - 2, PlantaDoAndar.CHAO_ESPECIAL);
        } else if (porta.y == cima) {
            x = esquerda + largura() / 2;
            y = baixo - 1;
            Pincel.preencher(planta, esquerda + 1, cima + 1, largura() - 2, 1, PlantaDoAndar.CHAO_ESPECIAL);
        } else if (porta.y == baixo) {
            x = esquerda + largura() / 2;
            y = cima + 1;
            Pincel.preencher(planta, esquerda + 1, baixo - 1, largura() - 2, 1, PlantaDoAndar.CHAO_ESPECIAL);
        }
        Pincel.definir(planta, x, y, PlantaDoAndar.PEDESTAL);
        planta.adicionarPremio(x, y, Premio.EQUIPAMENTO_DA_SALA, true);
        planta.pedir(PlantaDoAndar.ItemPedido.INVISIBILIDADE);
        for (int i = 0; i < PIRANHAS; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio();
            } while (planta.tipo(lugar.x, lugar.y) != PlantaDoAndar.AGUA || temInimigo(planta, lugar));
            planta.adicionarInimigo(lugar.x, lugar.y, PlantaDoAndar.InimigoDaSala.PIRANHA, null, 0);
        }
    }

    private static boolean temInimigo(PlantaDoAndar planta, Ponto lugar) {
        return planta.inimigos().stream().anyMatch(inimigo -> inimigo.x() == lugar.x && inimigo.y() == lugar.y);
    }

    // A água da sala é dela (a água do andar não muda nada aqui)
    @Override
    public boolean podeTerAgua(Ponto p) {
        return false;
    }

    @Override
    public boolean podeTerGrama(Ponto p) {
        return false;
    }

    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
