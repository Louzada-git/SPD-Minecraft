package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.item.artefato.RosaSeca;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Pétala seca (a "Petal" da rosa seca do Shattered): com a missão do fantasma completa e a rosa em mãos, aparecem pelos
// andares (uma a cada 2 andares). Na mochila de quem tem a rosa, a pétala volta pra ela e a rosa sobe um nível;
// com a rosa já completa (+10), a pétala é descartada.
public class PetalaSeca extends Item {

    public PetalaSeca(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (mundo.isClientSide() || !(dono instanceof ServerPlayer jogador) || pilha.isEmpty() || jogador.tickCount % 10 != 0) {
            return;
        }
        ItemStack rosa = acharRosa(jogador);
        if (rosa == null) {
            return;
        }
        RosaSeca tipo = (RosaSeca) rosa.getItem();
        pilha.shrink(1);
        if (ArtefatoAchado.nivel(rosa) >= tipo.nivelMaximo()) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.petala_seca.sem_espaco").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        tipo.subirNivel(jogador, rosa);
        if (ArtefatoAchado.nivel(rosa) >= tipo.nivelMaximo()) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.petala_seca.completa").withStyle(ChatFormatting.GREEN), false);
        }
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    // A rosa seca do jogador (em qualquer lugar da mochila)
    @Nullable
    public static ItemStack acharRosa(Player jogador) {
        for (int i = 0; i < jogador.getInventory().getContainerSize(); i++) {
            ItemStack pilha = jogador.getInventory().getItem(i);
            if (pilha.getItem() instanceof RosaSeca) {
                return pilha;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.petala_seca.descricao", ChatFormatting.GRAY);
    }
}
