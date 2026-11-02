package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// O alçapão no chão da saída de cada andar, que leva para o andar de baixo.
public class BlocoEscadaDescida extends Block {
    public BlocoEscadaDescida(Properties propriedades) {
        super(propriedades);
    }

    // Clique direito no alçapão = descer
    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (jogador instanceof ServerPlayer jogadorNoServidor) {
            GerenciadorDaRun.descer(jogadorNoServidor, Andares.andarPelaAltura(posicao.getY()));
        }
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }
}
