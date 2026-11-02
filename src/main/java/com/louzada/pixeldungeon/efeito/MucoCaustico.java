package com.louzada.pixeldungeon.efeito;

import com.louzada.pixeldungeon.dungeon.Andares;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

// Muco cáustico (o "ooze" do Shattered): um ácido que gruda na pele e vai derretendo.
// O Goo passa ele em 1 de cada 3 golpes. Dura 20 turnos (20 s) e sai na hora se a pessoa entrar na água.
// Dano igual ao Shattered: no esgoto, 1 a cada 2 turnos; no andar 5, 1 por turno; mais fundo, 1 + andar/5 por turno.
public class MucoCaustico extends MobEffect {
    public static final int DURACAO = 20 * 20;

    public MucoCaustico() {
        super(MobEffectCategory.HARMFUL, 0x1E1E1E);
    }

    // Roda todo tick: assim dá pra lavar o muco assim que a pessoa pisa na água
    @Override
    public boolean shouldApplyEffectTickThisTick(int duracao, int amplificador) {
        return true;
    }

    // Devolver false tira o efeito
    @Override
    public boolean applyEffectTick(LivingEntity alvo, int amplificador) {
        if (alvo.level().isClientSide()) {
            return true;
        }
        if (alvo.isInWater()) {
            return false;
        }
        int andar = Andares.andarPelaAltura(alvo.getBlockY());
        if (andar > 5 && alvo.tickCount % 20 == 0) {
            alvo.hurt(alvo.damageSources().magic(), 1 + andar / 5);
        } else if (andar == 5 && alvo.tickCount % 20 == 0) {
            alvo.hurt(alvo.damageSources().magic(), 1);
        } else if (andar < 5 && alvo.tickCount % 40 == 0) {
            alvo.hurt(alvo.damageSources().magic(), 1);
        }
        return true;
    }
}
