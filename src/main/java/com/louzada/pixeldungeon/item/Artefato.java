package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

// Artefatos do Shattered (capa das sombras, tomo sagrado): sobem de nível com o uso, até o nível 10.
//   cargas máximas: nível + 3 (até 10)
//   recarga: 45 - (cargas faltando) turnos por carga
//   experiência: cada carga gasta dá 10 (mais se o herói estiver acima do nível "certo" do artefato)
public class Artefato {
    public static final int NIVEL_MAXIMO = 10;

    public static int nivel(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.NIVEL.get(), 0);
    }

    public static int cargasMaximas(ItemStack pilha) {
        return Math.min(nivel(pilha) + 3, 10);
    }

    public static long ticksParaCarregar(int faltando) {
        return (45 - faltando) * (long) Turno.TICKS;
    }

    // Igual ao Shattered: o nível "certo" do herói pra um artefato é 1 + 2 x nível do artefato
    public static void ganharExperiencia(ServerPlayer jogador, ItemStack pilha, int nivelDoHeroi, float cargasGastas, String chaveDaMensagem) {
        int nivel = nivel(pilha);
        int diferenca = nivelDoHeroi - (1 + nivel * 2);
        if (nivel >= 7) {
            diferenca -= nivel - 6;
        }
        double ganho = diferenca >= 0 ? 10 * Math.pow(1.1, diferenca) : 10 * Math.pow(0.75, -diferenca);
        int experiencia = pilha.getOrDefault(ModComponentes.EXPERIENCIA.get(), 0) + (int) Math.round(cargasGastas * ganho);
        if (experiencia >= (nivel + 1) * 50 && nivel < NIVEL_MAXIMO) {
            nivel++;
            experiencia -= nivel * 50;
            pilha.set(ModComponentes.NIVEL.get(), nivel);
            jogador.displayClientMessage(Component.translatable(chaveDaMensagem).withStyle(ChatFormatting.GREEN), false);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        pilha.set(ModComponentes.EXPERIENCIA.get(), Math.max(0, experiencia));
    }
}
