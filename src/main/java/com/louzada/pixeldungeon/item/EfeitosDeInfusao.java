package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;

// O que as infusões do Shattered fazem quando quebram (contando em quadradinhos de 4x4 blocos).
//   cáustica: muco cáustico em todo mundo até 3 quadradinhos
//   nevasca: uma nevasca (gás congelante) grande
//   infernal: fogo em tudo até 2 quadradinhos, e quem estiver ali pega fogo
//   de choque: eletricidade até 3 quadradinhos
//   aquática: um gêiser: o chão em volta vira água, as armadilhas desarmam, o fogo apaga e quem estiver perto é jogado pra longe
//   instável: vira uma poção de arremessar qualquer (gelo, chama líquida, gás tóxico, levitação, gás paralisante ou pureza);
//   bebida, vira uma poção de beber qualquer
public class EfeitosDeInfusao {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    // As chances da instável (as do Shattered)
    private static final TipoDePocao[] INSTAVEIS = { TipoDePocao.CURA, TipoDePocao.VISAO_MENTAL, TipoDePocao.GELO, TipoDePocao.CHAMA_LIQUIDA,
            TipoDePocao.GAS_TOXICO, TipoDePocao.PRESSA, TipoDePocao.INVISIBILIDADE, TipoDePocao.LEVITACAO, TipoDePocao.GAS_PARALISANTE,
            TipoDePocao.PUREZA, TipoDePocao.EXPERIENCIA };
    private static final float[] CHANCES = { 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1 };

    public static void quebrar(ServerLevel mundo, TipoDeInfusao tipo, Vec3 lugar, @Nullable Entity dono) {
        BlockPos controle = EfeitosDasPedras.quadradinhoEm(mundo, lugar);
        Vec3 centro = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(lugar), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        switch (tipo) {
            case CAUSTICA -> {
                for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 3)) {
                    Vec3 meio = Armadilhas.centro(quadradinho);
                    Particulas.explosao(mundo, Particulas.poeira(0x101010, 1.5F), meio.x, meio.y + 0.2, meio.z, 6, 1.0, 0.0);
                    for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                        criatura.addEffect(new MobEffectInstance(ModEfeitos.MUCO_CAUSTICO, MucoCaustico.DURACAO));
                    }
                }
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.0F, 0.6F);
            }
            case NEVASCA -> {
                NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.CONGELANTE, 7.0F, 12);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.POWDER_SNOW_BREAK, SoundSource.NEUTRAL, 1.5F, 0.8F);
            }
            case INFERNAL -> {
                for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 2)) {
                    for (BlockPos posicao : BlockPos.betweenClosed(quadradinho.offset(0, 1, 0), quadradinho.offset(T - 1, 1, T - 1))) {
                        BlockPos abaixo = posicao.below();
                        if (mundo.getBlockState(posicao).isAir() && mundo.getBlockState(abaixo).isFaceSturdy(mundo, abaixo, Direction.UP)) {
                            mundo.setBlockAndUpdate(posicao, BaseFireBlock.getState(mundo, posicao));
                        }
                    }
                    for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                        criatura.igniteForSeconds(10);
                        criatura.removeEffect(ModEfeitos.CONGELADO);
                    }
                }
                Particulas.explosao(mundo, ParticleTypes.FLAME, centro.x, centro.y + 0.5, centro.z, 80, 4.0, 0.05);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.5F, 0.8F);
            }
            case CHOQUE -> {
                NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.ELETRICO, 13.0F, 6);
                Particulas.explosao(mundo, ParticleTypes.ELECTRIC_SPARK, centro.x, centro.y + 0.5, centro.z, 100, 5.0, 0.3);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.2F, 1.2F);
            }
            case AQUATICA -> geiser(mundo, controle);
            case INSTAVEL -> {
                TipoDePocao pocao = sortear(mundo.getRandom(), true);
                long semente = Identificacao.sementeDaRun(mundo);
                EfeitosDePocao.quebrar(mundo, lugar, pocao, IdentidadesDaRun.corDoTipo(semente, pocao), dono);
            }
        }
    }

    // Gêiser (a armadilha do gêiser do Shattered): água em volta, o fogo apaga, e quem estiver perto é empurrado pra longe
    public static void geiser(ServerLevel mundo, BlockPos controle) {
        Vec3 centro = Armadilhas.centro(controle);
        List<BlockPos> quadradinhos = EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 2);
        for (BlockPos quadradinho : quadradinhos) {
            if (Armadilhas.estado(mundo, quadradinho) != null) {
                Armadilhas.desarmar(mundo, quadradinho);
            } else {
                EfeitosDePocaoExotica.virarAgua(mundo, quadradinho);
            }
            for (BlockPos posicao : BlockPos.betweenClosed(quadradinho.offset(0, 1, 0), quadradinho.offset(T - 1, 3, T - 1))) {
                if (mundo.getBlockState(posicao).is(BlockTags.FIRE)) {
                    mundo.removeBlock(posicao, false);
                }
            }
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                criatura.clearFire();
                Vec3 longe = criatura.position().subtract(centro);
                Vec3 direcao = longe.horizontalDistanceSqr() < 0.01 ? new Vec3(1, 0, 0) : new Vec3(longe.x, 0, longe.z).normalize();
                criatura.push(direcao.x * 1.6, 0.5, direcao.z * 1.6);
                criatura.hurtMarked = true;
            }
        }
        Particulas.explosao(mundo, ParticleTypes.SPLASH, centro.x, centro.y + 0.5, centro.z, 120, 3.0, 0.5);
        mundo.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, centro.x, centro.y + 0.5, centro.z, 40, 0.5, 1.5, 0.5, 0.3);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 2.0F, 0.8F);
    }

    // Bebeu a infusão instável: uma poção de beber qualquer (sem identificar)
    public static TipoDePocao beberInstavel(ServerPlayer jogador) {
        TipoDePocao pocao = sortear(jogador.getRandom(), false);
        EfeitosDePocao.beber(jogador, pocao, IdentidadesDaRun.corDoTipo(Identificacao.sementeDaRun(jogador), pocao));
        return pocao;
    }

    // Arremessada: só as que quebram (as ruins, a levitação e a pureza); bebida: só as de beber
    public static TipoDePocao sortear(RandomSource sorteio, boolean deArremessar) {
        while (true) {
            float total = 0;
            for (float chance : CHANCES) {
                total += chance;
            }
            float escolha = sorteio.nextFloat() * total;
            int i = 0;
            while (i < CHANCES.length - 1 && escolha >= CHANCES[i]) {
                escolha -= CHANCES[i];
                i++;
            }
            TipoDePocao tipo = INSTAVEIS[i];
            boolean ruim = tipo == TipoDePocao.GELO || tipo == TipoDePocao.CHAMA_LIQUIDA || tipo == TipoDePocao.GAS_TOXICO
                    || tipo == TipoDePocao.GAS_PARALISANTE;
            boolean quebra = ruim || tipo == TipoDePocao.LEVITACAO || tipo == TipoDePocao.PUREZA;
            if (deArremessar ? quebra : !ruim) {
                return tipo;
            }
        }
    }
}
