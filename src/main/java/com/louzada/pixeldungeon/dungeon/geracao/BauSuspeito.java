package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// O baú suspeito (o "SuspiciousChestRoom" do Shattered): uma sala vazia com um baú num pedestal no meio, guardando um dos
// itens que o andar ia ter (ou ouro). 1 em 3 vezes (mais com o dente de mímico) o baú é um mímico.
public class BauSuspeito extends ComodoComum {

    @Override
    public int minLargura() {
        return Math.max(5, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(5, super.minAltura());
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        portasComuns();
        Ponto meio = centro();
        Pincel.definir(planta, meio, PlantaDoAndar.PEDESTAL);
        planta.adicionarPremio(meio.x, meio.y, Premio.PREMIO_SUSPEITO, Recipiente.BAU);
    }
}
