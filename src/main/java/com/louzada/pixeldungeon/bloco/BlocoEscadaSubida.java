package com.louzada.pixeldungeon.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A escada que fica na entrada de cada andar e leva para o andar de cima.
public class BlocoEscadaSubida extends Block {
    // Formato de degrau: meia altura embaixo + metade de cima no lado leste (igual ao modelo de escada do Minecraft)
    private static final VoxelShape FORMATO = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(8, 8, 0, 16, 16, 16));

    public BlocoEscadaSubida(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }
}
