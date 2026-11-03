package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// Os túneis entre as salas (o "ConnectionRoom" + "TunnelRoom" do Shattered). O espaço do túnel é um retângulo
// de 3 a 10 quadradinhos, mas só o caminho entre as portas vira chão: no mod ele vira um corredor (mais baixo que as salas).
// (No esgoto do Shattered, 20 em cada 26 ligações são túneis; as outras são pontes e passarelas sobre abismo,
// que o mod ainda não tem.)
public class ComodoDeLigacao extends Comodo {

    @Override
    public int minLargura() {
        return 3;
    }

    @Override
    public int maxLargura() {
        return 10;
    }

    @Override
    public int minAltura() {
        return 3;
    }

    @Override
    public int maxAltura() {
        return 10;
    }

    @Override
    public int minLigacoes(int lado) {
        return lado == TODOS ? 2 : 0;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        byte chao = PlantaDoAndar.CHAO;
        Retangulo c = espacoDasLigacoes();
        // De cada porta, um caminho em "L" até o meio das portas
        for (Porta porta : ligados.values()) {
            Ponto inicio = new Ponto(porta);
            if (inicio.x == esquerda) {
                inicio.x++;
            } else if (inicio.y == cima) {
                inicio.y++;
            } else if (inicio.x == direita) {
                inicio.x--;
            } else if (inicio.y == baixo) {
                inicio.y--;
            }
            int paraDireita = inicio.x < c.esquerda ? c.esquerda - inicio.x : inicio.x > c.direita ? c.direita - inicio.x : 0;
            int paraBaixo = inicio.y < c.cima ? c.cima - inicio.y : inicio.y > c.baixo ? c.baixo - inicio.y : 0;
            Ponto meio;
            Ponto fim;
            if (porta.x == esquerda || porta.x == direita) {
                meio = new Ponto(inicio.x + paraDireita, inicio.y);
                fim = new Ponto(meio.x, meio.y + paraBaixo);
            } else {
                meio = new Ponto(inicio.x, inicio.y + paraBaixo);
                fim = new Ponto(meio.x + paraDireita, meio.y);
            }
            Pincel.linha(planta, inicio, meio, chao);
            Pincel.linha(planta, meio, fim, chao);
        }
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.TUNEL);
        }
    }

    private Retangulo espacoDasLigacoes() {
        float somaX = 0;
        float somaY = 0;
        for (Porta porta : ligados.values()) {
            somaX += porta.x;
            somaY += porta.y;
        }
        int quantas = ligados.size();
        Ponto c = new Ponto((int) somaX / quantas, (int) somaY / quantas);
        if (Dado.decimal() < somaX % 1) {
            c.x++;
        }
        if (Dado.decimal() < somaY % 1) {
            c.y++;
        }
        c.x = Math.max(esquerda + 1, Math.min(c.x, direita - 1));
        c.y = Math.max(cima + 1, Math.min(c.y, baixo - 1));
        return new Retangulo(c.x, c.y, c.x, c.y);
    }
}
