package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.rede.PacoteEstadoDoHeroi;

// O que o jogo do jogador sabe sobre o herói dele (chega do servidor pelo PacoteEstadoDoHeroi)
public class EstadoNoCliente {
    private static PacoteEstadoDoHeroi estado = new PacoteEstadoDoHeroi("", "", List.of());
    private static Map<String, Integer> talentos = new LinkedHashMap<>();

    public static void atualizar(PacoteEstadoDoHeroi novo) {
        estado = novo;
        talentos = new LinkedHashMap<>();
        for (String parte : novo.talentos().split(";")) {
            String[] idEPontos = parte.split("=");
            if (idEPontos.length == 2) {
                try {
                    talentos.put(idEPontos[0], Integer.parseInt(idEPontos[1]));
                } catch (NumberFormatException erro) {
                    // ignora um pedaço estragado
                }
            }
        }
    }

    @Nullable
    public static ClasseDoHeroi classe() {
        return ClasseDoHeroi.peloId(estado.classe());
    }

    // Os talentos do herói num tier, na ordem (os da classe, com os trocados pelo pergaminho da metamorfose)
    public static List<Talento> talentos(int tier) {
        List<Talento> lista = new ArrayList<>();
        for (String id : talentos.keySet()) {
            Talento talento = Talento.peloId(id);
            if (talento != null && talento.tier() == tier) {
                lista.add(talento);
            }
        }
        return lista;
    }

    public static int pontos(Talento talento) {
        return talentos.getOrDefault(talento.id(), 0);
    }

    public static int nivel() {
        return Math.max(1, estado.numero(0));
    }

    public static int pontosLivres() {
        return estado.numero(1);
    }

    // Os pontos livres de um tier (pelo nível e pelos pontos já gastos nos talentos dele)
    public static int pontosLivres(int tier) {
        int gastos = 0;
        for (Map.Entry<String, Integer> talento : talentos.entrySet()) {
            Talento qual = Talento.peloId(talento.getKey());
            if (qual != null && qual.tier() == tier) {
                gastos += talento.getValue();
            }
        }
        return Math.max(0, Talento.pontosGanhos(tier, nivel(), (inspiracoes() >> tier & 1) == 1 ? 2 : 0) - gastos);
    }

    public static int cargasDaHabilidade() {
        return estado.numero(2);
    }

    public static int cargasMaximasDaHabilidade() {
        return estado.numero(3);
    }

    public static int golpesSeguidos() {
        return estado.numero(4);
    }

    public static int ouro() {
        return estado.numero(5);
    }

    // Os tiers que ganharam os pontos da inspiração divina (um bit por tier)
    public static int inspiracoes() {
        return estado.numero(8);
    }

    // As páginas do guia de alquimia que o herói já leu (um bit por página)
    public static int paginasDoGuia() {
        return estado.numero(9);
    }

    public static int forca() {
        return estado.numero(6);
    }

    public static int energia() {
        return estado.numero(7);
    }
}
