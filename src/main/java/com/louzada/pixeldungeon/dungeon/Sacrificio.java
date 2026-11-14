package com.louzada.pixeldungeon.dungeon;

import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.bloco.FogoDeSacrificio;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.EstatuaAnimada;
import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.Piranha;
import com.louzada.pixeldungeon.entidade.Sentinela;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// O fogo de sacrifício do Shattered (a sala do sacrifício): marca quem está perto do altar (no quadradinho dele ou nos 8 em volta)
// por 2 turnos; quem morre marcado é sacrificado. O fogo precisa de 6 + 4 x andar de sacrifício; cada inimigo vale 2 ou 3 vezes:
//   estátua animada e mímico: 1 + andar; piranha e abelha: 1 + andar / 2; pedacinho de enxame: 1; os outros: 1 + o XP deles
// Quando completa, o fogo apaga e deixa uma arma da região seguinte, +1 mas amaldiçoada (se já tiver um encantamento bom,
// fica sem maldição).
public final class Sacrificio {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int TURNOS_MARCADO = 2;

    private Sacrificio() {
    }

    // Quanto de sacrifício o fogo do andar precisa
    public static int total(int andar) {
        return 6 + 4 * andar;
    }

    private static String chave(int andar) {
        return "sacrificio_" + andar;
    }

    // Quanto ainda falta
    public static int falta(DadosDaRun run, int andar) {
        return Math.max(0, total(andar) - run.quedas(chave(andar)));
    }

    // A área marcada: o quadradinho do altar e os 8 em volta
    private static AABB area(BlockPos controle) {
        int x0 = controle.getX() - 1 - T;
        int z0 = controle.getZ() - 1 - T;
        return new AABB(x0, controle.getY() - 2, z0, x0 + 3 * T, controle.getY() + Andares.ALTURA_DA_PAREDE, z0 + 3 * T);
    }

    public static void marcarEmVolta(ServerLevel mundo, BlockPos controle) {
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, area(controle))) {
            if (criatura instanceof Sentinela || !criatura.isAlive()) {
                continue;
            }
            if (!criatura.hasEffect(ModEfeitos.MARCADO)) {
                mundo.sendParticles(ParticleTypes.SOUL, criatura.getX(), criatura.getY(0.5), criatura.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
            }
            criatura.addEffect(new MobEffectInstance(ModEfeitos.MARCADO, TURNOS_MARCADO * 40 + 20, 0, false, true, true));
        }
    }

    // Alguém morreu: se estava marcado e o fogo está perto, é sacrificado
    public static void aoMorrer(LivingDeathEvent evento) {
        LivingEntity morto = evento.getEntity();
        if (!(morto.level() instanceof ServerLevel mundo) || !morto.hasEffect(ModEfeitos.MARCADO) || !(morto instanceof MobDoShattered inimigo)) {
            return;
        }
        BlockPos controle = fogoPerto(mundo, morto.blockPosition());
        if (controle == null) {
            return;
        }
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        int andar = Andares.andarPelaAltura(controle.getY());
        int valor = valor(inimigo, andar);
        if (valor <= 0) {
            avisar(mundo, controle, "mensagem.pixeldungeon.sacrificio.indigno", ChatFormatting.GRAY);
            return;
        }
        int falta = falta(run, andar);
        if (valor < falta) {
            run.definirQueda(chave(andar), run.quedas(chave(andar)) + valor);
            mundo.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, controle.getX() + 1, controle.getY() + 0.5, controle.getZ() + 1, 20, 0.5, 0.5, 0.5, 0.05);
            mundo.playSound(null, controle, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.8F, 0.6F);
            avisar(mundo, controle, "mensagem.pixeldungeon.sacrificio.digno", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        // Completou: o fogo apaga e deixa o prêmio
        run.definirQueda(chave(andar), total(andar));
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                mundo.setBlockAndUpdate(controle.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            }
        }
        mundo.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, controle.getX() + 1, controle.getY() + 0.5, controle.getZ() + 1, 60, 1.5, 0.8, 1.5, 0.08);
        mundo.playSound(null, controle, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 0.4F);
        mundo.playSound(null, controle, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        avisar(mundo, controle, "mensagem.pixeldungeon.sacrificio.premio", ChatFormatting.GOLD);
        ItemEntity item = new ItemEntity(mundo, controle.getX() + 1, controle.getY() + 0.5, controle.getZ() + 1,
                premio(new Random(run.semente() * 97 + andar), andar));
        item.setUnlimitedLifetime();
        mundo.addFreshEntity(item);
    }

    // Quanto o inimigo vale (igual à varinha de corrupção do Shattered, menos pros enxames)
    public static int valor(MobDoShattered inimigo, int andar) {
        int valor;
        if (inimigo instanceof EstatuaAnimada || inimigo instanceof Mimico) {
            valor = 1 + andar;
        } else if (inimigo instanceof Piranha || inimigo instanceof Abelha) {
            valor = 1 + andar / 2;
        } else if (inimigo instanceof EnxameDeMoscas && inimigo.xpAoMorrer() == 0) {
            valor = 1;
        } else if (inimigo.xpAoMorrer() > 0) {
            valor = 1 + inimigo.xpAoMorrer();
        } else {
            return 0;
        }
        return valor * (2 + inimigo.getRandom().nextInt(2));
    }

    // O prêmio: uma arma da região seguinte; sem maldição, ganha +1 e (se não tiver um encantamento bom) uma maldição
    public static ItemStack premio(Random sorteio, int andar) {
        ItemStack arma = SaqueDoAndar.armaAleatoria(sorteio, andar + 5);
        // (a maldição é sempre sorteada, pro resto do sorteio sair igual)
        var possiveis = Maldicao.doAlvo(Maldicao.Alvo.ARMA);
        Maldicao maldicao = possiveis.get(sorteio.nextInt(possiveis.size()));
        if (!NivelDoItem.amaldicoado(arma)) {
            NivelDoItem.definirNivel(arma, NivelDoItem.nivel(arma) + 1);
            if (NivelDoItem.encantamento(arma) == null) {
                arma.set(ModComponentes.MALDICAO.get(), maldicao.id());
            }
        }
        arma.set(ModComponentes.MALDICAO_CONHECIDA.get(), true);
        return arma;
    }

    // O controle do fogo de sacrifício perto desse lugar (ou null)
    @Nullable
    private static BlockPos fogoPerto(ServerLevel mundo, BlockPos lugar) {
        for (BlockPos posicao : BlockPos.betweenClosed(lugar.offset(-T - 3, -3, -T - 3), lugar.offset(T + 3, 3, T + 3))) {
            var estado = mundo.getBlockState(posicao);
            if (estado.is(ModBlocos.FOGO_DE_SACRIFICIO.get()) && estado.getValue(FogoDeSacrificio.CONTROLE)
                    && area(posicao).contains(lugar.getX() + 0.5, lugar.getY() + 0.5, lugar.getZ() + 0.5)) {
                return posicao.immutable();
            }
        }
        return null;
    }

    private static void avisar(ServerLevel mundo, BlockPos lugar, String chave, ChatFormatting cor) {
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, new AABB(lugar).inflate(32))) {
            jogador.displayClientMessage(Component.translatable(chave).withStyle(cor), false);
        }
    }
}
