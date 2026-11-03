package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// O que cada maldição de arma faz quando o herói acerta um golpe, com as chances do Shattered.
public class Maldicoes {
    private static final int TURNO = 20;
    private static final double ALCANCE_DA_VISAO = 32;

    // Inconstante ativada: a precisão cai pra um quinto
    public static float multiplicadorDePrecisao(ServerPlayer jogador, ItemStack arma) {
        return NivelDoItem.maldicao(arma) == Maldicao.INCONSTANTE && jogador.hasEffect(ModEfeitos.DESVIANTE) ? 0.2F : 1.0F;
    }

    // Chamado em cada golpe que acerta; devolve o dano (a polarizante muda o dano)
    public static float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano) {
        Maldicao maldicao = NivelDoItem.maldicao(arma);
        if (maldicao == null) {
            return dano;
        }
        RandomSource sorteio = jogador.getRandom();
        ServerLevel mundo = jogador.serverLevel();
        switch (maldicao) {
            case IRRITANTE -> {
                if (sorteio.nextFloat() < 1 / 20F) {
                    irritar(jogador, sorteio);
                }
            }
            case DESLOCAMENTO -> {
                if (sorteio.nextFloat() < 1 / 12F && !(alvo instanceof Goo)) {
                    Teleporte.paraLugarAleatorio(mundo, alvo);
                }
            }
            case OFUSCANTE -> {
                if (sorteio.nextFloat() < 1 / 10F) {
                    ofuscar(jogador, alvo);
                }
            }
            case POLARIZANTE -> {
                return sorteio.nextBoolean() ? dano * 1.5F : 0;
            }
            case SACRIFICANTE -> {
                if (sorteio.nextFloat() < 1 / 10F) {
                    // Quanto mais vida, mais sangue: (vida / vida máxima)² × vida máxima / 8
                    float porcentagem = jogador.getHealth() / jogador.getMaxHealth();
                    float sangue = porcentagem * porcentagem * jogador.getMaxHealth() / 8F;
                    if (sorteio.nextFloat() < sangue) {
                        EfeitosNoTick.sangrar(jogador, Math.max(1, Math.round(sangue)));
                    }
                }
            }
            case INCONSTANTE -> {
                // Acertar um golpe desfaz; senão, 1 em 4 golpes deixa imprecisa por 10 turnos
                if (jogador.hasEffect(ModEfeitos.DESVIANTE)) {
                    jogador.removeEffect(ModEfeitos.DESVIANTE);
                } else if (sorteio.nextFloat() < 1 / 4F) {
                    jogador.addEffect(new MobEffectInstance(ModEfeitos.DESVIANTE, 10 * TURNO));
                }
            }
        }
        return dano;
    }

    // Irritante: grita, e todos os inimigos do andar vêm atrás do herói
    private static void irritar(ServerPlayer jogador, RandomSource sorteio) {
        int fala = sorteio.nextInt(33) != 0 ? 1 + sorteio.nextInt(10) : 11 + sorteio.nextInt(3);
        jogador.displayClientMessage(Component.translatable("maldicao.pixeldungeon.irritante.fala_" + fala).withStyle(ChatFormatting.RED), false);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.PLAYERS, 1.5F, 1.0F);
        NumerosFlutuantes.mostrar(jogador, "!!!", NumerosFlutuantes.VERMELHO);
        Furtividade.revelar(jogador);
        for (MobDoShattered inimigo : jogador.level().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(64, 8, 64))) {
            inimigo.notar(jogador);
        }
    }

    // Ofuscante: um clarão cega quem bateu (10 turnos) e quem estiver vendo (5 turnos)
    private static void ofuscar(ServerPlayer jogador, LivingEntity alvo) {
        jogador.level().playSound(null, alvo.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (LivingEntity quemViu : jogador.level().getEntitiesOfClass(LivingEntity.class, alvo.getBoundingBox().inflate(ALCANCE_DA_VISAO))) {
            if (quemViu == jogador) {
                EfeitosNoTick.cegar(quemViu, 10 * TURNO);
            } else if (quemViu.hasLineOfSight(alvo)) {
                EfeitosNoTick.cegar(quemViu, 5 * TURNO);
            }
        }
    }
}
