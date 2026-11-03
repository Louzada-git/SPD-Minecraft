package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.bloco.PlantaDoShattered;
import com.louzada.pixeldungeon.entidade.SementeArremessada;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// Semente de uma planta do Shattered.
//   clique direito no chão: planta ali (a planta vale pelo quadradinho inteiro)
//   clique direito no ar: arremessa (a planta nasce onde a semente cair)
public class ItemSemente extends Item {
    private static final int TURNO = 20;
    private TipoDePlanta planta;

    public ItemSemente(Properties propriedades, TipoDePlanta planta) {
        super(propriedades);
        this.planta = planta;
    }

    public TipoDePlanta planta() {
        return planta;
    }

    public static Item daPlanta(TipoDePlanta planta) {
        return ModItens.SEMENTES.get(planta.ordinal()).get();
    }

    // Planta a planta desse tipo aqui (se der). Usado pela semente e pela semente arremessada.
    public static boolean plantar(Level mundo, BlockPos posicao, TipoDePlanta planta) {
        if (!PlantaDoShattered.daPraPlantar(mundo, posicao)) {
            return false;
        }
        // A grama alta tem duas partes: tira a de cima também
        if (mundo.getBlockState(posicao.above()).is(ModBlocos.CAPIM_ALTO.get())) {
            mundo.removeBlock(posicao.above(), false);
        }
        mundo.setBlock(posicao, ModBlocos.PLANTAS.get(planta.ordinal()).get().defaultBlockState(), 3);
        mundo.playSound(null, posicao, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        Level mundo = contexto.getLevel();
        BlockPos clicado = contexto.getClickedPos();
        BlockPos lugar = PlantaDoShattered.daPraPlantar(mundo, clicado) ? clicado : clicado.relative(contexto.getClickedFace());
        if (mundo.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!plantar(mundo, lugar, planta)) {
            return InteractionResult.FAIL;
        }
        Player jogador = contexto.getPlayer();
        if (jogador != null) {
            jogador.getCooldowns().addCooldown(this, TURNO);
            if (!jogador.getAbilities().instabuild) {
                contexto.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!mundo.isClientSide()) {
            SementeArremessada semente = new SementeArremessada(mundo, jogador);
            semente.setItem(pilha.copyWithCount(1));
            semente.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), -10.0F, 0.9F, 1.0F);
            mundo.addFreshEntity(semente);
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 1.2F);
        jogador.getCooldowns().addCooldown(this, TURNO);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "planta.pixeldungeon." + planta.id() + ".descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.semente_como_usar").withStyle(ChatFormatting.DARK_GRAY));
    }
}
