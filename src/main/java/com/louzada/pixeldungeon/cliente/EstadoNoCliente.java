package com.louzada.pixeldungeon.cliente;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.rede.PacoteEstadoDoHeroi;

// O que o jogo do jogador sabe sobre o herói dele (chega do servidor pelo PacoteEstadoDoHeroi)
public class EstadoNoCliente {
    private static PacoteEstadoDoHeroi estado = new PacoteEstadoDoHeroi("", "", List.of());
    private static Map<String, Integer> talentos = new HashMap<>();

    public static void atualizar(PacoteEstadoDoHeroi novo) {
        estado = novo;
        talentos = new HashMap<>();
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
        return Math.max(0, Talento.pontosGanhos(tier, nivel()) - gastos);
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

    public static int forca() {
        return estado.numero(6);
    }
}
