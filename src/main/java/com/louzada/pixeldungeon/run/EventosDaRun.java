package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.dungeon.Andares;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

// Coisas que acontecem sozinhas: quando o servidor liga, quando alguém entra e quando alguém renasce.
// Esses métodos são ligados ao jogo lá no PixelDungeon.java.
public class EventosDaRun {

    // Servidor ligou (também acontece no single player, ao abrir o mundo)
    public static void aoIniciarServidor(ServerStartedEvent evento) {
        MinecraftServer servidor = evento.getServer();
        configurarRegras(servidor);

        // Mundo novo: começa a primeira run.
        // (Depois isso vai sair daqui: a run vai começar quando alguém escolher a classe no menu.)
        if (DadosDaRun.pegar(servidor).numeroDaRun() == 0) {
            GerenciadorDaRun.novaRun(servidor);
        }

        // Só no teste do GitHub: constrói todos os andares de uma vez pra ver se algum dá erro
        if (Boolean.getBoolean("pixeldungeon.testarAndares")) {
            for (int andar = 1; andar <= Andares.ULTIMO_ANDAR_LIBERADO; andar++) {
                GerenciadorDaRun.garantirAndar(servidor, andar);
            }
        }
    }

    // Alguém entrou no mundo: se ainda não está na run atual, vai pro andar 1
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            DadosDaRun dados = DadosDaRun.pegar(jogador.serverLevel().getServer());
            if (!dados.estaNaRunAtual(jogador.getUUID())) {
                GerenciadorDaRun.colocarNaRun(jogador);
            }
        }
    }

    // Alguém renasceu: volta pro andar 1.
    // (Depois isso muda: quem morrer vira espectador até alguém usar um ankh.)
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            GerenciadorDaRun.levarPara(jogador, 1, true);
        }
    }

    // Regras de jogo da dungeon: sem dia/noite, sem chuva e sem mobs nascendo pelo jeito do Minecraft
    private static void configurarRegras(MinecraftServer servidor) {
        GameRules regras = servidor.getGameRules();
        regras.getRule(GameRules.RULE_DAYLIGHT).set(false, servidor);
        regras.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, servidor);
        regras.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, servidor);
        regras.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, servidor);
        regras.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, servidor);
        regras.getRule(GameRules.RULE_DOINSOMNIA).set(false, servidor);
    }
}
