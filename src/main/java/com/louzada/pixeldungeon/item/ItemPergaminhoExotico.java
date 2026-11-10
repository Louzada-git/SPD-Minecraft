package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.registro.ModItens;

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

// Um pergaminho exótico de uma runa (o pergaminho preto). O que ele faz depende do pergaminho comum da mesma runa
// nesta run (ver TipoDePergaminhoExotico e EfeitosDePergaminhoExotico). Clique direito: lê.
public class ItemPergaminhoExotico extends Item {
    private RunaDePergaminho runa;

    public ItemPergaminhoExotico(Properties propriedades, RunaDePergaminho runa) {
        super(propriedades);
        this.runa = runa;
    }

    public RunaDePergaminho runa() {
        return runa;
    }

    public static Item daRuna(RunaDePergaminho runa) {
        return ModItens.PERGAMINHOS_EXOTICOS.get(runa.ordinal()).get();
    }

    @Override
    public Component getName(ItemStack pilha) {
        TipoDePergaminho base = ConhecimentoDoCliente.pergaminho(runa);
        if (base != null) {
            return Component.translatable(TipoDePergaminhoExotico.doBase(base).chaveDoNome());
        }
        return super.getName(pilha);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer leitor) {
            if (EfeitosDePergaminhoExotico.bloqueado(leitor, "pergaminho")) {
                return InteractionResultHolder.fail(pilha);
            }
            EfeitosDePergaminhoExotico.ler(leitor, runa);
        }
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDePergaminho base = ConhecimentoDoCliente.pergaminho(runa);
        String descricao = base != null ? TipoDePergaminhoExotico.doBase(base).chaveDaDescricao()
                : "item.pixeldungeon.pergaminho_exotico.desconhecido.descricao";
        Textos.adicionarQuebrado(linhas, descricao, ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.pergaminho_como_usar").withStyle(ChatFormatting.DARK_GRAY));
    }
}
