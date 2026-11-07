package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// Raio solar do Clérigo (talento do tier 2): um raio de sol que dá 4-8 / 6-12 de dano e cega por 4/6 turnos.
// Igual ao Shattered, cada alvo só fica cego pelo raio uma vez: acertando de novo enquanto ele está cego, ele fica paralisado.
public class RaioSolar extends MisselMagico {
    private static final int DOURADO = 0xFFD966;

    // Os pontos no talento de quem lançou (1 ou 2)
    private int pontos = 1;

    public RaioSolar(EntityType<? extends RaioSolar> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public RaioSolar(Level mundo, LivingEntity dono, int pontos) {
        super(ModEntidades.RAIO_SOLAR.get(), mundo, dono, 0);
        this.pontos = Math.max(1, pontos);
    }

    @Override
    protected int cor() {
        return DOURADO;
    }

    @Override
    protected boolean ehVarinha() {
        return false;
    }

    @Override
    protected int bonusDeQuemLancou(ServerPlayer jogador) {
        return 0;
    }

    @Override
    protected int sortearDano() {
        int minimo = 2 + 2 * pontos;
        return Sorteio.normal(random, minimo, 2 * minimo);
    }

    @Override
    protected void aoAtingir(LivingEntity alvo) {
        // Igual ao Shattered: cega cada alvo só uma vez; acertando de novo enquanto ele está cego, paralisa
        int turnos = 2 + 2 * pontos;
        CompoundTag marcas = alvo.getPersistentData();
        long agora = alvo.level().getGameTime();
        String cegoAte = TalentosDoTier2.RAIO_SOLAR_CEGOU + "_ate";
        if (marcas.getLong(cegoAte) > agora) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.ticks(turnos)));
            marcas.remove(cegoAte);
        } else if (!marcas.getBoolean(TalentosDoTier2.RAIO_SOLAR_CEGOU)) {
            EfeitosNoTick.cegar(alvo, Turno.ticks(turnos));
            marcas.putBoolean(TalentosDoTier2.RAIO_SOLAR_CEGOU, true);
            marcas.putLong(cegoAte, agora + Turno.ticks(turnos));
        }
    }
}
