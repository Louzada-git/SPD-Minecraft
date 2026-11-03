package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: "quero pôr um ponto neste talento"
public record PacoteAprimorarTalento(String talento) implements CustomPacketPayload {
    public static final Type<PacoteAprimorarTalento> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "aprimorar_talento"));

    public static final StreamCodec<ByteBuf, PacoteAprimorarTalento> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteAprimorarTalento::talento,
            PacoteAprimorarTalento::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
