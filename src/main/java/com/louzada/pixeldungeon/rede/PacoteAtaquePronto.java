package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: "o próximo golpe já está carregado" (ímpeto letal e empunhar ágil: o golpe não gastou tempo)
public record PacoteAtaquePronto() implements CustomPacketPayload {
    public static final Type<PacoteAtaquePronto> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "ataque_pronto"));

    public static final StreamCodec<ByteBuf, PacoteAtaquePronto> CODEC = StreamCodec.unit(new PacoteAtaquePronto());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
