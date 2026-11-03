package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

// Um retângulo da planta (o "Rect" do Shattered): esquerda, cima, direita e baixo.
// A largura é direita - esquerda (os cômodos somam 1, porque contam as duas paredes).
public class Retangulo {
    public int esquerda;
    public int cima;
    public int direita;
    public int baixo;

    public Retangulo() {
        this(0, 0, 0, 0);
    }

    public Retangulo(Retangulo outro) {
        this(outro.esquerda, outro.cima, outro.direita, outro.baixo);
    }

    public Retangulo(int esquerda, int cima, int direita, int baixo) {
        this.esquerda = esquerda;
        this.cima = cima;
        this.direita = direita;
        this.baixo = baixo;
    }

    public int largura() {
        return direita - esquerda;
    }

    public int altura() {
        return baixo - cima;
    }

    public int area() {
        return largura() * altura();
    }

    public Retangulo definir(int esquerda, int cima, int direita, int baixo) {
        this.esquerda = esquerda;
        this.cima = cima;
        this.direita = direita;
        this.baixo = baixo;
        return this;
    }

    public Retangulo definir(Retangulo outro) {
        return definir(outro.esquerda, outro.cima, outro.direita, outro.baixo);
    }

    // Leva o retângulo pra (x, y) sem mudar o tamanho
    public Retangulo posicionar(int x, int y) {
        return definir(x, y, x + (direita - esquerda), y + (baixo - cima));
    }

    public Retangulo deslocar(int dx, int dy) {
        return definir(esquerda + dx, cima + dy, direita + dx, baixo + dy);
    }

    public Retangulo redimensionar(int largura, int altura) {
        return definir(esquerda, cima, esquerda + largura, cima + altura);
    }

    public boolean vazio() {
        return direita <= esquerda || baixo <= cima;
    }

    public Retangulo esvaziar() {
        esquerda = direita = cima = baixo = 0;
        return this;
    }

    // A parte em comum com o outro (se eles só se encostam, sai uma linha)
    public Retangulo intersecao(Retangulo outro) {
        Retangulo resultado = new Retangulo();
        resultado.esquerda = Math.max(esquerda, outro.esquerda);
        resultado.direita = Math.min(direita, outro.direita);
        resultado.cima = Math.max(cima, outro.cima);
        resultado.baixo = Math.min(baixo, outro.baixo);
        return resultado;
    }

    public boolean contem(Ponto p) {
        return p.x >= esquerda && p.x < direita && p.y >= cima && p.y < baixo;
    }

    public Ponto centro() {
        return new Ponto((esquerda + direita) / 2 + (((direita - esquerda) % 2) == 0 ? Dado.ate(2) : 0),
                (cima + baixo) / 2 + (((baixo - cima) % 2) == 0 ? Dado.ate(2) : 0));
    }

    // Todos os quadradinhos, da esquerda até a direita e de cima até embaixo (incluídos)
    public List<Ponto> pontos() {
        List<Ponto> pontos = new ArrayList<>();
        for (int x = esquerda; x <= direita; x++) {
            for (int y = cima; y <= baixo; y++) {
                pontos.add(new Ponto(x, y));
            }
        }
        return pontos;
    }
}
