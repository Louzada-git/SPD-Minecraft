package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala listrada (o "StripedRoom" do Shattered): chão especial com listras de grama alta, no sentido do lado mais comprido.
// Nas grandes, as listras viram quadrados um dentro do outro. Duas listradas juntas ficam ligadas por chão especial.
public class SalaListrada extends ComodoComum {

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 2, 1, 0 };
    }

    @Override
    public void juntar(PlantaDoAndar planta, Comodo outro, Retangulo pedaco, byte tipo) {
        super.juntar(planta, outro, pedaco, outro instanceof SalaListrada && tipo == PlantaDoAndar.CHAO ? PlantaDoAndar.CHAO_ESPECIAL : tipo);
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        portasComuns();
        if (tamanho == Tamanho.NORMAL) {
            Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
            if (largura() > altura() || (largura() == altura() && Dado.ate(2) == 0)) {
                for (int x = esquerda + 2; x < direita; x += 2) {
                    capim(planta, x, cima + 1, 1, altura() - 2);
                }
            } else {
                for (int y = cima + 2; y < baixo; y += 2) {
                    capim(planta, esquerda + 1, y, largura() - 2, 1);
                }
            }
        } else {
            int camadas = (Math.min(largura(), altura()) - 1) / 2;
            for (int i = 1; i <= camadas; i++) {
                if (i % 2 == 1) {
                    especial(planta, esquerda + i, cima + i, largura() - 2 * i, altura() - 2 * i);
                } else {
                    capim(planta, esquerda + i, cima + i, largura() - 2 * i, altura() - 2 * i);
                }
            }
        }
    }

    // Grama alta (no mod, a grama fica por cima do chão comum)
    private static void capim(PlantaDoAndar planta, int x, int y, int largura, int altura) {
        for (int qy = y; qy < y + altura; qy++) {
            for (int qx = x; qx < x + largura; qx++) {
                planta.definir(qx, qy, PlantaDoAndar.CHAO);
                planta.definirGrama(qx, qy, PlantaDoAndar.GRAMA_ALTA);
            }
        }
    }

    private static void especial(PlantaDoAndar planta, int x, int y, int largura, int altura) {
        for (int qy = y; qy < y + altura; qy++) {
            for (int qx = x; qx < x + largura; qx++) {
                planta.definir(qx, qy, PlantaDoAndar.CHAO_ESPECIAL);
                planta.definirGrama(qx, qy, PlantaDoAndar.SEM_GRAMA);
            }
        }
    }
}
