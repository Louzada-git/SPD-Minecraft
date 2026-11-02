package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.efeito.MucoCaustico;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Efeitos do Shattered (os "buffs" e "debuffs" de lá viram efeitos do Minecraft).
public class ModEfeitos {
    public static final DeferredRegister<MobEffect> EFEITOS = DeferredRegister.create(Registries.MOB_EFFECT, PixelDungeon.MODID);

    // Muco cáustico do Goo: dano com o tempo, sai na água
    public static final DeferredHolder<MobEffect, MucoCaustico> MUCO_CAUSTICO = EFEITOS.register("muco_caustico", MucoCaustico::new);
}
