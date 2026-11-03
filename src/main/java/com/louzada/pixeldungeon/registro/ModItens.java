package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista.Habilidade;
import com.louzada.pixeldungeon.item.ArcoEspiritual;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.Baga;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.ItemCarneMisteriosa;
import com.louzada.pixeldungeon.item.ItemComDescricao;
import com.louzada.pixeldungeon.item.ItemAnkh;
import com.louzada.pixeldungeon.item.Odre;
import com.louzada.pixeldungeon.item.RacaoDeSuprimentos;
import com.louzada.pixeldungeon.item.TomoSagrado;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os itens do mod ficam registrados aqui.
public class ModItens {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(PixelDungeon.MODID);

    // ---- Armas (números do Shattered) ----
    // nome, tier, dano mínimo, dano máximo, precisão, atraso entre ataques, defesa extra, surpresa forte,
    // habilidade da Duelista e o número dela (dano a mais; no furtivo, o alcance em quadradinhos)

    // Tier 1: as armas iniciais das classes
    public static final DeferredItem<ArmaDoShattered> ESPADA_CURTA_DESGASTADA = arma("espada_curta_desgastada", 1, 1, 10, 1.0f, 1.0f, 0, 0,
            Habilidade.CORTAR, 3);
    // Também lança mísseis mágicos com o clique direito (vem com a varinha de míssil mágico dentro)
    public static final DeferredItem<CajadoDoMago> CAJADO_DO_MAGO = ITENS.registerItem("cajado_do_mago", CajadoDoMago::new);
    // Ataque surpresa quase sempre no dano máximo
    public static final DeferredItem<ArmaDoShattered> ADAGA = arma("adaga", 1, 1, 8, 1.0f, 1.0f, 0, 0.75f, Habilidade.FURTIVO, 5);
    // Ataca 2x mais rápido, mas bate fraco
    public static final DeferredItem<ArmaDoShattered> LUVAS_CRAVEJADAS = arma("luvas_cravejadas", 1, 1, 5, 1.0f, 0.5f, 0, 0,
            Habilidade.COMBINACAO, 3);
    // Dá 1 de defesa extra
    public static final DeferredItem<ArmaDoShattered> FLORETE = arma("florete", 1, 1, 8, 1.0f, 1.0f, 1, 0, Habilidade.ESTOCADA, 5);
    // 40% a mais de precisão
    public static final DeferredItem<ArmaDoShattered> MACA = arma("maca", 1, 1, 8, 1.4f, 1.0f, 0, 0, Habilidade.GOLPE_PESADO, 3);

    // ---- Itens únicos das classes ----

    // Ladino: fica invisível
    public static final DeferredItem<CapaDasSombras> CAPA_DAS_SOMBRAS = ITENS.registerItem("capa_das_sombras", CapaDasSombras::new);
    // Caçadora: flechas infinitas
    public static final DeferredItem<ArcoEspiritual> ARCO_ESPIRITUAL = ITENS.registerItem("arco_espiritual", ArcoEspiritual::new);
    // Clérigo: magias
    public static final DeferredItem<TomoSagrado> TOMO_SAGRADO = ITENS.registerItem("tomo_sagrado", TomoSagrado::new);
    // A flecha que o arco solta (não dá pra pegar; só serve pra desenhar a flecha voando)
    public static final DeferredItem<Item> FLECHA_ESPIRITUAL = ITENS.registerSimpleItem("flecha_espiritual");

    // Odre: todo herói começa com um; guarda as gotas de orvalho
    public static final DeferredItem<Odre> ODRE = ITENS.registerItem("odre", Odre::new);
    // A gota de orvalho voando (não dá pra pegar; ela vai sozinha pro odre)
    public static final DeferredItem<Item> GOTA_DE_ORVALHO = ITENS.registerSimpleItem("gota_de_orvalho");

    // ---- Armaduras ----

    // Armadura de pano: defesa 0-2, todas as classes começam com ela
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_PANO = ITENS.registerItem("armadura_de_pano",
            propriedades -> new ArmaduraDoShattered(ModMateriais.PANO, propriedades, "armadura_de_pano", 1));

    // ---- Comida ----

