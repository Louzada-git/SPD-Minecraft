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

    // A varinha que está dentro do cajado do Mago (sem esse dado, a de míssil mágico)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> VARINHA_DO_CAJADO = COMPONENTES.registerComponentType(
            "varinha_do_cajado", construtor -> construtor.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Desgaste de um conjunto de armas de arremesso: quanto ainda falta (de 100) pra próxima quebrar (sem esse dado, 100)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> DURABILIDADE = COMPONENTES.registerComponentType(
            "durabilidade", construtor -> construtor.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    // De que run e de que andar é a chave de ferro (ela só abre as portas trancadas de lá)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUN_DA_CHAVE = COMPONENTES.registerComponentType(
            "run_da_chave", construtor -> construtor.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ANDAR_DA_CHAVE = COMPONENTES.registerComponentType(
            "andar_da_chave", construtor -> construtor.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Selo quebrado do Guerreiro afixado na armadura
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SELO = COMPONENTES.registerComponentType("selo",
            construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // O selo guarda um aprimoramento (ganha quando a armadura dele é melhorada) e leva pra próxima armadura
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SELO_MELHORIA = COMPONENTES.registerComponentType(
            "selo_melhoria", construtor -> construtor.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // O glifo que o selo carrega (o da armadura em que ele estava; a transferência rúnica leva pra próxima)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELO_GLIFO = COMPONENTES.registerComponentType(
            "selo_glifo", construtor -> construtor.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Maldição explosiva: o quanto falta (de 100) pra arma explodir
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PRESSAO_EXPLOSIVA = COMPONENTES.registerComponentType(
            "pressao_explosiva", construtor -> construtor.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // O glifo da armadura ou o encantamento da arma (o id), se tiver
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ENCANTO = COMPONENTES.registerComponentType("encanto",
            construtor -> construtor.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Os artefatos achados na dungeon: a parte da próxima carga que já encheu (0 a 1)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> CARGA_PARCIAL = COMPONENTES.registerComponentType(
            "carga_parcial", construtor -> construtor.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    // O que o artefato guarda: os pergaminhos que o livro instável ainda aceita, a semente das sandálias...
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUARDADO = COMPONENTES.registerComponentType("guardado",
            construtor -> construtor.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
}
