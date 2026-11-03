package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Baus;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.GnollExplorador;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.GotaDeOrvalho;
import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.entidade.MisselMagico;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.entidade.SlimeDeEsgoto;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.ChaveDeFerro;
import com.louzada.pixeldungeon.item.Odre;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
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
    // Teste das portas: um rato dormindo do lado de uma porta comum e outro do lado de uma trancada
    private static BlockPos portaDoTeste = null;
    private static BlockPos trancadaDoTeste = null;
    private static List<RatoMarsupial> ratosDasPortas = new ArrayList<>();
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
            testarPortas(servidor);
            testarClasses(servidor);
            testarSaque(servidor);
            TestesDosItens.testar(servidor);
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

    // Só no teste do GitHub: as portas do andar 1 começam fechadas; um rato encostado faz a comum abrir
    // (o resultado sai junto com o do Goo, 5 s depois)
    private static void testarPortas(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        PlantaDoAndar planta = GeradorDeAndar.gerar(run.semente(), 1);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), 1);
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.tipo(x, y) == PlantaDoAndar.PORTA && portaDoTeste == null) {
                    portaDoTeste = ConstrutorDeAndar.controleDaPorta(origem, planta, x, y);
                } else if (planta.tipo(x, y) == PlantaDoAndar.PORTA_TRANCADA && trancadaDoTeste == null) {
                    trancadaDoTeste = ConstrutorDeAndar.controleDaPorta(origem, planta, x, y);
                }
            }
        }
        if (portaDoTeste == null || trancadaDoTeste == null) {
            PixelDungeon.LOGGER.info("Teste das portas: o andar 1 não tem porta comum e trancada");
            return;
        }
        PixelDungeon.LOGGER.info("Teste das portas: comum fechada no começo = {}, trancada fechada no começo = {}", !portaAberta(mundo, portaDoTeste),
                !portaAberta(mundo, trancadaDoTeste) && mundo.getBlockState(trancadaDoTeste).getValue(PortaGigante.TRANCADA));
        for (BlockPos porta : List.of(portaDoTeste, trancadaDoTeste)) {
            mundo.setChunkForced(porta.getX() >> 4, porta.getZ() >> 4, true);
            RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
            if (rato != null) {
                // Dentro do quadradinho da porta, 2 blocos pro lado de passar
                boolean pelaX = mundo.getBlockState(porta).getValue(PortaGigante.EIXO) == Direction.Axis.X;
                rato.moveTo(porta.getX() + (pelaX ? 2.5 : 1.5), porta.getY(), porta.getZ() + (pelaX ? 1.5 : 2.5));
                mundo.addFreshEntity(rato);
                rato.dormir();
                ratosDasPortas.add(rato);
            }
        }
    }

    private static boolean portaAberta(ServerLevel mundo, BlockPos controle) {
        BlockState estado = mundo.getBlockState(controle);
        return estado.is(ModBlocos.PORTA_GIGANTE.get()) && estado.getValue(PortaGigante.ABERTA);
    }

    // Só no teste do GitHub: um jogador de mentira testa os itens das classes, os talentos e as habilidades
    private static void testarClasses(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);

        // Talentos: no nível 3 o herói tem 2 pontos, e cada talento vai até +2
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        dados.definirNivel(3);
        Talentos.aprimorar(heroi, Talento.REFEICAO_SAUDAVEL);
        Talentos.aprimorar(heroi, Talento.REFEICAO_SAUDAVEL);
        Talentos.aprimorar(heroi, Talento.REFEICAO_SAUDAVEL);
        Talentos.aprimorar(heroi, Talento.DETERMINACAO_DE_FERRO);
        PixelDungeon.LOGGER.info("Teste dos talentos: refeição saudável +{}, determinação de ferro +{}, pontos livres {}",
                dados.pontos(Talento.REFEICAO_SAUDAVEL), dados.pontos(Talento.DETERMINACAO_DE_FERRO), dados.pontosLivres());

        // Guerreiro: o selo na armadura de pano dá 5 de blindagem
        ItemStack armadura = new ItemStack(ModItens.ARMADURA_DE_PANO.get());
        armadura.set(ModComponentes.SELO.get(), true);
        heroi.setItemSlot(EquipmentSlot.CHEST, armadura);
        PixelDungeon.LOGGER.info("Teste do selo: tem selo = {}, blindagem do selo = {}", Escudos.temSelo(heroi), Escudos.escudoDoSelo(heroi));

        // Duelista: cortar com a espada curta num rato na frente dela
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        dados.definir(HabilidadesDaDuelista.CARGAS, 2);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItens.ESPADA_CURTA_DESGASTADA.get()));
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            rato.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() - 1.5);
            mundo.addFreshEntity(rato);
            heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getEyePosition());
            float antes = rato.getHealth();
            HabilidadesDaDuelista.usar(heroi);
            float depois = rato.isAlive() ? rato.getHealth() : 0;
            PixelDungeon.LOGGER.info("Teste da Duelista: cortar tirou {} de vida do rato, sobraram {} cargas", antes - depois,
                    dados.inteiro(HabilidadesDaDuelista.CARGAS));
            rato.discard();
        }

        // Clérigo: arma celestial gasta 2 das 3 cargas do tomo
        dados.comecar(ClasseDoHeroi.CLERIGO, run.numeroDaRun());
        heroi.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItens.MACA.get()));
        ItemStack tomo = new ItemStack(ModItens.TOMO_SAGRADO.get());
        heroi.setItemInHand(InteractionHand.OFF_HAND, tomo);
        MagiasDoClerigo.lancar(heroi, MagiasDoClerigo.Magia.ARMA_CELESTIAL);
        PixelDungeon.LOGGER.info("Teste do Clérigo: arma celestial = {}, cargas do tomo = {}", heroi.hasEffect(ModEfeitos.ARMA_CELESTIAL),
                Cargas.cargas(tomo, Artefato.cargasMaximas(tomo)));

        // Ladino: a capa deixa invisível e gasta 1 carga
        dados.comecar(ClasseDoHeroi.LADINO, run.numeroDaRun());
        ItemStack capa = new ItemStack(ModItens.CAPA_DAS_SOMBRAS.get());
        heroi.setItemInHand(InteractionHand.OFF_HAND, capa);
        ModItens.CAPA_DAS_SOMBRAS.get().use(mundo, heroi, InteractionHand.OFF_HAND);
        PixelDungeon.LOGGER.info("Teste do Ladino: invisível = {}, cargas da capa = {}", Furtividade.invisivel(heroi),
                Cargas.cargas(capa, Artefato.cargasMaximas(capa)));
        Furtividade.revelar(heroi);

        // Grama alta: o Guerreiro encosta num quadradinho de grama alta do andar 1 e ele vira grama baixa
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        PlantaDoAndar planta = GeradorDeAndar.gerar(run.semente(), 1);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), 1);
        BlockPos grama = null;
        for (int y = 0; y < planta.altura() && grama == null; y++) {
            for (int x = 0; x < planta.largura() && grama == null; x++) {
                BlockPos aqui = origem.offset(x * Andares.TAMANHO_DO_QUADRADINHO + 1, 1, y * Andares.TAMANHO_DO_QUADRADINHO + 1);
                // (onde caiu um item, a grama alta foi aparada)
                if (planta.grama(x, y) == PlantaDoAndar.GRAMA_ALTA && mundo.getBlockState(aqui).is(ModBlocos.CAPIM_ALTO.get())) {
                    grama = aqui;
                }
            }
        }
        if (grama != null) {
            boolean eraAlta = mundo.getBlockState(grama).is(ModBlocos.CAPIM_ALTO.get());
            if (eraAlta) {
                Grama.pisotear(mundo, grama, mundo.getBlockState(grama), heroi);
            }
            PixelDungeon.LOGGER.info("Teste da grama: era grama alta = {}, virou musgo = {}", eraAlta,
                    mundo.getBlockState(grama).is(ModBlocos.MUSGO_ESGOTO.get()));
        } else {
            PixelDungeon.LOGGER.info("Teste da grama: o andar 1 não tem grama alta");
        }

        // Odre: uma gota de orvalho entra nele, e beber cura
        ItemStack odre = new ItemStack(ModItens.ODRE.get());
        heroi.setItemInHand(InteractionHand.MAIN_HAND, odre);
        GotaDeOrvalho gota = ModEntidades.GOTA_DE_ORVALHO.get().create(mundo);
        if (gota != null) {
            gota.moveTo(heroi.getX(), heroi.getY() + 1, heroi.getZ());
            mundo.addFreshEntity(gota);
            gota.absorver(heroi);
        }
        int gotasDepoisDaGota = Odre.volume(odre);
        heroi.setHealth(15);
        float vidaAntes = heroi.getHealth();
        ModItens.ODRE.get().use(mundo, heroi, InteractionHand.MAIN_HAND);
        PixelDungeon.LOGGER.info("Teste do odre: gotas depois da gota = {}, vida {} -> {}, gotas depois de beber = {}", gotasDepoisDaGota,
                vidaAntes, heroi.getHealth(), Odre.volume(odre));
    }

    // Só no teste do GitHub: as armas novas, o ouro, os baús, o mímico e o que os inimigos deixam
    private static void testarSaque(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);

        // Armas: todas têm uma habilidade da Duelista
        int armas = 0;
        int comHabilidade = 0;
        int[] porTier = new int[6];
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaDoShattered arma && !(arma instanceof CajadoDoMago)) {
                armas++;
                porTier[arma.tier()]++;
                if (arma.habilidade() != null) {
                    comHabilidade++;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das armas: {} armas, {} com habilidade, por tier {}/{}/{}/{}/{}", armas, comHabilidade, porTier[1], porTier[2],
                porTier[3], porTier[4], porTier[5]);

        // Duelista com 18 de força e a espada larga: cortar (+7) mata um rato de uma vez
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        dados.definirForca(18);
        dados.definir(HabilidadesDaDuelista.CARGAS, 2);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItens.ESPADA_LARGA.get()));
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            rato.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() - 1.5);
            mundo.addFreshEntity(rato);
            heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getEyePosition());
            HabilidadesDaDuelista.usar(heroi);
            PixelDungeon.LOGGER.info("Teste da espada larga: o rato morreu = {}", !rato.isAlive());
            rato.discard();
        }

        // Ouro: pegar uma pilha do chão manda as moedas pra bolsa (e a pilha some)
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        ItemEntity pilha = SaqueDoAndar.soltar(mundo, heroi.getX(), heroi.getY(), heroi.getZ(), Ouro.pilha(50));
        Ouro.aoEncostar(new ItemEntityPickupEvent.Pre(heroi, pilha));
        ItemStack doAndar3 = Ouro.sortear(new Random(7), 3);
        PixelDungeon.LOGGER.info("Teste do ouro: ouro do herói = {}, a pilha sumiu = {}, pilha do andar 3 com moedas entre 60 e 120 = {}",
                Ouro.ouroDe(dados), pilha.isRemoved(), Ouro.quantidade(doAndar3) >= 60 && Ouro.quantidade(doAndar3) <= 120);

        // Baú comum: o item fica no meio dele
        BlockPos lugarDoBau = lugar.offset(2, 0, 2);
        Baus.colocar(mundo, lugarDoBau, Direction.NORTH, List.of(new ItemStack(ModItens.ESPADA.get())), false);
        boolean bauComEspada = mundo.getBlockEntity(lugarDoBau) instanceof ChestBlockEntity bau && bau.getItem(13).is(ModItens.ESPADA.get())
                && !Baus.ehMimico(bau);
        PixelDungeon.LOGGER.info("Teste do baú: tem a espada dentro = {}", bauComEspada);

        // Mímico: quem mexe no baú faz ele aparecer; quando morre, solta o que guardava
        BlockPos lugarDoMimico = lugar.offset(2, 0, -2);
        Baus.colocar(mundo, lugarDoMimico, Direction.NORTH, List.of(new ItemStack(ModItens.KATANA.get()), Ouro.pilha(30)), true);
        if (mundo.getBlockEntity(lugarDoMimico) instanceof ChestBlockEntity bauFalso) {
            boolean eraMimico = Baus.ehMimico(bauFalso);
            Mimico mimico = Mimico.revelar(mundo, lugarDoMimico, bauFalso, heroi);
            boolean bauSumiu = mundo.getBlockState(lugarDoMimico).isAir();
            if (mimico != null) {
                int guardados = mimico.itens().size();
                float vida = mimico.getMaxHealth();
                mimico.kill();
                List<ItemEntity> caidos = mundo.getEntitiesOfClass(ItemEntity.class, new AABB(lugarDoMimico).inflate(3),
                        item -> item.getItem().is(ModItens.KATANA.get()) || item.getItem().is(ModItens.OURO.get()));
                PixelDungeon.LOGGER.info("Teste do mímico: era mímico = {}, o baú sumiu = {}, vida {}, guardava {} itens, caíram {}", eraMimico,
                        bauSumiu, vida, guardados, caidos.size());
            }
        }

        // O que os inimigos deixam: o gnoll às vezes deixa ouro, o slime às vezes deixa uma arma de tier 2
        GnollExplorador gnoll = ModEntidades.GNOLL_EXPLORADOR.get().create(mundo);
        SlimeDeEsgoto slime = ModEntidades.SLIME.get().create(mundo);
        boolean gnollDeixouOuro = false;
        boolean slimeDeixouArma = false;
        for (int tentativa = 0; tentativa < 100 && gnoll != null && slime != null; tentativa++) {
            gnollDeixouOuro |= gnoll.sortearSaque().is(ModItens.OURO.get());
            slimeDeixouArma |= slime.sortearSaque().getItem() instanceof ArmaDoShattered arma && arma.tier() == 2;
        }
        PixelDungeon.LOGGER.info("Teste dos drops: gnoll deixou ouro = {}, slime deixou arma de tier 2 = {}", gnollDeixouOuro, slimeDeixouArma);
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
        if (portaDoTeste != null && trancadaDoTeste != null) {
            ServerLevel mundo = evento.getServer().overworld();
            PixelDungeon.LOGGER.info("Teste das portas: a comum abriu com o rato perto = {}, a trancada continuou fechada = {}",
                    portaAberta(mundo, portaDoTeste), !portaAberta(mundo, trancadaDoTeste));
            // Um herói com a chave de ferro do andar 1 destranca a trancada e gasta a chave
            FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
            DadosDaRun run = DadosDaRun.pegar(evento.getServer());
            heroi.getInventory().clearContent();
            heroi.getInventory().add(ChaveDeFerro.criar(run.numeroDaRun(), 1));
            boolean destrancou = ModBlocos.PORTA_GIGANTE.get().usarChave(mundo, trancadaDoTeste, heroi);
            PixelDungeon.LOGGER.info("Teste da chave de ferro: destrancou = {}, gastou a chave = {}, ficou comum = {}", destrancou,
                    ChaveDeFerro.procurar(heroi, run.numeroDaRun(), 1).isEmpty(), !mundo.getBlockState(trancadaDoTeste).getValue(PortaGigante.TRANCADA));
            for (RatoMarsupial rato : ratosDasPortas) {
                rato.discard();
            }
            ratosDasPortas.clear();
            for (BlockPos porta : List.of(portaDoTeste, trancadaDoTeste)) {
                mundo.setChunkForced(porta.getX() >> 4, porta.getZ() >> 4, false);
            }
            portaDoTeste = null;
            trancadaDoTeste = null;
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
