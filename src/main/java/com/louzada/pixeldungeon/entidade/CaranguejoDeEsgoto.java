package com.louzada.pixeldungeon.entidade;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Caranguejo de esgoto (andares 3 e 4): muito rápido e com carapaça dura.
public class CaranguejoDeEsgoto extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(15, 12, 5, 1, 7, 0, 4, 4, 9, 0.38);

    public CaranguejoDeEsgoto(EntityType<? extends CaranguejoDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS);
    }
}
