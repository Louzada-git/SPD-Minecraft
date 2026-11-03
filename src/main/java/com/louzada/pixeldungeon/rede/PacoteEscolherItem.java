package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: abre a tela de escolher um item pro pergaminho (melhoria, identificação...).
//   pergaminho: o tipo do pergaminho ("melhoria")
//   espacos: os espaços do inventário com itens que servem
public record PacoteEscolherItem(String pergaminho, List<Integer> espacos) implements CustomPacketPayload {
    public static final Type<PacoteEscolherItem> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "escolher_item"));

    public static final StreamCodec<ByteBuf, PacoteEscolherItem> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteEscolherItem::pergaminho,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), PacoteEscolherItem::espacos,
            PacoteEscolherItem::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
