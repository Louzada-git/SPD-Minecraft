package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// O arbusto de fruta-sem-graça do Shattered (nos jardins e na despensa secreta): igual às outras plantas, vale pelo
// quadradinho inteiro; quem pisar nele colhe uma fruta-sem-graça (ela cai no chão) e o arbusto some.
public class ArbustoDeFruta extends PlantaDoShattered {

    public ArbustoDeFruta(Properties propriedades) {
        super(propriedades, null);
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        LivingEntity quemPisou = quemEstaNoQuadradinho(mundo, posicao);
        if (quemPisou != null) {
            colher(mundo, posicao);
            return;
        }
        mundo.scheduleTick(posicao, this, TICKS_ENTRE_OLHADAS);
    }

    public static void colher(ServerLevel mundo, BlockPos posicao) {
        mundo.setBlock(posicao, Blocks.AIR.defaultBlockState(), 3);
        SaqueDoAndar.soltar(mundo, posicao.getX() + 0.5, posicao.getY(), posicao.getZ() + 0.5, new ItemStack(ModItens.FRUTA_SEM_GRACA.get()));
        mundo.sendParticles(ParticleTypes.HAPPY_VILLAGER, posicao.getX() + 0.5, posicao.getY() + 0.5, posicao.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
        mundo.playSound(null, posicao, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
