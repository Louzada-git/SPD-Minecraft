package com.louzada.pixeldungeon.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Parede (e teto) do esgoto. Igual ao Shattered, em cima das poças a água vai pingando do teto.
public class BlocoParedeEsgoto extends Block {
    // Só alguns pontos do teto têm goteira (1 em cada 11 blocos), senão vira chuva
    private static final int UM_EM_CADA = 11;
    // Altura máxima entre o teto e a poça
    private static final int DISTANCIA_ATE_A_AGUA = 10;

    public BlocoParedeEsgoto(Properties propriedades) {
        super(propriedades);
    }

    // Chamado só no jogo do jogador, de vez em quando, pros blocos perto dele
    @Override
    public void animateTick(BlockState estado, Level mundo, BlockPos posicao, RandomSource sorteio) {
        if (sorteio.nextInt(4) != 0 || Math.floorMod(Mth.getSeed(posicao), UM_EM_CADA) != 0) {
            return;
        }
        if (!mundo.getBlockState(posicao.below()).isAir()) {
            return;
        }
        // Só pinga se embaixo tiver água (o primeiro bloco que não é ar)
        for (int descida = 2; descida <= DISTANCIA_ATE_A_AGUA; descida++) {
            BlockPos embaixo = posicao.below(descida);
            if (mundo.getBlockState(embaixo).isAir()) {
                continue;
            }
            if (mundo.getFluidState(embaixo).is(FluidTags.WATER)) {
                ParticleUtils.spawnParticleBelow(mundo, posicao, sorteio, ParticleTypes.DRIPPING_WATER);
            }
            return;
        }
    }
}
