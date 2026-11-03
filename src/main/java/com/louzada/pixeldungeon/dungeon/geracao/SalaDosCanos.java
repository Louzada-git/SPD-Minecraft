package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala dos canos do esgoto (o "SewerPipeRoom" do Shattered): canais de água ligando as portas,
// com uma calçada de chão em volta da água. O resto da sala fica fechado.
// Duas salas de canos encostadas ligam os canais (a porta entre elas vira água).
public class SalaDosCanos extends ComodoComum {
    private Ponto[] cantos;

    @Override
    public int minLargura() {
        return Math.max(7, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(7, super.minAltura());
    }

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 3, 2, 1 };
    }

    @Override
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        return false;
    }

    // As portas ficam longe dos cantos (o canal faz a curva a 2 quadradinhos da parede)
    @Override
    public boolean podeLigar(Ponto p) {
        return super.podeLigar(p) && ((p.x > esquerda + 1 && p.x < direita - 1) || (p.y > cima + 1 && p.y < baixo - 1));
    }

    @Override
    public boolean podeTerAgua(Ponto p) {
        return false;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        byte agua = PlantaDoAndar.AGUA;
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Retangulo c = espacoDasLigacoes();

        if (ligados.size() == 1 || (ligados.size() == 2 && tamanho == Tamanho.NORMAL)) {
            // Cada porta manda um canal até o meio
            for (Porta porta : ligados.values()) {
                Ponto inicio = new Ponto(porta);
                if (inicio.x == esquerda) {
                    inicio.x += 2;
                } else if (inicio.y == cima) {
                    inicio.y += 2;
                } else if (inicio.x == direita) {
                    inicio.x -= 2;
                } else if (inicio.y == baixo) {
                    inicio.y -= 2;
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
                Pincel.linha(planta, inicio, meio, agua);
                Pincel.linha(planta, meio, fim, agua);
            }
        } else {
            // Com mais portas, os canais andam pela beirada da sala ligando as portas mais próximas
            List<Ponto> pontosDasPortas = new ArrayList<>(ligados.values());
            if (pontosDasPortas.size() == 2) {
                // Com só duas portas, inventa uma terceira pra o canal dar a volta
                Ponto extra = new Ponto();
                boolean valido;
                do {
                    valido = true;
                    if (Dado.ate(2) == 0) {
                        extra.x = Dado.ate(2) == 0 ? esquerda : direita;
                        extra.y = Dado.entre(cima + 2, baixo - 2);
                    } else {
                        extra.x = Dado.entre(esquerda + 2, direita - 2);
                        extra.y = Dado.ate(2) == 0 ? cima : baixo;
                    }
                    for (Ponto porta : ligados.values()) {
                        if (porta.x == extra.x || porta.y == extra.y) {
                            valido = false;
                        }
                    }
                } while (!valido);
                pontosDasPortas.add(extra);
            }

            List<Ponto> faltam = new ArrayList<>();
            for (Ponto porta : pontosDasPortas) {
                Ponto p = new Ponto(porta);
                if (p.y == cima) {
                    p.y += 2;
                } else if (p.y == baixo) {
                    p.y -= 2;
                } else if (p.x == esquerda) {
                    p.x += 2;
                } else {
                    p.x -= 2;
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
                        int distancia = distanciaEntre(f, t);
                        if (distancia < menor) {
                            de = f;
                            ate = t;
                            menor = distancia;
                        }
                    }
                }
                encherEntre(planta, de, ate, agua);
                feitos.add(ate);
                faltam.remove(ate);
            }
        }

        // A calçada: tudo que encosta na água (até na diagonal) vira chão
        for (Ponto p : pontos()) {
            if (planta.tipo(p.x, p.y) != agua) {
                continue;
            }
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (planta.tipo(p.x + dx, p.y + dy) == PlantaDoAndar.PAREDE) {
                        planta.definir(p.x + dx, p.y + dy, PlantaDoAndar.CHAO);
                    }
                }
            }
        }

        for (Comodo outro : ligados.keySet()) {
            Porta porta = ligados.get(outro);
            if (outro instanceof SalaDosCanos) {
                // O canal passa pela porta e continua na outra sala
                Pincel.preencher(planta, porta.x - 1, porta.y - 1, 3, 3, PlantaDoAndar.CHAO);
                if (porta.x == esquerda || porta.x == direita) {
                    Pincel.preencher(planta, porta.x - 1, porta.y, 3, 1, agua);
                } else {
                    Pincel.preencher(planta, porta.x, porta.y - 1, 1, 3, agua);
                }
                porta.definir(Porta.Tipo.AGUA);
            } else {
                porta.definir(Porta.Tipo.COMUM);
            }
        }
    }

    private Retangulo espacoDasLigacoes() {
        Ponto c = ligados.size() <= 1 ? centro() : centroDasPortas();
        return new Retangulo(c.x, c.y, c.x, c.y);
    }

    private Ponto centroDasPortas() {
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
        c.x = Math.max(esquerda + 2, Math.min(c.x, direita - 2));
        c.y = Math.max(cima + 2, Math.min(c.y, baixo - 2));
        return c;
    }

    private int espacoEntre(int a, int b) {
        return Math.abs(a - b) - 1;
    }

    // Quantos quadradinhos o canal anda de um ponto ao outro pela beirada da sala
    private int distanciaEntre(Ponto a, Ponto b) {
        if (((a.x == esquerda + 2 || a.x == direita - 2) && a.y == b.y) || ((a.y == cima + 2 || a.y == baixo - 2) && a.x == b.x)) {
            return Math.max(espacoEntre(a.x, b.x), espacoEntre(a.y, b.y));
        }
        return Math.min(espacoEntre(esquerda, a.x) + espacoEntre(esquerda, b.x), espacoEntre(direita, a.x) + espacoEntre(direita, b.x))
                + Math.min(espacoEntre(cima, a.y) + espacoEntre(cima, b.y), espacoEntre(baixo, a.y) + espacoEntre(baixo, b.y))
                - 1;
    }

    private void encherEntre(PlantaDoAndar planta, Ponto de, Ponto ate, byte tipo) {
        // Os dois no mesmo lado: uma reta
        if (((de.x == esquerda + 2 || de.x == direita - 2) && de.x == ate.x) || ((de.y == cima + 2 || de.y == baixo - 2) && de.y == ate.y)) {
            Pincel.preencher(planta, Math.min(de.x, ate.x), Math.min(de.y, ate.y), espacoEntre(de.x, ate.x) + 2, espacoEntre(de.y, ate.y) + 2,
                    tipo);
            return;
        }
        if (cantos == null) {
            cantos = new Ponto[] { new Ponto(esquerda + 2, cima + 2), new Ponto(direita - 2, cima + 2), new Ponto(direita - 2, baixo - 2),
                    new Ponto(esquerda + 2, baixo - 2) };
        }
        // Lados vizinhos: faz a curva no canto
        for (Ponto canto : cantos) {
            if ((canto.x == de.x || canto.y == de.y) && (canto.x == ate.x || canto.y == ate.y)) {
                Pincel.linha(planta, de, canto, tipo);
                Pincel.linha(planta, canto, ate, tipo);
                return;
            }
        }
        // Lados opostos: passa pelo meio de um dos lados que sobram
        Ponto lado;
        if (de.y == cima + 2 || de.y == baixo - 2) {
            if (espacoEntre(esquerda, de.x) + espacoEntre(esquerda, ate.x) <= espacoEntre(direita, de.x) + espacoEntre(direita, ate.x)) {
                lado = new Ponto(esquerda + 2, cima + altura() / 2);
            } else {
                lado = new Ponto(direita - 2, cima + altura() / 2);
            }
        } else {
            if (espacoEntre(cima, de.y) + espacoEntre(cima, ate.y) <= espacoEntre(baixo, de.y) + espacoEntre(baixo, ate.y)) {
                lado = new Ponto(esquerda + largura() / 2, cima + 2);
            } else {
                lado = new Ponto(esquerda + largura() / 2, baixo - 2);
            }
        }
        encherEntre(planta, de, lado, tipo);
        encherEntre(planta, lado, ate, tipo);
    }
}
