package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: o item escolhido pro pergaminho (o espaço do inventário, ou -1 se desistiu)
public record PacoteItemEscolhido(int espaco) implements CustomPacketPayload {
    public static final Type<PacoteItemEscolhido> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "item_escolhido"));

    public static final StreamCodec<ByteBuf, PacoteItemEscolhido> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacoteItemEscolhido::espaco,
            PacoteItemEscolhido::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
