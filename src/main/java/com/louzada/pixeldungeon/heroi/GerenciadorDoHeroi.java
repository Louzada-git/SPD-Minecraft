package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.SeloQuebrado;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// O que acontece com o herói sozinho, com o tempo: blindagem, talentos, capa, cargas da Duelista, armas de arremesso, busca.
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
            tirarPecasQueOShatteredNaoTem(jogador);
            // Os anéis: o que foi posto pelo inventário revela a maldição; e soma os bônus
            NivelDoItem.aoEquipar(jogador, jogador.getItemBySlot(Aneis.PRIMEIRO));
            NivelDoItem.aoEquipar(jogador, jogador.getItemBySlot(Aneis.SEGUNDO));
            Aneis.atualizar(jogador, dados);
            PesoDoEquipamento.atualizar(jogador, dados);
            GlifosDoHeroi.atualizarVelocidade(jogador);
            EncantamentosDoHeroi.atualizar(jogador);
            // O Guerreiro vestiu outra armadura: oferece passar o selo
            if (dados.classe() == ClasseDoHeroi.GUERREIRO) {
                SeloQuebrado.talvezPerguntar(jogador, dados);
            }
            // As armas de arremesso que voltaram pra mochila se juntam de novo ao conjunto delas
            ArmaDeArremesso.juntarPilhas(jogador);
        }
        boolean umTurno = Turno.virou(jogador);
        Escudos.aoTick(jogador, dados, umTurno);
        GlifosDoHeroi.aoTick(jogador, dados, umTurno);
        Talentos.aoTick(jogador, dados);
        TalentosDoTier2.aoTick(jogador, dados);
        if (umTurno) {
            TalentosDoTier2.aoTurno(jogador, dados);
            Aneis.aoTurno(jogador, dados);
            EncantamentosDoHeroi.aoTurno(jogador, dados);
            // Os artefatos achados na dungeon que estão equipados (recargas, o cálice, a ampulheta...)
            ArtefatoAchado.turnoDoHeroi(jogador);
        }
        CapaDasSombras.aoTickDoHeroi(jogador, dados, umTurno);
        // Andando pela dungeon, às vezes o herói nota uma porta escondida
        Busca.aoTick(jogador);
        if (umTurno && dados.classe() == ClasseDoHeroi.DUELISTA) {
            HabilidadesDaDuelista.recarregar(dados);
        }
        // O jogo do jogador precisa saber dos talentos e das cargas (tela de talentos, barra da Duelista)
        if (jogador.tickCount % 10 == 0) {
            RedeDoHeroi.enviarEstado(jogador, false);
        }
    }

    // Igual ao Shattered, o herói só veste uma armadura (no peito): capacete, calça e bota voltam pra mochila
    // (a calça e a bota viraram as casinhas dos anéis)
    private static void tirarPecasQueOShatteredNaoTem(ServerPlayer jogador) {
        for (EquipmentSlot lugar : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            ItemStack peca = jogador.getItemBySlot(lugar);
            if (!peca.isEmpty() && !(Aneis.casinhaDeAnel(lugar) && peca.getItem() instanceof ItemAnel)) {
                jogador.setItemSlot(lugar, ItemStack.EMPTY);
                if (!jogador.getInventory().add(peca)) {
                    jogador.drop(peca, false);
                }
            }
        }
    }
}
