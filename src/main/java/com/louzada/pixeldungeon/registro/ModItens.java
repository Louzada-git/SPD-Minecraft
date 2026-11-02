package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.item.ArmaDoShattered;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os itens do mod ficam registrados aqui.
public class ModItens {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(PixelDungeon.MODID);

    // ---- Armas iniciais das classes (números do Shattered) ----
    // nome, tier, dano mínimo, dano máximo, precisão, atraso entre ataques

    public static final DeferredItem<ArmaDoShattered> ESPADA_CURTA_DESGASTADA = arma("espada_curta_desgastada", 1, 1, 10, 1.0f, 1.0f);
    public static final DeferredItem<ArmaDoShattered> CAJADO_DO_MAGO = arma("cajado_do_mago", 1, 1, 6, 1.0f, 1.0f);
    public static final DeferredItem<ArmaDoShattered> ADAGA = arma("adaga", 1, 1, 8, 1.0f, 1.0f);
    // Ataca 2x mais rápido, mas bate fraco
    public static final DeferredItem<ArmaDoShattered> LUVAS_CRAVEJADAS = arma("luvas_cravejadas", 1, 1, 5, 1.0f, 0.5f);
    public static final DeferredItem<ArmaDoShattered> FLORETE = arma("florete", 1, 1, 8, 1.0f, 1.0f);
    // 40% a mais de precisão
    public static final DeferredItem<ArmaDoShattered> MACA = arma("maca", 1, 1, 8, 1.4f, 1.0f);

    // ---- Comida ----

    // Ração do Shattered: mata bastante a fome
    public static final DeferredItem<Item> RACAO = ITENS.registerSimpleItem("racao",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build()));

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

    private static DeferredItem<ArmaDoShattered> arma(String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso) {
        return ITENS.registerItem(nome, propriedades -> new ArmaDoShattered(propriedades, nome, tier, danoMinimo, danoMaximo, precisao, atraso));
    }

    // Usado pela aba do criativo para mostrar todos os itens do mod
    public static void colocarTodos(CreativeModeTab.Output saida) {
        for (var item : ITENS.getEntries()) {
            saida.accept(item.get());
        }
    }
}
