package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Os 17 encantamentos de arma do Shattered, com as chances e os números de lá.
// Igual ao Shattered, 10% das armas achadas (de perto e de arremesso) vêm encantadas, e o encantamento aparece no nome:
//   comuns (50%): flamejante, congelante, inercial, chocante, pestilente
//   incomuns (40%): bloqueante, florescente, eldritch, elástica, da sorte, projetante, instável, vorpal
//   raros (10%): de corrupção, de cristal, mortal, do vampirismo
// Cada um age quando o golpe acerta (aoGolpear, depois da armadura do alvo), com o nível da arma.
// O anel do arcano deixa todos mais fortes (o "poder": mais chance de ativar e, passando de 100%, efeito mais forte).
public enum Encantamento {
    // Chance (nível + 1) / (nível + 3): põe fogo (8 turnos); se já estava pegando fogo, queima na hora
    FLAMEJANTE("flamejante", Raridade.COMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 3F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                float forca = Math.max(1F, chance);
                if (!alvo.isOnFire()) {
                    alvo.igniteForSeconds(Turno.ticks(8) / 20F);
                    forca -= 1;
                }
                if (forca > 0) {
                    int andar = Math.max(1, Andares.andarPelaAltura(alvo.getBlockY()));
                    int queimadura = Math.round(Sorteio.normal(jogador.getRandom(), 1, 3 + andar / 4) * 0.67F * forca);
                    if (queimadura > 0) {
                        machucar(alvo, alvo.damageSources().onFire(), queimadura);
                    }
                }
                Particulas.subindoEmVolta(alvo, ParticleTypes.FLAME, nivel + 4, 0.05);
            }
            return dano;
        }
    },
    // Chance (nível + 1) / (nível + 4): resfria por 3 turnos (até 6)
    CONGELANTE("congelante", Raridade.COMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 4F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                float forca = Math.max(1F, chance);
                MobEffectInstance frio = alvo.getEffect(ModEfeitos.RESFRIADO);
                int agora = frio == null ? 0 : frio.getDuration();
                int depois = Math.min(agora + Turno.ticks(3 * forca), Turno.ticks(6 * forca));
                if (depois > agora) {
                    alvo.addEffect(new MobEffectInstance(ModEfeitos.RESFRIADO, depois));
                }
                Particulas.subindoEmVolta(alvo, ParticleTypes.SNOWFLAKE, 6, 0.05);
            }
            return dano;
        }
    },
    // O dano que sobra ao matar um inimigo fica guardado e vai pro próximo golpe (o guardado diminui com o tempo)
    INERCIAL("inercial", Raridade.COMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            int guardado = (int) Math.ceil(dados.decimal(DANO_CONSERVADO));
            dano += guardado;
            float sobra = dano - alvo.getHealth();
            int novo = sobra > 0 ? Math.round((sobra - guardado) * poder) : 0;
            if (novo > 0) {
                dados.definir(DANO_CONSERVADO, (float) novo);
                jogador.addEffect(new MobEffectInstance(ModEfeitos.DANO_CONSERVADO, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            } else {
                dados.definir(DANO_CONSERVADO, 0F);
                jogador.removeEffect(ModEfeitos.DANO_CONSERVADO);
            }
            return dano;
        }
    },
    // 1 em 3: um raio pula pros inimigos a até 2 quadradinhos do alvo, com metade do dano do golpe
    CHOCANTE("chocante", Raridade.COMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = 1 / 3F * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                int choque = Math.round(dano * 0.5F * Math.max(1F, chance));
                for (LivingEntity perto : inimigosEmVolta(jogador, alvo, 2 * QUADRADINHO)) {
                    linha(jogador.serverLevel(), alvo.getBoundingBox().getCenter(), perto.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK);
                    if (choque > 0) {
                        machucar(perto, EfeitosDeVarinha.fonte(jogador), choque);
                    }
                }
                Particulas.subindoEmVolta(alvo, ParticleTypes.ELECTRIC_SPARK, 8, 0.1);
                jogador.level().playSound(null, alvo.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            return dano;
        }
    },
    // 1 em 3: envenena (3 + nível / 2 turnos a mais)
    PESTILENTE("pestilente", Raridade.COMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = 1 / 3F * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                MobEffectInstance veneno = alvo.getEffect(ModEfeitos.VENENO);
                int agora = veneno == null ? 0 : veneno.getDuration();
                alvo.addEffect(new MobEffectInstance(ModEfeitos.VENENO, agora + Turno.ticks(Math.max(1F, chance) * (nivel / 2F + 3))));
                Particulas.subindoEmVolta(alvo, Particulas.poeira(0x8E44AD, 1.0F), 6, 0.0);
            }
            return dano;
        }
    },
    // Chance (nível + 4) / (nível + 40): blindagem de 2 + nível depois do golpe
    BLOQUEANTE("bloqueante", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 4F) / (nivel + 40F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                int escudo = Math.round(Math.max(1F, chance) * (2 + nivel));
                Escudos.darBarreira(jogador, escudo);
                NumerosFlutuantes.mostrar(jogador, "+" + escudo, Escudos.COR_DO_ESCUDO);
                Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 5, 0.02);
            }
            return dano;
        }
    },
    // Chance (nível + 1) / (nível + 3): nasce grama alta em (1,5 + 0,1 x nível) quadradinhos em volta do alvo
    FLORESCENTE("florescente", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 3F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                float plantas = (1.5F + 0.1F * nivel) * Math.max(1F, chance);
                int quantas = jogador.getRandom().nextFloat() < plantas % 1 ? (int) Math.ceil(plantas) : (int) Math.floor(plantas);
                florescer(jogador, alvo, quantas);
            }
            return dano;
        }
    },
    // Chance (nível + 1) / (nível + 4): os outros inimigos que veem o golpe ficam aterrorizados por 5 turnos
    ELDRITCH("eldritch", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 4F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                for (LivingEntity perto : inimigosEmVolta(jogador, alvo, 8 * QUADRADINHO)) {
                    if (perto.hasLineOfSight(jogador) || perto.hasLineOfSight(alvo)) {
                        perto.addEffect(new MobEffectInstance(ModEfeitos.ATERRORIZADO, Turno.ticks(5 * Math.max(1F, chance))));
                        if (perto instanceof MobDoShattered inimigo) {
                            inimigo.setTarget(null);
                        }
                    }
                }
                Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFF0000, 1.4F), 12, 0.0);
                jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return dano;
        }
    },
    // Chance (nível + 1) / (nível + 5): joga o alvo 2 quadradinhos pra trás
    ELASTICA("elastica", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 5F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                Vec3 direcao = alvo.position().subtract(jogador.position()).multiply(1, 0, 1);
                if (direcao.lengthSqr() < 1.0E-4) {
                    direcao = jogador.getViewVector(1.0F).multiply(1, 0, 1);
                }
                EfeitosDeVarinha.empurrar(jogador, alvo, direcao.normalize(), Math.round(2 * Math.max(1F, chance)));
            }
            return dano;
        }
    },
    // Chance (nível + 4) / (nível + 40): se esse golpe matar, cai um prêmio a mais (poção, pergaminho, semente ou ouro)
    DA_SORTE("da_sorte", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 4F) / (nivel + 40F) * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                alvo.getPersistentData().putBoolean(SORTE, true);
            } else {
                alvo.getPersistentData().remove(SORTE);
            }
            return dano;
        }
    },
    // Mais alcance pras armas de perto (ver EncantamentosDoHeroi)
    PROJETANTE("projetante", Raridade.INCOMUM),
    // Cada golpe ativa um encantamento diferente, sorteado
    INSTAVEL("instavel", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            int guardado = (int) Math.ceil(dados.decimal(DANO_CONSERVADO));
            dados.definir(DANO_CONSERVADO, 0F);
            jogador.removeEffect(ModEfeitos.DANO_CONSERVADO);
            Encantamento sorteado = DO_INSTAVEL[jogador.getRandom().nextInt(DO_INSTAVEL.length)];
            return sorteado.aoGolpear(jogador, alvo, arma, dano, nivel, poder) + guardado;
        }
    },
    // 1 em 4: o golpe vira sangramento (2 + metade do dano), a não ser que ele já mate
    VORPAL("vorpal", Raridade.INCOMUM) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = 1 / 4F * poder;
            if (jogador.getRandom().nextFloat() < chance && dano < alvo.getHealth()) {
                int sangue = Math.round(Math.max(1F, chance) * (2 + dano / 2F));
                EfeitosNoTick.sangrar(alvo, sangue);
                NumerosFlutuantes.mostrar(alvo, String.valueOf(sangue), NumerosFlutuantes.VERMELHO);
                return 0;
            }
            return dano;
        }
    },
    // Chance (nível + 5) / (nível + 20): se esse golpe matar, o inimigo é corrompido em vez de morrer (vira aliado)
    DE_CORRUPCAO("de_corrupcao", Raridade.RARO) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float chance = (nivel + 5F) / (nivel + 20F) * poder;
            if (alvo instanceof MobDoShattered inimigo && !inimigo.chefe() && !inimigo.aliado() && dano >= alvo.getHealth()
                    && jogador.getRandom().nextFloat() < chance) {
                alvo.getPersistentData().putUUID(CORROMPER, jogador.getUUID());
            }
            return dano;
        }
    },
    // +33% de dano mágico em cada golpe; a arma se desgasta um pouco a cada golpe e se conserta com o tempo (pode se estilhaçar)
    DE_CRISTAL("de_cristal", Raridade.RARO) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            if (!(arma.getItem() instanceof ArmaDeArremesso)) {
                float usos = 50 - Math.min(20, 20 * nivel / 6F);
                float integridade = arma.getOrDefault(ModComponentes.DURABILIDADE.get(), 100F)
                        - Sorteio.normalDecimal(jogador.getRandom(), 0.5F, 1.5F) * (1F / usos);
                arma.set(ModComponentes.DURABILIDADE.get(), integridade);
                if (integridade <= 0) {
                    jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.cristal_quebrou")
                            .withStyle(ChatFormatting.RED), false);
                    jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    arma.shrink(1);
                }
            }
            return dano + (float) Math.ceil(dano * 0.33F * poder);
        }
    },
    // Quanto mais ferido o alvo ficar, mais chance (até 50% + 5% por nível) de executá-lo na hora
    MORTAL("mortal", Raridade.RARO) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float sobra = alvo.getHealth() - dano;
            if (sobra > 0 && !(alvo instanceof Goo)) {
                float ferido = (alvo.getMaxHealth() - sobra) / alvo.getMaxHealth();
                float chance = (0.5F + 0.05F * nivel) * poder * ferido * ferido;
                if (jogador.getRandom().nextFloat() < chance) {
                    Particulas.subindoEmVolta(alvo, ParticleTypes.SOUL, 6, 0.05);
                    return dano + sobra;
                }
            }
            return dano;
        }
    },
    // Chance 5% + 25% x (o quanto quem bate está ferido): cura metade do dano do golpe
    DO_VAMPIRISMO("do_vampirismo", Raridade.RARO) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
            float faltando = (jogador.getMaxHealth() - jogador.getHealth()) / jogador.getMaxHealth();
            float chance = (0.05F + 0.25F * faltando) * poder;
            if (jogador.getRandom().nextFloat() < chance && !(alvo instanceof MobDoShattered inimigo && inimigo.aliado())) {
                int cura = Math.min(Math.round(dano * 0.5F * Math.max(1F, chance)), (int) (jogador.getMaxHealth() - jogador.getHealth()));
                if (cura > 0) {
                    jogador.heal(cura);
                    NumerosFlutuantes.mostrar(jogador, "+" + cura, NumerosFlutuantes.VERDE);
                }
            }
            return dano;
        }
    };

    public enum Raridade {
        COMUM(50), INCOMUM(40), RARO(10);

        private int peso;

        Raridade(int peso) {
            this.peso = peso;
        }
    }

    // O dano guardado pelo encantamento inercial (fica nos dados do herói)
    public static final String DANO_CONSERVADO = "dano_conservado";
    // Marcas nos inimigos: da sorte (cai um prêmio a mais) e de corrupção (vira aliado ao morrer)
    public static final String SORTE = "pixeldungeon_sorte";
    public static final String CORROMPER = "pixeldungeon_corromper";
    private static final double QUADRADINHO = 4.0;
    // Os que o instável pode sortear (igual ao Shattered)
    private static final Encantamento[] DO_INSTAVEL = { FLAMEJANTE, BLOQUEANTE, FLORESCENTE, CONGELANTE, INERCIAL, DE_CORRUPCAO, ELASTICA, MORTAL,
            DA_SORTE, CHOCANTE, DO_VAMPIRISMO, PESTILENTE, VORPAL, ELDRITCH };

    private String id;
    private Raridade raridade;

    Encantamento(String id, Raridade raridade) {
        this.id = id;
        this.raridade = raridade;
    }

    public String id() {
        return id;
    }

    public Raridade raridade() {
        return raridade;
    }

    public String chaveDoNome() {
        return "encantamento.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "encantamento.pixeldungeon." + id + ".descricao";
    }

    // O que o encantamento faz quando o golpe acerta, depois da armadura do alvo (devolve o dano)
    protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, int nivel, float poder) {
        return dano;
    }

    // ---- Usar ----

    // Chamado em cada golpe que a arma acerta
    public static float aoGolpearCom(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano, float poder) {
        Encantamento encantamento = NivelDoItem.encantamento(arma);
        return encantamento == null ? dano : encantamento.aoGolpear(jogador, alvo, arma, dano, NivelDoItem.nivel(arma), poder);
    }

    // ---- Sortear ----

    // Igual ao Shattered: primeiro a raridade (50/40/10), depois um encantamento dela
    public static Encantamento sortear(Random sorteio) {
        int total = 0;
        for (Raridade raridade : Raridade.values()) {
            total += raridade.peso;
        }
        int rolagem = sorteio.nextInt(total);
        Raridade escolhida = Raridade.RARO;
        for (Raridade raridade : Raridade.values()) {
            if (rolagem < raridade.peso) {
                escolhida = raridade;
                break;
            }
            rolagem -= raridade.peso;
        }
        List<Encantamento> daRaridade = new ArrayList<>();
        for (Encantamento encantamento : values()) {
            if (encantamento.raridade == escolhida) {
                daRaridade.add(encantamento);
            }
        }
        return daRaridade.get(sorteio.nextInt(daRaridade.size()));
    }

    @Nullable
    public static Encantamento peloId(@Nullable String id) {
        for (Encantamento encantamento : values()) {
            if (encantamento.id.equals(id)) {
                return encantamento;
            }
        }
        return null;
    }

    // ---- Ajudantes ----

    // Dano por fora do golpe (o fogo a mais, o raio): ignora o tempo de invulnerabilidade do Minecraft
    private static void machucar(LivingEntity alvo, DamageSource fonte, float dano) {
        alvo.invulnerableTime = 0;
        alvo.hurt(fonte, dano);
    }

    // Os inimigos (não os heróis, as imagens espelhadas nem os aliados) em volta do alvo, menos ele mesmo
    private static List<LivingEntity> inimigosEmVolta(ServerPlayer jogador, LivingEntity alvo, double alcance) {
        return jogador.level().getEntitiesOfClass(LivingEntity.class, new AABB(alvo.position(), alvo.position()).inflate(alcance),
                criatura -> criatura != alvo && criatura.isAlive() && criatura instanceof MobDoShattered inimigo && !inimigo.aliado()
                        && criatura.distanceTo(alvo) <= alcance);
    }

    private static void linha(ServerLevel mundo, Vec3 inicio, Vec3 fim, ParticleOptions particula) {
        Vec3 caminho = fim.subtract(inicio);
        int passos = Math.max(1, (int) (caminho.length() * 2));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = inicio.add(caminho.scale(i / (double) passos));
            mundo.sendParticles(particula, ponto.x, ponto.y, ponto.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    // Florescente: grama alta no quadradinho do alvo e nos de volta (o de quem bate por último)
    private static void florescer(ServerPlayer jogador, LivingEntity alvo, int quantas) {
        ServerLevel mundo = jogador.serverLevel();
        int baixo = alvo.getBlockY();
        int x = alvo.getBlockX() - Math.floorMod(alvo.getBlockX(), 4);
        int z = alvo.getBlockZ() - Math.floorMod(alvo.getBlockZ(), 4);
        if (quantas <= 0) {
            return;
        }
        if (Grama.crescer(mundo, x, baixo, z) && --quantas <= 0) {
            return;
        }
        int[][] vizinhos = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
        List<int[]> lugares = new ArrayList<>(List.of(vizinhos));
        Collections.shuffle(lugares, new Random(jogador.getRandom().nextLong()));
        for (int[] lugar : lugares) {
            if (Grama.crescer(mundo, x + 4 * lugar[0], baixo, z + 4 * lugar[1]) && --quantas <= 0) {
                return;
            }
        }
    }
}
