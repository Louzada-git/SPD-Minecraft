package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;

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

// Um elixir do Shattered (ver TipoDeElixir e EfeitosDeElixir): clique direito bebe.
public class ItemElixir extends Item {
    private TipoDeElixir tipo;

    public ItemElixir(Properties propriedades, TipoDeElixir tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDeElixir tipo() {
        return tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi) {
            EfeitosDeElixir.beber(heroi, tipo);
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + tipo.nomeDoItem() + ".descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.elixir").withStyle(ChatFormatting.DARK_GRAY));
    }
}
