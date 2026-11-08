package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A biblioteca secreta (o "SecretLibraryRoom" do Shattered): toda cheia de estantes, com um oval de chão no meio,
// um caminho da porta até ele e 2 ou 3 pergaminhos.
// No Shattered dá pra andar na diagonal, então às vezes o caminho só encosta no oval pela quina. Aqui não dá
// (os pergaminhos ficariam presos), então, quando precisa, o mod tira as poucas estantes que faltam pra ligar.
public class BibliotecaSecreta extends ComodoSecreto {

    @Override
    public int minLargura() {
        return Math.max(7, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(7, super.minAltura());
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ESTANTE);
        Pincel.elipse(planta, this, 2, PlantaDoAndar.CHAO_ESPECIAL);
        boolean[][] oval = new boolean[largura()][altura()];
        for (int x = 0; x < largura(); x++) {
            for (int y = 0; y < altura(); y++) {
                oval[x][y] = planta.tipo(x + esquerda, y + cima) == PlantaDoAndar.CHAO_ESPECIAL;
            }
        }
        Porta porta = entrada();
        if (porta.x == esquerda || porta.x == direita) {
            Pincel.desenharDentro(planta, this, porta, (largura() - 3) / 2, PlantaDoAndar.CHAO_ESPECIAL);
        } else {
            Pincel.desenharDentro(planta, this, porta, (altura() - 3) / 2, PlantaDoAndar.CHAO_ESPECIAL);
        }
        ligarAoOval(planta, oval);
        esconder(planta);
        int quantos = Dado.entre(2, 3);
        for (int i = 0; i < quantos; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.PERGAMINHO, false);
        }
    }

    // Anda da porta até o oval tirando o menor número possível de estantes (as do caminho não contam)
    private void ligarAoOval(PlantaDoAndar planta, boolean[][] oval) {
        int largura = largura();
        int altura = altura();
        int[] custo = new int[largura * altura];
        int[] veioDe = new int[largura * altura];
        Arrays.fill(custo, Integer.MAX_VALUE);
        Porta porta = entrada();
        int inicio = (porta.x - esquerda) + largura * (porta.y - cima);
        custo[inicio] = 0;
        veioDe[inicio] = -1;
        Deque<Integer> fila = new ArrayDeque<>();
        fila.add(inicio);
        int achou = -1;
        int[][] passos = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!fila.isEmpty()) {
            int aqui = fila.pollFirst();
            int x = aqui % largura;
            int y = aqui / largura;
            if (oval[x][y]) {
                achou = aqui;
                break;
            }
            for (int[] passo : passos) {
                int nx = x + passo[0];
                int ny = y + passo[1];
                if (nx < 1 || ny < 1 || nx > largura - 2 || ny > altura - 2) {
                    continue;
                }
                int vizinho = nx + largura * ny;
                boolean estante = planta.tipo(nx + esquerda, ny + cima) != PlantaDoAndar.CHAO_ESPECIAL;
                int novo = custo[aqui] + (estante ? 1 : 0);
                if (novo < custo[vizinho]) {
                    custo[vizinho] = novo;
                    veioDe[vizinho] = aqui;
                    if (estante) {
                        fila.addLast(vizinho);
                    } else {
                        fila.addFirst(vizinho);
                    }
                }
            }
        }
        for (int i = achou; i != inicio && i >= 0; i = veioDe[i]) {
            planta.definir(i % largura + esquerda, i / largura + cima, PlantaDoAndar.CHAO_ESPECIAL);
        }
    }
}
