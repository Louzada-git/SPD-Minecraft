package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.BombaArremessada;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Uma bomba do Shattered (ver TipoDeBomba e EfeitosDasBombas).
//   clique direito: acende o pavio e arremessa (explode 2 turnos depois de cair; clique direito nela apaga o pavio)
//   agachado: arremessa sem acender (ela só cai no chão)
public class ItemBomba extends Item {
    private TipoDeBomba tipo;

    public ItemBomba(Properties propriedades, TipoDeBomba tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDeBomba tipo() {
        return tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        boolean acesa = !jogador.isShiftKeyDown();
        if (!mundo.isClientSide()) {
            BombaArremessada bomba = new BombaArremessada(mundo, jogador, acesa);
            bomba.setItem(pilha.copyWithCount(1));
            bomba.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 1.3F, 0.5F);
            mundo.addFreshEntity(bomba);
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), acesa ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, 1.0F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + tipo.id() + ".descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.bomba").withStyle(ChatFormatting.DARK_GRAY));
    }
}
