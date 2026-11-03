package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.Furtividade;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

// O ataque corpo a corpo dos inimigos do Shattered (e da imagem espelhada):
//   - um golpe por turno (2 segundos)
//   - quem está caçando um herói invisível continua indo atrás dele, mas não consegue atacar
public class AtaqueDoShattered extends MeleeAttackGoal {
    private PathfinderMob quemAtaca;
    private long proximoAtaque = 0;

    public AtaqueDoShattered(PathfinderMob quemAtaca, double velocidade, boolean seguirSemVer) {
        super(quemAtaca, velocidade, seguirSemVer);
        this.quemAtaca = quemAtaca;
    }

    @Override
    protected boolean isTimeToAttack() {
        return quemAtaca.level().getGameTime() >= proximoAtaque;
    }

    @Override
    protected void resetAttackCooldown() {
        super.resetAttackCooldown();
        proximoAtaque = quemAtaca.level().getGameTime() + Turno.TICKS;
    }

    @Override
    protected int getAttackInterval() {
        return adjustedTickDelay(Turno.TICKS);
    }

    @Override
    protected boolean canPerformAttack(LivingEntity alvo) {
        return !Furtividade.invisivel(alvo) && super.canPerformAttack(alvo);
    }
}
