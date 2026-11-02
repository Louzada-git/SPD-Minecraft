package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Aba do modo criativo com as coisas do mod.
public class ModAbas {
    public static final DeferredRegister<CreativeModeTab> ABAS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PixelDungeon.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ABA_PRINCIPAL = ABAS.register("principal", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pixeldungeon"))
            .icon(() -> new ItemStack(ModItens.PAREDE_ESGOTO.get()))
            .displayItems((parametros, saida) -> ModItens.colocarTodos(saida))
            .build());
}
