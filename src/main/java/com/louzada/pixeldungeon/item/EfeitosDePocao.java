package com.louzada.pixeldungeon.item;

import java.util.List;
import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.bloco.FogoEterno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// O que cada poção do Shattered faz, com os números de lá (1 turno = 2 segundos).
// As poções ruins (gelo, chama líquida, gases) quebram em volta de quem bebe; o certo é arremessar.
public class EfeitosDePocao {

    // ---- Beber ----

    public static void beber(ServerPlayer jogador, TipoDePocao tipo, CorDePocao cor) {
        ServerLevel mundo = jogador.serverLevel();
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        switch (tipo) {
            case FORCA -> {
                dados.definirForca(dados.forcaBase() + 1);
                NumerosFlutuantes.mostrarTraduzido(jogador, "efeito.pixeldungeon.forca.mostrar", NumerosFlutuantes.VERDE);
                avisar(jogador, Component.translatable("efeito.pixeldungeon.forca", dados.forca()), ChatFormatting.GREEN);
                Particulas.subindoEmVolta(jogador, ParticleTypes.HAPPY_VILLAGER, 12, 0.1);
                RedeDoHeroi.enviarEstado(jogador, true);
            }
            case CURA -> {
                // Cura 80% da vida máxima + 14, aos poucos, e tira os efeitos ruins
                curarEfeitosRuins(jogador);
                EfeitosNoTick.curar(jogador, Math.round(0.8F * jogador.getMaxHealth() + 14));
                avisar(jogador, Component.translatable("efeito.pixeldungeon.cura"), ChatFormatting.GREEN);
            }
            case EXPERIENCIA -> {
                // Ganha o XP de um nível inteiro
                int xp = RegrasDoHeroi.xpParaSubir(dados.nivel());
                NumerosFlutuantes.mostrar(jogador, "+" + xp + " XP", NumerosFlutuantes.ROXO);
                RegrasDoHeroi.ganharXp(jogador, xp);
            }
            case VISAO_MENTAL -> {
                // Todos os inimigos do andar brilham através das paredes por 20 turnos
                jogador.addEffect(new MobEffectInstance(ModEfeitos.VISAO_MENTAL, 20 * Turno.TICKS));
                List<MobDoShattered> inimigos = inimigosDoAndar(jogador);
                for (MobDoShattered inimigo : inimigos) {
                    inimigo.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * Turno.TICKS, 0, false, false));
                }
                avisar(jogador, Component.translatable(inimigos.isEmpty() ? "efeito.pixeldungeon.visao_mental.nenhum" : "efeito.pixeldungeon.visao_mental"),
                        ChatFormatting.AQUA);
            }
            case PRESSA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.PRESSA, 20 * Turno.TICKS));
                avisar(jogador, Component.translatable("efeito.pixeldungeon.pressa"), ChatFormatting.YELLOW);
            }
            case INVISIBILIDADE -> {
                Furtividade.ficarInvisivel(jogador, 20 * Turno.TICKS);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
                avisar(jogador, Component.translatable("efeito.pixeldungeon.invisibilidade"), ChatFormatting.AQUA);
            }
            case LEVITACAO -> {
                EfeitosNoTick.levitar(jogador, 20 * Turno.TICKS);
                avisar(jogador, Component.translatable("efeito.pixeldungeon.levitacao"), ChatFormatting.AQUA);
            }
            case PUREZA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.IMUNE_A_GAS, 20 * Turno.TICKS));
                avisar(jogador, Component.translatable("efeito.pixeldungeon.pureza"), ChatFormatting.AQUA);
            }
            // As ruins quebram em volta de quem bebeu
            default -> quebrar(mundo, jogador.position(), tipo, cor, jogador);
        }
    }

    // ---- Arremessar ----

    // A poção quebrou nesse lugar. Devolve true se o efeito apareceu (aí quem arremessou descobre o tipo).
    public static boolean quebrar(ServerLevel mundo, Vec3 lugar, TipoDePocao tipo, CorDePocao cor, @Nullable Entity dono) {
        mundo.playSound(null, BlockPos.containing(lugar), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        Particulas.explosao(mundo, Particulas.poeira(cor.cor(), 1.5F), lugar.x, lugar.y + 0.3, lugar.z, 20, 0.6, 0.05);
        switch (tipo) {
            case GELO -> congelar(mundo, lugar);
            case CHAMA_LIQUIDA -> incendiar(mundo, lugar);
            case GAS_TOXICO -> NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.TOXICO, 7.0F, 15);
            case GAS_PARALISANTE -> NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.PARALISANTE, 7.0F, 10);
            // A levitação arremessada vira gás de confusão
            case LEVITACAO -> NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.CONFUSAO, 5.0F, 10);
            case PUREZA -> limparGasesEFogo(mundo, lugar, 12);
            default -> {
                // As outras só espirram, sem fazer nada (igual ao Shattered)
                if (dono instanceof ServerPlayer jogador) {
                    jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pocao_espirrou").withStyle(ChatFormatting.GRAY), true);
                }
                return false;
            }
        }
        return true;
    }

    // ---- As ruins ----

    // Gelo: congela por 10 turnos quem estiver perto e a água vira gelo (que derrete depois)
    public static void congelar(ServerLevel mundo, Vec3 lugar) {
        // O gelo apaga o fogo eterno (a parede inteira)
        FogoEterno.apagarPerto(mundo, lugar, 5);
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(lugar, 10, 6, 10))) {
            if (criatura.isSpectator()) {
                continue;
            }
            criatura.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, 10 * Turno.TICKS));
            criatura.setTicksFrozen(Math.max(criatura.getTicksFrozen(), 160));
            criatura.clearFire();
        }
        BlockPos centro = BlockPos.containing(lugar);
        for (BlockPos posicao : BlockPos.betweenClosed(centro.offset(-5, -2, -5), centro.offset(5, 1, 5))) {
            if (mundo.getBlockState(posicao).is(Blocks.WATER) && mundo.getFluidState(posicao).isSource()) {
                mundo.setBlockAndUpdate(posicao, Blocks.FROSTED_ICE.defaultBlockState());
                mundo.scheduleTick(posicao.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(mundo.getRandom(), 60, 120));
            } else if (mundo.getBlockState(posicao).is(BlockTags.FIRE)) {
                mundo.removeBlock(posicao, false);
            }
        }
        Particulas.explosao(mundo, ParticleTypes.SNOWFLAKE, lugar.x, lugar.y + 0.5, lugar.z, 60, 3.0, 0.05);
        mundo.playSound(null, centro, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 0.6F);
    }

    // Chama líquida: fogo em volta e em quem estiver perto
    public static void incendiar(ServerLevel mundo, Vec3 lugar) {
        BlockPos centro = BlockPos.containing(lugar);
        for (BlockPos posicao : BlockPos.betweenClosed(centro.offset(-3, -1, -3), centro.offset(3, 1, 3))) {
            if (mundo.getBlockState(posicao).isAir() && mundo.getBlockState(posicao.below()).isFaceSturdy(mundo, posicao.below(), Direction.UP)) {
                mundo.setBlockAndUpdate(posicao, BaseFireBlock.getState(mundo, posicao));
            }
        }
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(lugar, 7, 4, 7))) {
            criatura.igniteForSeconds(8);
            criatura.removeEffect(ModEfeitos.CONGELADO);
        }
        Particulas.explosao(mundo, ParticleTypes.FLAME, lugar.x, lugar.y + 0.5, lugar.z, 50, 2.0, 0.05);
        mundo.playSound(null, centro, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    // Pureza: some com as nuvens de gás e apaga o fogo em volta
    public static void limparGasesEFogo(ServerLevel mundo, Vec3 lugar, int raio) {
        // Igual ao Shattered, a pureza também limpa o fogo eterno
        FogoEterno.apagarPerto(mundo, lugar, raio);
        for (NuvemDeGas nuvem : mundo.getEntitiesOfClass(NuvemDeGas.class, AABB.ofSize(lugar, raio * 2, raio * 2, raio * 2))) {
            nuvem.discard();
        }
        BlockPos centro = BlockPos.containing(lugar);
        for (BlockPos posicao : BlockPos.betweenClosed(centro.offset(-raio, -2, -raio), centro.offset(raio, 2, raio))) {
            if (mundo.getBlockState(posicao).is(BlockTags.FIRE)) {
                mundo.removeBlock(posicao, false);
            }
        }
        Particulas.explosao(mundo, ParticleTypes.END_ROD, lugar.x, lugar.y + 1, lugar.z, 30, raio / 3.0, 0.02);
    }

    // ---- Ajudantes ----

    // A poção de cura e a erva-real tiram: veneno, aleijado, sangramento, cegueira, sono, lentidão e vertigem
    public static void curarEfeitosRuins(LivingEntity criatura) {
        criatura.removeEffect(ModEfeitos.VENENO);
        criatura.removeEffect(MobEffects.POISON);
        criatura.removeEffect(ModEfeitos.ALEIJADO);
        criatura.removeEffect(ModEfeitos.SANGRANDO);
        criatura.removeEffect(ModEfeitos.CEGO);
        criatura.removeEffect(MobEffects.BLINDNESS);
        criatura.removeEffect(ModEfeitos.SONOLENTO);
        criatura.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        criatura.removeEffect(ModEfeitos.VERTIGEM);
        criatura.removeEffect(MobEffects.CONFUSION);
        criatura.removeEffect(MobEffects.WEAKNESS);
    }

    private static List<MobDoShattered> inimigosDoAndar(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Integer andar = PopulacaoDoAndar.andarEm(run, jogador.getX(), jogador.getY(), jogador.getZ());
        AABB caixa = andar == null ? jogador.getBoundingBox().inflate(64)
                : PopulacaoDoAndar.caixaDoAndar(Andares.origemDoAndar(run.numeroDaRun(), andar));
        return jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, caixa);
    }

    private static void avisar(ServerPlayer jogador, Component mensagem, ChatFormatting cor) {
        jogador.displayClientMessage(mensagem.copy().withStyle(cor), true);
    }
}
