package com.louzada.pixeldungeon.dungeon;

import java.util.Random;

import com.louzada.pixeldungeon.bloco.CapimAlto;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.GotaDeOrvalho;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

// Pisotear a grama alta, igual ao Shattered. A grama vale pelo quadradinho inteiro (4x4 blocos):
// encostou em qualquer parte, o quadradinho todo é pisoteado e vira grama baixa.
//   - 1 em 6: cai uma gota de orvalho (ela voa sozinha pro odre de quem estiver perto)
//   - 1 em 25: cai uma semente
//   - Caçadora: passa sem derrubar a grama (fica "trilhada"), mas ainda ganha o que cair
//   - Caçadora com recompensa da natureza: às vezes cai uma baga
// Quem voa (o enxame de moscas) não pisoteia.
public class Grama {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int UMA_GOTA_EM = 6;
    private static final int UMA_SEMENTE_EM = 25;
    private static final String BAGAS_DADAS = "bagas_dadas";

    public static void pisotear(ServerLevel mundo, BlockPos posicao, BlockState estado, Entity entidade) {
        if (!(entidade instanceof LivingEntity criatura) || criatura.isSpectator() || criatura.isNoGravity()
                || criatura instanceof EnxameDeMoscas || !criatura.isAlive()) {
            return;
        }
        boolean cacadora = criatura instanceof ServerPlayer jogador && RegrasDoHeroi.dados(jogador).classe() == ClasseDoHeroi.CACADORA;
        boolean jaTrilhado = estado.getValue(CapimAlto.TRILHADO);
        if (cacadora && jaTrilhado) {
            // A Caçadora anda pela grama trilhada sem mexer nela
            return;
        }

        // O quadradinho onde está este bloco (os quadradinhos começam em múltiplos de 4)
        int baixo = estado.getValue(CapimAlto.HALF) == DoubleBlockHalf.LOWER ? posicao.getY() : posicao.getY() - 1;
        int inicioX = posicao.getX() - Math.floorMod(posicao.getX(), T);
        int inicioZ = posicao.getZ() - Math.floorMod(posicao.getZ(), T);
        BlockState gramaBaixa = ModBlocos.GRAMA_BAIXA.get().defaultBlockState();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                BlockPos embaixo = new BlockPos(inicioX + dx, baixo, inicioZ + dz);
                BlockState aqui = mundo.getBlockState(embaixo);
                if (!aqui.is(ModBlocos.CAPIM_ALTO.get()) || aqui.getValue(CapimAlto.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }
                if (mundo.getRandom().nextInt(3) == 0) {
                    mundo.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, aqui), embaixo.getX() + 0.5, baixo + 1.0,
                            embaixo.getZ() + 0.5, 6, 0.4, 0.6, 0.4, 0.05);
                }
                if (cacadora) {
                    // Trilhada: continua em pé
                    mundo.setBlock(embaixo, aqui.setValue(CapimAlto.TRILHADO, true), flags);
                    mundo.setBlock(embaixo.above(), aqui.setValue(CapimAlto.TRILHADO, true).setValue(CapimAlto.HALF, DoubleBlockHalf.UPPER), flags);
                } else {
                    mundo.setBlock(embaixo.above(), Blocks.AIR.defaultBlockState(), flags);
                    mundo.setBlock(embaixo, gramaBaixa, flags);
                }
            }
        }
        mundo.playSound(null, inicioX + 2, baixo, inicioZ + 2, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);

        // A grama trilhada já deu o que tinha pra dar
        if (!jaTrilhado) {
            soltarCoisas(mundo, criatura, inicioX + 2.0, baixo + 0.5, inicioZ + 2.0);
        }
    }

    private static void soltarCoisas(ServerLevel mundo, LivingEntity quemPisou, double x, double y, double z) {
        RandomSource sorteio = mundo.getRandom();
        if (sorteio.nextInt(UMA_GOTA_EM) == 0) {
            GotaDeOrvalho.soltar(mundo, x, y, z);
        }
        if (sorteio.nextInt(UMA_SEMENTE_EM) == 0) {
            SaqueDoAndar.soltar(mundo, x, y, z, SaqueDoAndar.sementeAleatoria(new Random(sorteio.nextLong())));
        }
        if (quemPisou instanceof ServerPlayer jogador && GerenciadorDoHeroi.emJogo(jogador)) {
            talvezBaga(mundo, jogador, x, y, z);
        }
    }

    // Recompensa da natureza (Caçadora): 4/6 bagas espalhadas pelos andares 2-4 / 2-8, igual ao Shattered.
    // Se ela estiver atrasada, a chance é maior (1 em 10); no andar certo, 1 em 30; adiantada, 1 em 90.
    private static void talvezBaga(ServerLevel mundo, ServerPlayer jogador, double x, double y, double z) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos = dados.pontos(Talento.RECOMPENSA_DA_NATUREZA);
        if (pontos <= 0) {
            return;
        }
        int sobrando = 2 + 2 * pontos - dados.inteiro(BAGAS_DADAS);
        if (sobrando <= 0) {
            return;
        }
        int andarCerto = 2 + 2 * pontos - sobrando;
        andarCerto += andarCerto >= 5 ? 3 : 2;
        int andar = Andares.andarPelaAltura((int) Math.floor(y));
        int chance = andar > andarCerto ? 10 : andar == andarCerto ? 30 : 90;
        if (mundo.getRandom().nextInt(chance) == 0) {
            dados.definir(BAGAS_DADAS, dados.inteiro(BAGAS_DADAS) + 1);
            ItemEntity baga = new ItemEntity(mundo, x, y, z, new ItemStack(ModItens.BAGA.get()));
            baga.setDefaultPickUpDelay();
            mundo.addFreshEntity(baga);
        }
    }
}
