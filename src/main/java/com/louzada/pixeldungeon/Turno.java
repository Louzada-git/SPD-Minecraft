package com.louzada.pixeldungeon;

import net.minecraft.world.entity.Entity;

// O tempo do Shattered dentro do Minecraft: 1 turno = 2 segundos = 40 ticks.
// Tudo que no Shattered conta em turnos (efeitos, recargas, ataques, inimigos) usa este número.
public final class Turno {
    public static final int TICKS = 40;

    private Turno() {
    }

    // Quantos ticks duram tantos turnos
    public static int ticks(float turnos) {
        return Math.round(turnos * TICKS);
    }

    // Quantos turnos (inteiros, pra cima) cabem em tantos ticks
    public static int turnos(int ticks) {
        return (ticks + TICKS - 1) / TICKS;
    }

    // Começou um turno novo pra essa entidade? (pras coisas que acontecem uma vez por turno)
    public static boolean virou(Entity entidade) {
        return entidade.tickCount % TICKS == 0;
    }
}
