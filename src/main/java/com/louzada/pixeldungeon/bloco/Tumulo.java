package com.louzada.pixeldungeon.bloco;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.Espectro;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// O túmulo do Shattered (o "TOMB" do cemitério gramado): uma lápide com itens guardados dentro.
// Quem abre (clicando com o botão direito) pega o que tinha dentro, mas solta os espectros em volta de si (ver Espectro).
public class Tumulo extends BaseEntityBlock {
    public static final MapCodec<Tumulo> CODEC = simpleCodec(Tumulo::new);
    public static final DirectionProperty FRENTE = HorizontalDirectionalBlock.FACING;
    // A base e a pedra em pé (virada pro norte/sul ou pro leste/oeste)
    private static final VoxelShape FORMATO_NS = Shapes.or(Block.box(1, 0, 4, 15, 2, 12), Block.box(3, 2, 6, 13, 14, 10));
    private static final VoxelShape FORMATO_LO = Shapes.or(Block.box(4, 0, 1, 12, 2, 15), Block.box(6, 2, 3, 10, 14, 13));

    public Tumulo(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FRENTE, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FRENTE);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return estado.getValue(FRENTE).getAxis() == Direction.Axis.Z ? FORMATO_NS : FORMATO_LO;
    }

    @Override
    protected RenderShape getRenderShape(BlockState estado) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos posicao, BlockState estado) {
        return new TumuloEntidade(posicao, estado);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (mundo instanceof ServerLevel servidor && jogador instanceof ServerPlayer quem && !quem.isSpectator()) {
            abrir(servidor, posicao, quem);
        }
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }

    // Coloca um túmulo com os itens dentro. Devolve se deu certo.
    public static boolean colocar(ServerLevel mundo, BlockPos lugar, Direction frente, List<ItemStack> itens) {
        mundo.setBlock(lugar, ModBlocos.TUMULO.get().defaultBlockState().setValue(FRENTE, frente), 3);
        if (!(mundo.getBlockEntity(lugar) instanceof TumuloEntidade tumulo)) {
            return false;
        }
        tumulo.guardar(itens);
        return true;
    }

    // Abre o túmulo: a lápide some, os itens ficam no chão e os espectros aparecem em volta de quem abriu (igual ao Shattered)
    public static void abrir(ServerLevel mundo, BlockPos lugar, ServerPlayer quem) {
        List<ItemStack> itens = mundo.getBlockEntity(lugar) instanceof TumuloEntidade tumulo ? tumulo.tirarTudo() : List.of();
        mundo.setBlock(lugar, Blocks.AIR.defaultBlockState(), 3);
        for (ItemStack item : itens) {
            SaqueDoAndar.soltar(mundo, lugar.getX() + 0.5, lugar.getY() + 0.1, lugar.getZ() + 0.5, item);
        }
        Particulas.explosao(mundo, ParticleTypes.SMOKE, lugar.getX() + 0.5, lugar.getY() + 0.5, lugar.getZ() + 0.5, 15, 0.4, 0.02);
        mundo.playSound(null, lugar, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        Espectro.criarEmVolta(mundo, quem.position(), SaqueDoAndar.andarDe(quem));
    }
}
