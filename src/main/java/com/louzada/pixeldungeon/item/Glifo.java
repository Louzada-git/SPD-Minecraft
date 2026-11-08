package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Os 13 glifos de armadura do Shattered, com as chances e os números de lá.
// Igual ao Shattered, 15% das armaduras achadas vêm com um glifo (e o glifo aparece no nome, mesmo sem identificar):
//   comuns (50%): ofuscamento, rapidez, viscosidade, potencial
//   incomuns (40%): enxofre, pedra, emaranhamento, repulsão, camuflagem, fluxo
//   raros (10%): afeição, antimagia, espinhos
// Os que agem quando quem veste é acertado ficam aqui (aoSerAtingido); os que agem o tempo todo
// (velocidade, furtividade, fogo, magia, grama) ficam em GlifosDoHeroi.
// O "nível" é o da armadura, e o anel do arcano deixa todos mais fortes (ver GlifosDoHeroi.poder).
public enum Glifo {
    OFUSCAMENTO("ofuscamento", Raridade.COMUM),
    RAPIDEZ("rapidez", Raridade.COMUM),
    // O dano que passa pela armadura não vem todo de uma vez: (nível + 1) / (nível + 6) dele vira dano adiado
    VISCOSIDADE("viscosidade", Raridade.COMUM),
    // Chance (nível + 1) / (nível + 6): uma carga em cada varinha (e no cajado)
    POTENCIAL("potencial", Raridade.COMUM) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 6F) * poder;
            if (jogador.getRandom().nextFloat() < chance && carregarVarinhas(jogador, Math.max(1F, chance)) > 0) {
                Particulas.subindoEmVolta(jogador, ParticleTypes.ELECTRIC_SPARK, 10, 0.05);
            }
            return dano;
        }
    },
    ENXOFRE("enxofre", Raridade.INCOMUM),
    // Não dá pra desviar, mas o golpe perde dano conforme a esquiva que o herói teria
    PEDRA("pedra", Raridade.INCOMUM),
    // 1 em 4: uma armadura de raízes que segura (5 + 2 x nível) de dano enquanto o herói não sair do lugar
    EMARANHAMENTO("emaranhamento", Raridade.INCOMUM) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
            float chance = 1 / 4F * poder;
            if (jogador.getRandom().nextFloat() < chance) {
                EfeitosNoTick.raizDaTerra(jogador, Math.round((5 + 2 * nivel) * Math.max(1F, chance)));
                Particulas.subindoEmVolta(jogador, Particulas.poeira(0x6B4A2B, 1.2F), 12, 0.0);
            }
            return dano;
        }
    },
    // Chance (nível + 1) / (nível + 5): joga quem bateu do lado 2 quadradinhos pra trás
    REPULSAO("repulsao", Raridade.INCOMUM) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
            float chance = (nivel + 1F) / (nivel + 5F) * poder;
            if (atacante != null && atacante.distanceTo(jogador) <= COLADO && jogador.getRandom().nextFloat() < chance) {
                Vec3 direcao = atacante.position().subtract(jogador.position()).multiply(1, 0, 1);
                if (direcao.lengthSqr() < 1.0E-4) {
                    direcao = jogador.getViewVector(1.0F).multiply(1, 0, 1);
                }
                EfeitosDeVarinha.empurrar(jogador, atacante, direcao.normalize(), Math.round(2 * Math.max(1F, chance)));
                jogador.level().playSound(null, atacante.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            return dano;
        }
    },
    CAMUFLAGEM("camuflagem", Raridade.INCOMUM),
    FLUXO("fluxo", Raridade.INCOMUM),
    // Chance (nível + 3) / (nível + 20): quem bateu fica encantado (não ataca) por 10 turnos
    AFEICAO("afeicao", Raridade.RARO) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
            float chance = (nivel + 3F) / (nivel + 20F) * poder;
            if (atacante != null && jogador.getRandom().nextFloat() < chance) {
                atacante.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, Turno.ticks(Math.round(10 * Math.max(1F, chance)))));
                Particulas.subindoEmVolta(atacante, ParticleTypes.HEART, 5, 0.05);
            }
            return dano;
        }
    },
    ANTIMAGIA("antimagia", Raridade.RARO),
    // Chance (nível + 2) / (nível + 12): quem bateu sangra (4 + nível)
    ESPINHOS("espinhos", Raridade.RARO) {
        @Override
        protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
            float chance = (nivel + 2F) / (nivel + 12F) * poder;
            if (atacante != null && jogador.getRandom().nextFloat() < chance) {
                EfeitosNoTick.sangrar(atacante, Math.round((4 + nivel) * Math.max(1F, chance)));
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

    // Colado: 1 quadradinho e meio (6 blocos)
    private static final double COLADO = 6.0;

    private String id;
    private Raridade raridade;

    Glifo(String id, Raridade raridade) {
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
        return "glifo.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "glifo.pixeldungeon." + id + ".descricao";
    }

    // O que o glifo faz quando quem veste é acertado, antes da armadura segurar o dano (devolve o dano)
    protected float aoSerAtingido(ServerPlayer jogador, @Nullable LivingEntity atacante, float dano, int nivel, float poder) {
        return dano;
    }

    // ---- Usar ----

    // Chamado em cada golpe que acerta quem veste a armadura
    public static float aoSerAtingidoCom(ServerPlayer jogador, @Nullable LivingEntity atacante, ItemStack armadura, float dano, float poder) {
        Glifo glifo = NivelDoItem.glifo(armadura);
        return glifo == null ? dano : glifo.aoSerAtingido(jogador, atacante, dano, NivelDoItem.nivel(armadura), poder);
    }

    // ---- Sortear ----

    // Igual ao Shattered: primeiro a raridade (50/40/10), depois um glifo dela
    public static Glifo sortear(Random sorteio) {
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
        List<Glifo> daRaridade = new ArrayList<>();
        for (Glifo glifo : values()) {
            if (glifo.raridade == escolhida) {
                daRaridade.add(glifo);
            }
        }
        return daRaridade.get(sorteio.nextInt(daRaridade.size()));
    }

    @Nullable
    public static Glifo peloId(@Nullable String id) {
        for (Glifo glifo : values()) {
            if (glifo.id.equals(id)) {
                return glifo;
            }
        }
        return null;
    }

    // ---- Ajudantes ----

    // Potencial: uma carga (ou mais, com o anel do arcano) em cada varinha da mochila e no cajado do Mago
    public static int carregarVarinhas(ServerPlayer jogador, float quanto) {
        int carregadas = 0;
        int cargas = Math.max(1, Math.round(quanto));
        for (ItemStack pilha : jogador.getInventory().items) {
            carregadas += carregar(pilha, cargas);
        }
        for (ItemStack pilha : jogador.getInventory().offhand) {
            carregadas += carregar(pilha, cargas);
        }
        return carregadas;
    }

    private static int carregar(ItemStack pilha, int cargas) {
        if (pilha.getItem() instanceof ItemVarinha) {
            Cargas.somar(pilha, ItemVarinha.cargasMaximas(pilha), cargas);
            return 1;
        }
        if (pilha.getItem() instanceof CajadoDoMago) {
            Cargas.somar(pilha, CajadoDoMago.cargasMaximas(pilha), cargas);
            return 1;
        }
        return 0;
    }
}
