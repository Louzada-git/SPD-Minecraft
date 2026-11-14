package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A ponte (o "BridgeRoom" do Shattered): um túnel comum, mas o resto do espaço dele é abismo (se tiver mais de 3 de largura).
// Quem cai da ponte vai parar no andar de baixo.
public class PonteSobreOAbismo extends ComodoDeLigacao {

    @Override
    protected boolean sobreOAbismo() {
        return true;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        if (Math.min(largura(), altura()) > 3) {
            encherDeAbismo(planta);
        }
        pintarCaminhos(planta);
        abrirParaOsVizinhosDeAbismo(planta);
    }
}
