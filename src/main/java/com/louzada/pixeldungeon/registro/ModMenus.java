package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// As telas com casinhas de item (o servidor e a tela conversam pelo menu)
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, PixelDungeon.MODID);

    // O pote de alquimia (e o kit do alquimista)
    public static final DeferredHolder<MenuType<?>, MenuType<MenuDaAlquimia>> ALQUIMIA = MENUS.register("alquimia",
            () -> new MenuType<>(MenuDaAlquimia::new, FeatureFlags.DEFAULT_FLAGS));
}
