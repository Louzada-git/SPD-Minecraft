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

    // Recarga parada (a capa não recarrega enquanto está em uso): quantos ticks já tinha enchido
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RECARGA_PAUSADA = COMPONENTES.registerComponentType(
            "recarga_pausada", construtor -> construtor.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // Nível e experiência dos artefatos (capa das sombras, tomo sagrado): ficam mais fortes com o uso
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NIVEL = COMPONENTES.registerComponentType("nivel",
            construtor -> construtor.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXPERIENCIA = COMPONENTES.registerComponentType(
            "experiencia", construtor -> construtor.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Selo quebrado do Guerreiro afixado na armadura
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SELO = COMPONENTES.registerComponentType("selo",
            construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
}
