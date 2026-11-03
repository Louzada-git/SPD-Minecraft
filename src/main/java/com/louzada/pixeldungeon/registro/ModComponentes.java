package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Informações guardadas dentro de um item (cada cajado lembra as cargas dele).
public class ModComponentes {
    public static final DeferredRegister.DataComponents COMPONENTES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            PixelDungeon.MODID);

    // Quantas cargas a varinha ainda tem (sem esse dado, ela está cheia)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARGAS = COMPONENTES.registerComponentType("cargas",
            construtor -> construtor.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Tick do jogo em que a próxima carga começou a encher
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> INICIO_DA_RECARGA = COMPONENTES.registerComponentType(
            "inicio_da_recarga", construtor -> construtor.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
}
