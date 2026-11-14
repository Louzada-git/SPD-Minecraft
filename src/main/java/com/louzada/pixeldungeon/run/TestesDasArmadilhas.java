package com.louzada.pixeldungeon.run;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.ArmadilhaBloco;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.artefato.TalismaDaVidencia;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as armadilhas. Primeiro confere as armadilhas sorteadas nos andares 1 a 4 (e as das salas
// das armadilhas e do tesouro secreto); depois testa cada armadilha numa plataforma de pedra longe da dungeon, com ratos
// em cima; e por fim a busca, o mapeamento e o talismã num andar de verdade. O log mostra o resultado (o build.yml confere).
public class TestesDasArmadilhas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    // Cada armadilha da plataforma fica numa faixa dela, longe das outras
    private static final int FAIXA = 24;

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        testarGeracao(mundo, run);
        testarSalas(run);
        testarInvocacaoSecreta();
        testarEfeitos(mundo, run);
        testarNoAndar(mundo, run);
    }

    // ---- As armadilhas dos andares ----

    private static void testarGeracao(ServerLevel mundo, DadosDaRun run) {
        int[] porAndar = new int[5];
        int escondidas = 0;
        boolean andar1SoDardos = true;
        boolean lugarCerto = true;
        boolean construidas = true;
        boolean foraDosLugaresLivres = true;
        Set<TipoDeArmadilha> doEsgoto = EnumSet.noneOf(TipoDeArmadilha.class);
        doEsgoto.addAll(List.of(TipoDeArmadilha.doAndar(2)));
        doEsgoto.addAll(List.of(TipoDeArmadilha.DA_SALA_DAS_ARMADILHAS));
        doEsgoto.add(TipoDeArmadilha.DARDOS_VENENOSOS);
        doEsgoto.add(TipoDeArmadilha.DESMORONAMENTO);
        // (as da sala queimada e do campo minado)
        doEsgoto.add(TipoDeArmadilha.FOGO);
        doEsgoto.add(TipoDeArmadilha.EXPLOSIVA);
        boolean tiposCertos = true;
        for (int andar = 1; andar <= 4; andar++) {
            PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
            BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
            for (int y = 0; y < planta.altura(); y++) {
                for (int x = 0; x < planta.largura(); x++) {
                    TipoDeArmadilha tipo = planta.armadilha(x, y);
                    if (tipo == null) {
                        continue;
                    }
                    porAndar[andar]++;
                    if (!planta.armadilhaVisivel(x, y)) {
                        escondidas++;
                    }
                    // (a sala das armadilhas pode cair no andar 1, igual ao Shattered, e as armadilhas dela são outras)
                    PlantaDoAndar.Sala sala = planta.salaEm(x, y);
                    if (andar == 1 && tipo != TipoDeArmadilha.DARDOS && (sala == null || !sala.especial())) {
                        andar1SoDardos = false;
                    }
                    if (!doEsgoto.contains(tipo)) {
                        tiposCertos = false;
                    }
                    if (planta.tipo(x, y) != PlantaDoAndar.CHAO || planta.grama(x, y) != PlantaDoAndar.SEM_GRAMA) {
                        lugarCerto = false;
                    }
                    // (as escondidas podem já ter sido achadas pelos testes da busca e do talismã)
                    BlockState estado = mundo.getBlockState(Armadilhas.controle(origem, x, y));
                    if (!(estado.getBlock() instanceof ArmadilhaBloco) || estado.getValue(ArmadilhaBloco.TIPO) != tipo
                            || (planta.armadilhaVisivel(x, y) && estado.getValue(ArmadilhaBloco.ESCONDIDA))) {
                        construidas = false;
                    }
                }
            }
            for (int[] lugar : PopulacaoDoAndar.lugaresLivres(planta)) {
                if (planta.temArmadilha(lugar[0], lugar[1])) {
                    foraDosLugaresLivres = false;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das armadilhas nos andares: por andar {}/{}/{}/{}, escondidas nos andares 2 a 4 = {}, andar 1 só de dardos = {}, "
                + "tipos do esgoto = {}, só em chão vazio = {}, construídas iguais à planta = {}, fora dos lugares de itens e inimigos = {}",
                porAndar[1], porAndar[2], porAndar[3], porAndar[4], escondidas, andar1SoDardos, tiposCertos, lugarCerto, construidas,
                foraDosLugaresLivres);
    }

    // ---- A sala das armadilhas e o tesouro secreto (procurando uma semente que tenha cada uma) ----

    private static void testarSalas(DadosDaRun run) {
        boolean achouSala = false;
        for (long semente = 1; semente <= 400 && !achouSala; semente++) {
            for (int andar = 1; andar <= 4 && !achouSala; andar++) {
                if (!ComodoEspecial.doAndar(semente, andar).contains(ComodoEspecial.Tipo.ARMADILHAS)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                PlantaDoAndar.PremioNoChao premio = null;
                for (PlantaDoAndar.PremioNoChao candidato : planta.premios()) {
                    if (candidato.premio() == PlantaDoAndar.Premio.PREMIO_DAS_ARMADILHAS) {
                        premio = candidato;
                    }
                }
                if (premio == null) {
                    continue;
                }
                PlantaDoAndar.Sala sala = planta.salaEm(premio.x(), premio.y());
                // (1 em 4 dessas salas é um abismo, sem armadilhas: o teste procura uma com armadilhas)
                boolean abismo = false;
                for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
                    for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                        abismo |= planta.tipo(x, y) == PlantaDoAndar.ABISMO;
                    }
                }
                if (abismo) {
                    continue;
                }
                achouSala = true;
                int armadilhas = 0;
                boolean todasAVista = true;
                Set<TipoDeArmadilha> tipos = EnumSet.noneOf(TipoDeArmadilha.class);
                for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
                    for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                        if (planta.temArmadilha(x, y)) {
                            armadilhas++;
                            tipos.add(planta.armadilha(x, y));
                            todasAVista &= planta.armadilhaVisivel(x, y);
                        }
                    }
                }
                boolean tipoDaSala = tipos.size() == 1 && List.of(TipoDeArmadilha.DA_SALA_DAS_ARMADILHAS).containsAll(tipos);
                List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(planta, andar, run);
                boolean bauComPremio = false;
                int levitacao = 0;
                for (SaqueDoAndar.Saque saque : saques) {
                    if (saque.x() == premio.x() && saque.y() == premio.y() && saque.onde() == SaqueDoAndar.Onde.BAU && !saque.itens().isEmpty()) {
                        bauComPremio = true;
                    }
                    for (ItemStack item : saque.itens()) {
                        if (Identificacao.tipoDaPocao(run.semente(), item) == TipoDePocao.LEVITACAO) {
                            levitacao++;
                        }
                    }
                }
                boolean premioSemArmadilha = !planta.temArmadilha(premio.x(), premio.y());
                PixelDungeon.LOGGER.info("Teste da sala das armadilhas: tem armadilhas = {}, todas à vista = {}, um tipo só da sala = {}, "
                        + "baú com prêmio = {}, o baú fora das armadilhas = {}, poção de levitação no andar = {}",
                        armadilhas > 0, todasAVista, tipoDaSala, bauComPremio, premioSemArmadilha, levitacao >= 1);
            }
        }
        if (!achouSala) {
            PixelDungeon.LOGGER.info("Teste da sala das armadilhas: nenhuma semente com a sala");
        }

        boolean achouTesouro = false;
        for (long semente = 1; semente <= 400 && !achouTesouro; semente++) {
            for (int andar = 2; andar <= 4 && !achouTesouro; andar++) {
                if (!ComodoSecreto.secretasDoAndar(semente, andar).contains(ComodoSecreto.Tipo.TESOURO)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                int doTesouro = 0;
                boolean todasAVista = true;
                for (int y = 0; y < planta.altura(); y++) {
                    for (int x = 0; x < planta.largura(); x++) {
                        TipoDeArmadilha tipo = planta.armadilha(x, y);
                        if (tipo == TipoDeArmadilha.DESMORONAMENTO || tipo == TipoDeArmadilha.DARDOS_VENENOSOS) {
                            doTesouro++;
                            todasAVista &= planta.armadilhaVisivel(x, y);
                        }
                    }
                }
                if (planta.salasSecretas() > 0) {
                    achouTesouro = true;
                    PixelDungeon.LOGGER.info("Teste do tesouro secreto: tem armadilhas de desmoronamento ou de dardo venenoso = {}, todas à vista = {}",
                            doTesouro > 0, todasAVista);
                }
            }
        }
    }

    // A sala de invocação secreta: o chão todo de armadilhas de invocação escondidas, e um item no meio
    private static void testarInvocacaoSecreta() {
        for (long semente = 1; semente <= 400; semente++) {
            for (int andar = 2; andar <= 4; andar++) {
                if (!ComodoSecreto.secretasDoAndar(semente, andar).contains(ComodoSecreto.Tipo.INVOCACAO)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                PlantaDoAndar.PremioNoChao item = null;
                for (PlantaDoAndar.PremioNoChao premio : planta.premios()) {
                    // (o do cemitério gramado fica num túmulo)
                    if (premio.premio() == PlantaDoAndar.Premio.ITEM_ALEATORIO && premio.recipiente() == PlantaDoAndar.Recipiente.OSSADA) {
                        item = premio;
                    }
                }
                if (item == null) {
                    continue;
                }
                PlantaDoAndar.Sala sala = planta.salaEm(item.x(), item.y());
                boolean todasDeInvocacao = true;
                boolean todasEscondidas = true;
                int armadilhas = 0;
                for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
                    for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                        armadilhas += planta.temArmadilha(x, y) ? 1 : 0;
                        todasDeInvocacao &= planta.armadilha(x, y) == TipoDeArmadilha.INVOCACAO;
                        todasEscondidas &= !planta.armadilhaVisivel(x, y);
                    }
                }
                PixelDungeon.LOGGER.info("Teste da invocação secreta: o chão todo de armadilhas de invocação = {}, todas escondidas = {}, "
                        + "o item em cima de uma = {}", todasDeInvocacao && armadilhas >= 9, todasEscondidas, planta.temArmadilha(item.x(), item.y()));
                return;
            }
        }
        PixelDungeon.LOGGER.info("Teste da invocação secreta: nenhuma semente com a sala");
    }

    // ---- Cada armadilha numa plataforma ----

    private static void testarEfeitos(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = run.andar(1).entrada().offset(2300, 0, 0);
        for (int x = -14; x <= 14; x++) {
            for (int z = -14; z <= FAIXA * 16; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 5; y++) {
                    mundo.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        FakePlayer heroi = heroi(mundo, run);
        int faixa = 0;

        // Dardos: o rato em cima leva o dardo; a armadilha desarma. Ela nunca fica escondida.
        BlockPos dardos = armadilha(mundo, base, faixa++, TipoDeArmadilha.DARDOS, true);
        boolean dardosAVista = !mundo.getBlockState(dardos).getValue(ArmadilhaBloco.ESCONDIDA);
        RatoMarsupial alvo = rato(mundo, dardos, 0, 0);
        float antes = alvo.getHealth();
        Armadilhas.pisar(mundo, dardos, alvo);
        boolean acertou = !alvo.isAlive() || alvo.getHealth() < antes;
        PixelDungeon.LOGGER.info("Teste da armadilha de dardos: nunca fica escondida = {}, o rato em cima levou o dardo = {}, desarmou = {}",
                dardosAVista, acertou, desarmada(mundo, dardos));

        // Dardo venenoso: ativada por um item, acerta o rato mais perto (a 2 quadradinhos) e envenena
        BlockPos venenosos = armadilha(mundo, base, faixa++, TipoDeArmadilha.DARDOS_VENENOSOS, false);
        RatoMarsupial longe = rato(mundo, venenosos, 2, 0);
        Armadilhas.pisar(mundo, venenosos, null);
        PixelDungeon.LOGGER.info("Teste da armadilha de dardo venenoso: acertou o rato mais perto = {}", longe.hasEffect(ModEfeitos.VENENO));

        // Resfriamento: resfria na hora e, ficando dentro, congela
        BlockPos resfriamento = armadilha(mundo, base, faixa++, TipoDeArmadilha.RESFRIAMENTO, false);
        RatoMarsupial friorento = rato(mundo, resfriamento, 0, 0);
        Armadilhas.pisar(mundo, resfriamento, friorento);
        boolean resfriado = friorento.hasEffect(ModEfeitos.RESFRIADO);
        NuvemDeGas fria = nuvem(mundo, resfriamento, NuvemDeGas.Tipo.CONGELANTE);
        int turnos = 1;
        while (fria != null && turnos < 8 && !friorento.hasEffect(ModEfeitos.CONGELADO)) {
            fria.afetar(friorento);
            turnos++;
        }
        PixelDungeon.LOGGER.info("Teste da armadilha de resfriamento: nuvem = {}, resfriado = {}, congelou no turno {}", fria != null, resfriado, turnos);

        // Elétrica: paralisa quem está dentro
        BlockPos eletrica = armadilha(mundo, base, faixa++, TipoDeArmadilha.ELETRICA, false);
        RatoMarsupial chocado = rato(mundo, eletrica, 1, 0);
        Armadilhas.pisar(mundo, eletrica, null);
        PixelDungeon.LOGGER.info("Teste da armadilha elétrica: nuvem = {}, o rato do lado ficou paralisado = {}",
                nuvem(mundo, eletrica, NuvemDeGas.Tipo.ELETRICO) != null, chocado.hasEffect(ModEfeitos.PARALISADO));

        // Os gases
        BlockPos toxica = armadilha(mundo, base, faixa++, TipoDeArmadilha.GAS_TOXICO, false);
        Armadilhas.pisar(mundo, toxica, null);
        BlockPos confusao = armadilha(mundo, base, faixa++, TipoDeArmadilha.CONFUSAO, false);
        Armadilhas.pisar(mundo, confusao, null);
        PixelDungeon.LOGGER.info("Teste das armadilhas de gás: tóxico = {}, confusão = {}", nuvem(mundo, toxica, NuvemDeGas.Tipo.TOXICO) != null,
                nuvem(mundo, confusao, NuvemDeGas.Tipo.CONFUSAO) != null);

        // Alarme: o rato dormindo longe acorda e vai até a armadilha
        BlockPos alarme = armadilha(mundo, base, faixa++, TipoDeArmadilha.ALARME, false);
        RatoMarsupial dorminhoco = rato(mundo, alarme, 0, 4);
        dorminhoco.dormir();
        Armadilhas.pisar(mundo, alarme, null);
        PixelDungeon.LOGGER.info("Teste da armadilha de alarme: acordou = {}, foi chamado = {}", !dorminhoco.dormindo(),
                dorminhoco.chamadoPara() != null);

        // Muco: pega quem está do lado, menos quem voa
        BlockPos muco = armadilha(mundo, base, faixa++, TipoDeArmadilha.MUCO, false);
        RatoMarsupial melecado = rato(mundo, muco, 1, 1);
        EnxameDeMoscas enxame = ModEntidades.ENXAME_DE_MOSCAS.get().create(mundo);
        Vec3 doLado = Armadilhas.centro(muco.offset(-T, 0, 0));
        enxame.moveTo(doLado.x, doLado.y, doLado.z);
        mundo.addFreshEntity(enxame);
        Armadilhas.pisar(mundo, muco, null);
        PixelDungeon.LOGGER.info("Teste da armadilha de muco: o rato do lado = {}, o enxame (voa) = {}", melecado.hasEffect(ModEfeitos.MUCO_CAUSTICO),
                enxame.hasEffect(ModEfeitos.MUCO_CAUSTICO));

        // Rebanho: ovelhas em todo quadradinho livre até 2 de distância (menos onde o rato está)
        BlockPos rebanho = armadilha(mundo, base, faixa++, TipoDeArmadilha.REBANHO, false);
        RatoMarsupial pastor = rato(mundo, rebanho, 0, 0);
        Armadilhas.pisar(mundo, rebanho, pastor);
        List<OvelhaMagica> ovelhas = mundo.getEntitiesOfClass(OvelhaMagica.class, area(rebanho, 2));
        boolean somem = !ovelhas.isEmpty() && ovelhas.get(0).ticksRestantes() > 0 && ovelhas.get(0).ticksRestantes() <= 8 * 40;
        PixelDungeon.LOGGER.info("Teste da armadilha de rebanho: {} ovelhas, somem sozinhas = {}", ovelhas.size(), somem);

        // Invocação: 1 a 3 inimigos nos quadradinhos do lado
        BlockPos invocacao = armadilha(mundo, base, faixa++, TipoDeArmadilha.INVOCACAO, false);
        RatoMarsupial invocador = rato(mundo, invocacao, 0, 0);
        Armadilhas.pisar(mundo, invocacao, invocador);
        int invocados = mundo.getEntitiesOfClass(MobDoShattered.class, area(invocacao, 1), mob -> mob != invocador).size();
        PixelDungeon.LOGGER.info("Teste da armadilha de invocação: invocou de 1 a 3 = {}", invocados >= 1 && invocados <= 3);

        // Agarradora: sangra e aleija, não desarma, e não agarra de novo quem fica parado em cima
        BlockPos agarradora = armadilha(mundo, base, faixa++, TipoDeArmadilha.AGARRADORA, false);
        RatoMarsupial preso = rato(mundo, agarradora, 0, 0);
        Armadilhas.pisar(mundo, agarradora, preso);
        boolean sangrou = preso.hasEffect(ModEfeitos.SANGRANDO);
        boolean aleijou = preso.hasEffect(ModEfeitos.ALEIJADO);
        Armadilhas.olhar(mundo, agarradora);
        preso.removeEffect(ModEfeitos.ALEIJADO);
        Armadilhas.olhar(mundo, agarradora);
        PixelDungeon.LOGGER.info("Teste da armadilha agarradora: sangrou = {}, aleijou = {}, continua armada = {}, agarrou de novo quem ficou parado = {}",
                sangrou, aleijou, !desarmada(mundo, agarradora), preso.hasEffect(ModEfeitos.ALEIJADO));

        // Desmoronamento (fora das salas: 2 quadradinhos em volta): pega os dois ratos
        BlockPos desmoronamento = armadilha(mundo, base, faixa++, TipoDeArmadilha.DESMORONAMENTO, false);
        RatoMarsupial esmagado = rato(mundo, desmoronamento, 0, 0);
        RatoMarsupial vizinho = rato(mundo, desmoronamento, 2, 0);
        Armadilhas.pisar(mundo, desmoronamento, esmagado);
        boolean pegouOsDois = (!esmagado.isAlive() || esmagado.getHealth() < esmagado.getMaxHealth())
                && (!vizinho.isAlive() || vizinho.getHealth() < vizinho.getMaxHealth());
        PixelDungeon.LOGGER.info("Teste da armadilha de desmoronamento: acertou os 2 ratos = {}", pegouOsDois);

        // Escondida: o rato não ativa; o herói ativa (e ela aparece); à vista, o rato ativa; quem voa não ativa
        BlockPos escondida = armadilha(mundo, base, faixa++, TipoDeArmadilha.GAS_TOXICO, true);
        rato(mundo, escondida, 0, 0);
        Armadilhas.olhar(mundo, escondida);
        Armadilhas.olhar(mundo, escondida);
        boolean ratoNaoAtivou = !desarmada(mundo, escondida) && mundo.getBlockState(escondida).getValue(ArmadilhaBloco.ESCONDIDA);
        Armadilhas.revelar(mundo, escondida);
        rato(mundo, escondida, 0, 0);
        Armadilhas.olhar(mundo, escondida);
        boolean ratoAtivouAVista = desarmada(mundo, escondida);
        BlockPos doHeroi = armadilha(mundo, base, faixa++, TipoDeArmadilha.CONFUSAO, true);
        Armadilhas.pisar(mundo, doHeroi, heroi);
        boolean heroiAtivou = desarmada(mundo, doHeroi) && nuvem(mundo, doHeroi, NuvemDeGas.Tipo.CONFUSAO) != null;
        BlockPos voando = armadilha(mundo, base, faixa++, TipoDeArmadilha.CONFUSAO, false);
        EnxameDeMoscas voador = ModEntidades.ENXAME_DE_MOSCAS.get().create(mundo);
        Vec3 meio = Armadilhas.centro(voando);
        voador.moveTo(meio.x, meio.y, meio.z);
        mundo.addFreshEntity(voador);
        Armadilhas.olhar(mundo, voando);
        PixelDungeon.LOGGER.info("Teste das armadilhas escondidas: o rato não ativou a escondida = {}, o rato ativou a que estava à vista = {}, "
                + "o herói ativou a escondida = {}, o enxame (voa) não ativou = {}", ratoNaoAtivou, ratoAtivouAVista, heroiAtivou,
                !desarmada(mundo, voando));

        // Itens: o que já estava lá quando a armadilha começou a olhar não ativa; o que cai depois ativa
        BlockPos doItem = armadilha(mundo, base, faixa++, TipoDeArmadilha.GAS_TOXICO, true);
        Vec3 meioDoItem = Armadilhas.centro(doItem);
        SaqueDoAndar.soltar(mundo, meioDoItem.x, meioDoItem.y, meioDoItem.z, new ItemStack(Items.STICK));
        Armadilhas.olhar(mundo, doItem);
        boolean jaEstavaNaoAtivou = !desarmada(mundo, doItem);
        SaqueDoAndar.soltar(mundo, meioDoItem.x + 0.5, meioDoItem.y + 1, meioDoItem.z, new ItemStack(Items.STICK));
        Armadilhas.olhar(mundo, doItem);
        PixelDungeon.LOGGER.info("Teste das armadilhas com itens: o item que já estava lá não ativou = {}, o que caiu em cima ativou = {}",
                jaEstavaNaoAtivou, desarmada(mundo, doItem));

        // Os inimigos desviam das que estão à vista
        BlockState aVista = ModBlocos.ARMADILHA.get().pedaco(TipoDeArmadilha.ALARME, false, false, 0, 0);
        BlockState escondidaEstado = ModBlocos.ARMADILHA.get().pedaco(TipoDeArmadilha.ALARME, true, false, 0, 0);
        BlockState desarmadaEstado = ModBlocos.ARMADILHA.get().pedaco(TipoDeArmadilha.ALARME, false, true, 0, 0);
        PixelDungeon.LOGGER.info("Teste do caminho dos inimigos: desviam da armadilha à vista = {}, da escondida = {}, da desarmada = {}",
                aVista.getBlockPathType(mundo, BlockPos.ZERO, null) == PathType.DAMAGE_OTHER,
                escondidaEstado.getBlockPathType(mundo, BlockPos.ZERO, null) != null,
                desarmadaEstado.getBlockPathType(mundo, BlockPos.ZERO, null) != null);

        for (Entity sobra : mundo.getEntities((Entity) null, new AABB(base).inflate(40, 10, FAIXA * 17),
                entidade -> entidade instanceof MobDoShattered || entidade instanceof OvelhaMagica || entidade instanceof NuvemDeGas
                        || entidade instanceof ItemEntity)) {
            sobra.discard();
        }
    }

    // ---- No andar de verdade: teletransporte, portal, busca, mapeamento e talismã ----

    private static void testarNoAndar(ServerLevel mundo, DadosDaRun run) {
        int andar = 3;
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        // Lugares livres com um vizinho livre do lado (pra pôr armadilhas e o herói)
        List<int[]> pares = new ArrayList<>();
        for (int[] lugar : PopulacaoDoAndar.lugaresLivres(planta)) {
            if (planta.tipo(lugar[0] + 1, lugar[1]) == PlantaDoAndar.CHAO && !planta.temArmadilha(lugar[0] + 1, lugar[1])
                    && !planta.temPremioEm(lugar[0] + 1, lugar[1]) && planta.salaEm(lugar[0] + 1, lugar[1]) == planta.salaEm(lugar[0], lugar[1])) {
                pares.add(lugar);
            }
        }
        if (pares.size() < 4) {
            PixelDungeon.LOGGER.info("Teste das armadilhas no andar: lugares livres de menos ({})", pares.size());
            return;
        }

        // Teletransporte: o rato e o item vão pra longe
        int[] tele = pares.get(0);
        BlockPos teletransporte = Armadilhas.controle(origem, tele[0], tele[1]);
        planta.colocarArmadilha(tele[0], tele[1], TipoDeArmadilha.TELETRANSPORTE, true);
        Armadilhas.colocar(mundo, teletransporte, TipoDeArmadilha.TELETRANSPORTE, false);
        RatoMarsupial viajante = rato(mundo, teletransporte, 0, 0);
        Vec3 meio = Armadilhas.centro(teletransporte);
        ItemEntity graveto = SaqueDoAndar.soltar(mundo, meio.x + 1, meio.y, meio.z, new ItemStack(Items.STICK));
        Armadilhas.pisar(mundo, teletransporte, viajante);
        PixelDungeon.LOGGER.info("Teste da armadilha de teletransporte: o rato foi pra longe = {}, o item também = {}, desarmou = {}",
                viajante.position().distanceTo(meio) > 2 * T, graveto.position().distanceTo(meio) > 1.5, desarmada(mundo, teletransporte));

        // Portal: sempre pro mesmo lugar, e continua armada
        int[] port = pares.get(pares.size() / 2);
        BlockPos portal = Armadilhas.controle(origem, port[0], port[1]);
        planta.colocarArmadilha(port[0], port[1], TipoDeArmadilha.PORTAL, true);
        Armadilhas.colocar(mundo, portal, TipoDeArmadilha.PORTAL, false);
        BlockPos destino = EfeitosDasArmadilhas.destinoDoPortal(mundo, portal);
        // (sem os inimigos do andar em volta da armadilha e do destino, pra só os 2 ratos irem)
        for (MobDoShattered perto : mundo.getEntitiesOfClass(MobDoShattered.class, area(portal, 1))) {
            perto.discard();
        }
        if (destino != null) {
            for (MobDoShattered perto : mundo.getEntitiesOfClass(MobDoShattered.class, area(destino, 1))) {
                perto.discard();
            }
        }
        RatoMarsupial primeiro = rato(mundo, portal, 0, 0);
        Armadilhas.pisar(mundo, portal, primeiro);
        RatoMarsupial segundo = rato(mundo, portal, 0, 0);
        Armadilhas.pisar(mundo, portal, segundo);
        boolean mesmoLugar = destino != null && primeiro.position().distanceTo(Armadilhas.centro(destino)) < 2.5 * T
                && segundo.position().distanceTo(Armadilhas.centro(destino)) < 2.5 * T;
        PixelDungeon.LOGGER.info("Teste da armadilha de portal: os 2 ratos foram pro mesmo lugar = {}, continua armada = {}", mesmoLugar,
                !desarmada(mundo, portal));
        primeiro.discard();
        segundo.discard();
        viajante.discard();
        graveto.discard();

        // A busca acha a armadilha escondida do lado; o talismã avisa antes
        FakePlayer heroi = heroi(mundo, run);
        int[] busca = pares.get(pares.size() - 1);
        BlockPos escondida = Armadilhas.controle(origem, busca[0] + 1, busca[1]);
        planta.colocarArmadilha(busca[0] + 1, busca[1], TipoDeArmadilha.GAS_TOXICO, false);
        Armadilhas.colocar(mundo, escondida, TipoDeArmadilha.GAS_TOXICO, true);
        Vec3 aoLado = Armadilhas.centro(Armadilhas.controle(origem, busca[0], busca[1]));
        heroi.moveTo(aoLado.x, aoLado.y, aoLado.z, 0.0F, 0.0F);
        boolean apreensivo = TalismaDaVidencia.avisarDasArmadilhas(heroi);
        int achou = Busca.procurar(heroi);
        boolean apareceu = !mundo.getBlockState(escondida).getValue(ArmadilhaBloco.ESCONDIDA);
        PixelDungeon.LOGGER.info("Teste da busca de armadilhas: o talismã avisou = {}, achou {} segredo(s), a armadilha apareceu = {}", apreensivo, achou,
                apareceu);

        // O mapeamento mágico revela todas as escondidas do andar (e o talismã para de avisar)
        int[] outra = pares.get(1);
        planta.colocarArmadilha(outra[0] + 1, outra[1], TipoDeArmadilha.ALARME, false);
        Armadilhas.colocar(mundo, Armadilhas.controle(origem, outra[0] + 1, outra[1]), TipoDeArmadilha.ALARME, true);
        int reveladas = Busca.revelarArmadilhas(heroi);
        int sobraram = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.temArmadilha(x, y) && Armadilhas.escondidaEm(mundo, origem, x, y)) {
                    sobraram++;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste do mapeamento das armadilhas: revelou alguma = {}, sobrou escondida = {}, o talismã parou de avisar = {}",
                reveladas > 0, sobraram, !TalismaDaVidencia.avisarDasArmadilhas(heroi));
    }

    // ---- Ajudantes ----

    private static FakePlayer heroi(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000a7a7"), "TesteDasArmadilhas"));
        RegrasDoHeroi.dados(heroi).comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        return heroi;
    }

    // Uma armadilha na faixa "indice" da plataforma (o chão da plataforma fica 1 bloco abaixo da base)
    private static BlockPos armadilha(ServerLevel mundo, BlockPos base, int indice, TipoDeArmadilha tipo, boolean escondida) {
        BlockPos controle = base.offset(-2, -1, indice * FAIXA);
        Armadilhas.colocar(mundo, controle, tipo, escondida);
        return controle;
    }

    // Um rato no quadradinho (dx, dz) a partir da armadilha
    private static RatoMarsupial rato(ServerLevel mundo, BlockPos controle, int dx, int dz) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        Vec3 meio = Armadilhas.centro(controle.offset(dx * T, 0, dz * T));
        rato.moveTo(meio.x, meio.y, meio.z);
        mundo.addFreshEntity(rato);
        return rato;
    }

    private static boolean desarmada(ServerLevel mundo, BlockPos controle) {
        BlockState estado = mundo.getBlockState(controle);
        return estado.getBlock() instanceof ArmadilhaBloco && estado.getValue(ArmadilhaBloco.DESARMADA);
    }

    private static NuvemDeGas nuvem(ServerLevel mundo, BlockPos controle, NuvemDeGas.Tipo tipo) {
        for (NuvemDeGas nuvem : mundo.getEntitiesOfClass(NuvemDeGas.class, area(controle, 0))) {
            if (nuvem.tipo() == tipo) {
                return nuvem;
            }
        }
        return null;
    }

    // Os quadradinhos até "raio" em volta da armadilha
    private static AABB area(BlockPos controle, int raio) {
        return new AABB(controle.getX() - raio * T, controle.getY(), controle.getZ() - raio * T, controle.getX() + (raio + 1) * T,
                controle.getY() + 4, controle.getZ() + (raio + 1) * T);
    }
}
