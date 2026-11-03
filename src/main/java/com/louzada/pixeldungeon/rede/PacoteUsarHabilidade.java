package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: apertou a tecla da habilidade da arma (Duelista)
public record PacoteUsarHabilidade() implements CustomPacketPayload {
    public static final Type<PacoteUsarHabilidade> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "usar_habilidade"));

    public static final StreamCodec<ByteBuf, PacoteUsarHabilidade> CODEC = StreamCodec.unit(new PacoteUsarHabilidade());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
