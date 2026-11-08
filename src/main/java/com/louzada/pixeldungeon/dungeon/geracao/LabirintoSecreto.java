package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O labirinto secreto (o "SecretMazeRoom" do Shattered): uma sala grande (14 a 18 quadradinhos) cheia de paredes
// formando um labirinto, com um baú no ponto mais longe da porta. O prêmio é uma arma ou armadura da região
// seguinte, nunca amaldiçoada, e às vezes (1 em 3) +1. O labirinto é gerado igual ao "Maze" do Shattered.
public class LabirintoSecreto extends ComodoSecreto {

    @Override
    public int minLargura() {
        return 14;
    }

    @Override
    public int minAltura() {
        return 14;
    }

    @Override
    public int maxLargura() {
        return 18;
    }

    @Override
    public int maxAltura() {
        return 18;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);

        // true = parede. A borda é parede, menos a porta.
        int largura = largura();
        int altura = altura();
        boolean[][] labirinto = new boolean[largura][altura];
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                labirinto[x][y] = x == 0 || x == largura - 1 || y == 0 || y == altura - 1;
            }
        }
        Porta porta = entrada();
        labirinto[porta.x - esquerda][porta.y - cima] = false;
        gerar(labirinto);
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (labirinto[x][y]) {
                    Pincel.definir(planta, x + esquerda, y + cima, PlantaDoAndar.PAREDE);
                }
            }
        }

        // O ponto mais longe da porta, andando pelo labirinto
        int[] distancia = new int[largura * altura];
        Arrays.fill(distancia, Integer.MAX_VALUE);
        Deque<int[]> fila = new ArrayDeque<>();
        int px = porta.x - esquerda;
        int py = porta.y - cima;
        distancia[px + largura * py] = 0;
        fila.add(new int[] { px, py });
        int melhor = 0;
        int melhorX = px;
        int melhorY = py;
        int[][] passos = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!fila.isEmpty()) {
            int[] aqui = fila.poll();
            int d = distancia[aqui[0] + largura * aqui[1]];
            if (d > melhor) {
                melhor = d;
                melhorX = aqui[0];
                melhorY = aqui[1];
            }
            for (int[] passo : passos) {
                int nx = aqui[0] + passo[0];
                int ny = aqui[1] + passo[1];
                if (nx >= 0 && ny >= 0 && nx < largura && ny < altura && !labirinto[nx][ny] && distancia[nx + largura * ny] == Integer.MAX_VALUE) {
                    distancia[nx + largura * ny] = d + 1;
                    fila.add(new int[] { nx, ny });
                }
            }
        }
        planta.adicionarPremio(melhorX + esquerda, melhorY + cima, Premio.PREMIO_DO_LABIRINTO, true);
        esconder(planta);
    }

    // O "Maze.generate" do Shattered: sai de uma parede qualquer e vai puxando paredes retas, sem encostar em outras
    private static void gerar(boolean[][] labirinto) {
        int falhas = 0;
        while (falhas < 2500) {
            int x;
            int y;
            do {
                x = Dado.ate(labirinto.length);
                y = Dado.ate(labirinto[0].length);
            } while (!labirinto[x][y]);
            int[] direcao = decidirDirecao(labirinto, x, y);
            if (direcao == null) {
                falhas++;
                continue;
            }
            falhas = 0;
            int passos = 0;
            do {
                x += direcao[0];
                y += direcao[1];
                labirinto[x][y] = true;
                passos++;
            } while (Dado.ate(passos) == 0 && podeAndar(labirinto, x, y, direcao));
        }
    }

    private static int[] decidirDirecao(boolean[][] labirinto, int x, int y) {
        if (Dado.ate(4) == 0 && podeAndar(labirinto, x, y, new int[] { 0, -1 })) {
            return new int[] { 0, -1 };
        }
        if (Dado.ate(3) == 0 && podeAndar(labirinto, x, y, new int[] { 1, 0 })) {
            return new int[] { 1, 0 };
        }
        if (Dado.ate(2) == 0 && podeAndar(labirinto, x, y, new int[] { 0, 1 })) {
            return new int[] { 0, 1 };
        }
        if (podeAndar(labirinto, x, y, new int[] { -1, 0 })) {
            return new int[] { -1, 0 };
        }
        return null;
    }

    private static boolean podeAndar(boolean[][] labirinto, int x, int y, int[] direcao) {
        int ladoX = 1 - Math.abs(direcao[0]);
        int ladoY = 1 - Math.abs(direcao[1]);
        x += direcao[0];
        y += direcao[1];
        if (x <= 0 || x >= labirinto.length - 1 || y <= 0 || y >= labirinto[0].length - 1) {
            return false;
        }
        if (labirinto[x][y] || labirinto[x + ladoX][y + ladoY] || labirinto[x - ladoX][y - ladoY]) {
            return false;
        }
        x += direcao[0];
        y += direcao[1];
        if (x <= 0 || x >= labirinto.length - 1 || y <= 0 || y >= labirinto[0].length - 1) {
            return false;
        }
        return !labirinto[x][y] && !labirinto[x + ladoX][y + ladoY] && !labirinto[x - ladoX][y - ladoY];
    }
}
