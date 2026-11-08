package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: apertou a tecla de procurar (ou o botão de procurar da barra do inventário)
public record PacoteProcurar() implements CustomPacketPayload {
    public static final Type<PacoteProcurar> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "procurar"));

    public static final StreamCodec<ByteBuf, PacoteProcurar> CODEC = StreamCodec.unit(new PacoteProcurar());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
