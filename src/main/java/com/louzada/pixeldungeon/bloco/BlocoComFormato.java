package com.louzada.pixeldungeon.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Bloco de decoração que não ocupa o bloco inteiro (lanterna, corrente, musgo...).
// O formato é o que o jogador "bate" e o contorno que aparece quando mira nele.
public class BlocoComFormato extends Block {
    // Formatos prontos (medidas em pixels, de 0 a 16)
    public static final VoxelShape FORMATO_LANTERNA_PENDURADA = Shapes.or(
            Block.box(5, 1, 5, 11, 8, 11),
            Block.box(6, 8, 6, 10, 10, 10));
    public static final VoxelShape FORMATO_CORRENTE = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
    public static final VoxelShape FORMATO_TAPETE = Block.box(0, 0, 0, 16, 1, 16);

    private VoxelShape formato;

    public BlocoComFormato(Properties propriedades, VoxelShape formato) {
        super(propriedades);
        this.formato = formato;
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return formato;
    }
}
