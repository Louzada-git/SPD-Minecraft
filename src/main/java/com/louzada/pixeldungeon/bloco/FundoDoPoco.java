package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.dungeon.Pocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// O fundo do poço mágico (embaixo da água, no meio do quadradinho): diz se a água é da saúde ou da consciência.
// O bloco do canto (o "controle") olha de tempos em tempos se alguém entrou na água (ou se jogaram um item nela).
public class FundoDoPoco extends Block {
    public static final BooleanProperty SAUDE = BooleanProperty.create("saude");
    public static final BooleanProperty CONTROLE = BooleanProperty.create("controle");
    private static final int INTERVALO = 10;

    public FundoDoPoco(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(SAUDE, true).setValue(CONTROLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(SAUDE, CONTROLE);
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        if (!estado.getValue(CONTROLE)) {
            return;
        }
        if (!Pocos.olhar(mundo, posicao, estado.getValue(SAUDE))) {
            mundo.scheduleTick(posicao, this, INTERVALO);
        }
    }

    @Override
    public void animateTick(BlockState estado, Level mundo, BlockPos posicao, RandomSource sorteio) {
        if (sorteio.nextInt(4) == 0) {
            mundo.addParticle(estado.getValue(SAUDE) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.GLOW, posicao.getX() + sorteio.nextDouble(),
                    posicao.getY() + 2.0, posicao.getZ() + sorteio.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
