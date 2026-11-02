package com.louzada.pixeldungeon.entidade;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Rato marsupial (andares 1 a 3): fraco, mas aparece em bando.
public class RatoMarsupial extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura, XP, nível máximo, velocidade
    public static final Atributos ATRIBUTOS = new Atributos(8, 8, 2, 1, 4, 0, 1, 1, 5, 0.25);

    public RatoMarsupial(EntityType<? extends RatoMarsupial> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS);
    }
}
