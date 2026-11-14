package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A ponte em anel (o "RingBridgeRoom" do Shattered): o túnel em anel, mas em volta dele é tudo abismo.
public class PonteEmAnel extends TunelEmAnel {

    @Override
    protected boolean sobreOAbismo() {
        return true;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        encherDeAbismo(planta);
        pintarAnel(planta);
        abrirParaOsVizinhosDeAbismo(planta);
    }
}
