package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;

// Os túneis entre as salas (o "ConnectionRoom" + "TunnelRoom" do Shattered). O espaço do túnel é um retângulo
// de 3 a 10 quadradinhos, mas só o caminho entre as portas vira chão: no mod ele vira um corredor (mais baixo que as salas).
// No andar abismal, o caminho é de chão especial (fora dele é tudo abismo).
// Igual ao Shattered, no esgoto 20 em cada 26 ligações são túneis; as outras são a ponte (1), a passarela (2),
// o túnel em anel (2) e a ponte em anel (1).
public class ComodoDeLigacao extends Comodo {

    public static ComodoDeLigacao sortear() {
        return switch (Dado.chances(new float[] { 20, 1, 2, 2, 1 })) {
            case 0 -> new ComodoDeLigacao();
            case 1 -> new PonteSobreOAbismo();
            case 2 -> new Passarela();
            case 3 -> new TunelEmAnel();
            default -> new PonteEmAnel();
        };
    }

    // O chão do caminho (o "tunnelTile" do Shattered)
    static byte chaoDoTunel() {
        return TemaDoAndar.atual().tema() == TemaDoAndar.Tema.ABISMOS ? PlantaDoAndar.CHAO_ESPECIAL : PlantaDoAndar.CHAO;
    }

    // As ligações que atravessam abismo (a ponte, a passarela e a ponte em anel): quando duas delas se encostam,
    // a parede entre elas vira abismo também
    protected boolean sobreOAbismo() {
        return false;
    }

    // O lado de dentro vira abismo, e a parede encostada em outra ligação sobre o abismo também
    protected void encherDeAbismo(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ABISMO);
    }

    protected void abrirParaOsVizinhosDeAbismo(PlantaDoAndar planta) {
        for (Comodo vizinho : vizinhos) {
            if (vizinho instanceof ComodoDeLigacao outra && outra.sobreOAbismo()) {
                Retangulo i = intersecao(vizinho);
                if (i.largura() != 0) {
                    i.esquerda++;
                    i.direita--;
                } else {
                    i.cima++;
                    i.baixo--;
                }
                Pincel.preencher(planta, i.esquerda, i.cima, i.largura() + 1, i.altura() + 1, PlantaDoAndar.ABISMO);
            }
        }
    }

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
        pintarCaminhos(planta);
    }

    protected void pintarCaminhos(PlantaDoAndar planta) {
        byte chao = chaoDoTunel();
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

    // O espaço onde todos os caminhos se encontram (quase sempre 1 quadradinho)
    protected Retangulo espacoDasLigacoes() {
        Ponto c = centroDasPortas();
        return new Retangulo(c.x, c.y, c.x, c.y);
    }

    // Um ponto à mesma distância de todas as portas
    protected Ponto centroDasPortas() {
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
        return c;
    }
}
