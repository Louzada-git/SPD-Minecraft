package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: abre o guia de alquimia nessa página (as páginas que ele tem vêm no estado do herói)
public record PacoteGuiaDeAlquimia(int pagina) implements CustomPacketPayload {
    public static final Type<PacoteGuiaDeAlquimia> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "guia_de_alquimia"));

    public static final StreamCodec<ByteBuf, PacoteGuiaDeAlquimia> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacoteGuiaDeAlquimia::pagina,
            PacoteGuiaDeAlquimia::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
