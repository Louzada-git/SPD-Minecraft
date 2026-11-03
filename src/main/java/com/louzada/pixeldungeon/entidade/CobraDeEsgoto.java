package com.louzada.pixeldungeon.entidade;

import java.util.Random;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Cobra de esgoto (andares 1 a 3): pouca vida, mas esquiva demais. O jeito é o ataque surpresa.
// Igual ao Shattered, 1 em 4 deixa uma semente.
public class CobraDeEsgoto extends MobDoShattered {
    private static final float CHANCE_DE_SEMENTE = 0.25F;

    public static final Atributos ATRIBUTOS = new Atributos(4, 10, 25, 1, 4, 0, 0, 2, 7, 0.27);
    public static final Sons SONS = new Sons(SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH);

    public CobraDeEsgoto(EntityType<? extends CobraDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    public ItemStack sortearSaque() {
        if (getRandom().nextFloat() < CHANCE_DE_SEMENTE) {
            return SaqueDoAndar.sementeAleatoria(new Random(getRandom().nextLong()));
        }
        return ItemStack.EMPTY;
    }
}
