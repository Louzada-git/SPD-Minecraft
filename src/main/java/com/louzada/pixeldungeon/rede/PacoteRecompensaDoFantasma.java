package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: a recompensa escolhida na conversa com o fantasma (0 = arma, 1 = armadura)
public record PacoteRecompensaDoFantasma(int qual) implements CustomPacketPayload {
    public static final Type<PacoteRecompensaDoFantasma> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID,
            "recompensa_do_fantasma"));

    public static final StreamCodec<ByteBuf, PacoteRecompensaDoFantasma> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacoteRecompensaDoFantasma::qual,
            PacoteRecompensaDoFantasma::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
