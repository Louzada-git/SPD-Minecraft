package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

// Os abismos do Shattered (o "Chasm"): quem chega no fundo do buraco cai pro andar de baixo.
//   - o herói aparece num lugar qualquer do andar de baixo (quem cai do piso frágil, na sala do poço), aleijado por 10 turnos,
//     sangrando (mais quanto mais vida tiver) e machucado (a metade da vida, ou mais): igual ao Shattered.
//     Com a queda suave (elixir), não se machuca. Levitando, não cai.
//   - os inimigos morrem na queda (os que voam não caem)
//   - os itens caem e aparecem num lugar qualquer do andar de baixo
public final class Abismo {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int TURNOS_ALEIJADO = 10;

    private Abismo() {
    }

    public static void cair(ServerLevel mundo, Entity entidade, BlockPos fundo) {
        if (entidade.isRemoved()) {
            return;
        }
        int andar = Andares.andarPelaAltura(fundo.getY() + 2);
        if (entidade instanceof ServerPlayer jogador) {
            if (!jogador.isSpectator() && !jogador.hasEffect(ModEfeitos.LEVITANDO)) {
                cairJogador(mundo, jogador, andar, fundo);
            }
        } else if (entidade instanceof ItemEntity item) {
            cairItem(mundo, item, andar);
        } else if (entidade instanceof LivingEntity criatura) {
            if (!(criatura instanceof Abelha) && !(criatura instanceof EnxameDeMoscas) && !criatura.isNoGravity() && criatura.isAlive()) {
                // Igual ao Shattered: inimigo que cai no abismo morre (o que ele deixar cai também)
                criatura.hurt(mundo.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
    }

    private static void cairJogador(ServerLevel mundo, ServerPlayer jogador, int andar, BlockPos fundo) {
        int destino = andar + 1;
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        if (destino > Andares.ULTIMO_ANDAR_LIBERADO || run.andar(andar) == null) {
            // Não tem pra onde cair: volta pra entrada do andar
            DadosDaRun.InfoAndar info = run.andar(andar);
            if (info != null) {
                jogador.teleportTo(mundo, info.entrada().getX() + 0.5, info.entrada().getY(), info.entrada().getZ() + 0.5, jogador.getYRot(),
                        jogador.getXRot());
                jogador.resetFallDistance();
            }
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bloqueado"), true);
            return;
        }
        // Caiu do piso frágil? Vai parar na sala do poço lá embaixo
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        int qx = Math.floorDiv(fundo.getX() - origem.getX(), T);
        int qy = Math.floorDiv(fundo.getZ() - origem.getZ(), T);
        Sala fragil = PopulacaoDoAndar.planta(run.semente(), andar).pisoFragil();
        boolean doPisoFragil = fragil != null && dentro(fragil, qx, qy);

        GerenciadorDaRun.garantirAndar(mundo.getServer(), destino);
        BlockPos lugar = lugarDeCair(run, destino, doPisoFragil, mundo.getRandom().nextLong());
        GerenciadorDaRun.chegarCaindo(jogador, destino, lugar);
        mundo.playSound(null, lugar, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 0.8F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.caiu").withStyle(ChatFormatting.RED), false);
        aterrissar(jogador);
    }

    // A queda, igual ao Shattered: quanto menos vida, mais sangramento e menos dano na hora
    public static void aterrissar(ServerPlayer jogador) {
        if (jogador.hasEffect(ModEfeitos.QUEDA_SUAVE)) {
            return;
        }
        float vida = jogador.getHealth();
        float maxima = jogador.getMaxHealth();
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, TURNOS_ALEIJADO * 40));
        EfeitosNoTick.sangrar(jogador, Math.round(maxima / (6F + 6F * (vida / maxima))));
        Combate.aplicar(jogador, danoDaQueda(jogador.getRandom(), vida, maxima), jogador.damageSources().fall());
    }

    // O dano da queda: pelo menos metade da vida que tinha
    public static int danoDaQueda(RandomSource sorteio, float vida, float maxima) {
        return Math.max((int) vida / 2, Sorteio.normal(sorteio, (int) vida / 2, (int) maxima / 4));
    }

    private static void cairItem(ServerLevel mundo, ItemEntity item, int andar) {
        int destino = andar + 1;
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        if (destino > Andares.ULTIMO_ANDAR_LIBERADO || run.andar(andar) == null) {
            item.discard();
            return;
        }
        GerenciadorDaRun.garantirAndar(mundo.getServer(), destino);
        BlockPos lugar = lugarDeCair(run, destino, false, mundo.getRandom().nextLong());
        item.teleportTo(lugar.getX() + 0.5, lugar.getY() + 0.5, lugar.getZ() + 0.5);
        item.setDeltaMovement(0, 0, 0);
        item.setUnlimitedLifetime();
    }

    // Onde se cai no andar: na sala do poço (se veio do piso frágil) ou num lugar livre qualquer
    public static BlockPos lugarDeCair(DadosDaRun run, int andar, boolean naSalaDoPoco, long semente) {
        Random sorteio = new Random(semente);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        Sala poco = planta.salaDoPoco();
        if (naSalaDoPoco && poco != null) {
            List<int[]> lugares = new ArrayList<>();
            for (int y = poco.y(); y < poco.y() + poco.altura(); y++) {
                for (int x = poco.x(); x < poco.x() + poco.largura(); x++) {
                    if (planta.passavel(x, y) && !planta.ehPorta(x, y)) {
                        lugares.add(new int[] { x, y });
                    }
                }
            }
            if (!lugares.isEmpty()) {
                int[] lugar = lugares.get(sorteio.nextInt(lugares.size()));
                return ConstrutorDeAndar.pontoDeChegada(origem, lugar[0], lugar[1]);
            }
        }
        BlockPos lugar = PopulacaoDoAndar.lugarAleatorio(run.semente(), origem, andar, sorteio);
        if (lugar != null) {
            return lugar;
        }
        DadosDaRun.InfoAndar info = run.andar(andar);
        return info != null ? info.entrada() : ConstrutorDeAndar.pontoDeChegada(origem, planta.entradaX(), planta.entradaY());
    }

    private static boolean dentro(Sala sala, int qx, int qy) {
        return qx >= sala.x() && qx < sala.x() + sala.largura() && qy >= sala.y() && qy < sala.y() + sala.altura();
    }
}
