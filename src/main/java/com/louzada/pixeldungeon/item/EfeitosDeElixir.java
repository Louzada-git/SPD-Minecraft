package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// O que os elixires do Shattered fazem (ver TipoDeElixir), com os números de lá.
//   sangue de dragão: por 50 turnos, imune a fogo, e cada golpe tem metade de chance de pôr fogo no inimigo
//   toque gélido: por 50 turnos, imune a frio, e cada golpe deixa o inimigo resfriado
//   essência tóxica: por 50 turnos, solta gás tóxico em volta (e fica imune a ele e a veneno)
//   poder: +1 de força pra sempre, e mais vida máxima (4 + vida/20) que vai sumindo nos 5 níveis seguintes
//   queda suave: por 50 turnos, cair não machuca
//   armadura arcana: 5 + nível/2 de armadura contra magia, que perde 1 a cada 80 turnos
//   rejuvenescimento aquático: cura 1,5x a vida máxima aos poucos (1/50 da vida por turno), só enquanto estiver na água
public class EfeitosDeElixir {
    public static final int TURNOS = 50;
    public static final int TURNOS_DA_ARMADURA_ARCANA = 80;
    public static final int NIVEIS_DO_PODER = 5;
    public static final String PODER = "elixir_do_poder";
    private static final String CURA_AQUATICA = "pixeldungeon_cura_aquatica";

