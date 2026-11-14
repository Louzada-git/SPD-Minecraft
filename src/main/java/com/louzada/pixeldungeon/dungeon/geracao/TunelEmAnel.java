package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// O túnel em anel (o "RingTunnelRoom" do Shattered): os caminhos de todas as portas chegam num anel de 3x3 em volta de um pilar.
public class TunelEmAnel extends ComodoDeLigacao {
    // (guardado: os caminhos e o anel têm que usar o mesmo lugar)
    private Retangulo anel;

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
        pintarAnel(planta);
    }

    protected void pintarAnel(PlantaDoAndar planta) {
        pintarCaminhos(planta);
        Retangulo r = espacoDasLigacoes();
        Pincel.preencher(planta, r.esquerda, r.cima, 3, 3, chaoDoTunel());
        Pincel.preencher(planta, r.esquerda + 1, r.cima + 1, 1, 1, PlantaDoAndar.PAREDE);
    }

    @Override
    protected Retangulo espacoDasLigacoes() {
        if (anel == null) {
            Ponto c = centroDasPortas();
            c.x = Math.max(esquerda + 2, Math.min(c.x, direita - 2));
            c.y = Math.max(cima + 2, Math.min(c.y, baixo - 2));
            anel = new Retangulo(c.x - 1, c.y - 1, c.x + 1, c.y + 1);
        }
        return anel;
    }
}
