package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O arsenal secreto (o "SecretArtilleryRoom" do Shattered): uma estátua no meio e armas de arremesso em volta.
// No Shattered vem também uma bomba dupla (o mod ainda não tem bombas).
public class ArsenalSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.definir(planta, centro(), PlantaDoAndar.ESTATUA);
        for (int i = 0; i < 2; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.ARREMESSO, false);
        }
        esconder(planta);
    }
}
