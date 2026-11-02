package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

// Coisas que acontecem sozinhas: servidor ligando, jogador entrando, renascendo e morrendo.
// Esses métodos são ligados ao jogo lá no PixelDungeon.java.
public class EventosDaRun {

    // Servidor ligou (também acontece no single player, ao abrir o mundo)
    public static void aoIniciarServidor(ServerStartedEvent evento) {
        MinecraftServer servidor = evento.getServer();
        configurarRegras(servidor);

        // Só no teste do GitHub: começa uma run e constrói todos os andares de uma vez pra ver se algum dá erro
        if (Boolean.getBoolean("pixeldungeon.testarAndares")) {
            if (!DadosDaRun.pegar(servidor).ativa()) {
                GerenciadorDaRun.novaRun(servidor);
            }
            for (int andar = 1; andar <= Andares.ULTIMO_ANDAR_LIBERADO; andar++) {
                GerenciadorDaRun.garantirAndar(servidor, andar);
            }
            testarCombate(servidor);
        }
    }

    // Só no teste do GitHub: dois ratos brigam na entrada do andar 1, pra ver se o combate funciona sem erro
    private static void testarCombate(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        BlockPos lugar = DadosDaRun.pegar(servidor).andar(1).entrada();
        RatoMarsupial atacante = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        RatoMarsupial alvo = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (atacante == null || alvo == null) {
            return;
        }
        atacante.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5);
        alvo.moveTo(lugar.getX() + 1.5, lugar.getY(), lugar.getZ() + 0.5);
        mundo.addFreshEntity(atacante);
        mundo.addFreshEntity(alvo);
        for (int golpe = 0; golpe < 20 && alvo.getHealth() >= alvo.getMaxHealth(); golpe++) {
            alvo.invulnerableTime = 0;
            alvo.hurt(mundo.damageSources().mobAttack(atacante), 1.0F);
        }
        PixelDungeon.LOGGER.info("Teste de combate: o rato ficou com {}/{} de vida", alvo.getHealth(), alvo.getMaxHealth());
    }

    // Alguém entrou no mundo: continua na run ou vai pro menu de escolha de herói
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            GerenciadorDaRun.aoEntrarNoMundo(jogador);
        }
    }

    // Renascer pelo jeito normal do Minecraft só acontece com quem não estava na run: vai pro menu
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            GerenciadorDaRun.aoEntrarNoMundo(jogador);
        }
    }

    // Jogador da run morreu: cancela a morte do Minecraft e vira espectador
    public static void aoMorrer(LivingDeathEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador && GerenciadorDaRun.estaVivoNaRun(jogador)) {
            evento.setCanceled(true);
            GerenciadorDaRun.aoMorrer(jogador);
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
