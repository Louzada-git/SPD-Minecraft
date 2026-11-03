package com.louzada.pixeldungeon.efeito;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

// O que os efeitos do Shattered fazem a cada turno (1 segundo), pra heróis e inimigos:
//   veneno, sangramento, cura da poção, grama-do-sol, raiz-da-terra, sono mágico, levitação,
//   e os inimigos cegos, tontos, aterrorizados ou furiosos.
// Os números que precisam ser lembrados (quanto ainda falta curar, onde a planta pegou...) ficam nos
// dados extras da própria criatura.
public class EfeitosNoTick {
    private static final int TURNO = 20;
    private static final String CURA_RESTANTE = "pixeldungeon_cura_restante";
    private static final String GRAMA_RESTANTE = "pixeldungeon_grama_restante";
    private static final String GRAMA_PARCIAL = "pixeldungeon_grama_parcial";
    private static final String GRAMA_LUGAR = "pixeldungeon_grama_lugar";
    private static final String RAIZ_RESTANTE = "pixeldungeon_raiz_restante";
    private static final String RAIZ_LUGAR = "pixeldungeon_raiz_lugar";
    private static final String LEVITANDO = "pixeldungeon_levitando";
    // Efeitos que só acabam quando a condição deles acaba (a cura terminar, sair do lugar...)
    private static final int PARA_SEMPRE = MobEffectInstance.INFINITE_DURATION;

    // ---- Começar efeitos ----

    // Veneno do Shattered: dura "turnos" turnos e o dano de cada turno é (turnos que faltam) / 3 + 1
    public static void envenenar(LivingEntity alvo, int turnos) {
        MobEffectInstance atual = alvo.getEffect(ModEfeitos.VENENO);
        int duracao = Math.max(turnos * TURNO, atual == null ? 0 : atual.getDuration());
        alvo.addEffect(new MobEffectInstance(ModEfeitos.VENENO, duracao));
    }

    // Sangramento: o nível vai caindo sozinho (o número do efeito guarda o nível)
    public static void sangrar(LivingEntity alvo, int nivel) {
        alvo.addEffect(new MobEffectInstance(ModEfeitos.SANGRANDO, PARA_SEMPRE, Math.min(250, Math.max(0, nivel - 1))));
    }

