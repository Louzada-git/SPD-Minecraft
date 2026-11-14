package com.louzada.pixeldungeon.bloco;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.registro.ModBlocos;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

// O fogo eterno do Shattered (a sala do fogo mágico): uma parede de fogo verde tão densa que não dá pra atravessar (igual ao
// Shattered), e que põe fogo em quem encosta. Não apaga sozinho e não dá pra apagar batendo.
// Água encostada ou gelo perto (poção de gelo, bomba de gelo, nevasca, gás congelante) apaga a parede de fogo inteira,
// igual ao Shattered.
public class FogoEterno extends BaseFireBlock {
    public static final MapCodec<FogoEterno> CODEC = simpleCodec(FogoEterno::new);
    // Uma parede de fogo de uma sala de até 10 quadradinhos (4 x 3 blocos cada)
    private static final int MAXIMO = 600;

    public FogoEterno(Properties propriedades) {
        super(propriedades, 1.0F);
    }

    @Override
    protected MapCodec<? extends BaseFireBlock> codec() {
        return CODEC;
    }

    // Quase um bloco inteiro: quem encosta fica "dentro" da beirada e pega fogo
    private static final VoxelShape FORMATO = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);

    @Override
    protected boolean canBurn(BlockState estado) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState estado, BlockGetter mundo, BlockPos posicao, @Nullable Mob mob) {
        return PathType.BLOCKED;
    }

    // Água do lado: apaga tudo
    @Override
    protected void neighborChanged(BlockState estado, Level mundo, BlockPos posicao, Block vizinho, BlockPos deOnde, boolean movido) {
        super.neighborChanged(estado, mundo, posicao, vizinho, deOnde, movido);
        if (mundo instanceof ServerLevel servidor && mundo.getFluidState(deOnde).is(FluidTags.WATER)) {
            apagar(servidor, posicao);
        }
    }

    // Apaga o fogo eterno que estiver a até "raio" blocos desse lugar (e, com ele, a parede inteira). Devolve se apagou algum.
    public static boolean apagarPerto(ServerLevel mundo, Vec3 centro, double raio) {
        int r = (int) Math.ceil(raio);
        BlockPos meio = BlockPos.containing(centro);
        for (BlockPos posicao : BlockPos.betweenClosed(meio.offset(-r, -r, -r), meio.offset(r, r, r))) {
            if (mundo.getBlockState(posicao).is(ModBlocos.FOGO_ETERNO.get()) && Vec3.atCenterOf(posicao).distanceTo(centro) <= raio + 0.5) {
                apagar(mundo, posicao.immutable());
                return true;
            }
        }
        return false;
    }

    // Apaga a parede de fogo inteira (todos os blocos de fogo eterno encostados, até na diagonal)
    public static void apagar(ServerLevel mundo, BlockPos inicio) {
        Set<BlockPos> vistos = new HashSet<>();
        Deque<BlockPos> fila = new ArrayDeque<>();
        fila.add(inicio.immutable());
        vistos.add(inicio.immutable());
        int apagados = 0;
        while (!fila.isEmpty() && apagados < MAXIMO) {
            BlockPos aqui = fila.poll();
            if (!mundo.getBlockState(aqui).is(ModBlocos.FOGO_ETERNO.get())) {
                continue;
            }
            mundo.setBlock(aqui, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            apagados++;
            if (apagados % 3 == 0) {
                mundo.sendParticles(ParticleTypes.LARGE_SMOKE, aqui.getX() + 0.5, aqui.getY() + 0.5, aqui.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos vizinho = aqui.offset(dx, dy, dz);
                        if (vistos.add(vizinho)) {
                            fila.add(vizinho);
                        }
                    }
                }
            }
        }
        if (apagados > 0) {
            mundo.playSound(null, inicio, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    // (o fogo comum some quando o bloco de baixo some; este fica, uma parede de 3 de altura)
    @Override
    protected BlockState updateShape(BlockState estado, Direction lado, BlockState vizinho, LevelAccessor mundo, BlockPos posicao,
            BlockPos posicaoDoVizinho) {
        return estado;
    }
}
