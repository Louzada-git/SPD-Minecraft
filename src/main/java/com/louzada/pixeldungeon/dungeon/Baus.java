package com.louzada.pixeldungeon.dungeon;

import java.util.List;

import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.registro.ModAnexos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
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
        if (evento.getState().is(Blocks.CHEST) && !evento.getEntity().isCreative()) {
            evento.setCanceled(true);
        }
    }
}
