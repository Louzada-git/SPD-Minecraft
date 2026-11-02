package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Ankh: clique direito pra trazer de volta um aliado que morreu (ele aparece do seu lado).
public class ItemAnkh extends Item {
    public ItemAnkh(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer jogadorNoServidor) {
            ServerPlayer revivido = GerenciadorDaRun.reviverAliado(jogadorNoServidor);
            if (revivido == null) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.ninguem_pra_reviver"), true);
                return InteractionResultHolder.fail(pilha);
            }
            if (!jogador.getAbilities().instabuild) {
                pilha.shrink(1);
            }
            mundo.playSound(null, jogador.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.ankh.descricao", ChatFormatting.GRAY);
    }
}