    // Ração do Shattered: mata bastante a fome
    public static final DeferredItem<Item> RACAO = ITENS.registerSimpleItem("racao",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build()));

    // Carne misteriosa: cai de caranguejos e ratos albinos (comer é arriscado)
    public static final DeferredItem<ItemCarneMisteriosa> CARNE_MISTERIOSA = ITENS.registerItem("carne_misteriosa", ItemCarneMisteriosa::new);

    // Baga: cai da grama alta pra Caçadora com o talento recompensa da natureza
    public static final DeferredItem<Baga> BAGA = ITENS.registerItem("baga", Baga::new);

    // Ração de suprimentos: aparece em baús pro Ladino com o talento rações guardadas
    public static final DeferredItem<RacaoDeSuprimentos> RACAO_DE_SUPRIMENTOS = ITENS.registerItem("racao_de_suprimentos", RacaoDeSuprimentos::new);

    // ---- Itens especiais ----

    // Traz de volta um aliado que morreu
    public static final DeferredItem<ItemAnkh> ANKH = ITENS.registerItem("ankh", ItemAnkh::new);

    // Bolha de gosma: cai do Goo quando ele morre (sozinha não faz nada; vai servir pra alquimia)
    public static final DeferredItem<ItemComDescricao> BOLHA_DE_GOSMA = ITENS.registerItem("bolha_de_gosma",
            propriedades -> new ItemComDescricao(propriedades, "bolha_de_gosma"));

    // ---- Ovos dos inimigos (pra testar no criativo) ----

    public static final DeferredItem<DeferredSpawnEggItem> OVO_RATO_MARSUPIAL = ITENS.registerItem("ovo_rato_marsupial",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.RATO_MARSUPIAL, 0x8C7B6B, 0xD9B8A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_RATO_ALBINO = ITENS.registerItem("ovo_rato_albino",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.RATO_ALBINO, 0xF2F2F2, 0xE88A9A, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_COBRA_DE_ESGOTO = ITENS.registerItem("ovo_cobra_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.COBRA_DE_ESGOTO, 0x6B8E23, 0xC8A44D, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_GNOLL_EXPLORADOR = ITENS.registerItem("ovo_gnoll_explorador",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.GNOLL_EXPLORADOR, 0xB08A55, 0x5C4325, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_CARANGUEJO_DE_ESGOTO = ITENS.registerItem("ovo_caranguejo_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.CARANGUEJO_DE_ESGOTO, 0xC8602B, 0xF0D7A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_ENXAME_DE_MOSCAS = ITENS.registerItem("ovo_enxame_de_moscas",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.ENXAME_DE_MOSCAS, 0x2B2B2B, 0x7A9E3B, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_SLIME = ITENS.registerItem("ovo_slime",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.SLIME, 0x5DBB3A, 0x2E6B1E, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_GOO = ITENS.registerItem("ovo_goo",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.GOO, 0x262626, 0x808080, propriedades));

    // ---- Blocos (pra poder pegar no criativo e testar) ----

    public static final DeferredItem<BlockItem> PAREDE_ESGOTO = ITENS.registerSimpleBlockItem("parede_esgoto", ModBlocos.PAREDE_ESGOTO);
    public static final DeferredItem<BlockItem> CHAO_ESGOTO = ITENS.registerSimpleBlockItem("chao_esgoto", ModBlocos.CHAO_ESGOTO);
    public static final DeferredItem<BlockItem> RODAPE_ESGOTO = ITENS.registerSimpleBlockItem("rodape_esgoto", ModBlocos.RODAPE_ESGOTO);
    public static final DeferredItem<BlockItem> PILAR_ESGOTO = ITENS.registerSimpleBlockItem("pilar_esgoto", ModBlocos.PILAR_ESGOTO);
    public static final DeferredItem<BlockItem> MOLDURA_ESGOTO = ITENS.registerSimpleBlockItem("moldura_esgoto", ModBlocos.MOLDURA_ESGOTO);
    public static final DeferredItem<BlockItem> CORNIJA_ESGOTO = ITENS.registerSimpleBlockItem("cornija_esgoto", ModBlocos.CORNIJA_ESGOTO);
    public static final DeferredItem<BlockItem> LANTERNA_ESGOTO = ITENS.registerSimpleBlockItem("lanterna_esgoto", ModBlocos.LANTERNA_ESGOTO);
    public static final DeferredItem<BlockItem> CORRENTE_ESGOTO = ITENS.registerSimpleBlockItem("corrente_esgoto", ModBlocos.CORRENTE_ESGOTO);
    public static final DeferredItem<BlockItem> MUSGO_ESGOTO = ITENS.registerSimpleBlockItem("musgo_esgoto", ModBlocos.MUSGO_ESGOTO);
    public static final DeferredItem<DoubleHighBlockItem> CAPIM_ALTO = ITENS.registerItem("capim_alto",
            propriedades -> new DoubleHighBlockItem(ModBlocos.CAPIM_ALTO.get(), propriedades));
    public static final DeferredItem<BlockItem> GRAMA_BAIXA = ITENS.registerSimpleBlockItem("grama_baixa", ModBlocos.GRAMA_BAIXA);
    public static final DeferredItem<BlockItem> ESCADA_SUBIDA = ITENS.registerSimpleBlockItem("escada_subida", ModBlocos.ESCADA_SUBIDA);
    public static final DeferredItem<BlockItem> ESCADA_DESCIDA = ITENS.registerSimpleBlockItem("escada_descida", ModBlocos.ESCADA_DESCIDA);

    private static DeferredItem<ArmaDoShattered> arma(String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso,
            int defesa, float surpresa, Habilidade habilidade, int valorDaHabilidade) {
        return ITENS.registerItem(nome, propriedades -> new ArmaDoShattered(propriedades, nome, tier, danoMinimo, danoMaximo, precisao, atraso,
                defesa, surpresa, habilidade, valorDaHabilidade));
    }

    // Usado pela aba do criativo para mostrar todos os itens do mod
    public static void colocarTodos(CreativeModeTab.Output saida) {
        for (var item : ITENS.getEntries()) {
            if (item.get() != FLECHA_ESPIRITUAL.get() && item.get() != GOTA_DE_ORVALHO.get()) {
                saida.accept(item.get());
            }
        }
    }
}
