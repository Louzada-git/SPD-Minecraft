package com.louzada.pixeldungeon.bloco;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Abismo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

// O fundo do abismo (o "CHASM" do Shattered): o escuro lá embaixo do buraco. Não segura ninguém: quem (ou o que) chega aqui
// cai pro andar de baixo (ver Abismo). Os inimigos não andam pra cá.
public class FundoDoAbismo extends Block {

    public FundoDoAbismo(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected void entityInside(BlockState estado, Level mundo, BlockPos posicao, Entity entidade) {
        if (mundo instanceof ServerLevel servidor) {
            Abismo.cair(servidor, entidade, posicao);
        }
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState estado, BlockGetter mundo, BlockPos posicao, @Nullable Mob mob) {
        return PathType.LAVA;
    }
}
