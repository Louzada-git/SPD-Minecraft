package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Comer, igual ao Shattered: a comida é comida na hora (sem a animação do Minecraft), mas o herói gasta
// os turnos de comer parado (efeito "Comendo"): ração e carne misteriosa, 3 turnos; baga e ração de suprimentos, 1.
// Também dá pra comer de barriga cheia, como no Shattered.
public class Comida {
    private static final int TURNOS_PRA_COMER = 3;
    private static final int TURNOS_PRA_COMER_RAPIDO = 1;

    public static int turnosPraComer(ItemStack pilha) {
        return pilha.getItem() instanceof Baga || pilha.getItem() instanceof RacaoDeSuprimentos ? TURNOS_PRA_COMER_RAPIDO : TURNOS_PRA_COMER;
    }

    // Clique direito com qualquer item: comendo (ou em estase) não dá pra usar nada; comida é comida na hora
    public static void aoUsarItem(PlayerInteractEvent.RightClickItem evento) {
        Player jogador = evento.getEntity();
        ItemStack pilha = evento.getItemStack();
        if (jogador.hasEffect(ModEfeitos.COMENDO) || jogador.hasEffect(ModEfeitos.ESTASE)) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (!pilha.has(DataComponents.FOOD) || jogador.isCreative()) {
            return;
        }
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.sidedSuccess(jogador.level().isClientSide()));
        if (jogador instanceof ServerPlayer heroi) {
            comer(heroi, pilha, evento);
        }
    }

    private static void comer(ServerPlayer jogador, ItemStack pilha, PlayerInteractEvent.RightClickItem evento) {
        boolean emJogo = GerenciadorDoHeroi.emJogo(jogador);
        // Os talentos de refeição do tier 2 fazem comer mais rápido
        int turnos = emJogo ? TalentosDoTier2.turnosPraComer(jogador, pilha, turnosPraComer(pilha)) : turnosPraComer(pilha);
        // O que o item faz quando termina de ser comido (a fome, a cura da ração de suprimentos, o azar da carne...)
        ItemStack resto = pilha.finishUsingItem(jogador.level(), jogador);
        jogador.setItemInHand(evento.getHand(), resto);
        if (emJogo) {
            Talentos.aoComer(jogador, RegrasDoHeroi.dados(jogador));
            TalentosDoTier2.aoComer(jogador, RegrasDoHeroi.dados(jogador));
        }
        if (turnos > 0) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.COMENDO, Turno.ticks(turnos), 0, false, false, true));
        }
    }
}
