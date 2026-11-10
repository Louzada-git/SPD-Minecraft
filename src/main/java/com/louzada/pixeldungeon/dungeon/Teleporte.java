package com.louzada.pixeldungeon.dungeon;

import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

// Teletransporte pra um lugar aleatório do andar (pergaminho de teletransporte, folha-sumida, maldição de deslocamento,
// armadilha de teletransporte).
// Igual ao Shattered, prefere um lugar longe de onde a criatura estava.
public class Teleporte {
    private static final int TENTATIVAS = 8;

    public static boolean paraLugarAleatorio(ServerLevel mundo, LivingEntity quem) {
        BlockPos destino = lugarLonge(mundo, quem);
        if (destino == null) {
            return false;
        }
        para(mundo, quem, destino);
        return true;
    }

    // Teletransporta pra esse bloco (o meio dele), com as partículas e o som (a armadilha de portal usa direto)
    public static void para(ServerLevel mundo, LivingEntity quem, BlockPos destino) {
        Particulas.explosao(mundo, ParticleTypes.PORTAL, quem.getX(), quem.getY(0.5), quem.getZ(), 30, 0.4, 0.3);
        mundo.playSound(null, quem.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (quem instanceof ServerPlayer jogador) {
            jogador.teleportTo(mundo, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, jogador.getYRot(), jogador.getXRot());
        } else {
            quem.teleportTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5);
        }
        quem.resetFallDistance();
        // Quem estava caçando perde o alvo de vista
        if (quem instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        Particulas.explosao(mundo, ParticleTypes.PORTAL, quem.getX(), quem.getY(0.5), quem.getZ(), 30, 0.4, 0.3);
        mundo.playSound(null, destino, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.2F);
    }

    // Um lugar livre do andar onde a criatura está, o mais longe dela entre alguns sorteados
    @Nullable
    public static BlockPos lugarLonge(ServerLevel mundo, LivingEntity quem) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        Integer andar = PopulacaoDoAndar.andarEm(run, quem.getX(), quem.getY(), quem.getZ());
        if (!run.ativa() || andar == null) {
            return null;
        }
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        Random sorteio = new Random(quem.getRandom().nextLong());
        BlockPos melhor = null;
        double maiorDistancia = -1;
        for (int i = 0; i < TENTATIVAS; i++) {
            BlockPos lugar = PopulacaoDoAndar.lugarAleatorio(run.semente(), origem, andar, sorteio);
            if (lugar == null) {
                continue;
            }
            double distancia = lugar.distSqr(quem.blockPosition());
            if (distancia > maiorDistancia) {
                maiorDistancia = distancia;
                melhor = lugar;
            }
        }
        return melhor;
    }
}
