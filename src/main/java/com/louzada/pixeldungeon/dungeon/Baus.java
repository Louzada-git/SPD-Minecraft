package com.louzada.pixeldungeon.dungeon;

import java.util.List;

import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.item.ChaveDeCristal;
import com.louzada.pixeldungeon.registro.ModAnexos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Baús do Shattered: baús comuns do Minecraft com o item dentro.
//   - ninguém consegue quebrar um baú (só no criativo), senão dava pra levar o baú embora
//   - alguns são mímicos: por fora são iguaizinhos, mas quem tenta abrir (ou bate neles) leva uma mordida
//     e o mímico aparece pra lutar (ver Mimico)
public class Baus {
    // Os itens vão no meio do baú (o do meio é o espaço 13, o segundo o 14...)
    private static final int PRIMEIRO_ESPACO = 13;

    // Coloca um baú (ou um mímico disfarçado) com os itens dentro. Devolve se deu certo.
    public static boolean colocar(ServerLevel mundo, BlockPos lugar, Direction frente, List<ItemStack> itens, boolean mimico) {
        mundo.setBlock(lugar, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, frente), 3);
        if (!(mundo.getBlockEntity(lugar) instanceof ChestBlockEntity bau)) {
            return false;
        }
        for (int i = 0; i < itens.size(); i++) {
            bau.setItem(PRIMEIRO_ESPACO + i, itens.get(i).copy());
        }
        if (mimico) {
            bau.setData(ModAnexos.MIMICO.get(), true);
        }
        bau.setChanged();
        return true;
    }

    // O baú de cristal (o "CRYSTAL_CHEST" do Shattered): um baú com um cristal de ametista em cima. Dá pra ver o que tem dentro
    // (clicando), mas ele só abre com a chave de cristal do andar, que é gasta.
    public static boolean colocarDeCristal(ServerLevel mundo, BlockPos lugar, Direction frente, List<ItemStack> itens) {
        if (!colocar(mundo, lugar, frente, itens, false) || !(mundo.getBlockEntity(lugar) instanceof ChestBlockEntity bau)) {
            return false;
        }
        bau.setData(ModAnexos.BAU_DE_CRISTAL.get(), true);
        bau.setChanged();
        mundo.setBlock(lugar.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
        return true;
    }

    public static boolean ehDeCristal(ChestBlockEntity bau) {
        return bau.hasData(ModAnexos.BAU_DE_CRISTAL.get()) && bau.getData(ModAnexos.BAU_DE_CRISTAL.get());
    }

    // Abre o baú de cristal com a chave do andar (gasta a chave). Sem a chave, mostra o que tem dentro.
    public static boolean abrirDeCristal(ServerLevel mundo, BlockPos lugar, ChestBlockEntity bau, ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        ItemStack chave = ChaveDeCristal.procurar(jogador, run.numeroDaRun(), SaqueDoAndar.andarDe(jogador));
        if (chave.isEmpty()) {
            ItemStack dentro = ItemStack.EMPTY;
            for (int i = 0; i < bau.getContainerSize() && dentro.isEmpty(); i++) {
                dentro = bau.getItem(i);
            }
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bau_de_cristal", dentro.getHoverName())
                    .withStyle(ChatFormatting.AQUA), true);
            return false;
        }
        chave.shrink(1);
        bau.setData(ModAnexos.BAU_DE_CRISTAL.get(), false);
        bau.setChanged();
        if (mundo.getBlockState(lugar.above()).is(Blocks.AMETHYST_CLUSTER)) {
            mundo.destroyBlock(lugar.above(), false);
        }
        mundo.playSound(null, lugar, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bau_de_cristal_aberto").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    // A ossada (o "SKELETON" do Shattered): um crânio num canto do quadradinho, com os itens no chão
    public static void colocarOssada(ServerLevel mundo, BlockPos canto, RandomSource sorteio) {
        mundo.setBlock(canto, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, sorteio.nextInt(16)), 3);
    }

    public static boolean ehMimico(ChestBlockEntity bau) {
        return bau.hasData(ModAnexos.MIMICO.get()) && bau.getData(ModAnexos.MIMICO.get());
    }

    // Clique direito num baú: se for um mímico, ele não abre, morde
    public static void aoClicarComDireito(PlayerInteractEvent.RightClickBlock evento) {
        if (evento.getLevel() instanceof ServerLevel mundo && evento.getEntity() instanceof ServerPlayer jogador
                && mundo.getBlockEntity(evento.getPos()) instanceof ChestBlockEntity bau && ehMimico(bau)) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.SUCCESS);
            Mimico.revelar(mundo, evento.getPos(), bau, jogador);
        } else if (evento.getLevel() instanceof ServerLevel mundo && evento.getEntity() instanceof ServerPlayer jogador
                && mundo.getBlockEntity(evento.getPos()) instanceof ChestBlockEntity bau && ehDeCristal(bau) && !abrirDeCristal(mundo, evento.getPos(), bau, jogador)) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // Bater num baú: se for um mímico, ele acorda e morde
    public static void aoClicarComEsquerdo(PlayerInteractEvent.LeftClickBlock evento) {
        if (evento.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && evento.getLevel() instanceof ServerLevel mundo
                && evento.getEntity() instanceof ServerPlayer jogador && mundo.getBlockEntity(evento.getPos()) instanceof ChestBlockEntity bau
                && ehMimico(bau)) {
            evento.setCanceled(true);
            Mimico.revelar(mundo, evento.getPos(), bau, jogador);
        }
    }

    // Só no criativo dá pra quebrar um baú
    public static void aoQuebrar(PlayerEvent.BreakSpeed evento) {
        if ((evento.getState().is(Blocks.CHEST) || evento.getState().is(Blocks.AMETHYST_CLUSTER) || evento.getState().is(Blocks.SKELETON_SKULL))
                && !evento.getEntity().isCreative()) {
            evento.setCanceled(true);
        }
    }
}
