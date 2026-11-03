package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A tesouraria (o "TreasuryRoom" do Shattered): uma estátua no meio e 2 ou 3 pilhas de ouro,
// ou todas em baús (a partir do andar 2, cada baú tem 1 chance em 5 de ser um mímico),
// ou todas no chão, com mais 6 pilhinhas de 5 a 12 moedas espalhadas.
public class Tesouraria extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Pincel.definir(planta, centro(), PlantaDoAndar.ESTATUA);

        boolean emBaus = Dado.ate(2) == 0;
        int pilhas = Dado.entre(2, 3);
        for (int i = 0; i < pilhas; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.OURO, emBaus);
        }
        if (!emBaus) {
            for (int i = 0; i < 6; i++) {
                Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
                planta.adicionarPremio(lugar.x, lugar.y, Premio.OURO_POUCO, false);
            }
        }
        trancar(planta);
    }
}
