package com.louzada.pixeldungeon.rede;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.Ouro;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

// Manda pro jogo do jogador o estado do herói (só quando alguma coisa mudou)
public class RedeDoHeroi {
    private static Map<UUID, PacoteEstadoDoHeroi> ultimosEnviados = new HashMap<>();

    public static void enviarEstado(ServerPlayer jogador, boolean sempre) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        ClasseDoHeroi classe = dados.classe();
        StringBuilder talentos = new StringBuilder();
        if (classe != null) {
            for (Talento talento : Talento.daClasse(classe)) {
                talentos.append(talento.id()).append('=').append(dados.pontos(talento)).append(';');
            }
        }
        long agora = jogador.level().getGameTime();
        List<Integer> numeros = new ArrayList<>(List.of(dados.nivel(), dados.pontosLivres(), dados.inteiro(HabilidadesDaDuelista.CARGAS),
                HabilidadesDaDuelista.cargasMaximas(dados), HabilidadesDaDuelista.golpesSeguidos(dados, agora), Ouro.ouroDe(dados), dados.forca()));
        PacoteEstadoDoHeroi pacote = new PacoteEstadoDoHeroi(classe == null ? "" : classe.id(), talentos.toString(), numeros);
        if (sempre || !pacote.equals(ultimosEnviados.get(jogador.getUUID()))) {
            ultimosEnviados.put(jogador.getUUID(), pacote);
            PacketDistributor.sendToPlayer(jogador, pacote);
        }
    }
}
