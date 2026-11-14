package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O esconderijo do mel (o "SecretHoneypotRoom" do Shattered): um pote quebrado entre o meio da sala e a porta escondida, com a
// abelha dele de guarda, e mais um pote de mel inteiro e uma bomba largados pela sala.
public class MelSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Ponto pote = centro();
        pote.x = (pote.x + entrada().x) / 2;
        pote.y = (pote.y + entrada().y) / 2;
        planta.adicionarInimigo(pote.x, pote.y, PlantaDoAndar.InimigoDaSala.ABELHA, null, 0);
        for (Premio premio : new Premio[] { Premio.POTE_DE_MEL, Premio.BOMBA }) {
            Ponto lugar;
            do {
                lugar = aleatorio();
            } while (planta.temPremioEm(lugar.x, lugar.y) || (lugar.x == pote.x && lugar.y == pote.y));
            planta.adicionarPremio(lugar.x, lugar.y, premio, false);
        }
        esconder(planta);
    }
}
