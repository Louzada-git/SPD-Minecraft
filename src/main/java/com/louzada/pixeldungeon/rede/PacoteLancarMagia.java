package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: escolheu uma magia na tela do tomo sagrado
public record PacoteLancarMagia(String magia) implements CustomPacketPayload {
    public static final Type<PacoteLancarMagia> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "lancar_magia"));

    public static final StreamCodec<ByteBuf, PacoteLancarMagia> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteLancarMagia::magia,
            PacoteLancarMagia::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
