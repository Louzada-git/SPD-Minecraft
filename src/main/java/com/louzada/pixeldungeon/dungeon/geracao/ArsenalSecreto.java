package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O arsenal secreto (o "SecretArtilleryRoom" do Shattered): uma estátua no meio, uma bomba dupla
// e 2 armas de arremesso em volta.
public class ArsenalSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.definir(planta, centro(), PlantaDoAndar.ESTATUA);
        Ponto bomba = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
        planta.adicionarPremio(bomba.x, bomba.y, Premio.BOMBA_DUPLA, false);
        for (int i = 0; i < 2; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.ARREMESSO, false);
        }
        esconder(planta);
    }
}
