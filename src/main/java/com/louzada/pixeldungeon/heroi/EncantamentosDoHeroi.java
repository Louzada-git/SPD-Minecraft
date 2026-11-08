package com.louzada.pixeldungeon.heroi;

import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// O que os encantamentos da arma do herói fazem fora do golpe, igual ao Shattered:
//   projetante: a arma de perto alcança 1 quadradinho a mais (4 blocos; mais com o anel do arcano)
//   inercial: o dano guardado cai 2,5% por turno (pelo menos 0,1)
//   de cristal: a arma se conserta 0,2 por turno
//   da sorte: o inimigo marcado deixa um prêmio a mais ao morrer
//   de corrupção: o inimigo marcado volta como aliado ao morrer
public class EncantamentosDoHeroi {
    private static final ResourceLocation ALCANCE = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "alcance_do_encantamento");

    // ---- Todo meio segundo: o alcance da arma projetante ----

    public static void atualizar(ServerPlayer jogador) {
        double extra = 0;
        if (NivelDoItem.encantamento(jogador.getMainHandItem()) == Encantamento.PROJETANTE) {
            extra = 4.0 * Math.round(GlifosDoHeroi.poder(jogador));
        }
        AttributeInstance alcance = jogador.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (alcance == null) {
            return;
        }
        AttributeModifier atual = alcance.getModifier(ALCANCE);
        if (atual != null && Math.abs(atual.amount() - extra) < 1e-6) {
            return;
        }
        alcance.removeModifier(ALCANCE);
        if (extra > 0) {
            alcance.addTransientModifier(new AttributeModifier(ALCANCE, extra, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ---- Uma vez por turno ----

    public static void aoTurno(ServerPlayer jogador, DadosDoHeroi dados) {
        // Inercial: o dano guardado vai sumindo
        float guardado = dados.decimal(Encantamento.DANO_CONSERVADO);
        if (guardado > 0) {
            guardado -= Math.max(guardado * 0.025F, 0.1F);
            dados.definir(Encantamento.DANO_CONSERVADO, Math.max(0, guardado));
            if (guardado <= 0) {
                jogador.removeEffect(ModEfeitos.DANO_CONSERVADO);
            }
        }
        // Cristal: as armas de cristal da mochila se consertam
        for (ItemStack pilha : jogador.getInventory().items) {
            consertarCristal(pilha);
        }
        for (ItemStack pilha : jogador.getInventory().offhand) {
            consertarCristal(pilha);
        }
    }

    private static void consertarCristal(ItemStack pilha) {
        if (NivelDoItem.encantamento(pilha) == Encantamento.DE_CRISTAL && pilha.has(ModComponentes.DURABILIDADE.get())) {
            float integridade = Math.min(100F, pilha.getOrDefault(ModComponentes.DURABILIDADE.get(), 100F) + 0.2F);
            if (integridade >= 100F) {
                pilha.remove(ModComponentes.DURABILIDADE.get());
            } else {
                pilha.set(ModComponentes.DURABILIDADE.get(), integridade);
            }
        }
    }

    // ---- Um inimigo morreu ----

    // Da sorte: o prêmio a mais. De corrupção: volta como aliado de quem corrompeu.
    public static void aoMorrer(MobDoShattered inimigo) {
        if (!(inimigo.level() instanceof ServerLevel mundo)) {
            return;
        }
        if (inimigo.getPersistentData().getBoolean(Encantamento.SORTE)) {
            Random sorteio = new Random(inimigo.getRandom().nextLong());
            int andar = Math.max(1, Andares.andarPelaAltura(inimigo.getBlockY()));
            ItemStack premio = Aneis.premioConsumivel(sorteio, andar, Identificacao.sementeDaRun(mundo));
            ItemEntity noChao = inimigo.spawnAtLocation(premio);
            if (noChao != null) {
                noChao.setUnlimitedLifetime();
            }
            Particulas.subindoEmVolta(inimigo, ParticleTypes.HAPPY_VILLAGER, 8, 0.05);
        }
        if (inimigo.getPersistentData().hasUUID(Encantamento.CORROMPER)) {
            UUID dono = inimigo.getPersistentData().getUUID(Encantamento.CORROMPER);
            if (inimigo.getType().create(mundo) instanceof MobDoShattered aliado) {
                aliado.moveTo(inimigo.getX(), inimigo.getY(), inimigo.getZ(), inimigo.getYRot(), 0.0F);
                aliado.ajustarVida(mundo, PopulacaoDoAndar.multiplicadorDeVida(mundo.getServer()));
                Player quemCorrompeu = mundo.getPlayerByUUID(dono);
                aliado.corromper(quemCorrompeu);
                mundo.addFreshEntity(aliado);
                mundo.playSound(null, inimigo.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
    }
}
