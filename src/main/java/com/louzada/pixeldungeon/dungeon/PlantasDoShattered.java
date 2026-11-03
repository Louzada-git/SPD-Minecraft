package com.louzada.pixeldungeon.dungeon;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.EfeitosDePocao;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.registro.ModEfeitos;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// O que cada planta do Shattered faz em quem pisa nela (1 turno = 1 segundo):
//   grama-solar: cura até a vida máxima, devagar, enquanto não sair do lugar
//   folha-fantasma: teletransporta pra outro lugar do andar
//   muda-de-gelo: congela quem estiver perto
//   flor-de-fogo: fogo
//   sofrimusgo: veneno (5 + 2/3 do andar)
//   cardo-ligeiro: o tempo para em volta de quem pisou (os inimigos perto ficam parados 5 turnos)
//   ceguerva: cega e aleija por 10 turnos
//   vinha-tempestuosa: vertigem por 10 turnos
//   terraíz: armadura de raízes enquanto não sair do lugar
//   magi-real: tira os efeitos ruins
//   flor-estrela: abençoa (+25% de precisão e esquiva) por 30 turnos
// Auxílio da natureza (Caçadora): quando uma planta é ativada perto dela, ganha pele de árvore.
public class PlantasDoShattered {
    private static final double ALCANCE_DO_CARDO = 16;
    private static final double ALCANCE_DO_AUXILIO = 32;

    public static void ativar(ServerLevel mundo, BlockPos posicao, TipoDePlanta tipo, @Nullable LivingEntity quem) {
        mundo.setBlock(posicao, Blocks.AIR.defaultBlockState(), 3);
        Vec3 centro = Vec3.atBottomCenterOf(posicao);
        mundo.playSound(null, posicao, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);

        switch (tipo) {
            case GRAMA_SOLAR -> {
                if (quem != null) {
                    EfeitosNoTick.saudeDaGrama(quem);
                }
                Particulas.explosao(mundo, Particulas.poeira(0xFFFF88, 1.2F), centro.x, centro.y + 1, centro.z, 20, 0.6, 0.02);
            }
            case FOLHA_FANTASMA -> {
                if (quem != null && !(quem instanceof Goo)) {
                    Teleporte.paraLugarAleatorio(mundo, quem);
                }
                Particulas.explosao(mundo, ParticleTypes.END_ROD, centro.x, centro.y + 0.5, centro.z, 10, 0.4, 0.05);
            }
            case MUDA_DE_GELO -> EfeitosDePocao.congelar(mundo, centro);
            case FLOR_DE_FOGO -> EfeitosDePocao.incendiar(mundo, centro);
            case SOFRIMUSGO -> {
                if (quem != null) {
                    int andar = Math.max(1, Andares.andarPelaAltura(posicao.getY()));
                    EfeitosNoTick.envenenar(quem, 5 + Math.round(2 * andar / 3F));
                }
                Particulas.explosao(mundo, Particulas.poeira(0x8844AA, 1.2F), centro.x, centro.y + 0.5, centro.z, 16, 0.4, 0.02);
            }
            case CARDO_LIGEIRO -> {
                if (quem instanceof Player) {
                    // O tempo para em volta do herói: os inimigos perto ficam parados
                    for (MobDoShattered inimigo : mundo.getEntitiesOfClass(MobDoShattered.class, quem.getBoundingBox().inflate(ALCANCE_DO_CARDO))) {
                        inimigo.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 5 * Turno.TICKS));
                    }
                } else if (quem != null) {
                    quem.addEffect(new MobEffectInstance(ModEfeitos.PRESSA, 5 * Turno.TICKS));
                }
                Particulas.explosao(mundo, ParticleTypes.ENCHANT, centro.x, centro.y + 1, centro.z, 30, 1.0, 0.5);
                mundo.playSound(null, posicao, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
            }
            case CEGUERVA -> {
                if (quem != null) {
                    EfeitosNoTick.cegar(quem, 10 * Turno.TICKS);
                    quem.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, 10 * Turno.TICKS));
                }
                Particulas.explosao(mundo, ParticleTypes.FLASH, centro.x, centro.y + 0.5, centro.z, 1, 0, 0);
            }
            case VINHA_TEMPESTUOSA -> {
                if (quem != null) {
                    EfeitosNoTick.tontear(quem, 10 * Turno.TICKS);
                }
                Particulas.explosao(mundo, Particulas.poeira(0x6688FF, 1.2F), centro.x, centro.y + 1, centro.z, 16, 0.5, 0.05);
            }
            case TERRAIZ -> {
                if (quem != null) {
                    EfeitosNoTick.raizDaTerra(quem);
                }
                Particulas.explosao(mundo, Particulas.poeira(0x8B5A2B, 1.5F), centro.x, centro.y + 0.2, centro.z, 24, 0.7, 0.02);
                mundo.playSound(null, posicao, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            case MAGI_REAL -> {
                if (quem != null) {
                    EfeitosDePocao.curarEfeitosRuins(quem);
                    if (quem instanceof ServerPlayer jogador) {
                        jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.magi_real").withStyle(ChatFormatting.GREEN), true);
                    }
                }
                Particulas.explosao(mundo, ParticleTypes.HAPPY_VILLAGER, centro.x, centro.y + 0.8, centro.z, 12, 0.5, 0.0);
            }
            case FLOR_ESTRELA -> {
                if (quem != null) {
                    quem.addEffect(new MobEffectInstance(ModEfeitos.ABENCOADO, 30 * Turno.TICKS));
                }
                Particulas.explosao(mundo, ParticleTypes.END_ROD, centro.x, centro.y + 1, centro.z, 20, 0.5, 0.1);
            }
        }
        auxilioDaNatureza(mundo, centro);
    }

    // Auxílio da natureza: a Caçadora que vê a planta ganha 0-2 de armadura por 3 / 5 turnos
    private static void auxilioDaNatureza(ServerLevel mundo, Vec3 centro) {
        for (ServerPlayer jogador : mundo.players()) {
            int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.AUXILIO_DA_NATUREZA);
            if (pontos <= 0 || RegrasDoHeroi.dados(jogador).classe() != ClasseDoHeroi.CACADORA
                    || jogador.position().distanceTo(centro) > ALCANCE_DO_AUXILIO) {
                continue;
            }
            jogador.addEffect(new MobEffectInstance(ModEfeitos.PELE_DE_ARVORE, (1 + 2 * pontos) * Turno.TICKS, 1));
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0x8B5A2B, 1.0F), 10, 0.05);
        }
    }
}
