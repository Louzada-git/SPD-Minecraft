package com.louzada.pixeldungeon.run;

import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.entidade.FantasmaAliado;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.rede.PacoteAbrirMenu;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

// Controla a run:
//   - a run começa quando o primeiro jogador escolhe a classe no menu
//   - quem escolhe depois entra no andar 1 da run que já está rolando
//   - quem morre vira espectador até alguém usar um ankh
//   - quando todos morrem, a run acaba e todo mundo volta pro menu
//   - nos andares de chefe, a saída só abre quando o chefe morre, e ninguém sobe a escada no meio da luta
public class GerenciadorDaRun {
    // Onde fica quem está no menu antes de qualquer run começar (no vazio, como espectador)
    private static final BlockPos LUGAR_DE_ESPERA = new BlockPos(0, 200, 0);

    // ---- Começo e fim ----

    // Começa uma run do zero: semente nova e andar 1 construído
    public static void novaRun(MinecraftServer servidor) {
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        dados.comecarNovaRun(new Random().nextLong());
        PixelDungeon.LOGGER.info("Começando a run {}", dados.numeroDaRun());

        DadosDaRun.InfoAndar primeiroAndar = garantirAndar(servidor, 1);
        servidor.overworld().setDefaultSpawnPos(primeiroAndar.entrada(), 0.0F);
    }

