package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// O aquário (o "AquariumRoom" do Shattered): uma piscina no meio da sala, com uma volta de chão especial e outra de chão comum.
// Na água moram de 1 a 3 piranhas (mais nas salas maiores). Nada nasce nem cai dentro da água.
public class Aquario extends ComodoComum {

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
        return new float[] { 3, 1, 0 };
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Pincel.preencher(planta, this, 2, PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.preencher(planta, this, 3, PlantaDoAndar.AGUA);
        int peixes = (Math.min(largura(), altura()) - 4) / 3;
        for (int i = 0; i < peixes; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio(3);
            } while (planta.tipo(lugar.x, lugar.y) != PlantaDoAndar.AGUA || temInimigo(planta, lugar));
            planta.adicionarInimigo(lugar.x, lugar.y, PlantaDoAndar.InimigoDaSala.PIRANHA, null, 0);
        }
        portasComuns();
    }

    private static boolean temInimigo(PlantaDoAndar planta, Ponto lugar) {
        for (PlantaDoAndar.InimigoNaPlanta inimigo : planta.inimigos()) {
            if (inimigo.x() == lugar.x && inimigo.y() == lugar.y) {
                return true;
            }
        }
        return false;
    }
}
