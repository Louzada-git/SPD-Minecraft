package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A fenda (o "FissureRoom" do Shattered): uma sala comum com um abismo no meio, deixando uma beirada de chão em volta
// (mais larga nas salas maiores, e com a beira meio irregular). Nas salas pequenas, só um buraco no meio.
public class Fenda extends ComodoComum {

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 6, 3, 1 };
    }

    @Override
    public int minLargura() {
        return Math.max(5, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(5, super.minAltura());
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        portasComuns();
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        if (largura() * altura() <= 25) {
            Pincel.definir(planta, centro(), PlantaDoAndar.ABISMO);
        } else {
            int menorLado = Math.min(largura(), altura());
            int beirada = (int) Math.sqrt(menorLado);
            // A chance de um quadradinho da beira do buraco continuar chão (nas salas maiores, perto de 50%)
            float chanceDeChao = (float) Math.sqrt(menorLado) % 1;
            chanceDeChao = (chanceDeChao + (beirada - 1) * 0.5f) / beirada;
            for (int y = cima + 2; y <= baixo - 2; y++) {
                for (int x = esquerda + 2; x <= direita - 2; x++) {
                    int v = Math.min(y - cima, baixo - y);
                    int h = Math.min(x - esquerda, direita - x);
                    if (Math.min(v, h) > beirada || (Math.min(v, h) == beirada && Dado.decimal() > chanceDeChao)) {
                        planta.definir(x, y, PlantaDoAndar.ABISMO);
                    }
                }
            }
        }
    }
}
