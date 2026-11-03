package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.MisselMagico;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.registro.ModEntidades;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Coisas que acontecem sozinhas: servidor ligando, jogador entrando, renascendo e morrendo.
// Esses métodos são ligados ao jogo lá no PixelDungeon.java.
public class EventosDaRun {
    // Teste do Goo (só no GitHub): espera 5 s e vê se ele se curou na água
    private static Goo gooDoTeste = null;
    private static int vidaDoGooNoTeste = 0;
    private static int ticksParaOTesteDoGoo = 0;
    // Teste do míssil mágico: o rato do teste de combate leva um míssil
    private static RatoMarsupial ratoDoMissil = null;
    private static float vidaDoRatoAntesDoMissil = 0;

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

        // O enxame tem que se dividir quando apanha
        EnxameDeMoscas enxame = ModEntidades.ENXAME_DE_MOSCAS.get().create(mundo);
        if (enxame == null) {
            return;
        }
        enxame.moveTo(lugar.getX() + 0.5, lugar.getY() + 1, lugar.getZ() + 1.5);
        mundo.addFreshEntity(enxame);
        for (int golpe = 0; golpe < 20 && enxame.getHealth() >= enxame.getMaxHealth(); golpe++) {
            enxame.invulnerableTime = 0;
            enxame.hurt(mundo.damageSources().mobAttack(atacante), 1.0F);
        }
        int enxames = mundo.getEntitiesOfClass(EnxameDeMoscas.class, enxame.getBoundingBox().inflate(4)).size();
        PixelDungeon.LOGGER.info("Teste do enxame: {} enxames depois do golpe", enxames);

        // Míssil mágico lançado pelo outro rato, voando reto até o rato do teste
        mundo.setChunkForced(alvo.chunkPosition().x, alvo.chunkPosition().z, true);
        MisselMagico missil = new MisselMagico(mundo, atacante, 0);
        missil.setPos(alvo.getX() - 2.5, alvo.getY() + 0.3, alvo.getZ());
        missil.shoot(1.0, 0.0, 0.0, 1.0F, 0.0F);
        mundo.addFreshEntity(missil);
        ratoDoMissil = alvo;
        vidaDoRatoAntesDoMissil = alvo.getHealth();

        // O Goo tem que estar dormindo no ninho (na água) e se curar lá
        BlockPos origem = Andares.origemDoAndar(DadosDaRun.pegar(servidor).numeroDaRun(), 5);
        List<Goo> goos = mundo.getEntitiesOfClass(Goo.class, PopulacaoDoAndar.caixaDoAndar(origem));
        if (goos.isEmpty()) {
            PixelDungeon.LOGGER.info("Teste do Goo: nenhum Goo no andar 5");
            return;
        }
        gooDoTeste = goos.get(0);
        // Mantém o pedaço do mundo dele carregado durante o teste, senão ele não se mexe
        mundo.setChunkForced(gooDoTeste.chunkPosition().x, gooDoTeste.chunkPosition().z, true);
        gooDoTeste.setHealth(gooDoTeste.getMaxHealth() - 20);
        vidaDoGooNoTeste = Math.round(gooDoTeste.getHealth());
        ticksParaOTesteDoGoo = 5 * 20;
    }

    // Só no teste do GitHub: termina o teste do Goo depois de 5 s
    public static void aoTickDoServidor(ServerTickEvent.Post evento) {
        if (gooDoTeste == null || --ticksParaOTesteDoGoo > 0) {
            return;
        }
        if (ratoDoMissil != null) {
            boolean acertou = !ratoDoMissil.isAlive() || ratoDoMissil.getHealth() < vidaDoRatoAntesDoMissil;
            PixelDungeon.LOGGER.info("Teste do míssil mágico: acertou o rato = {} (vida {} -> {})", acertou, vidaDoRatoAntesDoMissil,
                    ratoDoMissil.isAlive() ? ratoDoMissil.getHealth() : 0);
            evento.getServer().overworld().setChunkForced(ratoDoMissil.chunkPosition().x, ratoDoMissil.chunkPosition().z, false);
            ratoDoMissil = null;
        }
        PixelDungeon.LOGGER.info("Teste do Goo: tinha {} de vida, depois de 5 s na água ficou com {} (na água: {})",
                vidaDoGooNoTeste, Math.round(gooDoTeste.getHealth()), gooDoTeste.isInWater());
        evento.getServer().overworld().setChunkForced(gooDoTeste.chunkPosition().x, gooDoTeste.chunkPosition().z, false);
        gooDoTeste = null;
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
