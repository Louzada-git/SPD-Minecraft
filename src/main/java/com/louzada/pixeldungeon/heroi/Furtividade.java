package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.item.CapaDasSombras;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

// Invisibilidade do Shattered: os inimigos não veem o herói (perdem ele de vista e não acordam).
// Usa o efeito de invisibilidade do Minecraft, que também deixa o herói transparente pros outros jogadores.
// Atacar, lançar magia ou usar a varinha acaba com a invisibilidade.
public class Furtividade {

    public static boolean invisivel(LivingEntity criatura) {
        return criatura.hasEffect(MobEffects.INVISIBILITY);
    }

    // Fica invisível por um tempo (sem as bolinhas do efeito, pra não entregar onde está)
    public static void ficarInvisivel(ServerPlayer jogador, int ticks) {
        jogador.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
    }

    public static void revelar(ServerPlayer jogador) {
        if (CapaDasSombras.ativa(jogador)) {
            CapaDasSombras.desativar(jogador, false);
        }
        if (invisivel(jogador)) {
            jogador.removeEffect(MobEffects.INVISIBILITY);
        }
        // A defesa do escudo da Duelista também acaba (se já bloqueou algum ataque)
        HabilidadesDaDuelista.aoAgir(jogador);
    }
}
