package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// Servidor -> jogador: abre a janelinha de opções (ver EscolhaDeOpcao).
//   titulo, texto: o que aparece em cima
//   icone: o item do título (pode ser vazio)
//   opcoes: o texto de cada botão
public record PacoteOpcoes(Component titulo, Component texto, ItemStack icone, List<Component> opcoes) implements CustomPacketPayload {
    public static final Type<PacoteOpcoes> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "opcoes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteOpcoes> CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, PacoteOpcoes::titulo,
            ComponentSerialization.TRUSTED_STREAM_CODEC, PacoteOpcoes::texto,
            ItemStack.OPTIONAL_STREAM_CODEC, PacoteOpcoes::icone,
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()), PacoteOpcoes::opcoes,
            PacoteOpcoes::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
