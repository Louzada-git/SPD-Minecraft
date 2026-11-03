package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: a nova escolha de com quem compartilhar ("todos", "ninguem" ou "escolhidos" + nomes).
public record PacoteDefinirCompartilhar(String modo, List<String> escolhidos) implements CustomPacketPayload {
    public static final Type<PacoteDefinirCompartilhar> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "definir_compartilhar"));

    public static final StreamCodec<ByteBuf, PacoteDefinirCompartilhar> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteDefinirCompartilhar::modo,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PacoteDefinirCompartilhar::escolhidos,
            PacoteDefinirCompartilhar::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
