package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;
import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// O pote de alquimia do Shattered: um caldeirão de barro de 2x2 blocos no meio do quadradinho (cada bloco é um quarto
// dele; PARTE diz qual: 0 = noroeste, 1 = nordeste, 2 = sudeste, 3 = sudoeste). Clique direito em qualquer pedaço
// abre a alquimia (ver MenuDaAlquimia). Às vezes sobe uma bolha.
public class PoteDeAlquimia extends Block {
    public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 3);
    // O quarto do noroeste (os outros são ele girado), igual ao modelo: o corpo cheio até o caldo (arredondado no canto)
    // e a borda em volta, mais alta
    private static final VoxelShape[] FORMAS = new VoxelShape[4];
    static {
        for (int parte = 0; parte < 4; parte++) {
            FORMAS[parte] = Shapes.or(girar(parte, 1, 0, 7, 16, 10, 16), girar(parte, 3, 0, 3, 16, 10, 16), girar(parte, 7, 0, 1, 16, 10, 16),
                    girar(parte, 1, 10, 7, 3, 14, 16), girar(parte, 3, 10, 3, 5, 14, 7), girar(parte, 5, 10, 3, 7, 14, 5),
                    girar(parte, 7, 10, 1, 16, 14, 3));
        }
    }

    public PoteDeAlquimia(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(PARTE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(PARTE);
    }

    // O deslocamento (dx, dz) de cada parte a partir do canto noroeste do pote
    public static int dx(int parte) {
        return parte == 1 || parte == 2 ? 1 : 0;
    }

    public static int dz(int parte) {
        return parte >= 2 ? 1 : 0;
    }

    // Põe o pote inteiro (os 4 quartos) com o canto noroeste em "canto"
    public static void colocar(Level mundo, BlockPos canto) {
        BlockState estado = ModBlocos.POTE_DE_ALQUIMIA.get().defaultBlockState();
        for (int parte = 0; parte < 4; parte++) {
            mundo.setBlockAndUpdate(canto.offset(dx(parte), 0, dz(parte)), estado.setValue(PARTE, parte));
        }
    }

    // O canto noroeste do pote (de onde a alquimia sabe onde ele está)
    public static BlockPos cantoDe(BlockPos posicao, BlockState estado) {
        int parte = estado.getValue(PARTE);
        return posicao.offset(-dx(parte), 0, -dz(parte));
    }

    // Uma caixa do quarto noroeste, girada pra parte (de 90 em 90 graus, no sentido do relógio visto de cima)
    private static VoxelShape girar(int parte, double x0, double y0, double z0, double x1, double y1, double z1) {
        for (int i = 0; i < parte; i++) {
            double nx0 = 16 - z1;
            double nx1 = 16 - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
        }
        return Block.box(x0, y0, z0, x1, y1, z1);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMAS[estado.getValue(PARTE)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (!mundo.isClientSide() && jogador instanceof ServerPlayer heroi) {
            abrir(heroi, mundo, cantoDe(posicao, estado));
        }
        return InteractionResult.sidedSuccess(mundo.isClientSide());
    }

    // Abre a alquimia desse pote
    public static void abrir(ServerPlayer heroi, Level mundo, BlockPos canto) {
        heroi.openMenu(new SimpleMenuProvider((id, inventario, quem) -> new MenuDaAlquimia(id, inventario, ContainerLevelAccess.create(mundo, canto)),
                Component.translatable("tela.pixeldungeon.alquimia")));
    }

    // De vez em quando sobe uma bolha do caldo
    @Override
    public void animateTick(BlockState estado, Level mundo, BlockPos posicao, RandomSource sorteio) {
        if (sorteio.nextInt(4) == 0) {
            int parte = estado.getValue(PARTE);
            double x = posicao.getX() + (dx(parte) == 0 ? 0.5 + sorteio.nextDouble() * 0.5 : sorteio.nextDouble() * 0.5);
            double z = posicao.getZ() + (dz(parte) == 0 ? 0.5 + sorteio.nextDouble() * 0.5 : sorteio.nextDouble() * 0.5);
            mundo.addParticle(ParticleTypes.BUBBLE_POP, x, posicao.getY() + 0.65, z, 0.0, 0.02, 0.0);
            if (sorteio.nextInt(3) == 0) {
                mundo.addParticle(ParticleTypes.WITCH, x, posicao.getY() + 0.7, z, 0.0, 0.0, 0.0);
            }
        }
    }
}
