package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// O que acontece com o herói sozinho, com o tempo: blindagem, talentos, capa, cargas da Duelista.
// (1 turno do Shattered = 1 segundo = 20 ticks)
public class GerenciadorDoHeroi {

    // Herói vivo jogando a run (quem está no menu ou morto esperando um ankh não conta)
    public static boolean emJogo(Player jogador) {
        return jogador instanceof ServerPlayer heroi && GerenciadorDaRun.estaVivoNaRun(heroi);
    }

    public static void aoTickDoJogador(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !emJogo(jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        // Segurar uma arma desconhecida revela se ela é amaldiçoada
        if (jogador.tickCount % 10 == 0) {
            NivelDoItem.aoSegurar(jogador, jogador.getMainHandItem());
        }
        boolean umTurno = Turno.virou(jogador);
        Escudos.aoTick(jogador, dados, umTurno);
        Talentos.aoTick(jogador, dados);
        CapaDasSombras.aoTickDoHeroi(jogador, dados, umTurno);
        if (umTurno && dados.classe() == ClasseDoHeroi.DUELISTA) {
            HabilidadesDaDuelista.recarregar(dados);
        }
        // O jogo do jogador precisa saber dos talentos e das cargas (tela de talentos, barra da Duelista)
        if (jogador.tickCount % 10 == 0) {
            RedeDoHeroi.enviarEstado(jogador, false);
        }
    }
}
