package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.PocaoArremessada;

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

// Uma infusão do Shattered (ver TipoDeInfusao e EfeitosDeInfusao): clique direito arremessa (quebra onde cair).
// A instável dá pra beber (clique direito; agachado arremessa): vira uma poção qualquer.
public class ItemInfusao extends Item {
    private TipoDeInfusao tipo;

    public ItemInfusao(Properties propriedades, TipoDeInfusao tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDeInfusao tipo() {
        return tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (tipo == TipoDeInfusao.INSTAVEL && !jogador.isShiftKeyDown()) {
            if (jogador instanceof ServerPlayer heroi) {
                EfeitosDeInfusao.beberInstavel(heroi);
                mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            jogador.getCooldowns().addCooldown(this, Turno.TICKS);
            if (!jogador.getAbilities().instabuild) {
                pilha.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
        }
        arremessar(mundo, jogador, pilha);
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    public static void arremessar(Level mundo, Player jogador, ItemStack pilha) {
        if (!mundo.isClientSide()) {
            PocaoArremessada pocao = new PocaoArremessada(mundo, jogador);
            pocao.setItem(pilha.copyWithCount(1));
            pocao.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), -10.0F, 0.9F, 1.0F);
            mundo.addFreshEntity(pocao);
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.5F);
        jogador.getCooldowns().addCooldown(pilha.getItem(), Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + tipo.nomeDoItem() + ".descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable(tipo == TipoDeInfusao.INSTAVEL ? "dica.pixeldungeon.infusao_instavel" : "dica.pixeldungeon.infusao")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
