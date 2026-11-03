package com.louzada.pixeldungeon.efeito;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// Efeito que não faz nada sozinho: quem olha se ele está ativo é o combate (atordoado, iluminado, arma celestial...)
public class EfeitoSimples extends MobEffect {
    public EfeitoSimples(MobEffectCategory categoria, int cor) {
        super(categoria, cor);
    }
}
