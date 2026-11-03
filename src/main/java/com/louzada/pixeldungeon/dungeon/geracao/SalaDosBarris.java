package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayDeque;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala dos barris do esgoto (o "RegionDecoPatchRoom" do Shattered): manchas de barris de armazenamento
// espalhadas pelo chão, sempre deixando caminho livre entre as portas.
public class SalaDosBarris extends ComodoComum {
    // Onde ficam os barris (só o lado de dentro da sala)
    private boolean[] mancha;

    @Override
    public int minLargura() {
        return Math.max(5, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(5, super.minAltura());
    }

    // Quanto do chão vira barril: 20% e mais um pouco nas salas maiores
    private float preenchimento() {
        int area = Math.min(largura() * altura(), 10 * 10);
        return 0.20f + area / 1024f;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        portasComuns();
        sortearMancha();
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                if (mancha[indice(x, y)]) {
                    planta.definir(x, y, PlantaDoAndar.BARRIL);
                }
            }
        }
        porEscada(planta);
    }

    // A escada não fica em cima de um barril (porEscada ainda limpa os vizinhos)
    @Override
    protected boolean podeTerEscada(PlantaDoAndar planta, Ponto lugar) {
        return planta.tipo(lugar.x, lugar.y) != PlantaDoAndar.BARRIL;
    }

    // Igual ao "PatchRoom" do Shattered: sorteia manchas até todo o chão livre ser alcançável das portas.
    // (Aqui o caminho só vale andando reto, sem diagonal, porque no Minecraft não dá pra passar entre dois cantos.)
    private void sortearMancha() {
        int larguraDentro = largura() - 2;
        int alturaDentro = altura() - 2;
        float preenchimento = preenchimento();
        if (ligados.isEmpty()) {
            mancha = Mancha.gerar(larguraDentro, alturaDentro, preenchimento, 1, true);
            limparDiagonais();
            return;
        }
        int tentativas = 0;
        boolean valido;
        do {
            mancha = Mancha.gerar(larguraDentro, alturaDentro, preenchimento, 1, true);
            int inicio = indice(centro().x, centro().y);
            for (Porta porta : ligados.values()) {
                if (porta.x == esquerda) {
                    inicio = indice(porta.x + 1, porta.y);
                    mancha[indice(porta.x + 1, porta.y)] = false;
                    mancha[indice(porta.x + 2, porta.y)] = false;
                } else if (porta.x == direita) {
                    inicio = indice(porta.x - 1, porta.y);
                    mancha[indice(porta.x - 1, porta.y)] = false;
                    mancha[indice(porta.x - 2, porta.y)] = false;
                } else if (porta.y == cima) {
                    inicio = indice(porta.x, porta.y + 1);
                    mancha[indice(porta.x, porta.y + 1)] = false;
                    mancha[indice(porta.x, porta.y + 2)] = false;
                } else if (porta.y == baixo) {
                    inicio = indice(porta.x, porta.y - 1);
                    mancha[indice(porta.x, porta.y - 1)] = false;
                    mancha[indice(porta.x, porta.y - 2)] = false;
                }
            }
            limparDiagonais();
            valido = tudoAlcancavel(inicio, larguraDentro, alturaDentro);
            tentativas++;
            if (tentativas > 100) {
                preenchimento -= 0.01f;
                tentativas = 0;
            }
        } while (!valido);
    }

    // Todo chão livre dá pra alcançar a partir do início?
    private boolean tudoAlcancavel(int inicio, int larguraDentro, int alturaDentro) {
        if (inicio < 0 || inicio >= mancha.length || mancha[inicio]) {
            return false;
        }
        boolean[] visto = new boolean[mancha.length];
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        fila.add(inicio);
        visto[inicio] = true;
        while (!fila.isEmpty()) {
            int atual = fila.poll();
            int x = atual % larguraDentro;
            int y = atual / larguraDentro;
            int[][] passos = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] passo : passos) {
                int vx = x + passo[0];
                int vy = y + passo[1];
                if (vx < 0 || vy < 0 || vx >= larguraDentro || vy >= alturaDentro) {
                    continue;
                }
                int vizinho = vx + vy * larguraDentro;
                if (!visto[vizinho] && !mancha[vizinho]) {
                    visto[vizinho] = true;
                    fila.add(vizinho);
                }
            }
        }
        for (int i = 0; i < mancha.length; i++) {
            if (!mancha[i] && !visto[i]) {
                return false;
            }
        }
        return true;
    }

    // Tira os barris que só encostam em outro pela diagonal
    private void limparDiagonais() {
        int larguraDentro = largura() - 2;
        for (int i = 0; i < mancha.length - larguraDentro; i++) {
            if (!mancha[i]) {
                continue;
            }
            if (i % larguraDentro != 0) {
                if (mancha[i - 1 + larguraDentro] && !(mancha[i - 1] || mancha[i + larguraDentro])) {
                    mancha[i - 1 + larguraDentro] = false;
                }
            }
            if ((i + 1) % larguraDentro != 0) {
                if (mancha[i + 1 + larguraDentro] && !(mancha[i + 1] || mancha[i + larguraDentro])) {
                    mancha[i + 1 + larguraDentro] = false;
                }
            }
        }
    }

    private int indice(int x, int y) {
        return (x - esquerda - 1) + (y - cima - 1) * (largura() - 2);
    }
}
