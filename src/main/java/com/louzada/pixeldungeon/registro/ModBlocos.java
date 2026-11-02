package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.BlocoComFormato;
import com.louzada.pixeldungeon.bloco.BlocoEscadaDescida;
import com.louzada.pixeldungeon.bloco.BlocoEscadaSubida;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os blocos do mod ficam registrados aqui.
// Todos são inquebráveis: a dungeon é construída pelo mod e o jogador não desmonta ela.
public class ModBlocos {
    public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(PixelDungeon.MODID);

    // ---- Estrutura do esgoto ----

    // Paredes e teto (as texturas variam sozinhas: tijolo, tijolo com musgo, tijolo rachado)
    public static final DeferredBlock<Block> PAREDE_ESGOTO = BLOCOS.registerSimpleBlock("parede_esgoto", inquebravel());
    // Chão (tufo polido e tufo bruto)
    public static final DeferredBlock<Block> CHAO_ESGOTO = BLOCOS.registerSimpleBlock("chao_esgoto", inquebravel());
    // Faixa de tijolos de tufo na parte de baixo das paredes (rodapé)
    public static final DeferredBlock<Block> RODAPE_ESGOTO = BLOCOS.registerSimpleBlock("rodape_esgoto", inquebravel());
    // Pilares nos cantos das salas
    public static final DeferredBlock<Block> PILAR_ESGOTO = BLOCOS.registerSimpleBlock("pilar_esgoto", inquebravel());
    // Moldura de tufo entalhado em volta das escadas
    public static final DeferredBlock<Block> MOLDURA_ESGOTO = BLOCOS.registerSimpleBlock("moldura_esgoto", inquebravel());
    // Degraus de cabeça pra baixo no alto das paredes: fazem a cornija das salas e os arcos dos túneis
    public static final DeferredBlock<StairBlock> CORNIJA_ESGOTO = BLOCOS.registerBlock("cornija_esgoto",
            propriedades -> new StairBlock(Blocks.STONE_BRICKS.defaultBlockState(), propriedades), inquebravel());

    // ---- Decoração ----

    public static final DeferredBlock<BlocoComFormato> LANTERNA_ESGOTO = BLOCOS.registerBlock("lanterna_esgoto",
            propriedades -> new BlocoComFormato(propriedades, BlocoComFormato.FORMATO_LANTERNA_PENDURADA),
            inquebravel().sound(SoundType.LANTERN).lightLevel(estado -> 15).noOcclusion());
    public static final DeferredBlock<BlocoComFormato> CORRENTE_ESGOTO = BLOCOS.registerBlock("corrente_esgoto",
            propriedades -> new BlocoComFormato(propriedades, BlocoComFormato.FORMATO_CORRENTE),
            inquebravel().sound(SoundType.CHAIN).noOcclusion());
    public static final DeferredBlock<BlocoComFormato> MUSGO_ESGOTO = BLOCOS.registerBlock("musgo_esgoto",
            propriedades -> new BlocoComFormato(propriedades, BlocoComFormato.FORMATO_TAPETE),
            inquebravel().sound(SoundType.MOSS_CARPET).noOcclusion());

    // ---- Escadas entre os andares ----

    public static final DeferredBlock<BlocoEscadaSubida> ESCADA_SUBIDA = BLOCOS.registerBlock("escada_subida", BlocoEscadaSubida::new, inquebravel().noOcclusion());
    public static final DeferredBlock<BlocoEscadaDescida> ESCADA_DESCIDA = BLOCOS.registerBlock("escada_descida", BlocoEscadaDescida::new, inquebravel());

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
