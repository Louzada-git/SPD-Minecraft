package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;

// Gera a planta de um andar: salas ligadas por corredores, com entrada, saída e poças de água.
// A mesma semente + o mesmo andar sempre geram a mesma planta.
public class GeradorDeAndar {

    public static PlantaDoAndar gerar(long semente, int andar) {
        Random sorteio = new Random(semente * 1000003L + andar);

        if (Andares.ehAndarDeChefe(andar)) {
            return gerarArenaDoChefe();
        }

        // Às vezes o sorteio cria poucas salas; aí tenta de novo
        for (int tentativa = 0; tentativa < 50; tentativa++) {
            PlantaDoAndar planta = tentarGerar(sorteio);
            if (planta != null) {
                return planta;
            }
        }
        return gerarArenaDoChefe();
    }

    private static PlantaDoAndar tentarGerar(Random sorteio) {
        int tamanho = Andares.TAMANHO_DA_PLANTA;
        PlantaDoAndar planta = new PlantaDoAndar(tamanho, tamanho);

        // 1. Espalha salas sem deixar uma encostar na outra
        List<Sala> salas = new ArrayList<>();
        int quantidadeDesejada = 8 + sorteio.nextInt(5);
        for (int i = 0; i < 400 && salas.size() < quantidadeDesejada; i++) {
            int largura = 3 + sorteio.nextInt(5);
            int altura = 3 + sorteio.nextInt(5);
            int x = 1 + sorteio.nextInt(tamanho - largura - 1);
            int y = 1 + sorteio.nextInt(tamanho - altura - 1);
            Sala nova = new Sala(x, y, largura, altura);

            boolean encosta = false;
            for (Sala sala : salas) {
                if (nova.encostaEm(sala, 1)) {
                    encosta = true;
                    break;
                }
            }
            if (!encosta) {
                salas.add(nova);
            }
        }
        if (salas.size() < 5) {
            return null;
        }

        for (Sala sala : salas) {
            planta.adicionarSala(sala);
        }

        // 2. Liga todas as salas (árvore geradora mínima) e depois adiciona alguns caminhos extras
        //    para formar voltas, como no Shattered
        ligarSalas(planta, salas, sorteio);

        // 3. Entrada numa sala qualquer e saída na sala mais longe dela
        Sala salaDaEntrada = salas.get(sorteio.nextInt(salas.size()));
        Sala salaDaSaida = salaDaEntrada;
        int maiorDistancia = -1;
        for (Sala sala : salas) {
            int distancia = distancia(sala, salaDaEntrada);
            if (distancia > maiorDistancia) {
                maiorDistancia = distancia;
                salaDaSaida = sala;
            }
        }
        planta.definirEntrada(salaDaEntrada.centroX(), salaDaEntrada.centroY());
        planta.definirSaida(salaDaSaida.centroX(), salaDaSaida.centroY());

        // 4. Poças de água (o esgoto do Shattered é cheio delas)
        int pocas = 2 + sorteio.nextInt(3);
        for (int i = 0; i < pocas; i++) {
            Sala sala = salas.get(sorteio.nextInt(salas.size()));
            espalharAgua(planta, sala.centroX(), sala.centroY(), 6 + sorteio.nextInt(9), sorteio);
        }

        // 5. Grama (igual ao esgoto do Shattered: 20% de chance, em manchas)
        espalharGrama(planta, sorteio, 0.20F, 4);

        return planta;
    }

