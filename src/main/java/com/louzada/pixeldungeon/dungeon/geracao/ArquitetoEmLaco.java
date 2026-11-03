package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

// O andar em uma volta só (o "LoopBuilder" do Shattered): a entrada, as salas do caminho e a saída
// (no meio do caminho) formam um anel, e as outras salas saem em galhos dele.
public class ArquitetoEmLaco extends Arquiteto {
    private float[] centroDaVolta;

    @Override
    public List<Comodo> construir(List<Comodo> comodos) {
        separarComodos(comodos);
        if (entrada == null) {
            return null;
        }
        centroDaVolta = null;
        entrada.definirTamanho();
        entrada.posicionar(0, 0);

        float anguloInicial = Dado.decimal(0, 360);
        caminhoPrincipal.add(0, entrada);
        if (saida != null) {
            caminhoPrincipal.add((caminhoPrincipal.size() + 1) / 2, saida);
        }
        List<Comodo> volta = comTuneis(caminhoPrincipal, new float[][] { tuneisNoCaminho.clone() });
        if (!fazerVolta(comodos, volta, anguloInicial)) {
            return null;
        }
        centroDaVolta = centroDe(volta);

        List<Comodo> ondeCrescer = new ArrayList<>(volta);
        List<Comodo> paraPendurar = new ArrayList<>(variasLigacoes);
        paraPendurar.addAll(umaLigacao);
        pesarComodos(ondeCrescer);
        if (!criarGalhos(comodos, ondeCrescer, paraPendurar, tuneisNosGalhos)) {
            return null;
        }
        ligacoesExtras(comodos);
        return comodos;
    }

    // Os galhos crescem mais pra dentro da volta
    @Override
    protected float anguloDoGalho(Comodo comodo) {
        if (centroDaVolta == null) {
            return super.anguloDoGalho(comodo);
        }
        return anguloPertoDoCentro(comodo, centroDaVolta[0], centroDaVolta[1]);
    }
}
