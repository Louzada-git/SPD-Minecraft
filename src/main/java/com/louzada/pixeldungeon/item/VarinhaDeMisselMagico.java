package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.entidade.MisselMagico;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

// Varinha de míssil mágico, igual ao Shattered: 3 cargas, um raio de energia que sempre acerta
// e dá 2-8 de dano mágico (+1/+2 por nível). A armadura não segura.
public class VarinhaDeMisselMagico extends Varinha {

    public static int danoMinimo(int nivel) {
        return 2 + nivel;
    }

    public static int danoMaximo(int nivel) {
        return 8 + 2 * nivel;
    }

    public static int sortearDano(RandomSource sorteio, int nivel) {
        return Sorteio.normal(sorteio, danoMinimo(nivel), danoMaximo(nivel));
    }

    @Override
    public int cargasIniciais() {
        return 3;
    }

    @Override
    public void lancar(ServerPlayer jogador, int nivel) {
        MisselMagico missil = new MisselMagico(jogador.level(), jogador, nivel);
        missil.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 2.0F, 0.0F);
        jogador.level().addFreshEntity(missil);
        jogador.level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F,
                1.4F);
    }

    @Override
    public void dica(List<Component> linhas, int nivel) {
        linhas.add(Component.translatable("dica.pixeldungeon.missel_magico", danoMinimo(nivel), danoMaximo(nivel)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
