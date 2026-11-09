package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// Servidor -> jogador: abre a conversa com o fantasma triste.
//   texto: o que o fantasma diz
//   arma, armadura: as recompensas pra escolher (vazias se ainda não é hora)
public record PacoteDoFantasma(Component texto, ItemStack arma, ItemStack armadura) implements CustomPacketPayload {
    public static final Type<PacoteDoFantasma> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "fantasma"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteDoFantasma> CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, PacoteDoFantasma::texto,
            ItemStack.OPTIONAL_STREAM_CODEC, PacoteDoFantasma::arma,
            ItemStack.OPTIONAL_STREAM_CODEC, PacoteDoFantasma::armadura,
            PacoteDoFantasma::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
