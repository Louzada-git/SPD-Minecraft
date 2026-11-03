package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Em cada run, as cores das poções e as runas dos pergaminhos são embaralhadas, igual ao Shattered:
// numa run a poção escarlate é de cura, na outra é de gás tóxico.
// O embaralhamento sai da semente da run, então não precisa salvar nada.
public class IdentidadesDaRun {

    public static TipoDePocao tipoDaCor(long semente, CorDePocao cor) {
        return embaralharPocoes(semente).get(cor.ordinal());
    }

    public static CorDePocao corDoTipo(long semente, TipoDePocao tipo) {
        return CorDePocao.values()[embaralharPocoes(semente).indexOf(tipo)];
    }

    public static TipoDePergaminho tipoDaRuna(long semente, RunaDePergaminho runa) {
        return embaralharPergaminhos(semente).get(runa.ordinal());
    }

    public static RunaDePergaminho runaDoTipo(long semente, TipoDePergaminho tipo) {
        return RunaDePergaminho.values()[embaralharPergaminhos(semente).indexOf(tipo)];
    }

    // Posição i da lista = tipo escondido pela cor i
    private static List<TipoDePocao> embaralharPocoes(long semente) {
        List<TipoDePocao> tipos = new ArrayList<>(List.of(TipoDePocao.values()));
        Collections.shuffle(tipos, new Random(semente ^ 0x506F43414FL));
        return tipos;
    }

    private static List<TipoDePergaminho> embaralharPergaminhos(long semente) {
        List<TipoDePergaminho> tipos = new ArrayList<>(List.of(TipoDePergaminho.values()));
        Collections.shuffle(tipos, new Random(semente ^ 0x5065726761L));
        return tipos;
    }
}
