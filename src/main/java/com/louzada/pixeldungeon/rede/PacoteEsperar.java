package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: apertou (ou está segurando) a tecla de esperar, ou clicou no botão de esperar da barra do inventário
public record PacoteEsperar() implements CustomPacketPayload {
    public static final Type<PacoteEsperar> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "esperar"));

    public static final StreamCodec<ByteBuf, PacoteEsperar> CODEC = StreamCodec.unit(new PacoteEsperar());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
