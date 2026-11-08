package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O tesouro secreto (o "SecretHoardRoom" do Shattered): ouro espalhado em metade do chão da sala
// (no total, mais ou menos o mesmo que 8 pilhas de ouro). No Shattered metade do chão tem armadilhas;
// o mod ainda não tem armadilhas.
public class TesouroSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        int pilhas = ((largura() - 2) * (altura() - 2)) / 2;
        for (int i = 0; i < pilhas; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.OURO_DO_TESOURO, false);
        }
        esconder(planta);
    }
}
