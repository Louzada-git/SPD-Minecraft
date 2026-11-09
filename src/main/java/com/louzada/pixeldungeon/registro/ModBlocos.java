package com.louzada.pixeldungeon.registro;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.BlocoComFormato;
import com.louzada.pixeldungeon.bloco.CapimAlto;
import com.louzada.pixeldungeon.bloco.BlocoEscadaDescida;
import com.louzada.pixeldungeon.bloco.BlocoEscadaSubida;
import com.louzada.pixeldungeon.bloco.BlocoParedeEsgoto;
import com.louzada.pixeldungeon.bloco.GramaBaixa;
import com.louzada.pixeldungeon.bloco.ParedeDaChave;
import com.louzada.pixeldungeon.bloco.PlantaDoShattered;
import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.item.TipoDePlanta;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os blocos do mod ficam registrados aqui.
// Todos são inquebráveis: a dungeon é construída pelo mod e o jogador não desmonta ela.
public class ModBlocos {
    public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(PixelDungeon.MODID);

    // ---- Estrutura do esgoto ----

    // Paredes e teto (as texturas variam sozinhas: tijolo, tijolo com musgo, tijolo rachado).
    // Em cima das poças, o teto pinga água.
    public static final DeferredBlock<BlocoParedeEsgoto> PAREDE_ESGOTO = BLOCOS.registerBlock("parede_esgoto", BlocoParedeEsgoto::new, inquebravel());
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

    // Barris de armazenamento (o enfeite do esgoto do Shattered)
    public static final DeferredBlock<Block> BARRIL_ESGOTO = BLOCOS.registerSimpleBlock("barril_esgoto", inquebravel().sound(SoundType.WOOD));
    // Estantes de livros (a biblioteca)
    public static final DeferredBlock<Block> ESTANTE_ESGOTO = BLOCOS.registerSimpleBlock("estante_esgoto", inquebravel().sound(SoundType.WOOD));
    // As portas gigantes (4 de largura, 6 de altura), comuns e trancadas
    public static final DeferredBlock<PortaGigante> PORTA_GIGANTE = BLOCOS.registerBlock("porta_gigante", PortaGigante::new,
            inquebravel().sound(SoundType.WOOD).noOcclusion().isSuffocating((estado, mundo, posicao) -> false)
                    .isViewBlocking((estado, mundo, posicao) -> false));

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

    // ---- Grama ----

    // Grama alta (3 blocos de altura): quebra sozinha quando alguém encosta
    public static final DeferredBlock<CapimAlto> CAPIM_ALTO = BLOCOS.registerBlock("capim_alto", CapimAlto::new,
            planta().offsetType(BlockBehaviour.OffsetType.XZ));
    // Grama baixa: o que sobra da grama alta pisoteada
    public static final DeferredBlock<GramaBaixa> GRAMA_BAIXA = BLOCOS.registerBlock("grama_baixa", GramaBaixa::new,
            planta().replaceable().offsetType(BlockBehaviour.OffsetType.XZ));

    // ---- Plantas do Shattered (nascem das sementes) ----

    // Uma pra cada tipo, na ordem do TipoDePlanta. Não dá pra quebrar: só pisando nelas.
    public static final List<DeferredBlock<PlantaDoShattered>> PLANTAS = registrarPlantas();

    // ---- Escadas entre os andares ----

    public static final DeferredBlock<BlocoEscadaSubida> ESCADA_SUBIDA = BLOCOS.registerBlock("escada_subida", BlocoEscadaSubida::new, inquebravel().noOcclusion());
    public static final DeferredBlock<BlocoEscadaDescida> ESCADA_DESCIDA = BLOCOS.registerBlock("escada_descida", BlocoEscadaDescida::new, inquebravel());

    // A parede espectral da chave de crânio (some sozinha depois de 10 turnos)
    public static final DeferredBlock<ParedeDaChave> PAREDE_DA_CHAVE = BLOCOS.registerBlock("parede_da_chave", ParedeDaChave::new,
            inquebravel().sound(SoundType.AMETHYST).noOcclusion().lightLevel(estado -> 6).pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((estado, mundo, posicao) -> false).isSuffocating((estado, mundo, posicao) -> false));

    private static List<DeferredBlock<PlantaDoShattered>> registrarPlantas() {
        List<DeferredBlock<PlantaDoShattered>> lista = new ArrayList<>();
        for (TipoDePlanta tipo : TipoDePlanta.values()) {
            lista.add(BLOCOS.registerBlock(tipo.nomeDaPlanta(), propriedades -> new PlantaDoShattered(propriedades, tipo),
                    planta().strength(-1.0F, 3600000.0F)));
        }
        return lista;
    }

    // Planta: atravessável, quebra na hora, não dropa nada e não segura a luz
    public static BlockBehaviour.Properties planta() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .sound(SoundType.GRASS)
                .noCollission()
                .instabreak()
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY)
                .isValidSpawn((estado, mundo, posicao, tipoDeMob) -> false);
    }

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
