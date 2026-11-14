package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// Uma salinha dentro de uma sala especial (o "EmptyRoom" que algumas salas do Shattered usam por dentro).
// Igual a um cômodo, conta as duas pontas: a salinha de esquerda 2 a direita 3 tem 2 quadradinhos de largura.
public class SubSala extends Retangulo {

    public SubSala() {
        super();
    }

    public SubSala(int esquerda, int cima, int direita, int baixo) {
        super(esquerda, cima, direita, baixo);
    }

    // A salinha que começa em (x, y) e vai "largura" e "altura" além disso (o "setPos(x, y).resize(w, h)" do Shattered)
    public static SubSala em(int x, int y, int largura, int altura) {
        return new SubSala(x, y, x + largura, y + altura);
    }

    public void preencher(PlantaDoAndar planta, byte tipo) {
        Pincel.preencher(planta, esquerda, cima, largura() + 1, altura() + 1, tipo);
    }

    // O meio (com as medidas pares, um dos dois do meio, sorteado)
    public Ponto meio() {
        return new Ponto((esquerda + direita) / 2 + (((direita - esquerda) % 2) == 1 ? Dado.ate(2) : 0),
                (cima + baixo) / 2 + (((baixo - cima) % 2) == 1 ? Dado.ate(2) : 0));
    }

    // Um quadradinho sorteado, a pelo menos "margem" das bordas
    public Ponto sorteado(int margem) {
        return new Ponto(Dado.entre(esquerda + margem, direita - margem), Dado.entre(cima + margem, baixo - margem));
    }

    // Quantos quadradinhos ela tem
    public int quadradinhos() {
        return (largura() + 1) * (altura() + 1);
    }

    public boolean temDentro(Ponto p) {
        return p.x >= esquerda && p.x <= direita && p.y >= cima && p.y <= baixo;
    }
}
