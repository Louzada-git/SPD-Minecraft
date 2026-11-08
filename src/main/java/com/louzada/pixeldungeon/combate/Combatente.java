package com.louzada.pixeldungeon.combate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

// Quem luta com as regras do Shattered (o "Char" do Shattered): o herói, os inimigos e a imagem espelhada.
// O Combate pergunta os números pra cada um e avisa quando acerta ou é acertado. Cada um muda só o que é dele.
public interface Combatente {
    LivingEntity entidade();

    // Precisão (quanto maior, mais acerta)
    float precisao(Golpe golpe);

    // Esquiva (quanto maior, mais desvia)
    float esquiva(Golpe golpe);

    // Dano sorteado do golpe, antes da armadura de quem apanha
    float sortearDano(Golpe golpe);

    // Armadura sorteada contra esse golpe (o dano cai esse tanto)
    int sortearArmadura(Golpe golpe);

    // O próximo golpe acerta sem sortear (a mordida do mímico escondido)
    default boolean acertoGarantido() {
        return false;
    }

    // Consegue pegar os outros de surpresa? (só o herói)
    default boolean fazAtaqueSurpresa() {
        return false;
    }

    // Esse atacante pegou de surpresa? (o inimigo dormindo ou distraído)
    default boolean surpreendidoPor(Combatente atacante) {
        return false;
    }

    // Antes da armadura: dano a mais de quem bate (a luz abrasadora do Clérigo, no inimigo iluminado)
    default float antesDaArmadura(Golpe golpe, float dano) {
        return dano;
    }

    // Foi acertado: as maldições da armadura agem aqui (devolve o dano)
    default float aoSerAtingido(Golpe golpe, float dano) {
        return dano;
    }

    // Depois da armadura segurar o dano (o glifo da viscosidade adia parte do que passou)
    default float depoisDaArmadura(Golpe golpe, float dano) {
        return dano;
    }

    // Acertou: talentos, maldições da arma, efeitos especiais dos inimigos (devolve o dano)
    default float aoAcertar(Golpe golpe, float dano) {
        return dano;
    }

    // Depois de atacar, acertando ou não (atacar tira a invisibilidade do herói)
    default void depoisDeAtacar(Golpe golpe, boolean acertou) {
    }

    // O Combatente de qualquer criatura: os inimigos e a imagem espelhada já são, o herói ganha um
    // HeroiEmCombate, e os bichos que não são do Shattered usam números padrão (CombatenteComum)
    static Combatente de(LivingEntity criatura) {
        if (criatura instanceof Combatente combatente) {
            return combatente;
        }
        if (criatura instanceof ServerPlayer jogador) {
            return new HeroiEmCombate(jogador);
        }
        return new CombatenteComum(criatura);
    }
}
