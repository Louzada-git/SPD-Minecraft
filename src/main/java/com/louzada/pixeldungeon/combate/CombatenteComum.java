package com.louzada.pixeldungeon.combate;

import net.minecraft.world.entity.LivingEntity;

// Criaturas que não são do Shattered (do Minecraft ou de outros mods): precisão 10, nenhuma esquiva nem armadura,
// e o dano que o Minecraft calculou.
public record CombatenteComum(LivingEntity entidade) implements Combatente {
    private static final float PRECISAO = 10;

    @Override
    public float precisao(Golpe golpe) {
        return PRECISAO;
    }

    @Override
    public float esquiva(Golpe golpe) {
        return 0;
    }

    @Override
    public float sortearDano(Golpe golpe) {
        return golpe.danoDoMinecraft();
    }

    @Override
    public int sortearArmadura(Golpe golpe) {
        return 0;
    }
}
