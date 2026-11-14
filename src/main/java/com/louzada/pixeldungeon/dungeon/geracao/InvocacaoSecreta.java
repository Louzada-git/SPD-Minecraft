package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;

// A sala de invocação secreta (o "SecretSummoningRoom" do Shattered): o chão inteiro coberto de armadilhas de invocação
// escondidas (de 3x3 até 6x6), e no meio uma ossada com um item qualquer, em cima delas.
// Igual ao Shattered, se o item for amaldiçoado a ossada é assombrada (quem pega o item solta um espectro: ver Espectro).
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
        planta.adicionarPremio(meio.x, meio.y, Premio.ITEM_ALEATORIO, PlantaDoAndar.Recipiente.OSSADA);
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                planta.colocarArmadilha(x, y, TipoDeArmadilha.INVOCACAO, false);
            }
        }
        esconder(planta);
    }
}
