package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ItemAnkh;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os itens do mod ficam registrados aqui.
public class ModItens {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(PixelDungeon.MODID);

    // ---- Armas iniciais das classes (números do Shattered) ----
    // nome, tier, dano mínimo, dano máximo, precisão, atraso entre ataques, defesa extra, surpresa forte

    public static final DeferredItem<ArmaDoShattered> ESPADA_CURTA_DESGASTADA = arma("espada_curta_desgastada", 1, 1, 10, 1.0f, 1.0f, 0, false);
    public static final DeferredItem<ArmaDoShattered> CAJADO_DO_MAGO = arma("cajado_do_mago", 1, 1, 6, 1.0f, 1.0f, 0, false);
    // Ataque surpresa quase sempre no dano máximo
    public static final DeferredItem<ArmaDoShattered> ADAGA = arma("adaga", 1, 1, 8, 1.0f, 1.0f, 0, true);
    // Ataca 2x mais rápido, mas bate fraco
    public static final DeferredItem<ArmaDoShattered> LUVAS_CRAVEJADAS = arma("luvas_cravejadas", 1, 1, 5, 1.0f, 0.5f, 0, false);
    // Dá 1 de defesa extra
    public static final DeferredItem<ArmaDoShattered> FLORETE = arma("florete", 1, 1, 8, 1.0f, 1.0f, 1, false);
    // 40% a mais de precisão
    public static final DeferredItem<ArmaDoShattered> MACA = arma("maca", 1, 1, 8, 1.4f, 1.0f, 0, false);

    // ---- Armaduras ----

    // Armadura de pano: defesa 0-2, todas as classes começam com ela
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_PANO = ITENS.registerItem("armadura_de_pano",
            propriedades -> new ArmaduraDoShattered(ModMateriais.PANO, propriedades, "armadura_de_pano", 1));

    // ---- Comida ----

    // Ração do Shattered: mata bastante a fome
    public static final DeferredItem<Item> RACAO = ITENS.registerSimpleItem("racao",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build()));

    // ---- Itens especiais ----

    // Traz de volta um aliado que morreu
    public static final DeferredItem<ItemAnkh> ANKH = ITENS.registerItem("ankh", ItemAnkh::new);

    // ---- Ovos dos inimigos (pra testar no criativo) ----

    public static final DeferredItem<DeferredSpawnEggItem> OVO_RATO_MARSUPIAL = ITENS.registerItem("ovo_rato_marsupial",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.RATO_MARSUPIAL, 0x8C7B6B, 0xD9B8A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_COBRA_DE_ESGOTO = ITENS.registerItem("ovo_cobra_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.COBRA_DE_ESGOTO, 0x6B8E23, 0xC8A44D, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_GNOLL_EXPLORADOR = ITENS.registerItem("ovo_gnoll_explorador",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.GNOLL_EXPLORADOR, 0xB08A55, 0x5C4325, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_CARANGUEJO_DE_ESGOTO = ITENS.registerItem("ovo_caranguejo_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.CARANGUEJO_DE_ESGOTO, 0xC8602B, 0xF0D7A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_SLIME = ITENS.registerItem("ovo_slime",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.SLIME, 0x5DBB3A, 0x2E6B1E, propriedades));

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
    public static final DeferredItem<BlockItem> ESCADA_SUBIDA = ITENS.registerSimpleBlockItem("escada_subida", ModBlocos.ESCADA_SUBIDA);
    public static final DeferredItem<BlockItem> ESCADA_DESCIDA = ITENS.registerSimpleBlockItem("escada_descida", ModBlocos.ESCADA_DESCIDA);

    private static DeferredItem<ArmaDoShattered> arma(String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso,
            int defesa, boolean surpresaForte) {
        return ITENS.registerItem(nome, propriedades -> new ArmaDoShattered(propriedades, nome, tier, danoMinimo, danoMaximo, precisao, atraso,
                defesa, surpresaForte));
    }

    // Usado pela aba do criativo para mostrar todos os itens do mod
    public static void colocarTodos(CreativeModeTab.Output saida) {
        for (var item : ITENS.getEntries()) {
            saida.accept(item.get());
        }
    }
}
