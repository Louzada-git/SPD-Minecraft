package com.louzada.pixeldungeon.efeito;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Efeito que não faz nada sozinho: quem olha se ele está ativo é o combate (atordoado, iluminado, arma celestial...)
// ou o EfeitosNoTick (veneno, sangramento...). Alguns mudam a velocidade de quem está com eles.
public class EfeitoSimples extends MobEffect {
    public EfeitoSimples(MobEffectCategory categoria, int cor) {
        super(categoria, cor);
    }

    // Paralisado, congelado, dormindo: não anda nem pula
    public static EfeitoSimples quePrende(MobEffectCategory categoria, int cor, String nome) {
        EfeitoSimples efeito = new EfeitoSimples(categoria, cor);
        efeito.addAttributeModifier(Attributes.MOVEMENT_SPEED, id(nome + "_velocidade"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        efeito.addAttributeModifier(Attributes.JUMP_STRENGTH, id(nome + "_pulo"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return efeito;
    }

    // Muda a velocidade de andar (aleijado: -50%, pressa: +200%)
    public static EfeitoSimples queMudaAVelocidade(MobEffectCategory categoria, int cor, String nome, double quanto) {
        EfeitoSimples efeito = new EfeitoSimples(categoria, cor);
        efeito.addAttributeModifier(Attributes.MOVEMENT_SPEED, id(nome + "_velocidade"), quanto, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return efeito;
    }

    // Muda a velocidade de ataque (0,6 = 60% mais rápido)
    public static EfeitoSimples queMudaAVelocidadeDeAtaque(MobEffectCategory categoria, int cor, String nome, double quanto) {
        EfeitoSimples efeito = new EfeitoSimples(categoria, cor);
        efeito.addAttributeModifier(Attributes.ATTACK_SPEED, id(nome + "_ataque"), quanto, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return efeito;
    }

    private static ResourceLocation id(String nome) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "efeito/" + nome);
    }
}
