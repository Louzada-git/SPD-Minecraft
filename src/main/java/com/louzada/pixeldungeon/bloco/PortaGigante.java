package com.louzada.pixeldungeon.bloco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.item.ChaveDeFerro;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// As portas do Shattered, gigantes: cada porta tem 4 blocos de largura (um quadradinho inteiro) e 6 de altura.
// Cada bloco é um pedaço do desenho (a coluna e a linha dele). O pedaço de baixo da esquerda é o "controle":
// de tempos em tempos ele olha se tem alguém perto e abre ou fecha a porta inteira.
//   - aberta, a porta some (dá pra passar e ver através); fechada, bloqueia a passagem e a visão
//   - os inimigos andam pelas portas (elas abrem quando eles chegam perto), igual ao Shattered
//   - a trancada só abre pra quem tem a chave de ferro do andar (a chave é gasta), e aí vira uma porta comum
public class PortaGigante extends Block {
    public static final int LARGURA = 4;
    public static final int ALTURA = 6;
    // De quantos em quantos ticks o controle olha em volta
    private static final int INTERVALO = 4;
    // Até onde (em blocos, na direção de passar) alguém faz a porta abrir
    private static final double ALCANCE = 3.5;
    // Tempo mínimo entre os avisos de "trancada" pro mesmo jogador
    private static final long TEMPO_ENTRE_AVISOS = 60;

    public static final IntegerProperty COLUNA = IntegerProperty.create("coluna", 0, LARGURA - 1);
    public static final IntegerProperty LINHA = IntegerProperty.create("linha", 0, ALTURA - 1);
    // A direção em que se atravessa a porta (x ou z)
    public static final EnumProperty<Direction.Axis> EIXO = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty ABERTA = BlockStateProperties.OPEN;
    public static final BooleanProperty TRANCADA = BooleanProperty.create("trancada");

    private static final Map<UUID, Long> ultimoAviso = new HashMap<>();

    public PortaGigante(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(COLUNA, 0).setValue(LINHA, 0).setValue(EIXO, Direction.Axis.X).setValue(ABERTA, false)
                .setValue(TRANCADA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(COLUNA, LINHA, EIXO, ABERTA, TRANCADA);
    }

    // O pedaço (coluna, linha) de uma porta atravessada no eixo
    public BlockState pedaco(Direction.Axis eixo, int coluna, int linha, boolean trancada) {
        return defaultBlockState().setValue(EIXO, eixo).setValue(COLUNA, coluna).setValue(LINHA, linha).setValue(TRANCADA, trancada);
    }

    // As colunas ficam de lado em relação à direção de passar
    public static Direction lado(Direction.Axis eixo) {
        return eixo == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }

    // ---- Forma ----

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return estado.getValue(ABERTA) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState estado) {
        return estado.getValue(ABERTA) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState estado, PathComputationType tipo) {
        return estado.getValue(ABERTA) || !estado.getValue(TRANCADA);
    }

    // Os inimigos tratam a porta comum como aberta (ela abre quando eles chegam); a trancada é parede
    @Override
    public @Nullable PathType getBlockPathType(BlockState estado, BlockGetter mundo, BlockPos posicao, @Nullable Mob mob) {
        return estado.getValue(TRANCADA) && !estado.getValue(ABERTA) ? PathType.BLOCKED : PathType.DOOR_OPEN;
    }

    // ---- O controle ----

    public static boolean ehControle(BlockState estado) {
        return estado.getValue(COLUNA) == 0 && estado.getValue(LINHA) == 0;
    }

    @Override
    protected void onPlace(BlockState estado, Level mundo, BlockPos posicao, BlockState antes, boolean movido) {
        if (!mundo.isClientSide() && ehControle(estado) && !antes.is(this)) {
            mundo.scheduleTick(posicao, this, INTERVALO);
        }
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        if (!ehControle(estado)) {
            return;
        }
        mundo.scheduleTick(posicao, this, INTERVALO);
        Direction.Axis eixo = estado.getValue(EIXO);
        List<LivingEntity> perto = mundo.getEntitiesOfClass(LivingEntity.class, areaDeAbrir(posicao, eixo),
                criatura -> criatura.isAlive() && !criatura.isSpectator());

        boolean trancada = estado.getValue(TRANCADA);
        if (trancada) {
            if (!tentarDestrancar(mundo, posicao, perto)) {
                return;
            }
            trancada = false;
        }
        boolean abrir = !perto.isEmpty();
        if (abrir != estado.getValue(ABERTA)) {
            mudar(mundo, posicao, eixo, abrir, trancada);
            mundo.playSound(null, posicao.relative(lado(eixo), LARGURA / 2).above(), abrir ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    // A área em volta da porta: os 4 blocos de largura, os 6 de altura e um pouco pra frente e pra trás
    private static AABB areaDeAbrir(BlockPos controle, Direction.Axis eixo) {
        Direction lado = lado(eixo);
        BlockPos outroCanto = controle.relative(lado, LARGURA - 1).above(ALTURA - 1);
        AABB area = new AABB(controle.getX(), controle.getY(), controle.getZ(), outroCanto.getX() + 1, outroCanto.getY() + 1, outroCanto.getZ() + 1);
        return eixo == Direction.Axis.X ? area.inflate(ALCANCE, 0, 0) : area.inflate(0, 0, ALCANCE);
    }

    // Quem chega perto com a chave do andar destranca a porta (e gasta a chave). Sem a chave, um aviso.
    private boolean tentarDestrancar(ServerLevel mundo, BlockPos posicao, List<LivingEntity> perto) {
        for (LivingEntity criatura : perto) {
            if (!(criatura instanceof ServerPlayer jogador)) {
                continue;
            }
            if (usarChave(mundo, posicao, jogador)) {
                return true;
            }
            long agora = mundo.getGameTime();
            Long antes = ultimoAviso.get(jogador.getUUID());
            if (antes == null || agora - antes >= TEMPO_ENTRE_AVISOS) {
                ultimoAviso.put(jogador.getUUID(), agora);
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.porta_trancada").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return false;
    }

    // Se o jogador tem a chave de ferro do andar (e da run) dessa porta, gasta a chave e destranca a porta inteira
    public boolean usarChave(ServerLevel mundo, BlockPos controle, ServerPlayer jogador) {
        BlockState estado = mundo.getBlockState(controle);
        if (!estado.is(this) || !estado.getValue(TRANCADA)) {
            return false;
        }
        int andar = Andares.andarPelaAltura(controle.getY());
        int run = Math.floorDiv(controle.getX(), Andares.DISTANCIA_ENTRE_RUNS);
        ItemStack chave = ChaveDeFerro.procurar(jogador, run, andar);
        if (chave.isEmpty()) {
            return false;
        }
        chave.shrink(1);
        mudar(mundo, controle, estado.getValue(EIXO), estado.getValue(ABERTA), false);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.porta_destrancada").withStyle(ChatFormatting.YELLOW), true);
        mundo.playSound(null, controle, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    // Muda a porta inteira (os 24 pedaços)
    private void mudar(ServerLevel mundo, BlockPos controle, Direction.Axis eixo, boolean aberta, boolean trancada) {
        Direction lado = lado(eixo);
        for (int coluna = 0; coluna < LARGURA; coluna++) {
            for (int linha = 0; linha < ALTURA; linha++) {
                BlockPos aqui = controle.relative(lado, coluna).above(linha);
                BlockState estado = mundo.getBlockState(aqui);
                if (estado.is(this)) {
                    mundo.setBlock(aqui, estado.setValue(ABERTA, aberta).setValue(TRANCADA, trancada), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
