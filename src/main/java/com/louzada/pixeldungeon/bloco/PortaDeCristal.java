package com.louzada.pixeldungeon.bloco;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.item.ChaveDeCristal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;

// A porta de cristal do Shattered: dá pra ver através dela, mas só abre com a chave de cristal do andar (que é gasta).
// Clique direito com a chave no inventário: a porta inteira some.
public class PortaDeCristal extends Block {
    // O tamanho máximo de uma porta (4 x 6 blocos), pra a procura não sair andando
    private static final int MAXIMO = 32;

    public PortaDeCristal(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos posicao, Player jogador, BlockHitResult acerto) {
        if (!(mundo instanceof ServerLevel servidor) || !(jogador instanceof ServerPlayer heroi)) {
            return InteractionResult.SUCCESS;
        }
        abrir(servidor, posicao, heroi);
        return InteractionResult.CONSUME;
    }

    // Abre (gastando a chave) se o jogador tiver a chave de cristal do andar; senão, avisa
    public static boolean abrir(ServerLevel mundo, BlockPos posicao, ServerPlayer jogador) {
        int andar = Andares.andarPelaAltura(posicao.getY());
        int run = Math.floorDiv(posicao.getX(), Andares.DISTANCIA_ENTRE_RUNS);
        ItemStack chave = ChaveDeCristal.procurar(jogador, run, andar);
        if (chave.isEmpty()) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.porta_de_cristal").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        chave.shrink(1);
        for (BlockPos pedaco : pedacos(mundo, posicao)) {
            mundo.setBlock(pedaco, Blocks.AIR.defaultBlockState(), 3);
            mundo.sendParticles(ParticleTypes.END_ROD, pedaco.getX() + 0.5, pedaco.getY() + 0.5, pedaco.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.01);
        }
        mundo.playSound(null, posicao, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        mundo.playSound(null, posicao, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.porta_de_cristal_aberta").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    // Todos os blocos dessa porta (os de cristal encostados uns nos outros)
    public static List<BlockPos> pedacos(Level mundo, BlockPos inicio) {
        List<BlockPos> lista = new ArrayList<>();
        Set<BlockPos> vistos = new HashSet<>();
        Deque<BlockPos> fila = new ArrayDeque<>();
        fila.add(inicio.immutable());
        vistos.add(inicio.immutable());
        while (!fila.isEmpty() && lista.size() < MAXIMO) {
            BlockPos aqui = fila.poll();
            if (!(mundo.getBlockState(aqui).getBlock() instanceof PortaDeCristal)) {
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

    // Vidro: os blocos vizinhos iguais não desenham a face entre eles
    @Override
    protected boolean skipRendering(BlockState estado, BlockState vizinho, Direction lado) {
        return vizinho.is(this) || super.skipRendering(estado, vizinho, lado);
    }

    @Override
    protected float getShadeBrightness(BlockState estado, BlockGetter mundo, BlockPos posicao) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState estado, BlockGetter mundo, BlockPos posicao) {
        return true;
    }
}
