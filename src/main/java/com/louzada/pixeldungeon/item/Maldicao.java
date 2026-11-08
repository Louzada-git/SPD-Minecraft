package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.PlantasDoShattered;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// As maldições do Shattered, com as chances do Shattered. Cada uma tem um nome que vai junto com o nome do item
// ("espada irritante", "armadura de malha de fedor") e só aparece depois que a maldição é descoberta.
//   maldições de arma: agem quando o herói acerta um golpe (aoGolpear)
//   maldições de armadura: agem quando o herói é acertado (aoSerAtingido), antes da armadura segurar o dano
//   varinhas amaldiçoadas: a magia sai aleatória
//   anéis amaldiçoados: o efeito fica ao contrário e o anel gruda no dedo
public enum Maldicao {

    // ---- Armas ----

    // 1 em 20 golpes: grita e chama todos os inimigos do andar
    IRRITANTE("irritante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 20F) {
                irritar(jogador, jogador.getRandom());
            }
            return dano;
        }
    },
    // 1 em 12 golpes: teletransporta o inimigo pra outro lugar do andar
    DESLOCAMENTO("deslocamento", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 12F && !(alvo instanceof Goo)) {
                Teleporte.paraLugarAleatorio(jogador.serverLevel(), alvo);
            }
            return dano;
        }
    },
    // 1 em 10 golpes: um clarão cega quem bateu (10 turnos) e quem estiver vendo (5 turnos)
    OFUSCANTE("ofuscante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 10F) {
                ofuscar(jogador, alvo);
            }
            return dano;
        }
    },
    // Metade dos golpes dá 50% a mais de dano; a outra metade não dá nada
    POLARIZANTE("polarizante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            return jogador.getRandom().nextBoolean() ? dano * 1.5F : 0;
        }
    },
    // 1 em 10 golpes: quem bate sangra (mais, quanto mais vida tiver)
    SACRIFICANTE("sacrificante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            RandomSource sorteio = jogador.getRandom();
            if (sorteio.nextFloat() < 1 / 10F) {
                // (vida / vida máxima)² × vida máxima / 8
                float porcentagem = jogador.getHealth() / jogador.getMaxHealth();
                float sangue = porcentagem * porcentagem * jogador.getMaxHealth() / 8F;
                if (sorteio.nextFloat() < sangue) {
                    EfeitosNoTick.sangrar(jogador, Math.max(1, Math.round(sangue)));
                }
            }
            return dano;
        }
    },
    // 1 em 4 golpes: fica muito imprecisa por 10 turnos (o próximo acerto desfaz)
    INCONSTANTE("inconstante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.hasEffect(ModEfeitos.DESVIANTE)) {
                jogador.removeEffect(ModEfeitos.DESVIANTE);
            } else if (jogador.getRandom().nextFloat() < 1 / 4F) {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.DESVIANTE, Turno.ticks(10)));
            }
            return dano;
        }
    },

    // ---- Armaduras ----

    // 1 em 8 golpes recebidos: congela tudo em volta, mas põe fogo em quem veste (a não ser na água)
    ANTIENTROPIA("antientropia", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 8F) {
                for (LivingEntity perto : jogador.serverLevel().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(QUADRADINHO))) {
                    if (perto != jogador && !perto.isSpectator()) {
                        perto.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, Turno.ticks(10)));
                        perto.setTicksFrozen(Math.max(perto.getTicksFrozen(), 160));
                    }
                }
                Particulas.explosao(jogador.serverLevel(), ParticleTypes.SNOWFLAKE, jogador.getX(), jogador.getY(0.5), jogador.getZ(), 40, 2.0, 0.05);
                if (!jogador.isInWater()) {
                    jogador.igniteForSeconds(Turno.ticks(4) / 20F);
                    Particulas.subindoEmVolta(jogador, ParticleTypes.FLAME, 5, 0.05);
                }
            }
            return dano;
        }
    },
    // 1 em 10 golpes recebidos: espirra um muco corrosivo em quem estiver em volta (e em quem veste)
    CORROSAO("corrosao", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 10F) {
                for (LivingEntity perto : jogador.serverLevel().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(QUADRADINHO))) {
                    if (!perto.isSpectator()) {
                        perto.addEffect(new MobEffectInstance(ModEfeitos.MUCO_CAUSTICO, MucoCaustico.DURACAO / 2));
                    }
                }
                Particulas.explosao(jogador.serverLevel(), Particulas.poeira(0x000000, 1.5F), jogador.getX(), jogador.getY(0.5), jogador.getZ(), 30, 2.0, 0.0);
            }
            return dano;
        }
    },
    // 1 em 20 golpes recebidos: teletransporta quem veste pra outro lugar do andar (e o golpe não pega)
    DESLOCAMENTO_DA_ARMADURA("deslocamento_da_armadura", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 20F && Teleporte.paraLugarAleatorio(jogador.serverLevel(), jogador)) {
                return 0;
            }
            return dano;
        }
    },
    // 1 em 6 golpes recebidos: troca fome por vida (até 4 de vida, se não estiver morrendo de fome)
    METABOLISMO("metabolismo", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 6F && jogador.getFoodData().getFoodLevel() > 0) {
                int cura = (int) Math.min(4, jogador.getMaxHealth() - jogador.getHealth());
                if (cura > 0) {
                    jogador.getFoodData().setFoodLevel(Math.max(0, jogador.getFoodData().getFoodLevel() - (cura + 1) / 2));
                    jogador.heal(cura);
                    NumerosFlutuantes.mostrar(jogador, "+" + cura, NumerosFlutuantes.VERDE);
                }
            }
            return dano;
        }
    },
    // 1 em 20 golpes recebidos: uma planta qualquer nasce e é ativada em quem veste
    SUPERCRESCIMENTO("supercrescimento", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 20F) {
                TipoDePlanta[] plantas = TipoDePlanta.values();
                float[] pesos = new float[plantas.length];
                for (int i = 0; i < plantas.length; i++) {
                    pesos[i] = plantas[i].peso();
                }
                TipoDePlanta planta = plantas[SaqueDoAndar.sortearPeso(new Random(jogador.getRandom().nextLong()), pesos)];
                Particulas.subindoEmVolta(jogador, ParticleTypes.COMPOSTER, 10, 0.05);
                PlantasDoShattered.ativar(jogador.serverLevel(), jogador.blockPosition(), planta, jogador);
            }
            return dano;
        }
    },
    // 1 em 8 golpes recebidos: solta uma nuvem de gás tóxico em volta de quem veste
    FEDOR("fedor", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 8F) {
                NuvemDeGas.soltar(jogador.serverLevel(), jogador.position(), NuvemDeGas.Tipo.TOXICO, 5.0F, 10);
            }
            return dano;
        }
    },

    // ---- Varinhas ----

    // A magia sai aleatória e quase sempre ruim (ver EfeitosDeVarinha.lancarAmaldicoada)
    CAOTICA("caotica", Alvo.VARINHA),

    // ---- Anéis ----

    // O efeito do anel fica ao contrário (bônus de 0 a -2) e ele não sai do dedo (ver Aneis)
    ANEL_AMALDICOADO("anel_amaldicoado", Alvo.ANEL);

    // Em que tipo de item a maldição aparece
    public enum Alvo {
        ARMA, ARMADURA, VARINHA, ANEL
    }

    private static final double ALCANCE_DA_VISAO = 32;
    // Um quadradinho do Shattered (o que fica "em volta")
    private static final double QUADRADINHO = 4;

    private String id;
    private Alvo alvo;

    Maldicao(String id, Alvo alvo) {
        this.id = id;
        this.alvo = alvo;
    }

    public String id() {
        return id;
    }

    public Alvo alvo() {
        return alvo;
    }

    public String chaveDoNome() {
        return "maldicao.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "maldicao.pixeldungeon." + id + ".descricao";
    }

    // O que a maldição faz quando o herói acerta um golpe com a arma (devolve o dano)
    protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
        return dano;
    }

    // O que a maldição faz quando o herói é acertado vestindo a armadura (devolve o dano)
    protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
        return dano;
    }

    // ---- Usar ----

    // Chamado em cada golpe que a arma acerta
    public static float aoGolpearCom(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano) {
        Maldicao maldicao = NivelDoItem.maldicao(arma);
        return maldicao == null || maldicao.alvo != Alvo.ARMA ? dano : maldicao.aoGolpear(jogador, alvo, dano);
    }

    // Chamado em cada golpe que acerta quem veste a armadura
    public static float aoSerAtingidoCom(ServerPlayer jogador, @Nullable LivingEntity atacante, ItemStack armadura, float dano) {
        Maldicao maldicao = NivelDoItem.maldicao(armadura);
        return maldicao == null || maldicao.alvo != Alvo.ARMADURA ? dano : maldicao.aoSerAtingido(jogador, atacante, dano);
    }

    // Inconstante ativada: a precisão cai pra um quinto
    public static float multiplicadorDePrecisao(ServerPlayer jogador, ItemStack arma) {
        return NivelDoItem.maldicao(arma) == INCONSTANTE && jogador.hasEffect(ModEfeitos.DESVIANTE) ? 0.2F : 1.0F;
    }

    // As maldições que um tipo de item pode ter
    public static List<Maldicao> doAlvo(Alvo alvo) {
        List<Maldicao> lista = new ArrayList<>();
        for (Maldicao maldicao : values()) {
            if (maldicao.alvo == alvo) {
                lista.add(maldicao);
            }
        }
        return lista;
    }

    @Nullable
    public static Maldicao peloId(@Nullable String id) {
        for (Maldicao maldicao : values()) {
            if (maldicao.id.equals(id)) {
                return maldicao;
            }
        }
        return null;
    }

    // ---- Ajudantes ----

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
        ServerLevel mundo = jogador.serverLevel();
        mundo.playSound(null, alvo.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (LivingEntity quemViu : mundo.getEntitiesOfClass(LivingEntity.class, alvo.getBoundingBox().inflate(ALCANCE_DA_VISAO))) {
            if (quemViu == jogador) {
                EfeitosNoTick.cegar(quemViu, Turno.ticks(10));
            } else if (quemViu.hasLineOfSight(alvo)) {
                EfeitosNoTick.cegar(quemViu, Turno.ticks(5));
            }
        }
    }
}
