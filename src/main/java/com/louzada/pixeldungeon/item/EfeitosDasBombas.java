package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.bloco.CapimAlto;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.BombaAcesa;
import com.louzada.pixeldungeon.entidade.GotaDeOrvalho;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// O que as bombas do Shattered fazem quando explodem (ver TipoDeBomba). Tudo conta em quadradinhos (4x4 blocos).
//   explosão comum: dano de (4 + andar) a (12 + 3 × andar), menos a armadura, em todo mundo até o alcance da bomba
//   (inclusive em quem jogou); quebra os itens frágeis do chão (as poções quebram e fazem efeito), queima a grama,
//   e as outras bombas que estiverem ali explodem também
//   gelo: congela por 2 turnos quem estiver perto, a água vira gelo e o fogo apaga
//   lã: uma ovelha mágica em cada quadradinho livre até 4 passos, por 200 turnos
//   fogo: fogo em todos os quadradinhos
//   barulho: vira um alarme (ver BombaAcesa) e explode quando alguém pisa nela
//   fumaça: uma nuvem de fumaça grande, que ninguém enxerga através
//   clarão: um choque (1/4 do dano da explosão) que paralisa por 10 turnos
//   brotamento: não machuca; cura o herói e os aliados, cresce grama alta, nascem 2 ou 3 plantas e cai o que uma planta
//   especial daria (gotas de orvalho ou sementes)
//   sagrada: a explosão comum, e os mortos-vivos e demônios levam metade do dano a mais (o esgoto ainda não tem nenhum)
//   arcana: não quebra nada; o dano ignora a armadura
public class EfeitosDasBombas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    public static void explodir(ServerLevel mundo, TipoDeBomba tipo, BlockPos controle, @Nullable Entity dono) {
        List<Runnable> emSeguida = new ArrayList<>();
        Vec3 meio = Armadilhas.centro(controle);
        int alcance = tipo.alcance();
        if (tipo.destrutiva()) {
            explosao(mundo, controle, alcance, dono, emSeguida);
        } else {
            mundo.playSound(null, BlockPos.containing(meio), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 1.4F);
        }
        List<BlockPos> quadradinhos = EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, alcance);
        switch (tipo) {
            case GELO -> gelo(mundo, quadradinhos);
            case LA -> la(mundo, controle);
            case FOGO -> fogo(mundo, quadradinhos);
            case FUMACA -> NuvemDeGas.soltar(mundo, meio, NuvemDeGas.Tipo.NEBLINA, 10.0F, 25);
            case CLARAO -> clarao(mundo, quadradinhos, meio, dono);
            case BROTAMENTO -> brotamento(mundo, quadradinhos, meio);
            case SAGRADA -> sagrada(mundo, quadradinhos, meio, dono);
            case ARCANA -> arcana(mundo, quadradinhos, meio, dono);
            default -> {
            }
        }
        for (Runnable outra : emSeguida) {
            outra.run();
        }
    }

    // O andar que manda no dano (fora da dungeon, 1)
    private static int andar(ServerLevel mundo, Vec3 lugar) {
        return EfeitosDasPedras.andarDe(mundo, lugar);
    }

    public static int dano(ServerLevel mundo, Vec3 lugar) {
        int andar = andar(mundo, lugar);
        return Sorteio.normal(mundo.getRandom(), 4 + andar, 12 + 3 * andar);
    }

    // ---- A explosão comum ----

    private static void explosao(ServerLevel mundo, BlockPos controle, int alcance, @Nullable Entity dono, List<Runnable> emSeguida) {
        Vec3 meio = Armadilhas.centro(controle);
        long semente = Identificacao.sementeDaRun(mundo);
        DamageSource fonte = mundo.damageSources().explosion(null, dono);
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, alcance)) {
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.LARGE_SMOKE, centro.x, centro.y + 0.5, centro.z, 6, 1.0, 0.4, 1.0, 0.02);
            if (!quadradinho.equals(controle)) {
                mundo.sendParticles(ParticleTypes.EXPLOSION, centro.x, centro.y + 0.5, centro.z, 1, 0.8, 0.3, 0.8, 0);
            }
            EfeitosDasPedras.queimarGrama(mundo, quadradinho);
            for (ItemEntity item : EfeitosDasArmadilhas.itensEmVolta(mundo, quadradinho, 0)) {
                ItemStack pilha = item.getItem();
                if (pilha.getItem() instanceof ItemBomba bomba) {
                    // As bombas que estavam no chão explodem também
                    item.discard();
                    BlockPos onde = EfeitosDasPedras.quadradinhoEm(mundo, item.position());
                    emSeguida.add(() -> explodir(mundo, bomba.tipo(), onde, dono));
                } else if (!EfeitosDasPedras.aguentaExplosao(pilha, semente)) {
                    if (pilha.getItem() instanceof ItemPocao pocao) {
                        EfeitosDePocao.quebrar(mundo, item.position(), IdentidadesDaRun.tipoDaCor(semente, pocao.cor()), pocao.cor(), dono);
                    }
                    item.discard();
                }
            }
            for (BombaAcesa acesa : mundo.getEntitiesOfClass(BombaAcesa.class, caixa(quadradinho))) {
                emSeguida.add(() -> acesa.explodir(mundo));
            }
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                if (criatura instanceof OvelhaMagica || !criatura.isAlive()) {
                    continue;
                }
                int dano = dano(mundo, centro) - EfeitosDasArmadilhas.armadura(criatura);
                if (dano > 0) {
                    Combate.aplicar(criatura, dano, fonte);
                }
            }
        }
        mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, meio.x, meio.y + 0.5, meio.z, 1, 0, 0, 0, 0);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    // Os blocos de um quadradinho (do chão até 3 blocos pra cima)
    private static AABB caixa(BlockPos controle) {
        return new AABB(controle.getX(), controle.getY(), controle.getZ(), controle.getX() + T, controle.getY() + 4,
                controle.getZ() + T);
    }

    // ---- As melhoradas ----

    private static void gelo(ServerLevel mundo, List<BlockPos> quadradinhos) {
        for (BlockPos quadradinho : quadradinhos) {
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                criatura.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, 2 * Turno.TICKS));
                criatura.setTicksFrozen(Math.max(criatura.getTicksFrozen(), 160));
                criatura.clearFire();
            }
            for (BlockPos posicao : BlockPos.betweenClosed(quadradinho.offset(0, -1, 0), quadradinho.offset(T - 1, 3, T - 1))) {
                if (mundo.getBlockState(posicao).is(Blocks.WATER) && mundo.getFluidState(posicao).isSource()) {
                    mundo.setBlockAndUpdate(posicao, Blocks.FROSTED_ICE.defaultBlockState());
                    mundo.scheduleTick(posicao.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(mundo.getRandom(), 60, 120));
                } else if (mundo.getBlockState(posicao).is(BlockTags.FIRE)) {
                    mundo.removeBlock(posicao, false);
                }
            }
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.SNOWFLAKE, centro.x, centro.y + 0.5, centro.z, 12, 1.5, 0.5, 1.5, 0.02);
        }
        mundo.playSound(null, BlockPos.containing(Armadilhas.centro(quadradinhos.get(0))), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static void la(ServerLevel mundo, BlockPos controle) {
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 4)) {
            if (EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0).isEmpty()) {
                OvelhaMagica.criar(mundo, Armadilhas.centro(quadradinho), 200);
            }
        }
        Vec3 meio = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
    }

    private static void fogo(ServerLevel mundo, List<BlockPos> quadradinhos) {
        for (BlockPos quadradinho : quadradinhos) {
            for (BlockPos posicao : BlockPos.betweenClosed(quadradinho.offset(0, 1, 0), quadradinho.offset(T - 1, 1, T - 1))) {
                BlockPos abaixo = posicao.below();
                if (mundo.getBlockState(posicao).isAir() && mundo.getBlockState(abaixo).isFaceSturdy(mundo, abaixo, Direction.UP)) {
                    mundo.setBlockAndUpdate(posicao, BaseFireBlock.getState(mundo, posicao));
                }
            }
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                criatura.igniteForSeconds(8);
                criatura.removeEffect(ModEfeitos.CONGELADO);
            }
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.FLAME, centro.x, centro.y + 0.3, centro.z, 10, 1.5, 0.3, 1.5, 0.02);
        }
        mundo.playSound(null, BlockPos.containing(Armadilhas.centro(quadradinhos.get(0))), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void clarao(ServerLevel mundo, List<BlockPos> quadradinhos, Vec3 meio, @Nullable Entity dono) {
        DamageSource fonte = mundo.damageSources().indirectMagic(dono, dono);
        for (BlockPos quadradinho : quadradinhos) {
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                if (!criatura.isAlive()) {
                    continue;
                }
                Combate.aplicar(criatura, Math.round(dano(mundo, meio) / 4.0F), fonte);
                if (criatura.isAlive()) {
                    criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 10 * Turno.TICKS));
                }
                linha(mundo, meio.add(0, 0.5, 0), criatura.position().add(0, criatura.getBbHeight() / 2, 0));
            }
        }
        mundo.sendParticles(ParticleTypes.FLASH, meio.x, meio.y + 0.5, meio.z, 1, 0, 0, 0, 0);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5F, 1.2F);
    }

    // Um raio de faíscas entre dois lugares
    private static void linha(ServerLevel mundo, Vec3 de, Vec3 ate) {
        int passos = Math.max(2, (int) (de.distanceTo(ate) * 2));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = de.lerp(ate, i / (double) passos);
            mundo.sendParticles(ParticleTypes.ELECTRIC_SPARK, ponto.x, ponto.y, ponto.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    // Brotamento: não machuca
    private static void brotamento(ServerLevel mundo, List<BlockPos> quadradinhos, Vec3 meio) {
        RandomSource sorteio = mundo.getRandom();
        List<BlockPos> livres = new ArrayList<>();
        for (BlockPos quadradinho : quadradinhos) {
            List<LivingEntity> criaturas = EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0);
            for (LivingEntity criatura : criaturas) {
                boolean aliado = criatura instanceof ServerPlayer || (criatura instanceof MobDoShattered mob && mob.aliado());
                if (aliado) {
                    EfeitosDePocao.curarEfeitosRuins(criatura);
                    EfeitosNoTick.curar(criatura, Math.round(0.8F * criatura.getMaxHealth() + 14));
                }
            }
            if (criaturas.isEmpty() && mundo.getBlockState(quadradinho.offset(T / 2, 1, T / 2)).canBeReplaced()) {
                livres.add(quadradinho);
                crescerGrama(mundo, quadradinho);
            }
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.HAPPY_VILLAGER, centro.x, centro.y + 0.5, centro.z, 6, 1.5, 0.4, 1.5, 0.02);
        }
        // 2 ou 3 plantas quaisquer (2 em 3 vezes, 2)
        int plantas = sorteio.nextInt(3) < 2 ? 2 : 3;
        for (int i = 0; i < plantas && !livres.isEmpty(); i++) {
            BlockPos quadradinho = livres.remove(sorteio.nextInt(livres.size()));
            TipoDePlanta planta = TipoDePlanta.values()[sorteio.nextInt(TipoDePlanta.values().length)];
            ItemSemente.plantar(mundo, BlockPos.containing(Armadilhas.centro(quadradinho)), planta);
        }
        // A planta especial do Shattered (que o mod ainda não tem): cai o que ela daria
        if (!livres.isEmpty()) {
            Vec3 lugar = Armadilhas.centro(livres.get(sorteio.nextInt(livres.size())));
            if (sorteio.nextInt(10) < 6) {
                // Pega-orvalho: 3 a 6 gotas de orvalho
                int gotas = 3 + sorteio.nextInt(4);
                for (int i = 0; i < gotas; i++) {
                    GotaDeOrvalho.soltar(mundo, lugar.x + sorteio.nextDouble() * 2 - 1, lugar.y + 0.3, lugar.z + sorteio.nextDouble() * 2 - 1);
                }
            } else {
                // Vagem de sementes: 2 a 4 sementes
                int sementes = 2 + sorteio.nextInt(3);
                for (int i = 0; i < sementes; i++) {
                    TipoDePlanta planta = TipoDePlanta.values()[sorteio.nextInt(TipoDePlanta.values().length)];
                    mundo.addFreshEntity(new ItemEntity(mundo, lugar.x, lugar.y + 0.3, lugar.z, new ItemStack(ItemSemente.daPlanta(planta))));
                }
            }
        }
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.5F, 0.8F);
    }

    // Grama alta em todo o quadradinho (onde tiver espaço)
    private static void crescerGrama(ServerLevel mundo, BlockPos controle) {
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                BlockPos baixo = controle.offset(dx, 1, dz);
                if (!mundo.getBlockState(baixo).isAir() && !mundo.getBlockState(baixo).is(ModBlocos.GRAMA_BAIXA.get())
                        || !mundo.getBlockState(baixo.above()).isAir()) {
                    continue;
                }
                mundo.setBlock(baixo, ModBlocos.CAPIM_ALTO.get().defaultBlockState().setValue(CapimAlto.HALF, DoubleBlockHalf.LOWER), 2);
                mundo.setBlock(baixo.above(), ModBlocos.CAPIM_ALTO.get().defaultBlockState().setValue(CapimAlto.HALF, DoubleBlockHalf.UPPER), 2);
            }
        }
    }

    private static void sagrada(ServerLevel mundo, List<BlockPos> quadradinhos, Vec3 meio, @Nullable Entity dono) {
        DamageSource fonte = mundo.damageSources().indirectMagic(dono, dono);
        for (BlockPos quadradinho : quadradinhos) {
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                if (criatura instanceof MobDoShattered mob && mob.mortoVivoOuDemonio() && criatura.isAlive()) {
                    Combate.aplicar(criatura, Math.round(dano(mundo, meio) * 0.5F), fonte);
                    mundo.sendParticles(ParticleTypes.SOUL, criatura.getX(), criatura.getY() + 1, criatura.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                }
            }
        }
        mundo.sendParticles(ParticleTypes.END_ROD, meio.x, meio.y + 1, meio.z, 40, 2.5, 1.0, 2.5, 0.05);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    private static void arcana(ServerLevel mundo, List<BlockPos> quadradinhos, Vec3 meio, @Nullable Entity dono) {
        DamageSource fonte = mundo.damageSources().indirectMagic(dono, dono);
        for (BlockPos quadradinho : quadradinhos) {
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.WITCH, centro.x, centro.y + 0.5, centro.z, 10, 1.5, 0.5, 1.5, 0.02);
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                if (criatura.isAlive()) {
                    Combate.aplicar(criatura, dano(mundo, meio), fonte);
                }
            }
        }
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5F, 0.8F);
    }
}
