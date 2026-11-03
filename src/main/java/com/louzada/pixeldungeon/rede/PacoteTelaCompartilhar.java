package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: abre a tela de compartilhar, com a escolha atual e os outros jogadores online.
public record PacoteTelaCompartilhar(String modo, List<String> jogadores, List<String> escolhidos) implements CustomPacketPayload {
    public static final Type<PacoteTelaCompartilhar> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "tela_compartilhar"));

    public static final StreamCodec<ByteBuf, PacoteTelaCompartilhar> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteTelaCompartilhar::modo,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PacoteTelaCompartilhar::jogadores,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PacoteTelaCompartilhar::escolhidos,
            PacoteTelaCompartilhar::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
