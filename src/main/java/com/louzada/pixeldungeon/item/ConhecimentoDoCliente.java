package com.louzada.pixeldungeon.item;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

// O que o jogador desta tela já descobriu (o servidor manda pelo PacoteConhecimento).
// Usado só pra mostrar o nome certo das poções e pergaminhos. Vazio = ainda não sabe.
public class ConhecimentoDoCliente {
    private static String[] pocoes = vazio(CorDePocao.values().length);
    private static String[] pergaminhos = vazio(RunaDePergaminho.values().length);

    @Nullable
    public static TipoDePocao pocao(CorDePocao cor) {
        return TipoDePocao.peloId(pocoes[cor.ordinal()]);
    }

    @Nullable
    public static TipoDePergaminho pergaminho(RunaDePergaminho runa) {
        return TipoDePergaminho.peloId(pergaminhos[runa.ordinal()]);
    }

    public static void atualizar(List<String> novasPocoes, List<String> novosPergaminhos) {
        pocoes = novasPocoes.size() == CorDePocao.values().length ? novasPocoes.toArray(new String[0]) : vazio(CorDePocao.values().length);
        pergaminhos = novosPergaminhos.size() == RunaDePergaminho.values().length ? novosPergaminhos.toArray(new String[0])
                : vazio(RunaDePergaminho.values().length);
    }

    private static String[] vazio(int tamanho) {
        String[] lista = new String[tamanho];
        Arrays.fill(lista, "");
        return lista;
    }
}
