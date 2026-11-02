package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Caranguejo de esgoto (andares 3 e 4): muito rápido e com carapaça dura. Às vezes deixa carne misteriosa.
public class CaranguejoDeEsgoto extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(15, 12, 5, 1, 7, 0, 4, 4, 9, 0.38);
    public static final Sons SONS = new Sons(SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH);
    // Igual ao Shattered: 1 em 6
    private static final float CHANCE_DE_CARNE = 0.167F;

    public CaranguejoDeEsgoto(EntityType<? extends CaranguejoDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    public ItemStack sortearSaque() {
        if (getRandom().nextFloat() < CHANCE_DE_CARNE) {
            return new ItemStack(ModItens.CARNE_MISTERIOSA.get());
        }
        return ItemStack.EMPTY;
    }
}
