package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.Grama;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Grama alta do esgoto (a "high grass" do Shattered). É a grama alta do Minecraft, só que maior:
// 3 blocos de altura e mais larga. Quando alguém encosta, ela é pisoteada (vira grama baixa)
// e às vezes solta uma gota de orvalho. A Caçadora passa sem pisotear (só deixa a grama "trilhada").
public class CapimAlto extends DoublePlantBlock {
    public static final MapCodec<CapimAlto> CODEC = simpleCodec(CapimAlto::new);
    // Já foi trilhada pela Caçadora (não dá mais nada, mas continua em pé)
    public static final BooleanProperty TRILHADO = BooleanProperty.create("trilhado");

    public CapimAlto(Properties propriedades) {
        super(propriedades);
        registerDefaultState(defaultBlockState().setValue(TRILHADO, false));
    }

    @Override
    public MapCodec<CapimAlto> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        super.createBlockStateDefinition(construtor);
        construtor.add(TRILHADO);
    }

    // Cresce em qualquer chão firme (no Minecraft, a grama só cresceria na terra)
    @Override
    protected boolean mayPlaceOn(BlockState estado, BlockGetter mundo, BlockPos posicao) {
        return estado.isFaceSturdy(mundo, posicao, Direction.UP);
    }

    // Encostou: pisoteia
    @Override
    protected void entityInside(BlockState estado, Level mundo, BlockPos posicao, Entity entidade) {
        if (mundo instanceof ServerLevel servidor) {
            Grama.pisotear(servidor, posicao, estado, entidade);
        }
    }
}
