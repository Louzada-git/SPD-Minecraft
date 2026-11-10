package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: o botão escolhido na janelinha de opções (-1 se fechou sem escolher)
public record PacoteOpcaoEscolhida(int indice) implements CustomPacketPayload {
    public static final Type<PacoteOpcaoEscolhida> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "opcao_escolhida"));

    public static final StreamCodec<ByteBuf, PacoteOpcaoEscolhida> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacoteOpcaoEscolhida::indice,
            PacoteOpcaoEscolhida::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
