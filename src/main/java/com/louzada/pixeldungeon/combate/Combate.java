package com.louzada.pixeldungeon.combate;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// Um golpe com as regras do Shattered, igual pra todo mundo (o "Char.attack" do Shattered):
//   1. acertou? Ataque surpresa, acerto garantido e alvo iluminado sempre acertam;
//      senão, sorteia de 0 até a precisão de quem bate e de 0 até a esquiva de quem apanha ("desviou")
//   2. dano sorteado de quem bate (vezes o multiplicador, mais o bônus) e o que vem antes da armadura
//   3. quem apanha reage (maldições e glifos da armadura) e a armadura dele tira um pouco do dano
//   4. quem bate reage (talentos, maldições da arma, efeitos especiais)
// Os números de cada um vêm do Combatente (o herói, os inimigos, a imagem espelhada).
public class Combate {
    public static final float ERROU = -1;
    // Colado no inimigo, flecha acerta pior (igual às armas de arremesso do Shattered)
    private static final double DISTANCIA_COLADO = 4.5;

    private static boolean golpeJaCalculado = false;

    // Devolve o dano final, ou ERROU (aí já mostrou "desviou")
    public static float resolver(Golpe golpe) {
        Combatente atacante = golpe.atacante();
        Combatente alvo = golpe.alvo();
        LivingEntity quemApanha = alvo.entidade();
        boolean surpresa = atacante.fazAtaqueSurpresa() && alvo.surpreendidoPor(atacante);
        boolean iluminado = quemApanha.hasEffect(ModEfeitos.ILUMINADO);
        golpe.descobrir(surpresa, iluminado);

        // 1. Acertou?
        boolean garantido = golpe.garantido() || atacante.acertoGarantido() || surpresa || iluminado;
        if (!garantido && !acertou(golpe)) {
            mostrarErro(quemApanha, atacante.entidade());
            atacante.depoisDeAtacar(golpe, false);
            return ERROU;
        }

        // 2. Dano (a luz guia some com o golpe)
        float dano = atacante.sortearDano(golpe) * golpe.multiplicador() + golpe.bonus();
        if (iluminado) {
            quemApanha.removeEffect(ModEfeitos.ILUMINADO);
        }
        dano = atacante.antesDaArmadura(golpe, dano);
        // Fraco (varinha de corrupção): 33% a menos
        if (atacante.entidade().hasEffect(ModEfeitos.FRACO)) {
            dano *= 0.67F;
        }

        // 3. Quem apanha (o glifo da viscosidade age depois da armadura; o vulnerável também, igual ao Shattered)
        dano = alvo.aoSerAtingido(golpe, dano);
        dano = Math.max(0, dano - armadura(golpe));
        dano = alvo.depoisDaArmadura(golpe, dano);
        if (quemApanha.hasEffect(ModEfeitos.VULNERAVEL)) {
            dano *= 1.33F;
        }

        // 4. Quem bate
        dano = atacante.aoAcertar(golpe, dano);
        atacante.depoisDeAtacar(golpe, true);
        return dano;
    }

    // Precisão contra esquiva, com os efeitos dos dois lados
    public static boolean acertou(Golpe golpe) {
        LivingEntity quemApanha = golpe.alvo().entidade();
        // Paralisado, congelado ou dormindo não consegue desviar
        float esquiva = EfeitosNoTick.semDefesa(quemApanha) ? 0 : golpe.alvo().esquiva(golpe) * pelosEfeitos(quemApanha);
        // Tufo de furão (acessório): todo mundo desvia mais, igual ao Shattered
        esquiva *= Acessorios.multiplicadorDeEsquiva(Acessorios.daRun(TipoDeAcessorio.TUFO_DE_FURAO));
        return acertou(golpe.atacante().entidade().getRandom(), precisaoDoGolpe(golpe), esquiva);
    }

    // A precisão de quem bate nesse golpe, com os efeitos.
    // Igual ao Shattered: o que é arremessado ou atirado acerta 1,5x melhor de longe e pela metade colado no alvo.
    public static float precisaoDoGolpe(Golpe golpe) {
        LivingEntity quemBate = golpe.atacante().entidade();
        float precisao = golpe.atacante().precisao(golpe) * pelosEfeitos(quemBate);
        if (golpe.deLonge()) {
            precisao *= quemBate.distanceTo(golpe.alvo().entidade()) < DISTANCIA_COLADO ? 0.5F : 1.5F;
        }
        return precisao;
    }

    // Regra de acerto do Shattered: sorteia de 0 até a precisão do atacante e de 0 até a esquiva do defensor;
    // acerta se o sorteio do atacante for maior ou igual
    public static boolean acertou(RandomSource sorteio, float precisao, float esquiva) {
        return sorteio.nextFloat() * precisao >= sorteio.nextFloat() * esquiva;
    }

    // Atordoado: metade. Feitiço (varinha de corrupção): -20%. Abençoado (flor-estrela): +25%. Vale pra precisão e pra esquiva.
    public static float pelosEfeitos(LivingEntity criatura) {
        float fator = 1.0F;
        if (criatura.hasEffect(ModEfeitos.ATORDOADO)) {
            fator *= 0.5F;
        }
        if (criatura.hasEffect(ModEfeitos.FEITICO)) {
            fator *= 0.8F;
        }
        if (criatura.hasEffect(ModEfeitos.ABENCOADO)) {
            fator *= 1.25F;
        }
        return fator;
    }

    // A armadura de quem apanha, mais a pele de árvore (auxílio da natureza): 0 até o nível dela, pra qualquer um
    public static int armadura(Golpe golpe) {
        LivingEntity quemApanha = golpe.alvo().entidade();
        MobEffectInstance pele = quemApanha.getEffect(ModEfeitos.PELE_DE_ARVORE);
        int extra = pele == null ? 0 : Sorteio.normal(quemApanha.getRandom(), 0, pele.getAmplifier() + 1);
        // Armadura de barro (poção exótica): soma do mesmo jeito
        MobEffectInstance barro = quemApanha.getEffect(ModEfeitos.ARMADURA_DE_BARRO);
        extra += barro == null ? 0 : Sorteio.normal(quemApanha.getRandom(), 0, barro.getAmplifier() + 1);
        return extra + golpe.alvo().sortearArmadura(golpe);
    }

    // ---- Golpes já calculados (habilidades da Duelista, flechas) ----

    // Aplica um golpe que já foi resolvido: o evento de dano não calcula de novo
    public static void aplicar(LivingEntity alvo, float dano, DamageSource fonte) {
        golpeJaCalculado = true;
        try {
            alvo.invulnerableTime = 0;
            alvo.hurt(fonte, dano);
        } finally {
            golpeJaCalculado = false;
        }
    }

    public static boolean golpeJaCalculado() {
        return golpeJaCalculado;
    }

    // ---- Avisos ----

    // "desviou", som de golpe no ar, e o alvo acorda
    public static void mostrarErro(LivingEntity alvo, @Nullable Entity atacante) {
        NumerosFlutuantes.mostrarTraduzido(alvo, "combate.pixeldungeon.desviou", NumerosFlutuantes.BRANCO);
        alvo.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        acordar(alvo, atacante);
    }

    // Apanhou (ou quase): o inimigo acorda e vai atrás de quem atacou
    public static void acordar(LivingEntity alvo, @Nullable Entity atacante) {
        if (alvo instanceof MobDoShattered inimigo && atacante instanceof LivingEntity quemAtacou) {
            inimigo.acordar();
            if (!(quemAtacou instanceof Player jogador) || !jogador.isCreative()) {
                inimigo.setTarget(quemAtacou);
            }
        }
    }
}
