package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Um feitiço do Shattered (ver TipoDeFeitico e EfeitosDosFeiticos): clique direito lança.
// Os que pedem um item só se gastam quando o item é escolhido. Imune à magia, não dá pra lançar.
public class ItemFeitico extends Item {
    private TipoDeFeitico tipo;

    public ItemFeitico(Properties propriedades, TipoDeFeitico tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDeFeitico tipo() {
        return tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi) {
            if (EfeitosDePergaminhoExotico.bloqueado(heroi, "item")) {
                return InteractionResultHolder.fail(pilha);
            }
            jogador.getCooldowns().addCooldown(this, Turno.TICKS);
            if (EfeitosDosFeiticos.lancar(heroi, tipo) && !jogador.getAbilities().instabuild) {
                pilha.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + tipo.nomeDoItem() + ".descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.feitico").withStyle(ChatFormatting.DARK_GRAY));
    }
}
