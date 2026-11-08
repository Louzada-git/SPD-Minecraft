package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PlantasDoShattered;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    // Cada golpe esquenta a arma (0 a 10 de 100); quando chega no limite, ela explode do lado do alvo (pega quem bate também)
    EXPLOSIVA("explosiva", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano) {
            int antes = arma.getOrDefault(ModComponentes.PRESSAO_EXPLOSIVA.get(), 100);
            int depois = antes - jogador.getRandom().nextInt(11);
            if (antes > 50 && depois <= 50) {
                jogador.displayClientMessage(Component.translatable("maldicao.pixeldungeon.explosiva.morna").withStyle(ChatFormatting.GOLD), false);
                Particulas.subindoEmVolta(jogador, ParticleTypes.SMOKE, 4, 0.02);
            } else if (antes > 10 && depois <= 10) {
                jogador.displayClientMessage(Component.translatable("maldicao.pixeldungeon.explosiva.quente").withStyle(ChatFormatting.RED), false);
                Particulas.subindoEmVolta(jogador, ParticleTypes.LAVA, 5, 0.02);
            } else if (depois <= 0) {
                explodir(jogador, alvo);
                depois += 100;
            }
            arma.set(ModComponentes.PRESSAO_EXPLOSIVA.get(), depois);
            return dano;
        }
    },
    // Encantado pelo alvo, o herói não consegue machucá-lo. 1 em 10 golpes: o herói fica encantado pelo alvo por 10 turnos,
    // e o alvo fica encantado pelo herói por 5 (não ataca)
    AMIGAVEL("amigavel", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            long agora = jogador.level().getGameTime();
            if (dados.ativo(AMIGAVEL_ATE, agora) && alvo.getUUID().equals(dados.alvo(AMIGAVEL_ALVO))) {
                dano = 0;
            }
            if (jogador.getRandom().nextFloat() < 1 / 10F) {
                dados.definirAlvo(AMIGAVEL_ALVO, alvo.getUUID());
                dados.ativarPor(AMIGAVEL_ATE, agora, Turno.ticks(10));
                alvo.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, Turno.ticks(5)));
                Particulas.subindoEmVolta(jogador, ParticleTypes.HEART, 5, 0.05);
                Particulas.subindoEmVolta(alvo, ParticleTypes.HEART, 5, 0.05);
            }
            return dano;
        }
    },
    // 1 em 8 golpes: um gêiser no alvo joga tudo em volta 2 quadradinhos pra longe (quem bate também) e apaga o fogo
    PRESSURIZANTE("pressurizante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 8F) {
                geiser(jogador, alvo);
            }
            return dano;
        }
    },
    // 1 em 8 golpes: uma magia aleatória de varinha amaldiçoada sai na direção do alvo
    MIRABOLANTE("mirabolante", Alvo.ARMA) {
        @Override
        protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 8F) {
                Particulas.subindoEmVolta(alvo, ParticleTypes.GLOW, 15, 0.1);
                EfeitosDeVarinha.lancarAmaldicoada(jogador);
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
    // 1 em 20 golpes recebidos: metade das vezes aparece uma imagem espelhada de quem veste; na outra metade, uma cópia de quem bateu
    MULTIPLICIDADE("multiplicidade", Alvo.ARMADURA) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
            if (jogador.getRandom().nextFloat() < 1 / 20F) {
                multiplicar(jogador, atacante);
            }
            return dano;
        }
    },
    // Passar pelas portas fica 3x mais lento (ver GlifosDoHeroi)
    VOLUMOSA("volumosa", Alvo.ARMADURA),

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
    // Maldição amigável: por quem o herói está encantado, e até quando
    private static final String AMIGAVEL_ALVO = "amigavel_alvo";
    private static final String AMIGAVEL_ATE = "amigavel_ate";
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

    // A mesma coisa, pra quem precisa da arma (a explosiva guarda o quanto esquentou nela)
    protected float aoGolpear(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano) {
        return aoGolpear(jogador, alvo, dano);
    }

    // O que a maldição faz quando o herói é acertado vestindo a armadura (devolve o dano)
    protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano) {
        return dano;
    }

    // ---- Usar ----

    // Chamado em cada golpe que a arma acerta
    public static float aoGolpearCom(ServerPlayer jogador, LivingEntity alvo, ItemStack arma, float dano) {
        Maldicao maldicao = NivelDoItem.maldicao(arma);
        return maldicao == null || maldicao.alvo != Alvo.ARMA ? dano : maldicao.aoGolpear(jogador, alvo, arma, dano);
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

    // Multiplicidade: uma imagem espelhada de quem veste, ou uma cópia de quem bateu (o Goo e o mímico não são copiados:
    // no lugar deles vem um inimigo qualquer do andar)
    private static void multiplicar(ServerPlayer jogador, @Nullable LivingEntity atacante) {
        ServerLevel mundo = jogador.serverLevel();
        if (jogador.getRandom().nextBoolean()) {
            ImagemEspelhada.criar(mundo, jogador);
            return;
        }
        EntityType<?> tipo;
        if (atacante instanceof MobDoShattered inimigo && !(inimigo instanceof Goo) && !(inimigo instanceof Mimico)) {
            tipo = inimigo.getType();
        } else {
            tipo = PopulacaoDoAndar.tipoAleatorio(Andares.andarPelaAltura(jogador.getBlockY()), new Random(jogador.getRandom().nextLong()));
        }
        if (!(tipo.create(mundo) instanceof MobDoShattered copia)) {
            return;
        }
        double angulo = jogador.getRandom().nextDouble() * Math.PI * 2;
        copia.moveTo(jogador.getX() + Math.cos(angulo) * QUADRADINHO, jogador.getY(), jogador.getZ() + Math.sin(angulo) * QUADRADINHO,
                jogador.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!mundo.noCollision(copia)) {
            copia.moveTo(jogador.getX(), jogador.getY(), jogador.getZ(), copia.getYRot(), 0.0F);
        }
        copia.ajustarVida(mundo, PopulacaoDoAndar.multiplicadorDeVida(mundo.getServer()));
        copia.setPersistenceRequired();
        mundo.addFreshEntity(copia);
        copia.notar(jogador);
        Particulas.explosao(mundo, ParticleTypes.PORTAL, copia.getX(), copia.getY(0.5), copia.getZ(), 20, 0.5, 0.1);
    }

    // Explosiva: uma explosão do Shattered (3x3 quadradinhos) do lado do alvo, virada pra quem bate:
    // 4 + andar até 12 + 3 x andar de dano, menos a armadura de cada um
    private static void explodir(ServerPlayer jogador, LivingEntity alvo) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 paraQuemBate = jogador.position().subtract(alvo.position());
        Vec3 centro = paraQuemBate.lengthSqr() < 1.0E-4 ? alvo.position()
                : alvo.position().add(paraQuemBate.normalize().scale(Math.min(QUADRADINHO, paraQuemBate.length())));
        int andar = Math.max(1, Andares.andarPelaAltura(alvo.getBlockY()));
        Particulas.explosao(mundo, ParticleTypes.EXPLOSION_EMITTER, centro.x, centro.y + 1, centro.z, 1, 0.0, 0.0);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        for (LivingEntity atingido : mundo.getEntitiesOfClass(LivingEntity.class, new AABB(centro, centro).inflate(QUADRADINHO * 1.5))) {
            if (!atingido.isAlive() || atingido.isSpectator()) {
                continue;
            }
            int dano = Sorteio.normal(jogador.getRandom(), 4 + andar, 12 + 3 * andar);
            if (atingido instanceof MobDoShattered inimigo) {
                dano -= inimigo.sortearArmadura();
            } else if (atingido.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmaduraDoShattered armadura) {
                dano -= armadura.sortearDefesa(atingido.getItemBySlot(EquipmentSlot.CHEST), jogador.getRandom(), 99);
            }
            if (dano > 0) {
                atingido.invulnerableTime = 0;
                atingido.hurt(mundo.damageSources().explosion(null, null), dano);
            }
        }
    }

    // Pressurizante: o gêiser joga quem estiver no quadradinho do alvo e em volta pra longe
    private static void geiser(ServerPlayer jogador, LivingEntity alvo) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 centro = alvo.position();
        Particulas.explosao(mundo, ParticleTypes.SPLASH, centro.x, centro.y + 0.5, centro.z, 80, 2.0, 0.3);
        Particulas.explosao(mundo, ParticleTypes.BUBBLE_POP, centro.x, centro.y + 1, centro.z, 30, 1.0, 0.2);
        mundo.playSound(null, alvo.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2F, 0.75F);
        for (LivingEntity perto : mundo.getEntitiesOfClass(LivingEntity.class, alvo.getBoundingBox().inflate(QUADRADINHO * 1.5))) {
            if (!perto.isAlive() || perto.isSpectator()) {
                continue;
            }
            perto.clearFire();
            Vec3 direcao = perto == alvo ? alvo.position().subtract(jogador.position()) : perto.position().subtract(centro);
            direcao = direcao.multiply(1, 0, 1);
            if (direcao.lengthSqr() < 1.0E-4) {
                continue;
            }
            EfeitosDeVarinha.empurrar(jogador, perto, direcao.normalize(), 2);
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