    public static void beber(ServerPlayer jogador, TipoDeElixir tipo) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        switch (tipo) {
            case SANGUE_DE_DRAGAO -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.FOGO_INTERIOR, TURNOS * Turno.TICKS));
                jogador.clearFire();
                Particulas.subindoEmVolta(jogador, ParticleTypes.FLAME, 20, 0.05);
            }
            case TOQUE_GELIDO -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.FRIO_INTERIOR, TURNOS * Turno.TICKS));
                jogador.removeEffect(ModEfeitos.CONGELADO);
                jogador.removeEffect(ModEfeitos.RESFRIADO);
                Particulas.subindoEmVolta(jogador, ParticleTypes.SNOWFLAKE, 20, 0.05);
            }
            case ESSENCIA_TOXICA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.ESSENCIA_TOXICA, TURNOS * Turno.TICKS));
                jogador.removeEffect(ModEfeitos.VENENO);
                jogador.removeEffect(MobEffects.POISON);
            }
            case PODER -> {
                dados.definirForca(dados.forcaBase() + 1);
                dados.definir(PODER, NIVEIS_DO_PODER);
                RegrasDoHeroi.aplicar(jogador);
                jogador.heal(bonusDeVida(dados));
                jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.elixir_poder", dados.forca()).withStyle(ChatFormatting.GREEN), false);
                Particulas.subindoEmVolta(jogador, ParticleTypes.HAPPY_VILLAGER, 16, 0.1);
                RedeDoHeroi.enviarEstado(jogador, true);
            }
            case QUEDA_SUAVE -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.QUEDA_SUAVE, TURNOS * Turno.TICKS));
                jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.queda_suave").withStyle(ChatFormatting.GREEN), true);
            }
            case ARMADURA_ARCANA -> {
                int nivel = 5 + dados.nivel() / 2;
                jogador.addEffect(new MobEffectInstance(ModEfeitos.ARMADURA_ARCANA, TURNOS_DA_ARMADURA_ARCANA * Turno.TICKS, nivel - 1));
                Particulas.subindoEmVolta(jogador, ParticleTypes.WITCH, 16, 0.05);
            }
            case REJUVENESCIMENTO_AQUATICO -> {
                int cura = Math.round(jogador.getMaxHealth() * 1.5F);
                jogador.getPersistentData().putInt(CURA_AQUATICA, Math.max(cura, jogador.getPersistentData().getInt(CURA_AQUATICA)));
                jogador.addEffect(new MobEffectInstance(ModEfeitos.REJUVENESCIMENTO, MobEffectInstance.INFINITE_DURATION));
            }
        }
    }

    // ---- O elixir do poder ----

    // A vida máxima a mais do elixir do poder agora (some aos poucos nos 5 níveis seguintes)
    public static int bonusDeVida(DadosDoHeroi dados) {
        int restante = Math.min(NIVEIS_DO_PODER, dados.inteiro(PODER));
        if (restante <= 0) {
            return 0;
        }
        float cheio = 4 + RegrasDoHeroi.vidaMaxima(dados.nivel()) / 20F;
        return Math.round(cheio * restante / NIVEIS_DO_PODER);
    }

    // Subiu de nível: o bônus de vida do elixir do poder diminui
    public static void aoSubirNivel(DadosDoHeroi dados, int niveis) {
        if (dados.inteiro(PODER) > 0) {
            dados.definir(PODER, Math.max(0, dados.inteiro(PODER) - niveis));
        }
    }

    // ---- A cada turno ----

    public static void aoTurno(LivingEntity criatura) {
        if (criatura.hasEffect(ModEfeitos.FOGO_INTERIOR)) {
            criatura.clearFire();
        }
        if (criatura.hasEffect(ModEfeitos.ESSENCIA_TOXICA) && criatura.level() instanceof ServerLevel mundo) {
            NuvemDeGas.soltar(mundo, criatura.position(), NuvemDeGas.Tipo.TOXICO, 3.0F, 2);
        }
        if (criatura.hasEffect(ModEfeitos.REJUVENESCIMENTO)) {
            int restante = criatura.getPersistentData().getInt(CURA_AQUATICA);
            if (restante <= 0) {
                criatura.removeEffect(ModEfeitos.REJUVENESCIMENTO);
            } else if (criatura.isInWater() && criatura.getHealth() < criatura.getMaxHealth()) {
                int cura = Math.min(restante, Math.max(1, Math.round(criatura.getMaxHealth() / 50F)));
                cura = Math.min(cura, (int) Math.ceil(criatura.getMaxHealth() - criatura.getHealth()));
                EfeitosNoTick.curarAgora(criatura, cura);
                criatura.getPersistentData().putInt(CURA_AQUATICA, restante - cura);
            }
        }
    }

    // A armadura arcana acabou o tempo: perde 1 nível
    public static boolean aoAcabar(LivingEntity criatura, MobEffectInstance efeito) {
        if (!efeito.is(ModEfeitos.ARMADURA_ARCANA)) {
            return false;
        }
        if (efeito.getAmplifier() > 0) {
            criatura.addEffect(new MobEffectInstance(ModEfeitos.ARMADURA_ARCANA, TURNOS_DA_ARMADURA_ARCANA * Turno.TICKS, efeito.getAmplifier() - 1));
        }
        return true;
    }

    // ---- Golpes e dano ----

    // O herói acertou alguém: o sangue de dragão (metade das vezes) põe fogo, o toque gélido resfria
    public static void aoAcertar(LivingEntity atacante, LivingEntity alvo) {
        if (atacante.hasEffect(ModEfeitos.FOGO_INTERIOR) && atacante.getRandom().nextInt(2) == 0) {
            alvo.igniteForSeconds(8);
        }
        if (atacante.hasEffect(ModEfeitos.FRIO_INTERIOR)) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.RESFRIADO, 3 * Turno.TICKS));
        }
    }

    // Imune a fogo (sangue de dragão); a armadura arcana diminui o dano mágico
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity alvo = evento.getEntity();
        if (alvo.level().isClientSide()) {
            return;
        }
        if (alvo.hasEffect(ModEfeitos.FOGO_INTERIOR) && evento.getSource().is(DamageTypeTags.IS_FIRE)) {
            alvo.clearFire();
            evento.setCanceled(true);
            return;
        }
        MobEffectInstance arcana = alvo.getEffect(ModEfeitos.ARMADURA_ARCANA);
        if (arcana != null && EfeitosDePergaminhoExotico.danoMagico(evento.getSource())) {
            int defesa = Sorteio.normal(alvo.getRandom(), 0, arcana.getAmplifier() + 1);
            evento.setAmount(Math.max(0, evento.getAmount() - defesa));
        }
    }

    // Os efeitos que os elixires não deixam pegar
    public static boolean imune(LivingEntity criatura, @Nullable MobEffectInstance efeito) {
        if (efeito == null) {
            return false;
        }
        Holder<MobEffect> qual = efeito.getEffect();
        if (criatura.hasEffect(ModEfeitos.FRIO_INTERIOR) && (qual.is(ModEfeitos.CONGELADO.getKey()) || qual.is(ModEfeitos.RESFRIADO.getKey()))) {
            return true;
        }
        return criatura.hasEffect(ModEfeitos.ESSENCIA_TOXICA) && (qual.is(ModEfeitos.VENENO.getKey()) || qual.equals(MobEffects.POISON));
    }

    // Queda suave: cair não machuca
    public static void aoCair(LivingFallEvent evento) {
        if (evento.getEntity().hasEffect(ModEfeitos.QUEDA_SUAVE)) {
            evento.setDamageMultiplier(0);
        }
    }
}
