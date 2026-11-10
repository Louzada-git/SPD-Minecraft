package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.PedraArremessada;

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

// Uma pedra rúnica do Shattered (ver TipoDePedra). Igual a ele, as pedras já vêm conhecidas.
//   as de arremesso (choque, medo...): clique direito arremessa; o efeito acontece onde ela cair
//   as de usar num item (encantamento, intuição, detecção mágica, alteração): clique direito abre a escolha do item
// O que cada uma faz está em EfeitosDasPedras.
public class ItemPedraRunica extends Item {
    private TipoDePedra tipo;

    public ItemPedraRunica(Properties propriedades, TipoDePedra tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDePedra tipo() {
        return tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (tipo.deInventario()) {
            if (jogador instanceof ServerPlayer heroi) {
                if (EfeitosDePergaminhoExotico.bloqueado(heroi, "item")) {
                    return InteractionResultHolder.fail(pilha);
                }
                EfeitosDasPedras.usar(heroi, tipo);
            }
            return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
        }
        if (!mundo.isClientSide()) {
            PedraArremessada pedra = new PedraArremessada(mundo, jogador);
            pedra.setItem(pilha.copyWithCount(1));
            pedra.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 1.5F, 0.5F);
            mundo.addFreshEntity(pedra);
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, tipo.chaveDaDescricao(), ChatFormatting.GRAY);
        if (tipo == TipoDePedra.INTUICAO) {
            Textos.adicionarQuebrado(linhas, "item.pixeldungeon.pedra_intuicao.duas_vezes", ChatFormatting.DARK_AQUA);
        }
        linhas.add(Component.translatable(tipo.deInventario() ? "dica.pixeldungeon.pedra_usar" : "dica.pixeldungeon.pedra_arremessar")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
