package com.louzada.pixeldungeon.bloco;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PlantasDoShattered;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Uma planta do Shattered no chão (nasce de uma semente).
// Igual ao Shattered, ela vale pelo quadradinho inteiro (4x4 blocos): quem pisar no quadradinho dela
// ativa o efeito e a planta some. Quem voa (o enxame, o herói levitando) não ativa.
public class PlantaDoShattered extends Block {
    private static final VoxelShape FORMATO = Block.box(2, 0, 2, 14, 12, 14);
    protected static final int TICKS_ENTRE_OLHADAS = 4;
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    private TipoDePlanta tipo;

    public PlantaDoShattered(Properties propriedades, TipoDePlanta tipo) {
        super(propriedades);
        this.tipo = tipo;
    }

    public TipoDePlanta tipo() {
        return tipo;
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos posicao, CollisionContext contexto) {
        return FORMATO;
    }

    // Só fica em cima de chão firme
    @Override
    protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos posicao) {
        return mundo.getBlockState(posicao.below()).isFaceSturdy(mundo, posicao.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState estado, Direction direcao, BlockState vizinho, LevelAccessor mundo, BlockPos posicao,
            BlockPos posicaoDoVizinho) {
        return direcao == Direction.DOWN && !canSurvive(estado, mundo, posicao) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(estado, direcao, vizinho, mundo, posicao, posicaoDoVizinho);
    }

    // Plantou: começa a olhar quem pisa
    @Override
    protected void onPlace(BlockState estado, Level mundo, BlockPos posicao, BlockState antes, boolean movendo) {
        super.onPlace(estado, mundo, posicao, antes, movendo);
        mundo.scheduleTick(posicao, this, TICKS_ENTRE_OLHADAS);
    }

    // De tempos em tempos vê se alguém está no quadradinho dela
    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        LivingEntity quemPisou = quemEstaNoQuadradinho(mundo, posicao);
        if (quemPisou != null) {
            PlantasDoShattered.ativar(mundo, posicao, tipo, quemPisou);
            return;
        }
        mundo.scheduleTick(posicao, this, TICKS_ENTRE_OLHADAS);
    }

    @Nullable
    protected static LivingEntity quemEstaNoQuadradinho(ServerLevel mundo, BlockPos posicao) {
        int inicioX = posicao.getX() - Math.floorMod(posicao.getX(), T);
        int inicioZ = posicao.getZ() - Math.floorMod(posicao.getZ(), T);
        AABB quadradinho = new AABB(inicioX, posicao.getY(), inicioZ, inicioX + T, posicao.getY() + 1.5, inicioZ + T);
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, quadradinho)) {
            if (!criatura.isAlive() || criatura.isSpectator() || criatura instanceof EnxameDeMoscas || criatura.hasEffect(ModEfeitos.LEVITANDO)
                    || (criatura instanceof Player jogador && (jogador.isCreative() || jogador.getAbilities().flying))) {
                continue;
            }
            return criatura;
        }
        return null;
    }

    // Usado pra saber se dá pra plantar aqui (ar, ou grama que sai do caminho). Igual ao Shattered,
    // não dá pra plantar em cima de armadilha (a semente cai no chão e ativa a armadilha).
    public static boolean daPraPlantar(Level mundo, BlockPos posicao) {
        BlockState aqui = mundo.getBlockState(posicao);
        boolean livre = aqui.isAir() || aqui.getBlock() instanceof GramaBaixa || aqui.getBlock() instanceof CapimAlto;
        BlockState chao = mundo.getBlockState(posicao.below());
        return livre && !(chao.getBlock() instanceof ArmadilhaBloco) && chao.isFaceSturdy(mundo, posicao.below(), Direction.UP);
    }
}
