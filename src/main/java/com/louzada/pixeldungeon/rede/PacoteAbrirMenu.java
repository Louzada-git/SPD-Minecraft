package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: "abra o menu de escolha de herói".
// runEmAndamento avisa se já tem gente jogando (aí o jogador entra no andar 1 da run atual).
public record PacoteAbrirMenu(boolean runEmAndamento) implements CustomPacketPayload {
    public static final Type<PacoteAbrirMenu> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "abrir_menu"));

    public static final StreamCodec<ByteBuf, PacoteAbrirMenu> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PacoteAbrirMenu::runEmAndamento,
            PacoteAbrirMenu::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
