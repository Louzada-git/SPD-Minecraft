package com.louzada.pixeldungeon.dungeon;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.run.DadosDaRun;

// O "sentimento" do andar no Shattered, só os que os acessórios trazem:
//   - moita de musgo: o andar fica cheio de água (85% em vez de 30%) ou de grama (80% em vez de 20%)
//   - mecanismo de armadilha: o andar tem 5x mais armadilhas (as que sobram ficam à vista)
// E o mecanismo também deixa à vista uma parte das armadilhas escondidas de qualquer andar.
// No Shattered, metade dos andares (a partir do 2) já ganha outro tema (escuro, grande, abismos...), que o mod ainda não tem:
// os acessórios só mexem na outra metade, igual lá.
// A planta do andar sai de novo da semente toda vez, então o tema de cada andar é decidido uma vez só, quando ele é construído,
// e fica salvo na run (DadosDaRun) e aqui.
public class TemaDoAndar {
    public enum Tema {
        NENHUM, AGUA, GRAMA, ARMADILHAS
    }

    // O que foi decidido pra cada andar de cada semente: o tema e o nível do mecanismo de armadilha (-1: ninguém tinha)
    public record Decisao(Tema tema, int mecanismo) {
        public static final Decisao NENHUMA = new Decisao(Tema.NENHUM, -1);

        int codigo() {
            return tema.ordinal() + 10 * (mecanismo + 1);
        }

        static Decisao doCodigo(int codigo) {
            return new Decisao(Tema.values()[codigo % 10], codigo / 10 - 1);
        }
    }

    private static final String CHAVE = "tema_do_andar_";
    private static final Map<String, Decisao> DECIDIDOS = new HashMap<>();
    // O que vale pra planta que está sendo pintada agora (o GeradorDeAndar marca)
    private static Decisao atual = Decisao.NENHUMA;

    public static Decisao de(long semente, int andar) {
        return DECIDIDOS.getOrDefault(semente + ":" + andar, Decisao.NENHUMA);
    }

    public static Decisao atual() {
        return atual;
    }

    public static void marcarAtual(Decisao decisao) {
        atual = decisao;
    }

    // Decide o tema do andar com os acessórios da run agora, e guarda (na run e aqui)
    public static Decisao decidir(DadosDaRun run, int andar) {
        Decisao decisao = sortear(run.semente(), andar, Acessorios.daRun(TipoDeAcessorio.MOITA_DE_MUSGO), Acessorios.daRun(TipoDeAcessorio.MECANISMO_DE_ARMADILHA));
        run.definirQueda(CHAVE + andar, decisao.codigo());
        DECIDIDOS.put(run.semente() + ":" + andar, decisao);
        return decisao;
    }

    public static Decisao sortear(long semente, int andar, int musgo, int mecanismo) {
        if (andar <= 1 || Andares.ehAndarDeChefe(andar) || (musgo < 0 && mecanismo < 0)) {
            return new Decisao(Tema.NENHUM, mecanismo);
        }
        Random sorteio = new Random(semente * 59 + andar * 23L);
        if (sorteio.nextInt(14) < 7) {
            return new Decisao(Tema.NENHUM, mecanismo);
        }
        float chanceDoMusgo = sorteio.nextFloat();
        float chanceDoMecanismo = sorteio.nextFloat();
        if (chanceDoMusgo < Acessorios.chanceDeTema(musgo)) {
            // Igual ao Shattered: 1 de grama pra cada 2 de água
            return new Decisao(sorteio.nextInt(3) == 0 ? Tema.GRAMA : Tema.AGUA, mecanismo);
        }
        if (chanceDoMecanismo < Acessorios.chanceDeTema(mecanismo)) {
            return new Decisao(Tema.ARMADILHAS, mecanismo);
        }
        return new Decisao(Tema.NENHUM, mecanismo);
    }

    // Guarda um tema sem a run (pros testes)
    public static void registrar(long semente, int andar, Decisao decisao) {
        DECIDIDOS.put(semente + ":" + andar, decisao);
    }

    // Os temas salvos na run (chamado quando a run é carregada)
    public static void carregar(DadosDaRun run, Map<String, Integer> quedas) {
        for (Map.Entry<String, Integer> queda : quedas.entrySet()) {
            if (queda.getKey().startsWith(CHAVE)) {
                int andar = Integer.parseInt(queda.getKey().substring(CHAVE.length()));
                DECIDIDOS.put(run.semente() + ":" + andar, Decisao.doCodigo(queda.getValue()));
            }
        }
    }
}
