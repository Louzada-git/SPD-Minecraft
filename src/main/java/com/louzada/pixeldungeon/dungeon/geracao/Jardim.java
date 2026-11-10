package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O jardim (o "GardenRoom" do Shattered): grama alta na beirada, grama baixa no meio, e às vezes plantas.
// Igual ao Shattered: 1 em 3 tem uma grama-solar, 1 em 3 tem um arbusto de fruta-sem-graça, e no resto,
// 1 em 5 tem os dois.
public class Jardim extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                boolean beirada = x == esquerda + 1 || x == direita - 1 || y == cima + 1 || y == baixo - 1;
                planta.definirGrama(x, y, beirada ? PlantaDoAndar.GRAMA_ALTA : PlantaDoAndar.GRAMA);
            }
        }
        trancar(planta);

        int arbustos = Dado.ate(3);
        if (arbustos == 0) {
            plantar(planta, Premio.PLANTA_GRAMA_SOLAR);
        } else if (arbustos == 1) {
            plantar(planta, Premio.ARBUSTO_DE_FRUTA);
        } else if (Dado.ate(5) == 0) {
            plantar(planta, Premio.PLANTA_GRAMA_SOLAR);
            plantar(planta, Premio.ARBUSTO_DE_FRUTA);
        }
    }

    private void plantar(PlantaDoAndar planta, Premio premio) {
        Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
        planta.definirGrama(lugar.x, lugar.y, PlantaDoAndar.GRAMA);
        planta.adicionarPremio(lugar.x, lugar.y, premio, false);
    }
}
