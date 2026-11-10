package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A despensa secreta (o "SecretLarderRoom" do Shattered): uma pocinha no meio com um arbusto de fruta-sem-graça
// e comida em volta (no esgoto, uma carne grelhada).
public class DespensaSecreta extends ComodoSecreto {

    @Override
    public int minLargura() {
        return Math.max(6, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(6, super.minAltura());
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        Ponto c = centro();
        Pincel.preencher(planta, c.x - 1, c.y - 1, 3, 3, PlantaDoAndar.AGUA);
        Pincel.definir(planta, c, PlantaDoAndar.CHAO);
        planta.definirGrama(c.x, c.y, PlantaDoAndar.GRAMA);
        planta.adicionarPremio(c.x, c.y, Premio.ARBUSTO_DE_FRUTA, false);
        Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
        planta.adicionarPremio(lugar.x, lugar.y, Premio.CARNE_GRELHADA, false);
        esconder(planta);
    }
}
