package com.louzada.pixeldungeon.bloco;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;

// A barricada de madeira do Shattered (a porta do depósito): não abre, mas está seca há anos. Encostou fogo, ela inteira queima
// (o andar com o depósito ganha uma poção de chama líquida).
public class Barricada extends Block {
    private static final int MAXIMO = 32;

    public Barricada(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.barricada").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }

    // Fogo do lado: queima tudo
    @Override
    protected void neighborChanged(BlockState estado, Level mundo, BlockPos posicao, Block vizinho, BlockPos deOnde, boolean movido) {
        super.neighborChanged(estado, mundo, posicao, vizinho, deOnde, movido);
        if (mundo instanceof ServerLevel servidor && mundo.getBlockState(deOnde).getBlock() instanceof BaseFireBlock) {
            queimar(servidor, posicao);
        }
    }

    @Override
    public void onCaughtFire(BlockState estado, Level mundo, BlockPos posicao, @Nullable Direction lado, @Nullable LivingEntity quem) {
        if (mundo instanceof ServerLevel servidor) {
            queimar(servidor, posicao);
        }
    }

    @Override
    public boolean isFlammable(BlockState estado, BlockGetter mundo, BlockPos posicao, Direction lado) {
        return true;
    }

    @Override
    public int getFlammability(BlockState estado, BlockGetter mundo, BlockPos posicao, Direction lado) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState estado, BlockGetter mundo, BlockPos posicao, Direction lado) {
        return 30;
    }

    // A barricada inteira vira fogo (no chão) e fumaça
    public static void queimar(ServerLevel mundo, BlockPos inicio) {
        List<BlockPos> pedacos = pedacos(mundo, inicio);
        for (BlockPos pedaco : pedacos) {
            mundo.setBlock(pedaco, Blocks.AIR.defaultBlockState(), 3);
            mundo.sendParticles(ParticleTypes.LARGE_SMOKE, pedaco.getX() + 0.5, pedaco.getY() + 0.5, pedaco.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
            mundo.sendParticles(ParticleTypes.FLAME, pedaco.getX() + 0.5, pedaco.getY() + 0.5, pedaco.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        }
        for (BlockPos pedaco : pedacos) {
            if (mundo.getBlockState(pedaco.below()).isFaceSturdy(mundo, pedaco.below(), Direction.UP) && mundo.isEmptyBlock(pedaco)) {
                mundo.setBlock(pedaco, BaseFireBlock.getState(mundo, pedaco), 3);
            }
        }
        if (!pedacos.isEmpty()) {
            mundo.playSound(null, inicio, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    private static List<BlockPos> pedacos(Level mundo, BlockPos inicio) {
        List<BlockPos> lista = new ArrayList<>();
        Set<BlockPos> vistos = new HashSet<>();
        Deque<BlockPos> fila = new ArrayDeque<>();
        fila.add(inicio.immutable());
        vistos.add(inicio.immutable());
        while (!fila.isEmpty() && lista.size() < MAXIMO) {
            BlockPos aqui = fila.poll();
            if (!(mundo.getBlockState(aqui).getBlock() instanceof Barricada)) {
                continue;
            }
            lista.add(aqui);
            for (Direction lado : Direction.values()) {
                BlockPos vizinho = aqui.relative(lado);
                if (vistos.add(vizinho)) {
                    fila.add(vizinho);
                }
            }
        }
        return lista;
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState estado, BlockGetter mundo, BlockPos posicao, @Nullable Mob mob) {
        return PathType.BLOCKED;
    }
}
