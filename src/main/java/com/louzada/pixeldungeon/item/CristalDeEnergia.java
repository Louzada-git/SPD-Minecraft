package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

// Cristal de energia alquímica (o "EnergyCrystal" do Shattered): aparece nos laboratórios. Igual ao ouro, quando o herói
// encosta nele vira energia na hora (não ocupa espaço no inventário): cada cristal da pilha vale 1 de energia.
public class CristalDeEnergia extends Item {

    public CristalDeEnergia(Properties propriedades) {
        super(propriedades);
    }

    // O herói encostou nos cristais no chão
    public static void aoEncostar(ItemEntityPickupEvent.Pre evento) {
        ItemEntity noChao = evento.getItemEntity();
        ItemStack pilha = noChao.getItem();
        if (!pilha.is(ModItens.CRISTAL_DE_ENERGIA.get()) || !(evento.getPlayer() instanceof ServerPlayer jogador)
                || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        evento.setCanPickup(TriState.FALSE);
        if (noChao.hasPickUpDelay() || noChao.isRemoved()) {
            return;
        }
        EnergiaAlquimica.ganhar(jogador, pilha.getCount());
        // A animação do item voando até o jogador
        jogador.take(noChao, pilha.getCount());
        noChao.discard();
    }

    // Se ele cair no inventário de outro jeito (um comando, por exemplo), vira energia do mesmo jeito
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && dono instanceof ServerPlayer jogador && GerenciadorDoHeroi.emJogo(jogador) && !pilha.isEmpty()) {
            EnergiaAlquimica.ganhar(jogador, pilha.getCount());
            pilha.setCount(0);
        }
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.cristal_de_energia.descricao", ChatFormatting.GRAY);
    }
}
