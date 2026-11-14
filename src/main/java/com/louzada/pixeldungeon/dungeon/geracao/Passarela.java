package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A passarela (o "WalkwayRoom" do Shattered, que é o "PerimeterRoom" sobre abismo): os caminhos entre as portas
// vão rente às paredes, e o meio é abismo (se tiver mais de 3 de largura).
public class Passarela extends ComodoDeLigacao {

    @Override
    protected boolean sobreOAbismo() {
        return true;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        if (Math.min(largura(), altura()) > 3) {
            encherDeAbismo(planta);
        }
        pintarBeiradas(planta, chaoDoTunel());
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.TUNEL);
        }
        abrirParaOsVizinhosDeAbismo(planta);
    }

    // Liga as portas pelos caminhos mais curtos rente às paredes (o "fillPerimiterPaths" do Shattered)
    private void pintarBeiradas(PlantaDoAndar planta, byte chao) {
        List<Ponto> faltam = new ArrayList<>();
        for (Porta porta : ligados.values()) {
            Ponto p = new Ponto(porta);
            if (p.y == cima) {
                p.y++;
            } else if (p.y == baixo) {
                p.y--;
            } else if (p.x == esquerda) {
                p.x++;
            } else {
                p.x--;
            }
            faltam.add(p);
        }
        List<Ponto> feitos = new ArrayList<>();
        feitos.add(faltam.remove(0));
        while (!faltam.isEmpty()) {
            Ponto de = null;
            Ponto ate = null;
            int menor = Integer.MAX_VALUE;
            for (Ponto f : feitos) {
                for (Ponto t : faltam) {
                    int distancia = distancia(f, t);
                    if (distancia < menor) {
                        de = f;
                        ate = t;
                        menor = distancia;
                    }
                }
            }
            ligar(planta, de, ate, chao);
            feitos.add(ate);
            faltam.remove(ate);
        }
    }

    private static int espacoEntre(int a, int b) {
        return Math.abs(a - b) - 1;
    }

    // O comprimento do caminho rente às paredes entre dois pontos
    private int distancia(Ponto a, Ponto b) {
        if (((a.x == esquerda + 1 || a.x == direita - 1) && a.y == b.y) || ((a.y == cima + 1 || a.y == baixo - 1) && a.x == b.x)) {
            return Math.max(espacoEntre(a.x, b.x), espacoEntre(a.y, b.y));
        }
        return Math.min(espacoEntre(esquerda, a.x) + espacoEntre(esquerda, b.x), espacoEntre(direita, a.x) + espacoEntre(direita, b.x))
                + Math.min(espacoEntre(cima, a.y) + espacoEntre(cima, b.y), espacoEntre(baixo, a.y) + espacoEntre(baixo, b.y)) - 1;
    }

    private void ligar(PlantaDoAndar planta, Ponto de, Ponto ate, byte chao) {
        // No mesmo lado
        if (((de.x == esquerda + 1 || de.x == direita - 1) && de.x == ate.x) || ((de.y == cima + 1 || de.y == baixo - 1) && de.y == ate.y)) {
            Pincel.preencher(planta, Math.min(de.x, ate.x), Math.min(de.y, ate.y), espacoEntre(de.x, ate.x) + 2, espacoEntre(de.y, ate.y) + 2, chao);
            return;
        }
        // Em lados vizinhos: passa pelo canto
        Ponto[] cantos = { new Ponto(esquerda + 1, cima + 1), new Ponto(direita - 1, cima + 1), new Ponto(direita - 1, baixo - 1),
                new Ponto(esquerda + 1, baixo - 1) };
        for (Ponto c : cantos) {
            if ((c.x == de.x || c.y == de.y) && (c.x == ate.x || c.y == ate.y)) {
                Pincel.linha(planta, de, c, chao);
                Pincel.linha(planta, c, ate, chao);
                return;
            }
        }
        // Em lados opostos: vai pelo lado mais curto, como se fossem duas ligações de lados vizinhos
        Ponto lado;
        if (de.y == cima + 1 || de.y == baixo - 1) {
            if (espacoEntre(esquerda, de.x) + espacoEntre(esquerda, ate.x) <= espacoEntre(direita, de.x) + espacoEntre(direita, ate.x)) {
                lado = new Ponto(esquerda + 1, cima + altura() / 2);
            } else {
                lado = new Ponto(direita - 1, cima + altura() / 2);
            }
        } else {
            if (espacoEntre(cima, de.y) + espacoEntre(cima, ate.y) <= espacoEntre(baixo, de.y) + espacoEntre(baixo, ate.y)) {
                lado = new Ponto(esquerda + largura() / 2, cima + 1);
            } else {
                lado = new Ponto(esquerda + largura() / 2, baixo - 1);
            }
        }
        ligar(planta, de, lado, chao);
        ligar(planta, lado, ate, chao);
    }
}
