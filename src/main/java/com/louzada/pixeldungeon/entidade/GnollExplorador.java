package com.louzada.pixeldungeon.entidade;

import java.util.Random;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.Ouro;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Gnoll explorador (andares 2 a 4): uma hiena humanoide. Igual ao Shattered, metade das vezes deixa ouro.
public class GnollExplorador extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(12, 10, 4, 1, 6, 0, 2, 2, 8, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH);
    private static final float CHANCE_DE_OURO = 0.5F;

    public GnollExplorador(EntityType<? extends GnollExplorador> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    public ItemStack sortearSaque() {
        if (getRandom().nextFloat() < CHANCE_DE_OURO) {
            return Ouro.sortear(new Random(getRandom().nextLong()), SaqueDoAndar.andarDe(this));
        }
        return ItemStack.EMPTY;
    }
}