    // Cego: inimigos perdem o alvo; heróis ficam com a tela escura
    public static void cegar(LivingEntity alvo, int ticks) {
        alvo.addEffect(new MobEffectInstance(ModEfeitos.CEGO, ticks));
        if (alvo instanceof Player) {
            alvo.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks));
        } else if (alvo instanceof MobDoShattered inimigo) {
            inimigo.setTarget(null);
        }
    }

    // Vertigem: inimigos andam pra qualquer lado; heróis ficam tontos
    public static void tontear(LivingEntity alvo, int ticks) {
        alvo.addEffect(new MobEffectInstance(ModEfeitos.VERTIGEM, ticks));
        if (alvo instanceof Player) {
            alvo.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks + 3 * TURNO));
        }
    }

    // Poção de cura: cura um total de "quantidade", um quarto do que falta a cada turno
    public static void curar(LivingEntity alvo, int quantidade) {
        alvo.getPersistentData().putInt(CURA_RESTANTE, quantidade);
        alvo.addEffect(new MobEffectInstance(ModEfeitos.CURANDO, PARA_SEMPRE));
    }

    // Grama-do-sol: cura até a vida máxima aos pouquinhos, enquanto não sair do quadradinho
    public static void saudeDaGrama(LivingEntity alvo) {
        CompoundTag dados = alvo.getPersistentData();
        dados.putFloat(GRAMA_RESTANTE, alvo.getMaxHealth());
        dados.putFloat(GRAMA_PARCIAL, 0);
        dados.putLong(GRAMA_LUGAR, quadradinho(alvo));
        alvo.addEffect(new MobEffectInstance(ModEfeitos.SAUDE_DA_GRAMA, PARA_SEMPRE));
    }

    // Raiz-da-terra: segura até (andar + 5) / 2 de cada golpe, até um total igual à vida máxima, enquanto não sair do lugar
    public static void raizDaTerra(LivingEntity alvo) {
        CompoundTag dados = alvo.getPersistentData();
        dados.putFloat(RAIZ_RESTANTE, alvo.getMaxHealth());
        dados.putLong(RAIZ_LUGAR, quadradinho(alvo));
        alvo.addEffect(new MobEffectInstance(ModEfeitos.RAIZ_DA_TERRA, PARA_SEMPRE));
    }

    // Quanto a raiz-da-terra segura deste golpe (e gasta da reserva)
    public static float segurarComARaiz(LivingEntity alvo, float dano) {
        if (!alvo.hasEffect(ModEfeitos.RAIZ_DA_TERRA) || dano <= 0) {
            return dano;
        }
        CompoundTag dados = alvo.getPersistentData();
        if (dados.getLong(RAIZ_LUGAR) != quadradinho(alvo)) {
            alvo.removeEffect(ModEfeitos.RAIZ_DA_TERRA);
            return dano;
        }
        int andar = Math.max(1, Andares.andarPelaAltura(alvo.getBlockY()));
        float segura = Math.min(dano, Math.min((andar + 5) / 2, dados.getFloat(RAIZ_RESTANTE)));
        float restante = dados.getFloat(RAIZ_RESTANTE) - segura;
        dados.putFloat(RAIZ_RESTANTE, restante);
        if (restante <= 0) {
            alvo.removeEffect(ModEfeitos.RAIZ_DA_TERRA);
        }
        return dano - segura;
    }

    // Levitação: o herói pode voar por um tempo
    public static void levitar(ServerPlayer jogador, int ticks) {
        jogador.addEffect(new MobEffectInstance(ModEfeitos.LEVITANDO, ticks));
        jogador.getPersistentData().putBoolean(LEVITANDO, true);
        jogador.getAbilities().mayfly = true;
        jogador.onUpdateAbilities();
    }

    // Canção de ninar: depois de 5 turnos sonolento, dorme
    public static void deixarSonolento(LivingEntity alvo) {
        alvo.addEffect(new MobEffectInstance(ModEfeitos.SONOLENTO, 5 * TURNO));
    }

    // ---- Quando acaba ----

    public static void aoAcabarEfeito(MobEffectEvent.Expired evento) {
        MobEffectInstance efeito = evento.getEffectInstance();
        LivingEntity criatura = evento.getEntity();
        if (efeito == null || criatura.level().isClientSide() || !efeito.is(ModEfeitos.SONOLENTO)) {
            return;
        }
        if (criatura instanceof MobDoShattered inimigo) {
            inimigo.dormir();
            NumerosFlutuantes.mostrar(inimigo, "z", NumerosFlutuantes.BRANCO);
        } else if (criatura instanceof Player && criatura.getHealth() < criatura.getMaxHealth()) {
            // O herói dorme e vai se curando até encher a vida (apanhar acorda)
            criatura.addEffect(new MobEffectInstance(ModEfeitos.SONO_MAGICO, PARA_SEMPRE));
        }
    }

    // Apanhar acorda do sono mágico
    public static void aoApanhar(LivingEntity alvo) {
        alvo.removeEffect(ModEfeitos.SONO_MAGICO);
    }

    // ---- Todo turno ----

    public static void aoTick(EntityTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof LivingEntity criatura) || criatura.level().isClientSide() || !criatura.isAlive()
                || criatura.tickCount % TURNO != 0) {
            return;
        }
        CompoundTag dados = criatura.getPersistentData();

        // Veneno: (turnos que faltam) / 3 + 1 de dano
        MobEffectInstance veneno = criatura.getEffect(ModEfeitos.VENENO);
        if (veneno != null) {
            criatura.invulnerableTime = 0;
            criatura.hurt(criatura.damageSources().magic(), veneno.getDuration() / TURNO / 3 + 1);
        }

        // Sangramento: o nível cai pra entre a metade e ele mesmo, e esse é o dano
        MobEffectInstance sangue = criatura.getEffect(ModEfeitos.SANGRANDO);
        if (sangue != null) {
            float nivel = sangue.getAmplifier() + 1;
            nivel = nivel / 2 + criatura.getRandom().nextFloat() * nivel / 2;
            int dano = Math.round(nivel);
            criatura.removeEffect(ModEfeitos.SANGRANDO);
            if (dano > 0) {
                criatura.invulnerableTime = 0;
                criatura.hurt(criatura.damageSources().magic(), dano);
                Particulas.sangue(criatura, Particulas.SANGUE_PADRAO, dano);
                criatura.addEffect(new MobEffectInstance(ModEfeitos.SANGRANDO, PARA_SEMPRE, dano - 1));
            }
        }

        // Poção de cura
        if (criatura.hasEffect(ModEfeitos.CURANDO)) {
            int restante = dados.getInt(CURA_RESTANTE);
            int cura = Math.max(1, Math.round(restante * 0.25F));
            curarAgora(criatura, cura);
            restante -= cura;
            dados.putInt(CURA_RESTANTE, restante);
            if (restante <= 0) {
                criatura.removeEffect(ModEfeitos.CURANDO);
            }
        }

        // Grama-do-sol: cura (40 + vida máxima) / 150 por turno, parado no lugar
        if (criatura.hasEffect(ModEfeitos.SAUDE_DA_GRAMA)) {
            if (dados.getLong(GRAMA_LUGAR) != quadradinho(criatura) || dados.getFloat(GRAMA_RESTANTE) <= 0) {
                criatura.removeEffect(ModEfeitos.SAUDE_DA_GRAMA);
            } else {
                float parcial = dados.getFloat(GRAMA_PARCIAL) + (40 + criatura.getMaxHealth()) / 150F;
                int cura = (int) parcial;
                dados.putFloat(GRAMA_PARCIAL, parcial - cura);
                if (cura > 0) {
                    dados.putFloat(GRAMA_RESTANTE, dados.getFloat(GRAMA_RESTANTE) - cura);
                    curarAgora(criatura, cura);
                }
            }
        }

        // Raiz-da-terra some se sair do lugar
        if (criatura.hasEffect(ModEfeitos.RAIZ_DA_TERRA) && dados.getLong(RAIZ_LUGAR) != quadradinho(criatura)) {
            criatura.removeEffect(ModEfeitos.RAIZ_DA_TERRA);
        }

        // Sono mágico do herói: cura 1 por turno até encher
        if (criatura.hasEffect(ModEfeitos.SONO_MAGICO)) {
            if (criatura.getHealth() >= criatura.getMaxHealth()) {
                criatura.removeEffect(ModEfeitos.SONO_MAGICO);
            } else {
                criatura.heal(1);
                if (criatura.tickCount % 60 == 0) {
                    NumerosFlutuantes.mostrar(criatura, "z", NumerosFlutuantes.BRANCO);
                }
            }
        }

        // A levitação acabou: para de voar e desce devagar
        if (criatura instanceof ServerPlayer jogador && dados.getBoolean(LEVITANDO) && !jogador.hasEffect(ModEfeitos.LEVITANDO)) {
            dados.remove(LEVITANDO);
            if (!jogador.isCreative() && !jogador.isSpectator()) {
                jogador.getAbilities().mayfly = false;
                jogador.getAbilities().flying = false;
                jogador.onUpdateAbilities();
            }
            jogador.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5 * TURNO));
        }

        if (criatura instanceof MobDoShattered inimigo) {
            comportamento(inimigo);
        }
    }

    // Inimigos cegos, tontos, aterrorizados ou furiosos
    private static void comportamento(MobDoShattered inimigo) {
        if (inimigo.hasEffect(ModEfeitos.CEGO)) {
            inimigo.setTarget(null);
            andarAToa(inimigo);
        } else if (inimigo.hasEffect(ModEfeitos.VERTIGEM)) {
            andarAToa(inimigo);
        }
        if (inimigo.hasEffect(ModEfeitos.ATERRORIZADO)) {
            // Foge do herói mais perto
            Player heroi = inimigo.level().getNearestPlayer(inimigo, 32);
            inimigo.setTarget(null);
            if (heroi != null) {
                Vec3 longe = inimigo.position().add(inimigo.position().subtract(heroi.position()).normalize().scale(8));
                inimigo.getNavigation().moveTo(longe.x, longe.y, longe.z, 1.2);
            }
        } else if (inimigo.hasEffect(ModEfeitos.FURIOSO)) {
            // Ataca o que estiver mais perto, até outro inimigo
            LivingEntity maisPerto = null;
            double menor = 16 * 16;
            for (LivingEntity outro : inimigo.level().getEntitiesOfClass(LivingEntity.class, inimigo.getBoundingBox().inflate(16))) {
                if (outro == inimigo || !outro.isAlive() || (outro instanceof Player jogador && (jogador.isSpectator() || jogador.isCreative()))) {
                    continue;
                }
                double distancia = outro.distanceToSqr(inimigo);
                if (distancia < menor) {
                    menor = distancia;
                    maisPerto = outro;
                }
            }
            if (maisPerto != null) {
                inimigo.acordar();
                inimigo.setTarget(maisPerto);
            }
        }
    }

    private static void andarAToa(MobDoShattered inimigo) {
        if (inimigo.getNavigation().isDone()) {
            double x = inimigo.getX() + (inimigo.getRandom().nextDouble() - 0.5) * 8;
            double z = inimigo.getZ() + (inimigo.getRandom().nextDouble() - 0.5) * 8;
            inimigo.getNavigation().moveTo(x, inimigo.getY(), z, 0.8);
        }
    }

    // ---- Ajudantes ----

    // Cura com o número verde em cima, igual ao Shattered
    public static void curarAgora(LivingEntity alvo, float quanto) {
        float antes = alvo.getHealth();
        alvo.heal(quanto);
        int curou = Math.round(alvo.getHealth() - antes);
        if (curou > 0) {
            NumerosFlutuantes.mostrar(alvo, "+" + curou, NumerosFlutuantes.VERDE);
            if (alvo.level() instanceof ServerLevel mundo) {
                Particulas.explosao(mundo, ParticleTypes.HAPPY_VILLAGER, alvo.getX(), alvo.getY(0.6), alvo.getZ(), 2, 0.3, 0.0);
            }
        }
    }

    // O quadradinho do Shattered (4x4 blocos) onde a criatura está (pular não conta como sair)
    public static long quadradinho(Entity entidade) {
        return new BlockPos(Math.floorDiv(entidade.getBlockX(), 4), 0, Math.floorDiv(entidade.getBlockZ(), 4)).asLong();
    }

    // Quem não consegue agir (paralisado, congelado, dormindo, aterrorizado) não ataca
    public static boolean impedidoDeAtacar(@Nullable Entity atacante) {
        return atacante instanceof LivingEntity criatura && (criatura.hasEffect(ModEfeitos.PARALISADO) || criatura.hasEffect(ModEfeitos.CONGELADO)
                || criatura.hasEffect(ModEfeitos.SONO_MAGICO) || criatura.hasEffect(ModEfeitos.ATERRORIZADO));
    }

    // Quem está preso (paralisado, congelado, dormindo) não consegue desviar
    public static boolean semDefesa(LivingEntity alvo) {
        return alvo.hasEffect(ModEfeitos.PARALISADO) || alvo.hasEffect(ModEfeitos.CONGELADO) || alvo.hasEffect(ModEfeitos.SONO_MAGICO);
    }
}
