package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

// O respiradouro de gás tóxico do Shattered (a sala de gás tóxico): uma grade no chão (4x4 blocos) que solta gás tóxico sem parar.
// O bloco do canto (o "controle") solta uma nuvem no quadradinho a cada 2 turnos, enquanto tiver alguém por perto.
public class Respiradouro extends Block {
    public static final BooleanProperty CONTROLE = BooleanProperty.create("controle");
    private static final int INTERVALO = 2 * Turno.TICKS;
    private static final double ALGUEM_PERTO = 40;

    public Respiradouro(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(CONTROLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(CONTROLE);
    }

    @Override
    protected void onPlace(BlockState estado, Level mundo, BlockPos posicao, BlockState antes, boolean movido) {
        if (!mundo.isClientSide() && estado.getValue(CONTROLE)) {
            mundo.scheduleTick(posicao, this, 5);
        }
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        if (!estado.getValue(CONTROLE)) {
            return;
        }
        Vec3 meio = meio(posicao);
        if (mundo.getNearestPlayer(meio.x, meio.y, meio.z, ALGUEM_PERTO, false) != null) {
            soltarGas(mundo, posicao);
        }
        mundo.scheduleTick(posicao, this, INTERVALO);
    }

    // O meio do quadradinho desse controle, um pouco acima do chão
    private static Vec3 meio(BlockPos controle) {
        return new Vec3(controle.getX() + Andares.TAMANHO_DO_QUADRADINHO / 2.0, controle.getY() + 1, controle.getZ() + Andares.TAMANHO_DO_QUADRADINHO / 2.0);
    }

    // Uma nuvem de gás tóxico no quadradinho do respiradouro (dura 3 turnos; sai outra a cada 2)
    public static NuvemDeGas soltarGas(ServerLevel mundo, BlockPos controle) {
        return NuvemDeGas.soltar(mundo, meio(controle), NuvemDeGas.Tipo.TOXICO, 4.5F, 3);
    }
}
