package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala do poço mágico (o "MagicWellRoom" do Shattered): um poço no meio, de água da saúde ou da consciência.
// Entrar no poço (ou jogar um item nele) usa a água, uma vez só. A porta fica trancada.
public class PocoMagico extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Ponto c = centro();
        planta.adicionarPoco(c.x, c.y, Dado.ate(2) == 1);
        trancar(planta);
    }
}