    // O andar 5 do esgoto: a arena do Goo (a primeira sala da planta), uma salinha de entrada embaixo
    // e a sala da saída do lado. A saída fica trancada até o Goo morrer.
    private static PlantaDoAndar gerarArenaDoChefe() {
        int tamanho = Andares.TAMANHO_DA_PLANTA;
        PlantaDoAndar planta = new PlantaDoAndar(tamanho, tamanho);

        Sala arena = new Sala(8, 4, 16, 16);
        Sala entrada = new Sala(13, 23, 6, 5);
        Sala saida = new Sala(26, 9, 5, 6);
        planta.adicionarSala(arena);
        planta.adicionarSala(entrada);
        planta.adicionarSala(saida);
        cavarCorredor(planta, entrada.centroX(), entrada.centroY(), arena.centroX(), arena.centroY(), true);
        cavarCorredor(planta, arena.centroX(), arena.centroY(), saida.centroX(), saida.centroY(), true);

        // Pilares grossos (1 quadradinho = 4x4 blocos) pra se esconder do chefe
        int[][] pilares = { { 11, 7 }, { 20, 7 }, { 11, 16 }, { 20, 16 } };
        for (int[] pilar : pilares) {
            planta.definir(pilar[0], pilar[1], PlantaDoAndar.PAREDE);
        }

        // Água: o Goo se cura enquanto está nela. O ninho dele fica numa poça grande no meio,
        // e cada pilar tem uma poça em volta. Quem luta tem que tirar o Goo da água.
        encherDeAgua(planta, arena.centroX() - 3, arena.centroY() - 3, 6, 6);
        for (int[] pilar : pilares) {
            encherDeAgua(planta, pilar[0] - 1, pilar[1] - 1, 3, 3);
        }

        planta.definirEntrada(entrada.centroX(), entrada.centroY());
        planta.definirSaida(saida.centroX(), saida.centroY());
        return planta;
    }

    // Transforma o chão de um retângulo em água (paredes e pilares ficam como estão)
    private static void encherDeAgua(PlantaDoAndar planta, int x, int y, int largura, int altura) {
        for (int qy = y; qy < y + altura; qy++) {
            for (int qx = x; qx < x + largura; qx++) {
                if (planta.tipo(qx, qy) == PlantaDoAndar.CHAO) {
                    planta.definir(qx, qy, PlantaDoAndar.AGUA);
                }
            }
        }
    }

    // Corredor em "L" de um ponto a outro. Só transforma parede em chão (não estraga o que já existe).
    private static void cavarCorredor(PlantaDoAndar planta, int x1, int y1, int x2, int y2, boolean horizontalPrimeiro) {
        int x = x1;
        int y = y1;
        if (horizontalPrimeiro) {
            while (x != x2) {
                abrir(planta, x, y);
                x += Integer.signum(x2 - x);
            }
            while (y != y2) {
                abrir(planta, x, y);
                y += Integer.signum(y2 - y);
            }
        } else {
            while (y != y2) {
                abrir(planta, x, y);
                y += Integer.signum(y2 - y);
            }
            while (x != x2) {
                abrir(planta, x, y);
                x += Integer.signum(x2 - x);
            }
        }
        abrir(planta, x2, y2);
    }

    private static void abrir(PlantaDoAndar planta, int x, int y) {
        if (planta.tipo(x, y) == PlantaDoAndar.PAREDE) {
            planta.definir(x, y, PlantaDoAndar.CHAO);
        }
    }

    private static void ligarSalas(PlantaDoAndar planta, List<Sala> salas, Random sorteio) {
        int quantidade = salas.size();
        boolean[] conectada = new boolean[quantidade];
        boolean[][] ligadas = new boolean[quantidade][quantidade];
        conectada[0] = true;

        // Algoritmo de Prim: sempre liga a sala desconectada mais perto de alguma sala já conectada
        for (int passo = 1; passo < quantidade; passo++) {
            int melhorA = -1;
            int melhorB = -1;
            int melhorDistancia = Integer.MAX_VALUE;
            for (int a = 0; a < quantidade; a++) {
                if (!conectada[a]) {
                    continue;
                }
                for (int b = 0; b < quantidade; b++) {
                    if (conectada[b]) {
                        continue;
                    }
                    int distancia = distancia(salas.get(a), salas.get(b));
                    if (distancia < melhorDistancia) {
                        melhorDistancia = distancia;
                        melhorA = a;
                        melhorB = b;
                    }
                }
            }
            conectada[melhorB] = true;
            ligadas[melhorA][melhorB] = true;
            ligadas[melhorB][melhorA] = true;
            ligar(planta, salas.get(melhorA), salas.get(melhorB), sorteio);
        }

        // Caminhos extras: cada sala tem 30% de chance de ganhar uma ligação com a vizinha mais próxima
        for (int a = 0; a < quantidade; a++) {
            if (sorteio.nextInt(100) >= 30) {
                continue;
            }
            int maisPerto = -1;
            int menorDistancia = Integer.MAX_VALUE;
            for (int b = 0; b < quantidade; b++) {
                if (b == a || ligadas[a][b]) {
                    continue;
                }
                int distancia = distancia(salas.get(a), salas.get(b));
                if (distancia < menorDistancia) {
                    menorDistancia = distancia;
                    maisPerto = b;
                }
            }
            if (maisPerto >= 0) {
                ligadas[a][maisPerto] = true;
                ligadas[maisPerto][a] = true;
                ligar(planta, salas.get(a), salas.get(maisPerto), sorteio);
            }
        }
    }

