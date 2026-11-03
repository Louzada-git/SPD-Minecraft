package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

// O andar em forma de oito (o "FigureEightBuilder" do Shattered): duas voltas que se encontram numa sala grande
// (a sala "marco"). A entrada fica na primeira volta e a saída na segunda.
public class ArquitetoEmOito extends Arquiteto {
    private Comodo marco;
    private List<Comodo> primeiraVolta;
    private List<Comodo> segundaVolta;
    private float[] centroDaPrimeira;
    private float[] centroDaSegunda;

    @Override
    public List<Comodo> construir(List<Comodo> comodos) {
        separarComodos(comodos);
        if (entrada == null) {
            return null;
        }
        primeiraVolta = null;
        segundaVolta = null;
        centroDaPrimeira = null;
        centroDaSegunda = null;

        // O marco: a sala do caminho com mais espaço (que aceite 4 portas ou mais)
        marco = null;
        for (Comodo comodo : caminhoPrincipal) {
            if (comodo.maxLigacoes(Comodo.TODOS) >= 4
                    && (marco == null || marco.minLargura() * marco.minAltura() < comodo.minLargura() * comodo.minAltura())) {
                marco = comodo;
            }
        }
        if (marco == null) {
            return null;
        }
        if (!variasLigacoes.isEmpty()) {
            caminhoPrincipal.add(variasLigacoes.remove(0));
        }
        caminhoPrincipal.remove(marco);
        variasLigacoes.remove(marco);

        float anguloInicial = Dado.decimal(0, 360);
        int naPrimeira = caminhoPrincipal.size() / 2;
        if (caminhoPrincipal.size() % 2 == 1) {
            naPrimeira += Dado.ate(2);
        }
        List<Comodo> sobra = new ArrayList<>(caminhoPrincipal);
        List<Comodo> primeira = new ArrayList<>();
        primeira.add(marco);
        for (int i = 0; i < naPrimeira; i++) {
            primeira.add(sobra.remove(0));
        }
        primeira.add((primeira.size() + 1) / 2, entrada);
        float[][] tuneis = { tuneisNoCaminho.clone() };
        primeiraVolta = comTuneis(primeira, tuneis);

        List<Comodo> segunda = new ArrayList<>();
        segunda.add(marco);
        segunda.addAll(sobra);
        if (saida != null) {
            segunda.add((segunda.size() + 1) / 2, saida);
        }
        segundaVolta = comTuneis(segunda, tuneis);

        marco.definirTamanho();
        marco.posicionar(0, 0);
        if (!fazerVolta(comodos, primeiraVolta, anguloInicial)) {
            return null;
        }
        if (!fazerVolta(comodos, segundaVolta, anguloInicial + 180f)) {
            return null;
        }
        if (!comodos.contains(marco)) {
            comodos.add(marco);
        }
        centroDaPrimeira = centroDe(primeiraVolta);
        centroDaSegunda = centroDe(segundaVolta);

        List<Comodo> ondeCrescer = new ArrayList<>(primeiraVolta);
        ondeCrescer.addAll(segundaVolta);
        // O marco está nas duas voltas: fica uma vez só
        ondeCrescer.remove(marco);
        List<Comodo> paraPendurar = new ArrayList<>(variasLigacoes);
        paraPendurar.addAll(umaLigacao);
        pesarComodos(ondeCrescer);
        if (!criarGalhos(comodos, ondeCrescer, paraPendurar, tuneisNosGalhos)) {
            return null;
        }
        ligacoesExtras(comodos);
        return comodos;
    }

    @Override
    protected float anguloDoGalho(Comodo comodo) {
        float[] centro = primeiraVolta != null && primeiraVolta.contains(comodo) ? centroDaPrimeira : centroDaSegunda;
        if (centro == null) {
            return super.anguloDoGalho(comodo);
        }
        return anguloPertoDoCentro(comodo, centro[0], centro[1]);
    }
}
