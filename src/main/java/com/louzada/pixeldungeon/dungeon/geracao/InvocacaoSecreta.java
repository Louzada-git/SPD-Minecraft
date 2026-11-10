package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;

// A sala de invocação secreta (o "SecretSummoningRoom" do Shattered): o chão inteiro coberto de armadilhas de invocação
// escondidas (de 3x3 até 6x6), e um item qualquer largado no meio, em cima delas.
// (No Shattered o item fica num monte de ossos e, se for amaldiçoado, solta um espectro; o mod ainda não tem espectros.)
public class InvocacaoSecreta extends ComodoSecreto {

    @Override
    public int maxLargura() {
        return 8;
    }

    @Override
    public int maxAltura() {
        return 8;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Ponto meio = centro();
        planta.adicionarPremio(meio.x, meio.y, Premio.ITEM_ALEATORIO, false);
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                planta.colocarArmadilha(x, y, TipoDeArmadilha.INVOCACAO, false);
            }
        }
        esconder(planta);
    }
}
