package com.louzada.pixeldungeon.entidade;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

// Os inimigos da missão do fantasma vagam pelo andar indo pro lado do herói (no Shattered, de dois lugares sorteados
// pra passear, eles escolhem o mais perto do herói): sem ninguém pra caçar, a cada 3 turnos andam na direção dele.
public class VagarAteOHeroi extends Goal {
    private static final double ALCANCE = 160;

    private MobDoShattered quem;
    @Nullable
    private Player heroi;

    public VagarAteOHeroi(MobDoShattered quem) {
        this.quem = quem;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (quem.getTarget() != null || quem.dormindo() || quem.tickCount % (3 * Turno.TICKS) != 0) {
            return false;
        }
        heroi = quem.level().getNearestPlayer(quem, ALCANCE);
        return heroi != null && !heroi.isSpectator() && !heroi.isCreative() && quem.distanceTo(heroi) > 6;
    }

    @Override
    public boolean canContinueToUse() {
        return heroi != null && quem.getTarget() == null && !quem.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (heroi != null) {
            quem.getNavigation().moveTo(heroi, 0.8);
        }
    }

    @Override
    public void stop() {
        heroi = null;
    }
}
