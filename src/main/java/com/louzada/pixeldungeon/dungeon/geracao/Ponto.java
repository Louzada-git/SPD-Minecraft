package com.louzada.pixeldungeon.dungeon.geracao;

// Um quadradinho da planta (o "Point" do Shattered)
public class Ponto {
    public int x;
    public int y;

    public Ponto() {
    }

    public Ponto(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Ponto(Ponto outro) {
        this(outro.x, outro.y);
    }

    public Ponto definir(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Ponto deslocar(int dx, int dy) {
        x += dx;
        y += dy;
        return this;
    }

    public float comprimento() {
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Ponto p && p.x == x && p.y == y;
    }

    @Override
    public int hashCode() {
        return x * 7919 + y;
    }
}
