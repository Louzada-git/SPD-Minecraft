package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// Luz guia do Clérigo: um raio de luz amarela, 2-8 de dano mágico.
// Quem é atingido fica iluminado: o próximo ataque físico contra ele acerta com certeza.
public class LuzGuia extends MisselMagico {
    private static final int AMARELO = 0xFFFF44;
    // No Shattered a luz fica até alguém bater; aqui dura 1 minuto
    private static final int DURACAO_DA_LUZ = Turno.ticks(60);

    public LuzGuia(EntityType<? extends LuzGuia> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public LuzGuia(Level mundo, LivingEntity dono) {
        super(ModEntidades.LUZ_GUIA.get(), mundo, dono, 0);
    }

    @Override
    protected int cor() {
        return AMARELO;
    }

    // Não é varinha: a refeição empoderadora não vale
    @Override
    protected int bonusDeQuemLancou(ServerPlayer jogador) {
        return 0;
    }

    @Override
    protected void aoAtingir(LivingEntity alvo) {
        alvo.addEffect(new MobEffectInstance(ModEfeitos.ILUMINADO, DURACAO_DA_LUZ));
    }
}
