package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// O estudo (o "StudyRoom" do Shattered): estantes encostadas nas paredes, chão especial e, no meio, um pedestal com um prêmio
// (metade das vezes um dos itens que o andar ia ter; senão, uma poção ou um pergaminho). Nos grandes, 4 cantos de estante.
public class Estudo extends ComodoComum {

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
        return new float[] { 2, 1, 0 };
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ESTANTE);
        Pincel.preencher(planta, this, 2, PlantaDoAndar.CHAO_ESPECIAL);
        for (Porta porta : ligados.values()) {
            Pincel.desenharDentro(planta, this, porta, 2, PlantaDoAndar.CHAO_ESPECIAL);
            porta.definir(Porta.Tipo.COMUM);
        }
        if (tamanho == Tamanho.GRANDE) {
            int pilarL = (largura() - 7) / 2;
            int pilarA = (altura() - 7) / 2;
            byte e = PlantaDoAndar.ESTANTE;
            Pincel.preencher(planta, esquerda + 3, cima + 3, pilarL, 1, e);
            Pincel.preencher(planta, esquerda + 3, cima + 3, 1, pilarA, e);
            Pincel.preencher(planta, esquerda + 3, baixo - 2 - 1, pilarL, 1, e);
            Pincel.preencher(planta, esquerda + 3, baixo - 2 - pilarA, 1, pilarA, e);
            Pincel.preencher(planta, direita - 2 - pilarL, cima + 3, pilarL, 1, e);
            Pincel.preencher(planta, direita - 2 - 1, cima + 3, 1, pilarA, e);
            Pincel.preencher(planta, direita - 2 - pilarL, baixo - 2 - 1, pilarL, 1, e);
            Pincel.preencher(planta, direita - 2 - 1, baixo - 2 - pilarA, 1, pilarA, e);
        }
        Ponto meio = centro();
        Pincel.definir(planta, meio, PlantaDoAndar.PEDESTAL);
        planta.adicionarPremio(meio.x, meio.y, Premio.PREMIO_DO_ESTUDO, Recipiente.CHAO);
    }
}
