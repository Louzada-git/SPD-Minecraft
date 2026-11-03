package com.louzada.pixeldungeon.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Grama baixa do esgoto (a grama pisoteada do Shattered): só enfeite, dá pra andar por cima
public class GramaBaixa extends Block {
    private static final VoxelShape FORMATO = Block.box(2, 0, 2, 14, 13, 14);

    public GramaBaixa(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }
}
