package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Jogador -> servidor: "escolhi esta classe" (o id da classe, tipo "guerreiro").
public record PacoteEscolherClasse(String classe) implements CustomPacketPayload {
    public static final Type<PacoteEscolherClasse> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "escolher_classe"));

    public static final StreamCodec<ByteBuf, PacoteEscolherClasse> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteEscolherClasse::classe,
            PacoteEscolherClasse::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
