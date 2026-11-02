package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Rato albino: um rato raro (1 em 50). Metade das mordidas fazem sangrar, e sempre deixa carne misteriosa.
public class RatoAlbino extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(12, 8, 2, 1, 4, 0, 1, 2, 5, 0.25);

    public RatoAlbino(EntityType<? extends RatoAlbino> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, RatoMarsupial.SONS);
    }

    // Sangramento: no Minecraft, um veneno curtinho
    @Override
    public void aoAcertar(Entity alvo, float dano) {
        if (dano > 0 && alvo instanceof LivingEntity criatura && getRandom().nextBoolean()) {
            criatura.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20));
        }
    }

    @Override
    public ItemStack sortearSaque() {
        return new ItemStack(ModItens.CARNE_MISTERIOSA.get());
    }
}
