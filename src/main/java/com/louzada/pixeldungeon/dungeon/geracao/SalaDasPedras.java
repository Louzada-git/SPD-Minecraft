package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A sala das pedras rúnicas (o "RunestoneRoom" do Shattered): um abismo em volta de uma ilha de chão, ligada à porta por um
// caminho de chão especial, com 2 ou 3 pedras rúnicas (ou o catalisador mágico, se ele estiver pra cair). A porta fica trancada.
public class SalaDasPedras extends ComodoEspecial {

    @Override
    public int minLargura() {
        return 6;
    }

    @Override
    public int minAltura() {
        return 6;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ABISMO);
        Pincel.desenharDentro(planta, this, entrada(), 2, PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.preencher(planta, this, 2, PlantaDoAndar.CHAO);
        int quantas = Dado.normalEntre(2, 3);
        for (int i = 0; i < quantas; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio();
            } while (planta.tipo(lugar.x, lugar.y) != PlantaDoAndar.CHAO || planta.temPremioEm(lugar.x, lugar.y));
            planta.adicionarPremio(lugar.x, lugar.y, Premio.PEDRA_DA_SALA, false);
        }
        trancar(planta);
    }
}
