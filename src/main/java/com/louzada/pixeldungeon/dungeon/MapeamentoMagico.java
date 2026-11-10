package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Pergaminho de mapeamento mágico. No Shattered ele mostra o mapa inteiro do andar; aqui,
// como não tem mapa, o leitor fica sabendo pra que lado e a quantos quadradinhos está a saída,
// e colunas de luz (que só ele vê, através das paredes) marcam a saída e a entrada por 30 turnos.
// Igual ao Shattered, ele também revela todas as portas escondidas do andar (e elas ganham uma coluna de luz)
// e todas as armadilhas escondidas.
public class MapeamentoMagico {
    private static final int DURACAO = 30 * 20;
    private static final int ALTURA_DA_COLUNA = 8;

    private record Marca(UUID jogador, List<BlockPos> lugares, int[] ticksRestantes) {
    }

    private static List<Marca> ativas = new ArrayList<>();

    public static void revelar(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Integer andar = PopulacaoDoAndar.andarEm(run, jogador.getX(), jogador.getY(), jogador.getZ());
        DadosDaRun.InfoAndar info = andar == null ? null : run.andar(andar);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (info == null) {
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.mapeamento.fora").withStyle(ChatFormatting.AQUA), true);
            return;
        }
        List<BlockPos> lugares = new ArrayList<>();
        lugares.add(info.entrada());
        if (info.saida() != null) {
            lugares.add(info.saida());
            int dx = info.saida().getX() - jogador.getBlockX();
            int dz = info.saida().getZ() - jogador.getBlockZ();
            int quadradinhos = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / Andares.TAMANHO_DO_QUADRADINHO);
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.mapeamento", quadradinhos,
                    Component.translatable(direcao(dx, dz))).withStyle(ChatFormatting.AQUA), false);
        } else {
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.mapeamento.sem_saida").withStyle(ChatFormatting.AQUA), false);
        }
        List<BlockPos> portas = Busca.revelarTodas(jogador);
        int armadilhas = Busca.revelarArmadilhas(jogador);
        lugares.addAll(portas);
        if (portas.size() + armadilhas > 0) {
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.mapeamento.segredos", portas.size() + armadilhas)
                    .withStyle(ChatFormatting.AQUA), false);
        }
        ativas.add(new Marca(jogador.getUUID(), lugares, new int[] { DURACAO }));
    }

    // Norte é -Z no Minecraft, leste é +X
    private static String direcao(int dx, int dz) {
        double angulo = Math.toDegrees(Math.atan2(dx, -dz));
        String[] direcoes = { "norte", "nordeste", "leste", "sudeste", "sul", "sudoeste", "oeste", "noroeste" };
        int indice = (int) Math.round(((angulo % 360) + 360) % 360 / 45.0) % 8;
        return "direcao.pixeldungeon." + direcoes[indice];
    }

    public static void aoTickDoServidor(ServerTickEvent.Post evento) {
        Iterator<Marca> iterador = ativas.iterator();
        while (iterador.hasNext()) {
            Marca marca = iterador.next();
            marca.ticksRestantes()[0]--;
            ServerPlayer jogador = evento.getServer().getPlayerList().getPlayer(marca.jogador());
            if (jogador == null || marca.ticksRestantes()[0] <= 0) {
                iterador.remove();
                continue;
            }
            if (marca.ticksRestantes()[0] % 10 != 0) {
                continue;
            }
            for (BlockPos lugar : marca.lugares()) {
                for (int altura = 0; altura < ALTURA_DA_COLUNA; altura++) {
                    jogador.serverLevel().sendParticles(jogador, ParticleTypes.END_ROD, true, lugar.getX() + 0.5, lugar.getY() + altura,
                            lugar.getZ() + 0.5, 1, 0.1, 0.3, 0.1, 0.0);
                }
            }
        }
    }
}
