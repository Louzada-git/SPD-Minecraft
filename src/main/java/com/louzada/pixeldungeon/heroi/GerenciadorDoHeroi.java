package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// O que acontece com o herói sozinho, com o tempo: blindagem, talentos, capa, cargas da Duelista.
// (1 turno do Shattered = 2 segundos = 40 ticks, ver Turno)
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
        if (jogador.tickCount % 10 == 0) {
            // Segurar a arma (ou vestir a armadura) revela se ela é amaldiçoada
            NivelDoItem.aoEquipar(jogador, jogador.getMainHandItem());
            NivelDoItem.aoEquipar(jogador, jogador.getItemBySlot(EquipmentSlot.CHEST));
            PesoDoEquipamento.atualizar(jogador, dados);
            tirarPecasQueOShatteredNaoTem(jogador);
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

    // Igual ao Shattered, o herói só veste uma armadura (no peito): capacete, calça e bota voltam pra mochila
    private static void tirarPecasQueOShatteredNaoTem(ServerPlayer jogador) {
        for (EquipmentSlot lugar : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            ItemStack peca = jogador.getItemBySlot(lugar);
            if (!peca.isEmpty()) {
                jogador.setItemSlot(lugar, ItemStack.EMPTY);
                if (!jogador.getInventory().add(peca)) {
                    jogador.drop(peca, false);
                }
            }
        }
    }
}
