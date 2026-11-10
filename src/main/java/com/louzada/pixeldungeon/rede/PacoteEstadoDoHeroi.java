package com.louzada.pixeldungeon.rede;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Servidor -> jogador: o que a tela de talentos, o inventário e a barra da Duelista precisam saber.
//   talentos: "id=pontos;id=pontos;..."
//   numeros: nível, pontos de talento livres, cargas da habilidade, cargas máximas, golpes seguidos (luvas), ouro, força,
//            energia alquímica, tiers com os pontos da inspiração divina (um bit por tier), páginas do guia de alquimia (um bit por página)
public record PacoteEstadoDoHeroi(String classe, String talentos, List<Integer> numeros) implements CustomPacketPayload {
    public static final Type<PacoteEstadoDoHeroi> TIPO = new Type<>(ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "estado_do_heroi"));

    public static final StreamCodec<ByteBuf, PacoteEstadoDoHeroi> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacoteEstadoDoHeroi::classe,
            ByteBufCodecs.STRING_UTF8, PacoteEstadoDoHeroi::talentos,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), PacoteEstadoDoHeroi::numeros,
            PacoteEstadoDoHeroi::new);

    public int numero(int posicao) {
        return posicao < numeros.size() ? numeros.get(posicao) : 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }
}
