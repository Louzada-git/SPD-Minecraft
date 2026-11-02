package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os blocos do mod ficam registrados aqui.
public class ModBlocos {
    public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(PixelDungeon.MODID);

    // Paredes, teto e chão da dungeon: ninguém consegue quebrar (igual bedrock).
    public static final DeferredBlock<Block> PAREDE_ESGOTO = BLOCOS.registerSimpleBlock("parede_esgoto", inquebravel());
    public static final DeferredBlock<Block> CHAO_ESGOTO = BLOCOS.registerSimpleBlock("chao_esgoto", inquebravel());

    // Propriedades de um bloco que não quebra, não explode, não dropa nada e onde mob não nasce.
    public static BlockBehaviour.Properties inquebravel() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .isValidSpawn((estado, mundo, posicao, tipoDeMob) -> false);
    }
}
