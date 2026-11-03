package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: o que ele já identificou.
// Uma posição por cor de poção e por runa de pergaminho, com o tipo descoberto ("cura") ou vazio se não sabe.
public record PacoteConhecimento(List<String> pocoes, List<String> pergaminhos) implements CustomPacketPayload {
    public static final Type<PacoteConhecimento> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "conhecimento"));

    public static final StreamCodec<ByteBuf, PacoteConhecimento> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PacoteConhecimento::pocoes,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PacoteConhecimento::pergaminhos,
            PacoteConhecimento::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
