package com.louzada.pixeldungeon.dungeon;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// O "sentimento" do andar (o "Level.Feeling" do Shattered). A partir do andar 2, metade dos andares ganha um tema,
// cada um com a mesma chance (1 em 14):
//   - abismal: o andar fica em cima de um abismo (fora das salas é tudo buraco) e os túneis viram pontes de chão especial
//   - inundado: muito mais água (85% em vez de 30%)
//   - gramado: muito mais grama (80% em vez de 20%)
//   - escuro: o andar tem bem menos luz, e os inimigos voltam mais rápido (2/3 do tempo)
//   - extenso: 9 salas comuns em vez de 4 a 6, uma sala especial a mais, um terço a mais de inimigos, 2 itens e uma comida a mais
//   - com armadilhas: 5x mais armadilhas (as que sobram ficam à vista)
//   - com segredos: uma sala secreta a mais, e as portas escondidas aparecem bem mais (e podem esconder salas comuns)
// Na outra metade, os acessórios podem dar um tema: a moita de musgo dá água ou grama, o mecanismo de armadilha dá
// armadilhas ou abismo (metade das vezes cada). E o mecanismo também deixa à vista uma parte das armadilhas escondidas de qualquer andar.
// O tema sorteado só pela semente (a primeira metade) sai sempre igual; o que vem dos acessórios depende do que a run tinha
// quando o andar foi construído, então é decidido uma vez só e fica salvo na run (DadosDaRun) e aqui.
// Quem chega no andar lê o aviso do tema no chat, igual ao Shattered.
public class TemaDoAndar {
    public enum Tema {
        // (a ordem não pode mudar: o número de cada um fica salvo na run)
        NENHUM("nenhum"), AGUA("agua"), GRAMA("grama"), ARMADILHAS("armadilhas"), ABISMOS("abismos"), ESCURO("escuro"), GRANDE("grande"),
        SEGREDOS("segredos");

        private final String id;

        Tema(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String chaveDoAviso() {
            return "tema.pixeldungeon." + id;
        }
    }

    // Os temas da primeira metade, na ordem do Shattered (abismo, água, grama, escuro, grande, armadilhas, segredos)
    private static final Tema[] SORTEADOS = { Tema.ABISMOS, Tema.AGUA, Tema.GRAMA, Tema.ESCURO, Tema.GRANDE, Tema.ARMADILHAS, Tema.SEGREDOS };

    // O que foi decidido pra cada andar de cada semente: o tema e o nível do mecanismo de armadilha (-1: ninguém tinha)
    public record Decisao(Tema tema, int mecanismo) {
        public static final Decisao NENHUMA = new Decisao(Tema.NENHUM, -1);

        int codigo() {
            return tema.ordinal() + 10 * (mecanismo + 1);
        }

        static Decisao doCodigo(int codigo) {
            int tema = codigo % 10;
            return new Decisao(tema < Tema.values().length ? Tema.values()[tema] : Tema.NENHUM, codigo / 10 - 1);
        }
    }

    private static final String CHAVE = "tema_do_andar_";
    private static final Map<String, Decisao> DECIDIDOS = new HashMap<>();
    // O que vale pra planta que está sendo pintada agora (o GeradorDeAndar marca)
    private static Decisao atual = Decisao.NENHUMA;

    // O tema do andar: o que foi decidido quando ele foi construído, ou (se ainda não foi) o que a semente sozinha dá
    public static Decisao de(long semente, int andar) {
        Decisao decidida = DECIDIDOS.get(semente + ":" + andar);
        return decidida != null ? decidida : sortear(semente, andar, -1, -1);
    }

    // O tema que só a semente dá (sem os acessórios): o extenso e o com segredos só vêm daqui, então
    // a escolha das salas especiais e secretas de todos os andares pode usar isto sem saber dos acessórios
    public static Tema daSemente(long semente, int andar) {
        return sortear(semente, andar, -1, -1).tema();
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
        if (andar <= 1 || Andares.ehAndarDeChefe(andar)) {
            return new Decisao(Tema.NENHUM, mecanismo);
        }
        Random sorteio = new Random(semente * 59 + andar * 23L);
        int dado = sorteio.nextInt(14);
        if (dado < SORTEADOS.length) {
            return new Decisao(SORTEADOS[dado], mecanismo);
        }
        if (musgo < 0 && mecanismo < 0) {
            return new Decisao(Tema.NENHUM, mecanismo);
        }
        // (os dois números são sorteados sempre, igual ao Shattered)
        float chanceDoMusgo = sorteio.nextFloat();
        float chanceDoMecanismo = sorteio.nextFloat();
        if (chanceDoMusgo < Acessorios.chanceDeTema(musgo)) {
            // Igual ao Shattered: 1 de grama pra cada 2 de água
            return new Decisao(sorteio.nextInt(3) == 0 ? Tema.GRAMA : Tema.AGUA, mecanismo);
        }
        if (chanceDoMecanismo < Acessorios.chanceDeTema(mecanismo)) {
            // Igual ao Shattered: metade das vezes armadilhas, metade abismo
            return new Decisao(sorteio.nextInt(2) == 0 ? Tema.ARMADILHAS : Tema.ABISMOS, mecanismo);
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

    // O aviso do tema pra quem chega no andar (igual ao Shattered, em laranja, toda vez que alguém chega)
    public static boolean avisar(ServerPlayer jogador, int andar) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Tema tema = de(run.semente(), andar).tema();
        if (tema == Tema.NENHUM) {
            return false;
        }
        jogador.displayClientMessage(Component.translatable(tema.chaveDoAviso()).withStyle(ChatFormatting.GOLD), false);
        return true;
    }
}
