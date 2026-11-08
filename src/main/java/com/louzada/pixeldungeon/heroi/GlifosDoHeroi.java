package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.Glifo;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDeAnel;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// O que os glifos (e as maldições volumosa e de multiplicidade) da armadura do herói fazem o tempo todo, igual ao Shattered:
//   ofuscamento: os inimigos dormindo notam menos (furtividade 1 + nível / 3)
//   rapidez: (1,2 + 0,04 x nível)x mais rápido sem inimigos a 3 quadradinhos
//   fluxo: (2 + 0,5 x nível)x mais rápido na água
//   volumosa (maldição): 3x mais lento passando pelas portas
//   enxofre: o fogo não machuca e se apaga
//   antimagia: o dano mágico cai de nível até 3 + 1,5 x nível
//   camuflagem: pisotear grama alta deixa invisível por 3 + nível / 2 turnos
//   viscosidade: (nível + 1) / (nível + 6) do dano de cada golpe vira dano adiado, que vem 10% por turno
//   pedra: não dá pra desviar, mas o dano cai conforme a esquiva
// O anel do arcano deixa tudo isso 1,175x mais forte por bônus (o "poder").
public class GlifosDoHeroi {
    private static final ResourceLocation VELOCIDADE = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "velocidade_do_glifo");
    public static final String DANO_ADIADO = "dano_adiado";
    // Inimigo "perto" pra rapidez: até 3 quadradinhos
    private static final double PERTO = 12.0;

    // ---- O que o herói está vestindo ----

    private static ItemStack armadura(Player jogador) {
        ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
        return peito.getItem() instanceof ArmaduraDoShattered ? peito : ItemStack.EMPTY;
    }

    // O nível do glifo na armadura do herói (-1 se ele não está vestindo esse glifo), igual ao "glyphLevel" do Shattered
    public static int nivel(Player jogador, Glifo glifo) {
        ItemStack peito = armadura(jogador);
        return !peito.isEmpty() && NivelDoItem.glifo(peito) == glifo ? NivelDoItem.nivel(peito) : -1;
    }

    public static boolean veste(Player jogador, Maldicao maldicao) {
        ItemStack peito = armadura(jogador);
        return !peito.isEmpty() && NivelDoItem.maldicao(peito) == maldicao;
    }

    // Anel do arcano: 1,175x por bônus
    public static float poder(Player jogador) {
        if (!(jogador instanceof ServerPlayer heroi)) {
            return 1.0F;
        }
        return (float) Math.pow(1.175, RegrasDoHeroi.dados(heroi).bonusDoAnel(TipoDeAnel.ARCANO));
    }

    // ---- Velocidade: rapidez, fluxo e volumosa (todo meio segundo) ----

    public static void atualizarVelocidade(ServerPlayer jogador) {
        float vezes = 1.0F;
        float poder = poder(jogador);
        int rapidez = nivel(jogador, Glifo.RAPIDEZ);
        if (rapidez >= 0 && !inimigoPerto(jogador)) {
            vezes *= (1.2F + 0.04F * rapidez) * poder;
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFF66, 0.8F), 1 + rapidez / 5, 0.0);
        }
        int fluxo = nivel(jogador, Glifo.FLUXO);
        if (fluxo >= 0 && jogador.isInWater()) {
            vezes *= (2.0F + 0.5F * fluxo) * poder;
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0x66AAFF, 0.8F), 2 + fluxo / 2, 0.0);
        }
        if (veste(jogador, Maldicao.VOLUMOSA) && naPorta(jogador)) {
            vezes *= 1 / 3F;
            Particulas.subindoEmVolta(jogador, ParticleTypes.SMOKE, 5, 0.02);
        }
        AttributeInstance velocidade = jogador.getAttribute(Attributes.MOVEMENT_SPEED);
        if (velocidade == null) {
            return;
        }
        double valor = vezes - 1.0;
        AttributeModifier atual = velocidade.getModifier(VELOCIDADE);
        if (atual != null && Math.abs(atual.amount() - valor) < 1e-6) {
            return;
        }
        velocidade.removeModifier(VELOCIDADE);
        if (Math.abs(valor) > 1e-6) {
            velocidade.addTransientModifier(new AttributeModifier(VELOCIDADE, valor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static boolean inimigoPerto(ServerPlayer jogador) {
        for (MobDoShattered inimigo : jogador.level().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(PERTO))) {
            if (inimigo.isAlive() && inimigo.distanceTo(jogador) <= PERTO && inimigo.hasLineOfSight(jogador)) {
                return true;
            }
        }
        return false;
    }

    private static boolean naPorta(ServerPlayer jogador) {
        return jogador.level().getBlockState(jogador.blockPosition()).getBlock() instanceof PortaGigante
                || jogador.level().getBlockState(jogador.blockPosition().above()).getBlock() instanceof PortaGigante;
    }

    // ---- Ofuscamento ----

    // A furtividade a mais (os inimigos dormindo sorteiam contra "distância + furtividade")
    public static float furtividade(Player jogador) {
        int nivel = nivel(jogador, Glifo.OFUSCAMENTO);
        return nivel < 0 ? 0 : (1 + nivel / 3F) * poder(jogador);
    }

    // ---- Dano: enxofre e antimagia ----

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        DamageSource fonte = evento.getSource();
        if (fonte.is(DamageTypeTags.IS_FIRE) && nivel(jogador, Glifo.ENXOFRE) >= 0) {
            jogador.clearFire();
            evento.setCanceled(true);
            return;
        }
        int antimagia = nivel(jogador, Glifo.ANTIMAGIA);
        if (antimagia >= 0 && magico(fonte)) {
            float poder = poder(jogador);
            int segura = Sorteio.normal(jogador.getRandom(), Math.round(antimagia * poder), Math.round((3 + antimagia * 1.5F) * poder));
            evento.setAmount(Math.max(0, evento.getAmount() - segura));
        }
    }

    private static boolean magico(DamageSource fonte) {
        return fonte.is(DamageTypes.MAGIC) || fonte.is(DamageTypes.INDIRECT_MAGIC) || fonte.is(DamageTypeTags.IS_LIGHTNING);
    }

    // ---- Todo tick: enxofre apaga o fogo; o dano adiado vem uma vez por turno ----

    public static void aoTick(ServerPlayer jogador, DadosDoHeroi dados, boolean umTurno) {
        if (jogador.isOnFire() && nivel(jogador, Glifo.ENXOFRE) >= 0) {
            jogador.clearFire();
        }
        if (umTurno) {
            float adiado = dados.decimal(DANO_ADIADO);
            if (adiado > 0) {
                // Igual ao Shattered: 10% do que falta por turno (pelo menos 1)
                float agora = Math.min(adiado, Math.max(1, (int) (adiado * 0.1F)));
                dados.definir(DANO_ADIADO, adiado - agora);
                jogador.invulnerableTime = 0;
                jogador.hurt(jogador.damageSources().generic(), agora);
                if (adiado - agora <= 0) {
                    jogador.removeEffect(ModEfeitos.DANO_ADIADO);
                }
            }
        }
    }

    // ---- Camuflagem ----

    // O herói pisoteou grama alta
    public static void aoPisotearGrama(ServerPlayer jogador) {
        int nivel = nivel(jogador, Glifo.CAMUFLAGEM);
        if (nivel < 0) {
            return;
        }
        int turnos = Math.round((3 + nivel / 2F) * poder(jogador));
        Furtividade.ficarInvisivel(jogador, Turno.ticks(turnos));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AZALEA_LEAVES_STEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ---- Golpes: pedra e viscosidade ----

    // Pedra: o golpe sempre pega, mas o dano cai conforme a chance que ele teria de acertar
    // (com a esquiva que o herói teria sem a pedra)
    public static float pedra(Golpe golpe, float esquivaSemPedra, float dano) {
        float precisao = Combate.precisaoDoGolpe(golpe);
        float esquiva = esquivaSemPedra * poder((Player) golpe.alvo().entidade());
        float chance;
        if (esquiva >= precisao) {
            chance = esquiva <= 0 ? 1.0F : (precisao / esquiva) / 2F;
        } else {
            chance = 1F - (esquiva / precisao) / 2F;
        }
        float fator = Math.max(0.25F, Math.min(1.0F, (1F + 3F * chance) / 4F));
        return (float) Math.ceil(dano * fator);
    }

    // Viscosidade: guarda parte do dano que passou pela armadura pra vir aos poucos
    public static float adiarDano(ServerPlayer jogador, DadosDoHeroi dados, int nivel, float dano) {
        if (dano <= 0) {
            return dano;
        }
        float parte = (nivel + 1) / (float) (nivel + 6) * poder(jogador);
        float adiado;
        if (parte > 1F) {
            dano = dano / parte;
            adiado = dano;
        } else {
            adiado = (float) Math.ceil(dano * parte);
        }
        if (adiado > 0) {
            dados.definir(DANO_ADIADO, dados.decimal(DANO_ADIADO) + adiado);
            jogador.addEffect(new MobEffectInstance(ModEfeitos.DANO_ADIADO, -1, 0, false, false, true));
            NumerosFlutuantes.mostrar(jogador, String.valueOf(Math.round(adiado)), NumerosFlutuantes.AMARELO);
        }
        return dano - adiado;
    }
}
