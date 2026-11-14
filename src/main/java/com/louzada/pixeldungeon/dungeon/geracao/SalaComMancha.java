package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayDeque;

// Uma sala comum com uma mancha aleatória no chão (o "PatchRoom" do Shattered): a bacia redonda (água) e a sala queimada
// (brasas e armadilhas de fogo). A mancha cobre só o lado de dentro da sala.
public abstract class SalaComMancha extends ComodoComum {
    protected boolean[] mancha;

    // Quanto do chão a mancha cobre, e quanto ela se agrupa
    protected abstract float preenchimento();

    protected abstract int agrupamento();

    // Sempre dá pra andar das portas até todo o chão livre?
    protected boolean garantirCaminho() {
        return false;
    }

    // Tira os pedaços que só encostam no resto pela diagonal
    protected boolean limparBeiradas() {
        return false;
    }

    protected void sortearMancha() {
        int larguraDentro = largura() - 2;
        int alturaDentro = altura() - 2;
        if (!garantirCaminho() || ligados.isEmpty()) {
            mancha = Mancha.gerar(larguraDentro, alturaDentro, preenchimento(), agrupamento(), true);
        } else {
            float preenchimento = preenchimento();
            int tentativas = 0;
            boolean valido;
            do {
                mancha = Mancha.gerar(larguraDentro, alturaDentro, preenchimento, agrupamento(), true);
                int inicio = indice(centro().x, centro().y);
                for (Porta porta : ligados.values()) {
                    Ponto um = pontoDentro(porta, 1);
                    Ponto dois = pontoDentro(porta, 2);
                    inicio = indice(um.x, um.y);
                    mancha[inicio] = false;
                    mancha[indice(dois.x, dois.y)] = false;
                }
                valido = tudoAlcancavel(inicio, larguraDentro, alturaDentro);
                if (++tentativas > 100) {
                    preenchimento -= 0.01f;
                    tentativas = 0;
                }
            } while (!valido);
        }
        if (limparBeiradas()) {
            limparDiagonais();
        }
    }

    protected boolean naMancha(Ponto p) {
        return dentro(p) && mancha != null && mancha[indice(p.x, p.y)];
    }

    protected boolean naMancha(int x, int y) {
        return naMancha(new Ponto(x, y));
    }

    private boolean tudoAlcancavel(int inicio, int larguraDentro, int alturaDentro) {
        if (inicio < 0 || inicio >= mancha.length || mancha[inicio]) {
            return false;
        }
        boolean[] visto = new boolean[mancha.length];
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        fila.add(inicio);
        visto[inicio] = true;
        int[][] passos = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!fila.isEmpty()) {
            int atual = fila.poll();
            int x = atual % larguraDentro;
            int y = atual / larguraDentro;
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

    private void limparDiagonais() {
        int larguraDentro = largura() - 2;
        for (int i = 0; i < mancha.length - larguraDentro; i++) {
            if (!mancha[i]) {
                continue;
            }
            if (i % larguraDentro != 0 && mancha[i - 1 + larguraDentro] && !(mancha[i - 1] || mancha[i + larguraDentro])) {
                mancha[i - 1 + larguraDentro] = false;
            }
            if ((i + 1) % larguraDentro != 0 && mancha[i + 1 + larguraDentro] && !(mancha[i + 1] || mancha[i + larguraDentro])) {
                mancha[i + 1 + larguraDentro] = false;
            }
        }
    }

    protected int indice(int x, int y) {
        return (x - esquerda - 1) + (y - cima - 1) * (largura() - 2);
    }
}
