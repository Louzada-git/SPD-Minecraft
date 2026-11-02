package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os itens do mod ficam registrados aqui.
public class ModItens {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(PixelDungeon.MODID);

    // Itens dos blocos (pra poder pegar no criativo e testar)
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

    // Usado pela aba do criativo para mostrar todos os itens do mod
    public static void colocarTodos(CreativeModeTab.Output saida) {
        for (var item : ITENS.getEntries()) {
            saida.accept(item.get());
        }
    }
}
