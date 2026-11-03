package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.efeito.EfeitoSimples;
import com.louzada.pixeldungeon.efeito.MucoCaustico;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Efeitos do Shattered (os "buffs" e "debuffs" de lá viram efeitos do Minecraft).
public class ModEfeitos {
    public static final DeferredRegister<MobEffect> EFEITOS = DeferredRegister.create(Registries.MOB_EFFECT, PixelDungeon.MODID);

    // Muco cáustico do Goo: dano com o tempo, sai na água
    public static final DeferredHolder<MobEffect, MucoCaustico> MUCO_CAUSTICO = EFEITOS.register("muco_caustico", MucoCaustico::new);

    // Atordoado (golpe pesado da maça): precisão e esquiva pela metade
    public static final DeferredHolder<MobEffect, EfeitoSimples> ATORDOADO = EFEITOS.register("atordoado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xCCCC66));

    // Iluminado (luz guia do Clérigo): o próximo ataque físico contra ele acerta com certeza
    public static final DeferredHolder<MobEffect, EfeitoSimples> ILUMINADO = EFEITOS.register("iluminado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFFFF88));

    // Arma celestial (Clérigo): +2 de dano mágico em cada golpe
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMA_CELESTIAL = EFEITOS.register("arma_celestial",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFEE77));

    // Armadura sagrada (Clérigo): bloqueia 1 de dano a mais em cada golpe
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMADURA_SAGRADA = EFEITOS.register("armadura_sagrada",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFEE77));

    // Escudo de luz (Clérigo): armadura extra contra um inimigo escolhido
    public static final DeferredHolder<MobEffect, EfeitoSimples> ESCUDO_DE_LUZ = EFEITOS.register("escudo_de_luz",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFFFCC));
}