    // Todos morreram (ou um comando mandou): acaba a run e todo mundo volta pro menu
    public static void fimDaRun(MinecraftServer servidor) {
        DadosDaRun.pegar(servidor).terminarRun();
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            esperarNoMenu(jogador);
        }
    }

    // ---- Menu e classe ----

    // Deixa o jogador como espectador e abre o menu de escolha de herói
    public static void esperarNoMenu(ServerPlayer jogador) {
        MinecraftServer servidor = jogador.serverLevel().getServer();
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        jogador.setGameMode(GameType.SPECTATOR);
        // Fora da run, não sabe o que é nenhuma poção nem pergaminho
        Identificacao.sincronizar(jogador);

        BlockPos lugar = LUGAR_DE_ESPERA;
        DadosDaRun.InfoAndar primeiroAndar = dados.ativa() ? dados.andar(1) : null;
        if (primeiroAndar != null) {
            // Com a run rolando, espera olhando a entrada do andar 1
            lugar = primeiroAndar.entrada().above(2);
        }
        teleportar(jogador, lugar);

        // Só no teste do GitHub que tira fotos da tela: entra direto, sem o menu
        if (HeroiDasFotos.ligado()) {
            HeroiDasFotos.preparar(jogador);
            return;
        }
        PacketDistributor.sendToPlayer(jogador, new PacoteAbrirMenu(dados.ativa()));
    }

    // O jogador escolheu a classe no menu
    public static void escolherClasse(ServerPlayer jogador, ClasseDoHeroi classe) {
        MinecraftServer servidor = jogador.serverLevel().getServer();
        if (estaNaRun(jogador)) {
            // Já está jogando (ou morto esperando um ankh): não pode trocar de classe no meio
            return;
        }

        DadosDaRun dados = DadosDaRun.pegar(servidor);
        if (!dados.ativa()) {
            // Primeiro a escolher: começa a run
            novaRun(servidor);
        }

        DadosDoHeroi heroi = RegrasDoHeroi.dados(jogador);
        heroi.comecar(classe, dados.numeroDaRun());
        // Igual ao Shattered: a Duelista começa com 2 cargas de habilidade
        heroi.definir(HabilidadesDaDuelista.CARGAS, 2);

        // Inventário novo com o kit da classe (armadura vestida)
        jogador.getInventory().clearContent();
        for (ItemStack item : classe.kitInicial()) {
            if (item.getItem() instanceof ArmorItem armadura) {
                jogador.setItemSlot(armadura.getEquipmentSlot(), item);
            } else {
                jogador.getInventory().add(item);
            }
        }

        jogador.setGameMode(GameType.SURVIVAL);
        RegrasDoHeroi.aplicar(jogador);
        jogador.setHealth(jogador.getMaxHealth());
        jogador.getFoodData().setFoodLevel(20);
        levarPara(jogador, 1, true);
        RedeDoHeroi.enviarEstado(jogador, true);
        Identificacao.sincronizar(jogador);

        servidor.getPlayerList().broadcastSystemMessage(Component.translatable("mensagem.pixeldungeon.entrou",
                jogador.getDisplayName(), Component.translatable(classe.chaveDoNome())), false);
    }

    // Tem classe na run atual (vivo ou morto)?
    public static boolean estaNaRun(ServerPlayer jogador) {
        DadosDaRun dados = DadosDaRun.pegar(jogador.serverLevel().getServer());
        DadosDoHeroi heroi = RegrasDoHeroi.dados(jogador);
        return dados.ativa() && heroi.run() == dados.numeroDaRun() && heroi.classe() != null;
    }

    public static boolean estaVivoNaRun(ServerPlayer jogador) {
        return estaNaRun(jogador) && !RegrasDoHeroi.dados(jogador).morto();
    }

    // Alguém entrou no mundo: continua de onde parou, ou vai pro menu
    public static void aoEntrarNoMundo(ServerPlayer jogador) {
        if (!estaNaRun(jogador)) {
            esperarNoMenu(jogador);
            return;
        }
        if (RegrasDoHeroi.dados(jogador).morto()) {
            jogador.setGameMode(GameType.SPECTATOR);
        }
        RegrasDoHeroi.aplicar(jogador);
        RedeDoHeroi.enviarEstado(jogador, true);
        Identificacao.sincronizar(jogador);
    }

    // ---- Morte e ankh ----

    // Chamado quando um jogador da run morre (a morte do Minecraft é cancelada antes)
    public static void aoMorrer(ServerPlayer jogador) {
        MinecraftServer servidor = jogador.serverLevel().getServer();
        RegrasDoHeroi.dados(jogador).definirMorto(true);
        jogador.setHealth(jogador.getMaxHealth());
        jogador.setGameMode(GameType.SPECTATOR);

        servidor.getPlayerList().broadcastSystemMessage(Component.translatable("mensagem.pixeldungeon.morreu", jogador.getDisplayName()), false);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.espere_ankh"), false);

        if (ninguemVivo(servidor)) {
            servidor.getPlayerList().broadcastSystemMessage(Component.translatable("mensagem.pixeldungeon.fim_da_run"), false);
            fimDaRun(servidor);
        }
    }

    private static boolean ninguemVivo(MinecraftServer servidor) {
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            if (estaVivoNaRun(jogador)) {
                return false;
            }
        }
        return true;
    }

    // Usado pelo ankh: traz de volta um aliado morto do lado de quem usou. Devolve quem voltou (ou null).
    @Nullable
    public static ServerPlayer reviverAliado(ServerPlayer quemUsou) {
        MinecraftServer servidor = quemUsou.serverLevel().getServer();
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            if (jogador != quemUsou && estaNaRun(jogador) && RegrasDoHeroi.dados(jogador).morto()) {
                RegrasDoHeroi.dados(jogador).definirMorto(false);
                jogador.setGameMode(GameType.SURVIVAL);
                jogador.teleportTo(quemUsou.serverLevel(), quemUsou.getX(), quemUsou.getY(), quemUsou.getZ(),
                        Set.of(), quemUsou.getYRot(), quemUsou.getXRot());
                RegrasDoHeroi.aplicar(jogador);
                jogador.setHealth(jogador.getMaxHealth());
                servidor.getPlayerList().broadcastSystemMessage(Component.translatable("mensagem.pixeldungeon.reviveu",
                        jogador.getDisplayName(), quemUsou.getDisplayName()), false);
                return jogador;
            }
        }
        return null;
    }

    // ---- Andares ----

    // Devolve as informações do andar, construindo ele primeiro se ninguém chegou lá ainda
    public static DadosDaRun.InfoAndar garantirAndar(MinecraftServer servidor, int andar) {
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        DadosDaRun.InfoAndar info = dados.andar(andar);
        if (info != null) {
            return info;
        }

        // Os acessórios de agora decidem o tema do andar (ver TemaDoAndar) e mexem nos itens dele
        Acessorios.atualizar(servidor);
        TemaDoAndar.decidir(dados, andar);
        PopulacaoDoAndar.esquecerPlanta(dados.semente(), andar);
        PlantaDoAndar planta = GeradorDeAndar.gerar(dados.semente(), andar);
        BlockPos origem = Andares.origemDoAndar(dados.numeroDaRun(), andar);
        // Os itens do andar são sorteados antes de construir, porque onde tem item a grama alta é aparada
        List<SaqueDoAndar.Saque> saque = SaqueDoAndar.sortear(planta, andar, dados);
        long inicio = System.currentTimeMillis();
        ConstrutorDeAndar.construir(servidor.overworld(), planta, origem, dados.semente() * 31 + andar);
        PixelDungeon.LOGGER.info("Andar {} construído em {} ms", andar, System.currentTimeMillis() - inicio);
        contarGrama(planta, andar);
        PopulacaoDoAndar.povoarNoInicio(servidor.overworld(), planta, origem, andar, dados.semente());
        SaqueDoAndar.colocar(servidor.overworld(), origem, andar, saque);
        // Num dos andares 2 a 4, o fantasma triste espera na sala da saída
        MissaoDoFantasma.talvezCriar(servidor.overworld(), planta, origem, andar, dados);

        BlockPos entrada = ConstrutorDeAndar.pontoDeChegada(origem, planta.entradaX(), planta.entradaY());
        BlockPos saida = null;
        if (planta.temSaida()) {
            saida = ConstrutorDeAndar.pontoDeChegada(origem, planta.saidaX(), planta.saidaY());
        }
        info = new DadosDaRun.InfoAndar(entrada, saida);
        dados.guardarAndar(andar, info);
        return info;
    }

    // Só pra conferir no log: o tamanho do andar, as salas, as portas e quantos quadradinhos de grama ele ganhou
    private static void contarGrama(PlantaDoAndar planta, int andar) {
        int grama = 0;
        int alta = 0;
        int portas = 0;
        int trancadas = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.grama(x, y) != PlantaDoAndar.SEM_GRAMA) {
                    grama++;
                }
                if (planta.grama(x, y) == PlantaDoAndar.GRAMA_ALTA) {
                    alta++;
                }
                if (planta.tipo(x, y) == PlantaDoAndar.PORTA) {
                    portas++;
                } else if (planta.tipo(x, y) == PlantaDoAndar.PORTA_TRANCADA) {
                    trancadas++;
                }
            }
        }
        PixelDungeon.LOGGER.info("Andar {}: {}x{} quadradinhos, {} salas, {} portas, {} trancadas, {} chaves de ferro", andar, planta.largura(),
                planta.altura(), planta.salas().size(), portas, trancadas, planta.chavesDeFerro());
        PixelDungeon.LOGGER.info("Andar {}: {} quadradinhos de grama ({} de grama alta)", andar, grama, alta);
    }

    // Leva o jogador para um andar. chegandoDeCima = aparece na entrada; senão aparece na saída.
    public static void levarPara(ServerPlayer jogador, int andar, boolean chegandoDeCima) {
        MinecraftServer servidor = jogador.serverLevel().getServer();
        DadosDaRun.InfoAndar info = garantirAndar(servidor, andar);

        BlockPos destino = info.entrada();
        if (!chegandoDeCima && info.saida() != null) {
            destino = info.saida();
        }
        teleportar(jogador, destino);
        // O fantasma da rosa seca vai junto
        FantasmaAliado.acompanhar(jogador);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.andar", andar,
                Component.translatable(Andares.regiaoDoAndar(andar))), true);
        // Primeira vez nesse andar: talentos que acontecem ao explorar (rações guardadas do Ladino)
        Talentos.aoChegarNoAndar(jogador, andar);
        // Previsão do ladino: avisa se o andar tem uma sala secreta
        Busca.aoChegarNoAndar(jogador, andar);
    }

    private static void teleportar(ServerPlayer jogador, BlockPos destino) {
        ServerLevel mundo = jogador.serverLevel().getServer().overworld();
        jogador.teleportTo(mundo, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(), jogador.getYRot(), jogador.getXRot());
        jogador.resetFallDistance();
    }

    // Chamado pelo alçapão de descida
    public static void descer(ServerPlayer jogador, int andarAtual) {
        DadosDaRun dados = DadosDaRun.pegar(jogador.serverLevel().getServer());
        if (Andares.ehAndarDeChefe(andarAtual) && !dados.chefeDerrotado(andarAtual)) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.saida_trancada"), true);
            return;
        }
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
        if (Andares.ehAndarDeChefe(andarAtual) && chefeLutando(jogador.serverLevel(), andarAtual)) {
            // Igual ao Shattered: o andar fica selado enquanto o chefe luta
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selado"), true);
            return;
        }
        levarPara(jogador, andarAtual - 1, false);
    }

    public static boolean chefeLutando(ServerLevel mundo, int andar) {
        BlockPos origem = Andares.origemDoAndar(DadosDaRun.pegar(mundo.getServer()).numeroDaRun(), andar);
        return !mundo.getEntitiesOfClass(Goo.class, PopulacaoDoAndar.caixaDoAndar(origem), Goo::lutando).isEmpty();
    }

    // Chamado quando um chefe morre: a saída do andar abre e todo mundo fica sabendo
    public static void aoDerrotarChefe(ServerLevel mundo, Entity chefe) {
        MinecraftServer servidor = mundo.getServer();
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        Integer andar = PopulacaoDoAndar.andarEm(dados, chefe.getX(), chefe.getY(), chefe.getZ());
        if (!dados.ativa() || andar == null || !Andares.ehAndarDeChefe(andar) || dados.chefeDerrotado(andar)) {
            return;
        }
        dados.marcarChefeDerrotado(andar);
        PixelDungeon.LOGGER.info("Chefe do andar {} derrotado", andar);

        servidor.getPlayerList().broadcastSystemMessage(Component.translatable("mensagem.pixeldungeon.chefe_derrotado",
                chefe.getType().getDescription()), false);
        // Quem estava no andar vê "Chefe derrotado!" no meio da tela, igual ao Shattered
        BlockPos origem = Andares.origemDoAndar(dados.numeroDaRun(), andar);
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, PopulacaoDoAndar.caixaDoAndar(origem))) {
            jogador.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("tela.pixeldungeon.chefe_derrotado")));
        }
    }
}
