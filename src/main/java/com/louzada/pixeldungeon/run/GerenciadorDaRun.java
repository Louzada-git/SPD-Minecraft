package com.louzada.pixeldungeon.run;

import java.util.Random;
import java.util.Set;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// Controla a run: começar uma nova, construir andares quando alguém chega neles
// e levar os jogadores de um andar para outro.
public class GerenciadorDaRun {

    // Começa uma run do zero: semente nova, andar 1 construído e todo mundo que está online vai pra lá
    public static void novaRun(MinecraftServer servidor) {
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        dados.comecarNovaRun(new Random().nextLong());
        PixelDungeon.LOGGER.info("Começando a run {}", dados.numeroDaRun());

        DadosDaRun.InfoAndar primeiroAndar = garantirAndar(servidor, 1);
        // Se alguém renascer pelo jeito normal do Minecraft, pelo menos aparece perto do andar 1
        servidor.overworld().setDefaultSpawnPos(primeiroAndar.entrada(), 0.0F);

        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            colocarNaRun(jogador);
        }
    }

    // Devolve as informações do andar, construindo ele primeiro se ninguém chegou lá ainda
    public static DadosDaRun.InfoAndar garantirAndar(MinecraftServer servidor, int andar) {
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        DadosDaRun.InfoAndar info = dados.andar(andar);
        if (info != null) {
            return info;
        }

        PlantaDoAndar planta = GeradorDeAndar.gerar(dados.semente(), andar);
        BlockPos origem = Andares.origemDoAndar(dados.numeroDaRun(), andar);
        ConstrutorDeAndar.construir(servidor.overworld(), planta, origem);

        BlockPos entrada = ConstrutorDeAndar.pontoDeChegada(origem, planta.entradaX(), planta.entradaY());
        BlockPos saida = null;
        if (planta.temSaida()) {
            saida = ConstrutorDeAndar.pontoDeChegada(origem, planta.saidaX(), planta.saidaY());
        }
        info = new DadosDaRun.InfoAndar(entrada, saida);
        dados.guardarAndar(andar, info);
        return info;
    }

    // Coloca o jogador na run atual, começando do andar 1
    public static void colocarNaRun(ServerPlayer jogador) {
        DadosDaRun.pegar(jogador.serverLevel().getServer()).registrarJogador(jogador.getUUID());
        levarPara(jogador, 1, true);
    }

    // Leva o jogador para um andar. chegandoDeCima = aparece na entrada; senão aparece na saída.
    public static void levarPara(ServerPlayer jogador, int andar, boolean chegandoDeCima) {
        MinecraftServer servidor = jogador.serverLevel().getServer();
        ServerLevel mundo = servidor.overworld();
        DadosDaRun.InfoAndar info = garantirAndar(servidor, andar);

        BlockPos destino = info.entrada();
        if (!chegandoDeCima && info.saida() != null) {
            destino = info.saida();
        }

        jogador.teleportTo(mundo, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(), jogador.getYRot(), jogador.getXRot());
        jogador.resetFallDistance();
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.andar", andar,
                Component.translatable(Andares.regiaoDoAndar(andar))), true);
    }

    // Chamado pelo alçapão de descida
    public static void descer(ServerPlayer jogador, int andarAtual) {
        if (andarAtual >= Andares.ULTIMO_ANDAR_LIBERADO) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bloqueado"), true);
            return;
        }
        levarPara(jogador, andarAtual + 1, true);
    }

    // Chamado pela escada de subida
    public static void subir(ServerPlayer jogador, int andarAtual) {
        if (andarAtual <= 1) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.sem_amuleto"), true);
            return;
        }
        levarPara(jogador, andarAtual - 1, false);
    }
}
