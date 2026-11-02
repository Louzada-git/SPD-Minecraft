package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    // Clique direito na escada = subir (no andar 1 não deixa sair sem o Amuleto de Yendor)
    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (jogador instanceof ServerPlayer jogadorNoServidor) {
            GerenciadorDaRun.subir(jogadorNoServidor, Andares.andarPelaAltura(posicao.getY()));
        }
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }
}
