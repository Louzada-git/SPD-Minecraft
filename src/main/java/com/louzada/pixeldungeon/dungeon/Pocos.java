package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.item.EfeitosDePocao;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

// Os poços mágicos do Shattered (a sala do poço mágico): entrar na água bebe dela, uma vez só, e o poço seca.
//   saúde: cura tudo (vida e efeitos ruins), tira as maldições do que está equipado e mata a fome
//   consciência: identifica o que está equipado, mostra se o resto do equipamento é amaldiçoado e revela as portas
//     escondidas e as armadilhas do andar
// Jogar um item na água da consciência identifica o item (e o poço seca); na da saúde, nada acontece.
public final class Pocos {
    private Pocos() {
    }

    // Olha a água do poço desse controle; devolve se o poço foi usado (aí ele para de olhar)
    public static boolean olhar(ServerLevel mundo, BlockPos controle, boolean saude) {
        if (!mundo.getBlockState(controle.above()).is(Blocks.WATER)) {
            return true;
        }
        AABB agua = new AABB(controle.getX(), controle.getY() + 1, controle.getZ(), controle.getX() + 2, controle.getY() + 3, controle.getZ() + 2);
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, agua)) {
            if (jogador.isSpectator() || !jogador.isAlive()) {
                continue;
            }
            if (saude) {
                beberSaude(jogador);
            } else {
                beberConsciencia(jogador);
            }
            secar(mundo, controle);
            return true;
        }
        if (!saude) {
            for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, agua)) {
                ServerPlayer perto = (ServerPlayer) mundo.getNearestPlayer(item, 32);
                if (perto != null && Identificacao.desconhecido(perto, item.getItem())) {
                    Identificacao.identificarItem(perto, item.getItem());
                    item.setItem(item.getItem().copy());
                    perto.displayClientMessage(Component.translatable("mensagem.pixeldungeon.poco.item", item.getItem().getHoverName())
                            .withStyle(ChatFormatting.AQUA), false);
                    secar(mundo, controle);
                    return true;
                }
            }
        }
        return false;
    }

    // A água da saúde
    public static void beberSaude(ServerPlayer jogador) {
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        EfeitosDePocao.curarEfeitosRuins(jogador);
        for (ItemStack pilha : equipados(jogador)) {
            NivelDoItem.removerMaldicao(pilha);
        }
        jogador.getFoodData().setFoodLevel(20);
        EfeitosNoTick.curarAgora(jogador, jogador.getMaxHealth());
        Particulas.subindoEmVolta(jogador, ParticleTypes.HEART, 8, 0.05);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.poco.saude").withStyle(ChatFormatting.GREEN), false);
    }

    // A água da consciência
    public static void beberConsciencia(ServerPlayer jogador) {
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ItemStack pilha : equipados(jogador)) {
            Identificacao.identificarItem(jogador, pilha);
        }
        for (ItemStack pilha : jogador.getInventory().items) {
            if (NivelDoItem.podeIdentificar(pilha)) {
                pilha.set(ModComponentes.MALDICAO_CONHECIDA.get(), true);
            }
        }
        Busca.revelarTodas(jogador);
        Busca.revelarArmadilhas(jogador);
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 16, 0.08);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.poco.consciencia").withStyle(ChatFormatting.AQUA), false);
    }

    // O que o herói tem equipado: as duas mãos e a armadura
    private static List<ItemStack> equipados(ServerPlayer jogador) {
        List<ItemStack> lista = new ArrayList<>();
        for (EquipmentSlot lugar : EquipmentSlot.values()) {
            ItemStack pilha = jogador.getItemBySlot(lugar);
            if (!pilha.isEmpty()) {
                lista.add(pilha);
            }
        }
        return lista;
    }

    // O poço seca: a água some e o fundo vira chão
    public static void secar(ServerLevel mundo, BlockPos controle) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos fundo = controle.offset(dx, 0, dz);
                mundo.setBlock(fundo, ModBlocos.CHAO_ESGOTO.get().defaultBlockState(), Block.UPDATE_ALL);
                mundo.setBlock(fundo.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                mundo.sendParticles(ParticleTypes.SPLASH, fundo.getX() + 0.5, fundo.getY() + 1.2, fundo.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
            }
        }
        mundo.playSound(null, controle.above(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
    }
}
