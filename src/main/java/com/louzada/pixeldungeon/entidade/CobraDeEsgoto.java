package com.louzada.pixeldungeon.entidade;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Cobra de esgoto (andares 1 a 3): pouca vida, mas esquiva demais. O jeito é o ataque surpresa.
public class CobraDeEsgoto extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(4, 10, 25, 1, 4, 0, 0, 2, 7, 0.27);
    public static final Sons SONS = new Sons(SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH);

    public CobraDeEsgoto(EntityType<? extends CobraDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }
}
