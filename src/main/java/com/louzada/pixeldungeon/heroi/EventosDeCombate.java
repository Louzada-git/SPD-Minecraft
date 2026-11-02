package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.item.ArmaDoShattered;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Regras de combate do Shattered no corpo a corpo:
//   1. Antes do dano, sorteia se o golpe acerta (precisão do atacante contra esquiva do defensor)
//   2. Se acertou com uma arma do Shattered, o dano é sorteado pela arma (e pela força do herói)
// Ataques de longe (flechas etc.) ficam pra depois.
public class EventosDeCombate {
    // Até existirem os mobs do Shattered, os outros mobs usam estes valores
    private static final float PRECISAO_PADRAO_DOS_MOBS = 10;
    private static final float ESQUIVA_PADRAO_DOS_MOBS = 0;
    private static final int XP_PADRAO_DOS_MOBS = 1;

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity alvo = evento.getEntity();
        DamageSource fonte = evento.getSource();
        Entity atacante = fonte.getEntity();
        boolean corpoACorpo = atacante != null && fonte.getDirectEntity() == atacante;
        if (!corpoACorpo || alvo.level().isClientSide()) {
            return;
        }
        RandomSource sorteio = alvo.getRandom();

        // 1. Acertou ou errou?
        if (!RegrasDoHeroi.acertou(sorteio, precisaoDe(atacante), esquivaDe(alvo))) {
            evento.setCanceled(true);
            avisarQueErrou(atacante, alvo);
            return;
        }

        // 2. Dano da arma do Shattered
        if (atacante instanceof Player jogador && jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
            // O Minecraft já calculou o ataque com base 1 (vezes a recarga do golpe e o crítico).
            // Mantemos essa proporção e trocamos o 1 pelo dano sorteado.
            double ataqueBase = jogador.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float proporcao = ataqueBase > 0 ? (float) (evento.getAmount() / ataqueBase) : 1.0F;
            int dano = arma.sortearDano(sorteio, RegrasDoHeroi.dados(jogador).forca());
            evento.setAmount(dano * proporcao);
        }
    }

    // Quem mata um mob ganha XP
    public static void aoMorrerAlguem(LivingDeathEvent evento) {
        if (evento.getEntity() instanceof Player) {
            return;
        }
        if (evento.getSource().getEntity() instanceof ServerPlayer jogador) {
            RegrasDoHeroi.ganharXp(jogador, XP_PADRAO_DOS_MOBS);
        }
    }

    private static float precisaoDe(Entity atacante) {
        if (atacante instanceof Player jogador) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            float precisao = RegrasDoHeroi.precisao(dados.nivel());
            if (jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
                precisao *= arma.precisaoCom(dados.forca());
            }
            return precisao;
        }
        return PRECISAO_PADRAO_DOS_MOBS;
    }

    private static float esquivaDe(LivingEntity alvo) {
        if (alvo instanceof Player jogador) {
            return RegrasDoHeroi.esquiva(RegrasDoHeroi.dados(jogador).nivel());
        }
        return ESQUIVA_PADRAO_DOS_MOBS;
    }

    // Igual ao Shattered: aparece "esquivou" pra quem desviou e "errou" pra quem atacou
    private static void avisarQueErrou(Entity atacante, LivingEntity alvo) {
        if (atacante instanceof Player jogador) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.errou"), true);
        }
        if (alvo instanceof Player jogador) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.esquivou"), true);
        }
        alvo.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
