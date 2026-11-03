package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A sala das plantas (o "PlantsRoom" do Shattered): grama em volta, grama alta no meio
// e uma, duas ou quatro plantas já plantadas.
public class SalaDasPlantas extends ComodoComum {

    @Override
    public int minLargura() {
        return Math.max(super.minLargura(), 5);
    }

    @Override
    public int minAltura() {
        return Math.max(super.minAltura(), 5);
    }

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 3, 1, 0 };
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        grama(planta, 1, PlantaDoAndar.GRAMA);
        grama(planta, 2, PlantaDoAndar.GRAMA_ALTA);
        if (Math.min(largura(), altura()) >= 7) {
            grama(planta, 3, PlantaDoAndar.GRAMA);
        }

        Ponto centro = centro();
        if (Math.max(largura(), altura()) >= 9) {
            if (Math.min(largura(), altura()) >= 11) {
                linhaDeGramaAlta(planta, new Ponto(esquerda + 2, centro.y), new Ponto(direita - 2, centro.y));
                linhaDeGramaAlta(planta, new Ponto(centro.x, cima + 2), new Ponto(centro.x, baixo - 2));
                plantar(planta, centro.x - 1, centro.y - 1);
                plantar(planta, centro.x + 1, centro.y - 1);
                plantar(planta, centro.x - 1, centro.y + 1);
                plantar(planta, centro.x + 1, centro.y + 1);
            } else if (largura() > altura() || (largura() == altura() && Dado.ate(2) == 0)) {
                linhaDeGramaAlta(planta, new Ponto(centro.x, cima + 2), new Ponto(centro.x, baixo - 2));
                plantar(planta, centro.x - 1, centro.y);
                plantar(planta, centro.x + 1, centro.y);
            } else {
                linhaDeGramaAlta(planta, new Ponto(esquerda + 2, centro.y), new Ponto(direita - 2, centro.y));
                plantar(planta, centro.x, centro.y - 1);
                plantar(planta, centro.x, centro.y + 1);
            }
        } else {
            plantar(planta, centro.x, centro.y);
        }
        portasComuns();
    }

    private void grama(PlantaDoAndar planta, int margem, byte tipo) {
        for (int y = cima + margem; y <= baixo - margem; y++) {
            for (int x = esquerda + margem; x <= direita - margem; x++) {
                planta.definirGrama(x, y, tipo);
            }
        }
    }

    private void linhaDeGramaAlta(PlantaDoAndar planta, Ponto de, Ponto ate) {
        for (int y = Math.min(de.y, ate.y); y <= Math.max(de.y, ate.y); y++) {
            for (int x = Math.min(de.x, ate.x); x <= Math.max(de.x, ate.x); x++) {
                planta.definirGrama(x, y, PlantaDoAndar.GRAMA_ALTA);
            }
        }
    }

    // A planta fica num quadradinho de grama baixa (a grama alta esconderia ela)
    private void plantar(PlantaDoAndar planta, int x, int y) {
        planta.definirGrama(x, y, PlantaDoAndar.GRAMA);
        planta.adicionarPremio(x, y, Premio.PLANTA_ALEATORIA, false);
    }
}
