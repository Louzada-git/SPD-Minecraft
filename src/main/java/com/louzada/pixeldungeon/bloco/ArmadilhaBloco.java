package com.louzada.pixeldungeon.bloco;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;

// Uma armadilha do Shattered no chão. Igual às plantas, ela vale pelo quadradinho inteiro: são 16 blocos de chão (4x4),
// cada um com um pedaço do desenho da armadilha (COLUNA e LINHA dizem qual pedaço).
//   escondida: parece o chão comum (até a textura é a mesma) até alguém achar
//   desarmada: já foi ativada (fica com o desenho preto)
// Só o pedaço da coluna 0, linha 0 (o "controle") olha quem pisa, de 2 em 2 ticks (ver Armadilhas.olhar).
// Os inimigos desviam das armadilhas que estão à vista. Clique direito numa armadilha à vista: o nome e o que ela faz.
public class ArmadilhaBloco extends Block {
    public static final EnumProperty<TipoDeArmadilha> TIPO = EnumProperty.create("tipo", TipoDeArmadilha.class);
    public static final BooleanProperty ESCONDIDA = BooleanProperty.create("escondida");
    public static final BooleanProperty DESARMADA = BooleanProperty.create("desarmada");
    public static final IntegerProperty COLUNA = IntegerProperty.create("coluna", 0, Andares.TAMANHO_DO_QUADRADINHO - 1);
    public static final IntegerProperty LINHA = IntegerProperty.create("linha", 0, Andares.TAMANHO_DO_QUADRADINHO - 1);
    public static final int INTERVALO = 2;

    public ArmadilhaBloco(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(TIPO, TipoDeArmadilha.DARDOS).setValue(ESCONDIDA, false)
                .setValue(DESARMADA, false).setValue(COLUNA, 0).setValue(LINHA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(TIPO, ESCONDIDA, DESARMADA, COLUNA, LINHA);
    }

    // O pedaço (coluna, linha) de uma armadilha
    public BlockState pedaco(TipoDeArmadilha tipo, boolean escondida, boolean desarmada, int coluna, int linha) {
        return defaultBlockState().setValue(TIPO, tipo).setValue(ESCONDIDA, escondida).setValue(DESARMADA, desarmada)
                .setValue(COLUNA, coluna).setValue(LINHA, linha);
    }

    public static boolean ehControle(BlockState estado) {
        return estado.getValue(COLUNA) == 0 && estado.getValue(LINHA) == 0;
    }

    // Onde fica o controle da armadilha deste pedaço
    public static BlockPos controleDe(BlockPos posicao, BlockState estado) {
        return posicao.offset(-estado.getValue(COLUNA), 0, -estado.getValue(LINHA));
    }

    // Armada: ainda pode ser ativada
    public static boolean armada(BlockState estado) {
        return estado.getBlock() instanceof ArmadilhaBloco && !estado.getValue(DESARMADA);
    }

    @Override
    protected void onPlace(BlockState estado, Level mundo, BlockPos posicao, BlockState antes, boolean movido) {
        if (!mundo.isClientSide() && ehControle(estado) && !estado.getValue(DESARMADA)) {
            mundo.scheduleTick(posicao, this, INTERVALO);
        }
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        if (!ehControle(estado) || estado.getValue(DESARMADA)) {
            Armadilhas.esquecer(posicao);
            return;
        }
        Armadilhas.olhar(mundo, posicao);
        if (armada(mundo.getBlockState(posicao))) {
            mundo.scheduleTick(posicao, this, INTERVALO);
        }
    }

    // Os inimigos desviam das armadilhas que eles enxergam (no Shattered, só pisam se não tiver outro caminho)
    @Override
    public @Nullable PathType getBlockPathType(BlockState estado, BlockGetter mundo, BlockPos posicao, @Nullable Mob mob) {
        return !estado.getValue(ESCONDIDA) && !estado.getValue(DESARMADA) ? PathType.DAMAGE_OTHER : null;
    }

    // Clique direito numa armadilha à vista: o nome e a descrição dela
    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (estado.getValue(ESCONDIDA)) {
            return InteractionResult.PASS;
        }
        if (!mundo.isClientSide()) {
            Armadilhas.descrever(jogador, estado);
        }
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }
}
