package com.louzada.pixeldungeon.entidade;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Gnoll explorador (andares 2 a 4): uma hiena humanoide.
public class GnollExplorador extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(12, 10, 4, 1, 6, 0, 2, 2, 8, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH);

    public GnollExplorador(EntityType<? extends GnollExplorador> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }
}