    private static void ligar(PlantaDoAndar planta, Sala a, Sala b, Random sorteio) {
        cavarCorredor(planta, a.centroX(), a.centroY(), b.centroX(), b.centroY(), sorteio.nextBoolean());
    }

    private static int distancia(Sala a, Sala b) {
        return Math.abs(a.centroX() - b.centroX()) + Math.abs(a.centroY() - b.centroY());
    }

    // Grama igual ao Shattered: sorteia manchas na planta inteira (cada quadradinho olha os 8 vizinhos e fica
    // igual à maioria, várias vezes) e põe grama no chão das salas que cair numa mancha.
    // Quanto mais vizinhos com grama, maior a chance de ser grama alta (de 1 em 12 até 9 em 12).
    private static void espalharGrama(PlantaDoAndar planta, Random sorteio, float preenchimento, int suavizar) {
        int largura = planta.largura();
        int altura = planta.altura();
        boolean[] mancha = new boolean[largura * altura];
        float chance = preenchimento + (0.5F - preenchimento) * 0.5F;
        for (int i = 0; i < mancha.length; i++) {
            mancha[i] = sorteio.nextFloat() < chance;
        }
        for (int vez = 0; vez < suavizar; vez++) {
            boolean[] nova = new boolean[mancha.length];
            for (int y = 0; y < altura; y++) {
                for (int x = 0; x < largura; x++) {
                    int comGrama = 0;
                    int vizinhos = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (planta.dentro(x + dx, y + dy)) {
                                vizinhos++;
                                if (mancha[(y + dy) * largura + x + dx]) {
                                    comGrama++;
                                }
                            }
                        }
                    }
                    nova[y * largura + x] = 2 * comGrama >= vizinhos;
                }
            }
            mancha = nova;
        }
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (!mancha[y * largura + x] || planta.tipo(x, y) != PlantaDoAndar.CHAO || planta.ehCorredor(x, y)) {
                    continue;
                }
                int comGrama = 1;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx != 0 || dy != 0) && planta.dentro(x + dx, y + dy) && mancha[(y + dy) * largura + x + dx]) {
                            comGrama++;
                        }
                    }
                }
                planta.definirGrama(x, y, sorteio.nextFloat() < comGrama / 12.0F ? PlantaDoAndar.GRAMA_ALTA : PlantaDoAndar.GRAMA);
            }
        }
    }

    // Caminha aleatoriamente a partir de um ponto, transformando chão em água
    private static void espalharAgua(PlantaDoAndar planta, int x, int y, int passos, Random sorteio) {
        for (int i = 0; i < passos; i++) {
            if (planta.tipo(x, y) == PlantaDoAndar.CHAO) {
                planta.definir(x, y, PlantaDoAndar.AGUA);
            }
            int direcao = sorteio.nextInt(4);
            int novoX = x + (direcao == 0 ? 1 : direcao == 1 ? -1 : 0);
            int novoY = y + (direcao == 2 ? 1 : direcao == 3 ? -1 : 0);
            // Só anda por lugares abertos, pra água nunca entrar na parede
            if (planta.aberto(novoX, novoY)) {
                x = novoX;
                y = novoY;
            }
        }
    }
}
