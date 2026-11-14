package com.louzada.pixeldungeon.run;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.ArmadilhaBloco;
import com.louzada.pixeldungeon.bloco.Tumulo;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar.Tema;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.entidade.Espectro;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

// Só no teste do GitHub: os temas dos andares (abismal, inundado, gramado, escuro, extenso, com armadilhas e com segredos),
// as salas comuns e as ligações novas do esgoto, as armadilhas de fogo e explosiva, o túmulo e o espectro.
// O log mostra o resultado (o build.yml confere).
public class TestesDosTemas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    // Sementes de mentira pros andares com tema forçado (longe das sementes de verdade)
    private static final long FINGIDA = 7_300_000_000L;

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        PixelDungeon.LOGGER.info(testarSorteio());
        PixelDungeon.LOGGER.info(testarPlantas());
        PixelDungeon.LOGGER.info(testarSaqueDoExtenso());
        PixelDungeon.LOGGER.info(testarSalas());
        testarNoMundo(mundo, run);
    }

    // ---- O sorteio do tema ----

    public static String testarSorteio() {
        int[] contagem = new int[Tema.values().length];
        boolean mecanismoArmadilhas = false;
        boolean mecanismoAbismo = false;
        boolean musgo = true;
        boolean igualSemAcessorio = true;
        for (long semente = 1; semente <= 400; semente++) {
            Tema daSemente = TemaDoAndar.daSemente(semente, 3);
            contagem[daSemente.ordinal()]++;
            Tema comMecanismo = TemaDoAndar.sortear(semente, 3, -1, 3).tema();
            Tema comMusgo = TemaDoAndar.sortear(semente, 3, 3, -1).tema();
            if (daSemente == Tema.NENHUM) {
                mecanismoArmadilhas |= comMecanismo == Tema.ARMADILHAS;
                mecanismoAbismo |= comMecanismo == Tema.ABISMOS;
                musgo &= comMusgo == Tema.AGUA || comMusgo == Tema.GRAMA;
            } else {
                igualSemAcessorio &= comMecanismo == daSemente && comMusgo == daSemente;
            }
        }
        boolean todos = true;
        for (Tema tema : Tema.values()) {
            if (tema != Tema.NENHUM) {
                todos &= contagem[tema.ordinal()] > 0;
            }
        }
        boolean metade = contagem[Tema.NENHUM.ordinal()] >= 150 && contagem[Tema.NENHUM.ordinal()] <= 250;
        boolean semTema = TemaDoAndar.daSemente(5, 1) == Tema.NENHUM && TemaDoAndar.daSemente(5, 5) == Tema.NENHUM;
        return "Teste do sorteio dos temas: os 7 aparecem = " + todos + ", metade sem tema = " + metade + " (" + contagem[Tema.NENHUM.ordinal()]
                + " de 400), andar 1 e chefe sem tema = " + semTema + ", o mecanismo dá armadilhas e abismo = " + (mecanismoArmadilhas && mecanismoAbismo)
                + ", o musgo dá água ou grama = " + musgo + ", os acessórios não mudam os temas da semente = " + igualSemAcessorio;
    }

    // ---- As plantas de cada tema ----

    public static String testarPlantas() {
        // Abismal: fora das salas é abismo, com a parede na beirada, e os túneis são de chão especial
        boolean abismal = true;
        for (int i = 0; i < 10; i++) {
            PlantaDoAndar planta = comTema(FINGIDA + i, 3, Tema.ABISMOS);
            int abismo = 0;
            int corredorEspecial = 0;
            int corredorComum = 0;
            for (int y = 0; y < planta.altura(); y++) {
                for (int x = 0; x < planta.largura(); x++) {
                    byte tipo = planta.tipo(x, y);
                    boolean beirada = x == 0 || y == 0 || x == planta.largura() - 1 || y == planta.altura() - 1;
                    if (beirada && tipo != PlantaDoAndar.PAREDE) {
                        abismal = false;
                    }
                    if (tipo == PlantaDoAndar.ABISMO) {
                        abismo++;
                    }
                    if (planta.ehCorredor(x, y) && tipo == PlantaDoAndar.CHAO_ESPECIAL) {
                        corredorEspecial++;
                    }
                    if (planta.ehCorredor(x, y) && tipo == PlantaDoAndar.CHAO) {
                        corredorComum++;
                    }
                }
            }
            // (a ligação entre duas salas dos canos também abre chão comum na parede, igual ao Shattered)
            abismal &= abismo > planta.largura() * planta.altura() / 5 && corredorEspecial > corredorComum;
        }

        // Extenso: mais salas que o normal, 3 salas especiais e mais inimigos
        int salasNormais = 0;
        int salasGrandes = 0;
        for (int i = 0; i < 10; i++) {
            salasNormais += comTema(FINGIDA + 100 + i, 3, Tema.NENHUM).salas().size();
            salasGrandes += comTema(FINGIDA + 100 + i, 3, Tema.GRANDE).salas().size();
        }
        long grande = sementeCom(Tema.GRANDE, 3);
        int base = 3 + 3 % 5 + new Random(grande * 7919 + 3).nextInt(3);
        boolean extenso = salasGrandes > salasNormais && ComodoEspecial.doAndar(grande, 3).size() >= 3
                && PopulacaoDoAndar.limite(grande, 3) == (int) Math.ceil(base * 1.33f);

        // Com segredos: uma sala secreta a mais e bem mais portas escondidas
        long segredos = sementeCom(Tema.SEGREDOS, 3);
        int escondidasNormais = 0;
        int escondidasComSegredos = 0;
        for (int i = 0; i < 20; i++) {
            escondidasNormais += contar(comTema(FINGIDA + 300 + i, 3, Tema.NENHUM), PlantaDoAndar.PORTA_ESCONDIDA);
            escondidasComSegredos += contar(comTema(FINGIDA + 300 + i, 3, Tema.SEGREDOS), PlantaDoAndar.PORTA_ESCONDIDA);
        }
        boolean comSegredos = !ComodoSecreto.secretasDoAndar(segredos, 3).isEmpty() && escondidasComSegredos > escondidasNormais;

        // Inundado, gramado e com armadilhas: mais água, grama e armadilhas que o normal
        int agua = 0;
        int aguaDoTema = 0;
        int grama = 0;
        int gramaDoTema = 0;
        int armadilhas = 0;
        int armadilhasDoTema = 0;
        for (int i = 0; i < 5; i++) {
            PlantaDoAndar normal = comTema(FINGIDA + 400 + i, 3, Tema.NENHUM);
            agua += contar(normal, PlantaDoAndar.AGUA);
            grama += contarGrama(normal);
            armadilhas += normal.quantasArmadilhas();
            aguaDoTema += contar(comTema(FINGIDA + 400 + i, 3, Tema.AGUA), PlantaDoAndar.AGUA);
            gramaDoTema += contarGrama(comTema(FINGIDA + 400 + i, 3, Tema.GRAMA));
            armadilhasDoTema += comTema(FINGIDA + 400 + i, 3, Tema.ARMADILHAS).quantasArmadilhas();
        }
        boolean outros = aguaDoTema > agua && gramaDoTema > grama && armadilhasDoTema > armadilhas;

        return "Teste das plantas dos temas: abismal = " + abismal + ", extenso = " + extenso + " (" + salasNormais + " -> " + salasGrandes
                + " salas), com segredos = " + comSegredos + " (" + escondidasNormais + " -> " + escondidasComSegredos
                + " portas escondidas), água, grama e armadilhas = " + outros;
    }

    // O andar extenso tem 2 itens e uma comida a mais
    private static String testarSaqueDoExtenso() {
        DadosDaRun semTema = new DadosDaRun();
        semTema.comecarNovaRun(FINGIDA + 200);
        DadosDaRun comTema = new DadosDaRun();
        comTema.comecarNovaRun(FINGIDA + 200);
        List<SaqueDoAndar.Saque> itensSemTema = SaqueDoAndar.sortear(comTema(FINGIDA + 200, 2, Tema.NENHUM), 2, semTema);
        List<SaqueDoAndar.Saque> itensComTema = SaqueDoAndar.sortear(comTema(FINGIDA + 200, 2, Tema.GRANDE), 2, comTema);
        boolean certo = comidas(itensComTema) >= 2 && itensComTema.size() >= itensSemTema.size() + 2;
        return "Teste do saque do andar extenso: 2 itens e uma comida a mais = " + certo + " (" + itensSemTema.size() + " -> " + itensComTema.size()
                + " itens, " + comidas(itensComTema) + " comidas)";
    }

    // ---- As salas comuns e as ligações novas (procurando nos andares de várias sementes) ----

    public static String testarSalas() {
        boolean aquario = false;
        boolean piranhasNaAgua = true;
        boolean estudo = false;
        boolean bauSuspeito = false;
        boolean cemiterio = false;
        boolean tumulosNaGrama = true;
        boolean campoMinado = false;
        boolean queimada = false;
        boolean anel = false;
        boolean escadaNoMeio = false;
        boolean escadaCerta = true;
        boolean abismoNasSalas = false;
        boolean pontes = false;
        boolean andar1 = true;
        for (long semente = 5001; semente <= 5300; semente++) {
            for (int andar = 1; andar <= 4; andar++) {
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                for (PlantaDoAndar.InimigoNaPlanta inimigo : planta.inimigos()) {
                    PlantaDoAndar.Sala sala = planta.salaEm(inimigo.x(), inimigo.y());
                    if (inimigo.tipo() == PlantaDoAndar.InimigoDaSala.PIRANHA && sala != null && !sala.especial()) {
                        aquario = true;
                        piranhasNaAgua &= planta.tipo(inimigo.x(), inimigo.y()) == PlantaDoAndar.AGUA;
                    }
                }
                for (PlantaDoAndar.PremioNoChao premio : planta.premios()) {
                    byte tipo = planta.tipo(premio.x(), premio.y());
                    if (premio.premio() == Premio.PREMIO_DO_ESTUDO) {
                        estudo |= tipo == PlantaDoAndar.PEDESTAL;
                    } else if (premio.premio() == Premio.PREMIO_SUSPEITO) {
                        bauSuspeito |= tipo == PlantaDoAndar.PEDESTAL && premio.recipiente() == Recipiente.BAU;
                        andar1 &= andar > 1;
                    } else if (premio.premio() == Premio.PREMIO_DO_ANEL) {
                        anel = true;
                    }
                    if (premio.recipiente() == Recipiente.TUMULO) {
                        cemiterio = true;
                        tumulosNaGrama &= tipo == PlantaDoAndar.CHAO && planta.grama(premio.x(), premio.y()) == PlantaDoAndar.GRAMA;
                        andar1 &= andar > 1;
                    }
                }
                for (int y = 0; y < planta.altura(); y++) {
                    for (int x = 0; x < planta.largura(); x++) {
                        TipoDeArmadilha armadilha = planta.armadilha(x, y);
                        if (armadilha == TipoDeArmadilha.EXPLOSIVA) {
                            campoMinado = true;
                            andar1 &= andar > 1;
                        }
                        if (armadilha == TipoDeArmadilha.FOGO && planta.armadilhaDesarmada(x, y)) {
                            queimada = true;
                            andar1 &= andar > 1;
                        }
                        if (planta.tipo(x, y) == PlantaDoAndar.ABISMO && planta.tema() != Tema.ABISMOS) {
                            PlantaDoAndar.Sala sala = planta.salaEm(x, y);
                            if (sala != null && !sala.especial()) {
                                abismoNasSalas = true;
                            } else if (sala == null) {
                                pontes = true;
                            }
                        }
                    }
                }
                // A escada no meio do anel ou da bacia: entrada a partir do andar 3, saída a partir do 2
                boolean entradaNoMeio = noMeio(planta, planta.entradaX(), planta.entradaY());
                boolean saidaNoMeio = noMeio(planta, planta.saidaX(), planta.saidaY());
                escadaNoMeio |= entradaNoMeio || saidaNoMeio;
                escadaCerta &= !(entradaNoMeio && andar < 3) && !(saidaNoMeio && andar < 2);
            }
        }
        return "Teste das salas comuns novas: aquário com piranhas na água = " + (aquario && piranhasNaAgua) + ", estudo = " + estudo
                + ", baú suspeito = " + bauSuspeito + ", cemitério com túmulos na grama = " + (cemiterio && tumulosNaGrama) + ", campo minado = "
                + campoMinado + ", sala queimada = " + queimada + ", prêmio do anel = " + anel + ", escada no meio = " + (escadaNoMeio && escadaCerta)
                + ", abismo nas salas = " + abismoNasSalas + ", pontes e passarelas = " + pontes + ", nada disso no andar 1 = " + andar1;
    }

    private static boolean noMeio(PlantaDoAndar planta, int x, int y) {
        return x >= 0 && planta.tipo(x - 1, y) == PlantaDoAndar.CHAO_ESPECIAL && planta.tipo(x + 1, y) == PlantaDoAndar.CHAO_ESPECIAL
                && planta.tipo(x, y - 1) == PlantaDoAndar.CHAO_ESPECIAL && planta.tipo(x, y + 1) == PlantaDoAndar.CHAO_ESPECIAL;
    }

    // ---- No mundo: o andar escuro, as armadilhas novas, o túmulo, o espectro e o aviso ----

    private static void testarNoMundo(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = run.andar(1).entrada().offset(3300, 0, 0);
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000d4d4"), "TesteDosTemas"));
        RegrasDoHeroi.dados(heroi).comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.removeAllEffects();

        // O andar escuro: a mesma sala, sem lanternas e com menos luz
        int[] claro = construirSala(mundo, base, Tema.NENHUM);
        int[] escuro = construirSala(mundo, base.offset(0, 0, 80), Tema.ESCURO);
        boolean semLanterna = claro[0] > 0 && escuro[0] == 0 && escuro[1] < claro[1];

        // O andar abismal construído: cada quadradinho de abismo é um buraco com o fundo do abismo lá embaixo
        PlantaDoAndar abismal = comTema(FINGIDA + 500, 3, Tema.ABISMOS);
        BlockPos origemDoAbismal = base.offset(0, 0, 400);
        ConstrutorDeAndar.construir(mundo, abismal, origemDoAbismal, FINGIDA + 500);
        int buracos = 0;
        boolean abismalConstruido = true;
        for (int qy = 0; qy < abismal.altura(); qy++) {
            for (int qx = 0; qx < abismal.largura(); qx++) {
                if (abismal.tipo(qx, qy) != PlantaDoAndar.ABISMO) {
                    continue;
                }
                buracos++;
                BlockPos meio = origemDoAbismal.offset(qx * T + 1, 0, qy * T + 1);
                abismalConstruido &= mundo.getBlockState(meio).isAir() && mundo.getBlockState(meio.below(2)).is(ModBlocos.FUNDO_DO_ABISMO.get());
            }
        }
        abismalConstruido &= buracos > 100;

        // A plataforma pras armadilhas, o túmulo e o espectro
        BlockPos chao = base.offset(0, 0, 160);
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 80; z++) {
                mundo.setBlockAndUpdate(chao.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 5; y++) {
                    mundo.setBlockAndUpdate(chao.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Armadilha de fogo: o rato em cima pega fogo, tem fogo em volta e ela desarma
        BlockPos fogo = chao.offset(-2, -1, 0);
        Armadilhas.colocar(mundo, fogo, TipoDeArmadilha.FOGO, false);
        RatoMarsupial queimado = rato(mundo, fogo);
        Armadilhas.pisar(mundo, fogo, queimado);
        int chamas = 0;
        for (BlockPos posicao : BlockPos.betweenClosed(fogo.offset(-T, 1, -T), fogo.offset(2 * T - 1, 1, 2 * T - 1))) {
            if (mundo.getBlockState(posicao).is(Blocks.FIRE)) {
                chamas++;
            }
        }
        boolean armadilhaDeFogo = queimado.isOnFire() && chamas >= 16 && desarmada(mundo, fogo);

        // Armadilha explosiva: o rato em cima leva a explosão e ela desarma
        BlockPos explosiva = chao.offset(-2, -1, 24);
        Armadilhas.colocar(mundo, explosiva, TipoDeArmadilha.EXPLOSIVA, false);
        RatoMarsupial explodido = rato(mundo, explosiva);
        float vidaAntes = explodido.getHealth();
        Armadilhas.pisar(mundo, explosiva, explodido);
        boolean armadilhaExplosiva = (!explodido.isAlive() || explodido.getHealth() < vidaAntes) && desarmada(mundo, explosiva);

        // O túmulo: quem abre pega os itens e solta os espectros em volta
        BlockPos lugarDoHeroi = chao.offset(0, 0, 48);
        heroi.moveTo(lugarDoHeroi.getX() + 0.5, lugarDoHeroi.getY(), lugarDoHeroi.getZ() + 0.5, 0.0F, 0.0F);
        BlockPos tumulo = lugarDoHeroi.offset(1, 0, 1);
        boolean colocou = Tumulo.colocar(mundo, tumulo, Direction.NORTH, List.of(Ouro.pilha(30), new ItemStack(ModItens.RACAO.get())));
        Tumulo.abrir(mundo, tumulo, heroi);
        boolean abriu = mundo.getBlockState(tumulo).isAir();
        int itens = mundo.getEntitiesOfClass(ItemEntity.class, new AABB(tumulo).inflate(2)).size();
        List<Espectro> espectros = mundo.getEntitiesOfClass(Espectro.class, new AABB(lugarDoHeroi).inflate(T + 2, 3, T + 2));
        boolean tumuloCerto = colocou && abriu && itens >= 2 && espectros.size() == 4;

        // O espectro: 1 de vida, esquiva 5x a precisão, morto-vivo, sem XP, e só age depois de aparecer
        boolean espectroCerto = !espectros.isEmpty();
        boolean esperaPraAgir = !espectros.isEmpty();
        for (Espectro espectro : espectros) {
            espectroCerto &= espectro.getMaxHealth() <= 1.01F && espectro.esquiva() == 5 * espectro.precisao() && espectro.precisao() == 10 + espectro.andar()
                    && espectro.mortoVivoOuDemonio() && espectro.xpAoMorrer() == 0;
            esperaPraAgir &= espectro.isNoAi();
        }
        if (!espectros.isEmpty()) {
            Espectro primeiro = espectros.get(0);
            for (int i = 0; i < 100; i++) {
                primeiro.tick();
            }
            esperaPraAgir &= !primeiro.isNoAi();
        }
        for (Espectro espectro : espectros) {
            espectro.discard();
        }

        // A ossada assombrada: o item amaldiçoado fica com a maldição à vista, e quem pega solta um espectro
        BlockPos ossada = chao.offset(-8, 0, 64);
        heroi.moveTo(ossada.getX() + 0.5, ossada.getY(), ossada.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack amaldicoada = new ItemStack(ModItens.ESPADA_CURTA.get());
        amaldicoada.set(ModComponentes.MALDICAO.get(), Maldicao.values()[0].id());
        amaldicoada.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        BlockPos origemDaOssada = ossada.offset(-1, -1, -1);
        SaqueDoAndar.colocar(mundo, origemDaOssada, 1, List.of(new SaqueDoAndar.Saque(0, 0, List.of(amaldicoada), SaqueDoAndar.Onde.OSSADA)));
        ItemEntity marcada = null;
        for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, new AABB(origemDaOssada).inflate(T))) {
            if (item.getPersistentData().getBoolean(SaqueDoAndar.ASSOMBRADO)) {
                marcada = item;
            }
        }
        boolean assombrada = marcada != null && NivelDoItem.maldicaoConhecida(marcada.getItem());
        int antes = mundo.getEntitiesOfClass(Espectro.class, new AABB(ossada).inflate(3 * T)).size();
        if (marcada != null) {
            NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(heroi, marcada, marcada.getItem().copy()));
        }
        int depois = mundo.getEntitiesOfClass(Espectro.class, new AABB(ossada).inflate(3 * T)).size();
        assombrada &= depois == antes + 1 && marcada != null && !marcada.getPersistentData().getBoolean(SaqueDoAndar.ASSOMBRADO);
        for (Espectro espectro : mundo.getEntitiesOfClass(Espectro.class, new AABB(ossada).inflate(3 * T))) {
            espectro.discard();
        }

        // O aviso do tema ao chegar no andar (um andar que ninguém visita no teste)
        TemaDoAndar.registrar(run.semente(), 9, new TemaDoAndar.Decisao(Tema.ESCURO, -1));
        TemaDoAndar.registrar(run.semente(), 8, new TemaDoAndar.Decisao(Tema.NENHUM, -1));
        boolean aviso = TemaDoAndar.avisar(heroi, 9) && !TemaDoAndar.avisar(heroi, 8);

        PixelDungeon.LOGGER.info("Teste dos temas no mundo: andar escuro sem lanternas = {} ({} -> {} de luz), andar abismal construído = {} ({} buracos), "
                + "armadilha de fogo = {}, armadilha explosiva = {}, túmulo = {} ({} espectros), espectro = {}, só age depois de aparecer = {}, "
                + "ossada assombrada = {}, aviso do tema = {}",
                semLanterna, claro[1], escuro[1], abismalConstruido, buracos, armadilhaDeFogo, armadilhaExplosiva, tumuloCerto, espectros.size(), espectroCerto,
                esperaPraAgir, assombrada, aviso);
    }

    // Constrói uma sala de 5x5 com o tema; devolve quantas lanternas e quanta luz de fundo (somada) ela tem
    private static int[] construirSala(ServerLevel mundo, BlockPos origem, Tema tema) {
        PlantaDoAndar planta = new PlantaDoAndar(9, 9);
        planta.adicionarSala(new PlantaDoAndar.Sala(2, 2, 5, 5));
        planta.definirTema(tema);
        ConstrutorDeAndar.construir(mundo, planta, origem, 1);
        int lanternas = 0;
        int luz = 0;
        for (BlockPos posicao : BlockPos.betweenClosed(origem, origem.offset(9 * T, Andares.ALTURA_DA_PAREDE + 1, 9 * T))) {
            BlockState estado = mundo.getBlockState(posicao);
            if (estado.is(ModBlocos.LANTERNA_ESGOTO.get())) {
                lanternas++;
            } else if (estado.is(Blocks.LIGHT)) {
                luz += estado.getValue(LightBlock.LEVEL);
            }
        }
        return new int[] { lanternas, luz };
    }

    // ---- Ajudantes ----

    private static PlantaDoAndar comTema(long semente, int andar, Tema tema) {
        TemaDoAndar.registrar(semente, andar, new TemaDoAndar.Decisao(tema, -1));
        return GeradorDeAndar.gerar(semente, andar);
    }

    // A primeira semente em que a própria semente dá esse tema pro andar
    private static long sementeCom(Tema tema, int andar) {
        long semente = 1;
        while (TemaDoAndar.daSemente(semente, andar) != tema) {
            semente++;
        }
        return semente;
    }

    private static int contar(PlantaDoAndar planta, byte tipo) {
        int total = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.tipo(x, y) == tipo) {
                    total++;
                }
            }
        }
        return total;
    }

    private static int contarGrama(PlantaDoAndar planta) {
        int total = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.grama(x, y) != PlantaDoAndar.SEM_GRAMA) {
                    total++;
                }
            }
        }
        return total;
    }

    private static int comidas(List<SaqueDoAndar.Saque> saques) {
        int total = 0;
        for (SaqueDoAndar.Saque saque : saques) {
            for (ItemStack item : saque.itens()) {
                if (item.is(ModItens.RACAO.get()) || item.is(ModItens.PASTEL.get())) {
                    total++;
                }
            }
        }
        return total;
    }

    private static RatoMarsupial rato(ServerLevel mundo, BlockPos controle) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        Vec3 meio = Armadilhas.centro(controle);
        rato.moveTo(meio.x, meio.y, meio.z);
        mundo.addFreshEntity(rato);
        return rato;
    }

    private static boolean desarmada(ServerLevel mundo, BlockPos controle) {
        BlockState estado = mundo.getBlockState(controle);
        return estado.getBlock() instanceof ArmadilhaBloco && estado.getValue(ArmadilhaBloco.DESARMADA);
    }
}
