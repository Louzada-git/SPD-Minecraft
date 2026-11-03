package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: "quero abrir a tela de compartilhar identificação"
public record PacotePedirCompartilhar() implements CustomPacketPayload {
    public static final Type<PacotePedirCompartilhar> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "pedir_compartilhar"));

    public static final StreamCodec<ByteBuf, PacotePedirCompartilhar> CODEC = StreamCodec.unit(new PacotePedirCompartilhar());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
