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

    // Gotas de orvalho guardadas no odre (de 0 a 20)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VOLUME = COMPONENTES.registerComponentType("volume",
            construtor -> construtor.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Quantas moedas tem numa pilha de ouro
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUANTIDADE = COMPONENTES.registerComponentType(
            "quantidade", construtor -> construtor.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Armas achadas na dungeon vêm sem identificar: o nível (e se tem maldição) só aparece depois.
    // Sem esse dado, a arma já é conhecida (as armas iniciais das classes).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IDENTIFICADO = COMPONENTES.registerComponentType(
            "identificado", construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // Quantos golpes ainda faltam pra arma se identificar sozinha
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> USOS_PARA_IDENTIFICAR = COMPONENTES.registerComponentType(
            "usos_para_identificar", construtor -> construtor.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    // A maldição da arma (o id dela), se tiver
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MALDICAO = COMPONENTES.registerComponentType("maldicao",
            construtor -> construtor.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    // Se já dá pra saber se a arma é amaldiçoada (sem esse dado, dá)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MALDICAO_CONHECIDA = COMPONENTES.registerComponentType(
            "maldicao_conhecida", construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Selo quebrado do Guerreiro afixado na armadura
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SELO = COMPONENTES.registerComponentType("selo",
            construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
}
