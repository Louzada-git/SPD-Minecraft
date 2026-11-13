package com.louzada.pixeldungeon.dungeon;

import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.dungeon.geracao.GeracaoDoShattered;

// Gera a planta de um andar. Os andares comuns são gerados igual ao Shattered (ver o pacote "geracao");
// o andar do chefe é a arena do Goo. A mesma semente + o mesmo andar sempre geram a mesma planta.
public class GeradorDeAndar {

    public static PlantaDoAndar gerar(long semente, int andar) {
        if (Andares.ehAndarDeChefe(andar)) {
            return gerarArenaDoChefe();
        }
        Random sorteio = new Random(semente * 1000003L + andar);
        List<ComodoEspecial.Tipo> especiais = ComodoEspecial.doAndar(semente, andar);
        List<ComodoSecreto.Tipo> secretas = ComodoSecreto.secretasDoAndar(semente, andar);
        // O tema que os acessórios deram pro andar (ver TemaDoAndar), pro pintor
        TemaDoAndar.marcarAtual(TemaDoAndar.de(semente, andar));
        try {
            // Às vezes o andar sai grande demais; aí gera de novo
            for (int tentativa = 0; tentativa < 50; tentativa++) {
                PlantaDoAndar planta = GeracaoDoShattered.gerar(sorteio, andar, especiais, secretas, Andares.TAMANHO_DA_PLANTA);
                if (planta != null) {
                    return planta;
                }
            }
        } finally {
            TemaDoAndar.marcarAtual(TemaDoAndar.Decisao.NENHUMA);
        }
        return gerarArenaDoChefe();
    }

    // O andar 5 do esgoto: a arena do Goo (a primeira sala da planta), uma salinha de entrada embaixo
    // e a sala da saída do lado. A saída fica trancada até o Goo morrer.
    private static PlantaDoAndar gerarArenaDoChefe() {
        PlantaDoAndar planta = new PlantaDoAndar(32, 32);

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
}
