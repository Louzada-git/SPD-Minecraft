package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala do anel (o "RingRoom" do Shattered): um corredor em volta de um bloco de parede no meio.
// Nas salas grandes, o bloco do meio é oco, com uma porta e barris dentro, e no meio dos barris fica guardado um dos itens
// que o andar ia ter (se tiver algum). Também pode ser a entrada ou a saída do andar (sempre grande):
// aí o bloco do meio é de chão especial, com a escada no meio.
public class SalaDoAnel extends ComodoComum {

    @Override
    public int minLargura() {
        return Math.max(super.minLargura(), 7);
    }

    @Override
    public int minAltura() {
        return Math.max(super.minAltura(), 7);
    }

    @Override
    public float[] chancesDoTamanho() {
        return escada != Escada.NENHUMA ? new float[] { 0, 1, 0 } : new float[] { 9, 3, 1 };
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);

        int menorLado = Math.min(largura(), altura());
        int larguraDoCorredor = (int) Math.floor(0.2f * (menorLado + 3));
        Pincel.preencher(planta, this, larguraDoCorredor + 1, PlantaDoAndar.PAREDE);

        if (menorLado >= 10) {
            Pincel.preencher(planta, this, larguraDoCorredor + 2, escada != Escada.NENHUMA ? PlantaDoAndar.CHAO_ESPECIAL : PlantaDoAndar.BARRIL);
            Ponto centro = centro();
            int dx = 0;
            int dy = 0;
            if (Dado.ate(2) == 0) {
                if (centro.x < (esquerda + direita) / 2f) {
                    dx = 1;
                } else if (centro.x > (esquerda + direita) / 2f) {
                    dx = -1;
                } else {
                    dx = Dado.ate(2) == 0 ? 1 : -1;
                }
            } else {
                if (centro.y < (cima + baixo) / 2f) {
                    dy = 1;
                } else if (centro.y > (cima + baixo) / 2f) {
                    dy = -1;
                } else {
                    dy = Dado.ate(2) == 0 ? 1 : -1;
                }
            }
            // Um caminho do meio até a parede do bloco, que ganha uma porta
            Pincel.definir(planta, centro, PlantaDoAndar.CHAO_ESPECIAL);
            Ponto meio = new Ponto(centro);
            centro.deslocar(dx, dy);
            while (planta.tipo(centro.x, centro.y) != PlantaDoAndar.PAREDE) {
                Pincel.definir(planta, centro, PlantaDoAndar.CHAO_ESPECIAL);
                centro.deslocar(dx, dy);
            }
            Pincel.definir(planta, centro, PlantaDoAndar.PORTA);
            if (escada != Escada.NENHUMA) {
                porEscadaEm(planta, meio);
            } else {
                planta.adicionarPremio(meio.x, meio.y, PlantaDoAndar.Premio.PREMIO_DO_ANEL, PlantaDoAndar.Recipiente.CHAO);
            }
        }

        portasComuns();
    }
}
