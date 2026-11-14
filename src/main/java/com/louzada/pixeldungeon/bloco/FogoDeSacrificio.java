package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.Sacrificio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// O fogo de sacrifício do Shattered (a sala do sacrifício): um fogo no altar que não queima ninguém. Uma vez por segundo ele
// marca quem estiver perto (no quadradinho dele ou nos 8 em volta); quem morrer marcado é sacrificado (ver Sacrificio).
public class FogoDeSacrificio extends Block {
    public static final BooleanProperty CONTROLE = BooleanProperty.create("controle");
    private static final VoxelShape FORMATO = Block.box(0, 0, 0, 16, 1, 16);

    public FogoDeSacrificio(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(CONTROLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(CONTROLE);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return Shapes.empty();
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        if (!estado.getValue(CONTROLE)) {
            return;
        }
        Sacrificio.marcarEmVolta(mundo, posicao);
        mundo.scheduleTick(posicao, this, 20);
    }

    @Override
    public void animateTick(BlockState estado, Level mundo, BlockPos posicao, RandomSource sorteio) {
        if (sorteio.nextInt(3) == 0) {
            mundo.addParticle(ParticleTypes.SOUL_FIRE_FLAME, posicao.getX() + sorteio.nextDouble(), posicao.getY() + 0.3, posicao.getZ() + sorteio.nextDouble(),
                    0.0, 0.03, 0.0);
        }
        if (sorteio.nextInt(30) == 0) {
            mundo.playLocalSound(posicao.getX() + 0.5, posicao.getY() + 0.5, posicao.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F,
                    0.7F, false);
        }
    }
}
