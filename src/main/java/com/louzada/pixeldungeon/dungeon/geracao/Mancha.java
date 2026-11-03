package com.louzada.pixeldungeon.dungeon.geracao;

// Manchas aleatórias (o "Patch" do Shattered), usadas pra água, grama e barris.
//   preenchimento: quanto da área começa marcada
//   agrupamento: quantas vezes cada quadradinho fica igual à maioria dos 8 vizinhos (mais vezes = manchas maiores)
//   forcarPreenchimento: no fim, marca ou desmarca o que faltar pra ficar com o preenchimento pedido
public final class Mancha {
    private Mancha() {
    }

    public static boolean[] gerar(int largura, int altura, float preenchimento, int agrupamento, boolean forcarPreenchimento) {
        int tamanho = largura * altura;
        boolean[] atual = new boolean[tamanho];
        boolean[] marcado = new boolean[tamanho];
        int diferenca = -Math.round(tamanho * preenchimento);

        if (forcarPreenchimento && agrupamento > 0) {
            preenchimento += (0.5f - preenchimento) * 0.5f;
        }
        for (int i = 0; i < tamanho; i++) {
            marcado[i] = Dado.decimal() < preenchimento;
            if (marcado[i]) {
                diferenca++;
            }
        }

        for (int vez = 0; vez < agrupamento; vez++) {
            for (int y = 0; y < altura; y++) {
                for (int x = 0; x < largura; x++) {
                    int marcados = 0;
                    int vizinhos = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int vx = x + dx;
                            int vy = y + dy;
                            if (vx < 0 || vy < 0 || vx >= largura || vy >= altura) {
                                continue;
                            }
                            vizinhos++;
                            if (marcado[vx + vy * largura]) {
                                marcados++;
                            }
                        }
                    }
                    int i = x + y * largura;
                    atual[i] = 2 * marcados >= vizinhos;
                    if (atual[i] != marcado[i]) {
                        diferenca += atual[i] ? +1 : -1;
                    }
                }
            }
            boolean[] troca = atual;
            atual = marcado;
            marcado = troca;
        }

        if (forcarPreenchimento && Math.min(largura, altura) > 2) {
            int[] vizinhos = { -largura - 1, -largura, -largura + 1, -1, 0, +1, +largura - 1, +largura, +largura + 1 };
            boolean crescendo = diferenca < 0;
            while (diferenca != 0) {
                int quadradinho;
                int tentativas = 0;
                do {
                    quadradinho = Dado.de(1, largura - 1) + Dado.de(1, altura - 1) * largura;
                    tentativas++;
                } while (marcado[quadradinho] != crescendo && tentativas * 10 < tamanho);
                for (int vizinho : vizinhos) {
                    if (diferenca != 0 && marcado[quadradinho + vizinho] != crescendo) {
                        marcado[quadradinho + vizinho] = crescendo;
                        diferenca += crescendo ? +1 : -1;
                    }
                }
            }
        }
        return marcado;
    }
}
