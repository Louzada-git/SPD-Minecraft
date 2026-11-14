package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.artefato.TalismaDaVidencia;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// A busca do Shattered: achar as portas escondidas das salas secretas e as armadilhas escondidas.
//   procurar (tecla G ou o botão da barra do inventário): sempre acha as portas e armadilhas que estiverem perto e à vista;
//     leva 2 turnos
//   andando: a cada quadradinho novo, cada segredo perto e à vista tem uma chance de ser notado
//     (porta: 20% no andar 0, caindo 1% por andar: 18% no andar 2; armadilha: 40%, caindo 0,4% por andar)
//   perto: 1 quadradinho em volta (3x3); o Ladino procura 2 (5x5); a grande busca soma 1 (+1: um círculo, +2: um quadrado)
//   o pergaminho de mapeamento mágico revela todas as portas e armadilhas escondidas do andar
//   a previsão do ladino avisa, ao chegar num andar com uma sala secreta, que ela existe (+1: 75%, +2: sempre)
//   o talismã da vidência revela as portas e as armadilhas num cone (e carrega quando a busca acha alguma:
//     10 por porta, 2 por armadilha), e deixa o herói apreensivo quando tem armadilha escondida perto
public class Busca {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int TEMPO_PARA_PROCURAR = Turno.ticks(2);

    // Quando cada herói pode procurar de novo, e em que quadradinho ele estava (pra notar quando muda)
    private static Map<UUID, Long> podeProcurarEm = new HashMap<>();
    private static Map<UUID, Long> ultimoQuadradinho = new HashMap<>();

    // Onde o herói está: o andar (já construído), a planta, a origem e o quadradinho
    private record Lugar(DadosDaRun run, int andar, PlantaDoAndar planta, BlockPos origem, int qx, int qy) {
    }

    // ---- Procurar de propósito ----

    // Devolve quantos segredos (portas e armadilhas) achou
    public static int procurar(ServerPlayer jogador) {
        long agora = jogador.serverLevel().getGameTime();
        if (agora < podeProcurarEm.getOrDefault(jogador.getUUID(), 0L)) {
            return 0;
        }
        podeProcurarEm.put(jogador.getUUID(), agora + TEMPO_PARA_PROCURAR);
        NumerosFlutuantes.mostrarTraduzido(jogador, "efeito.pixeldungeon.procurar", NumerosFlutuantes.BRANCO);
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            return 0;
        }
        return procurarEmVolta(jogador, lugar, true);
    }

    // ---- Andando ----

    public static void aoTick(ServerPlayer jogador) {
        if (jogador.tickCount % 5 != 0) {
            return;
        }
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            ultimoQuadradinho.remove(jogador.getUUID());
            return;
        }
        long quadradinho = ((long) lugar.andar() << 32) | ((long) lugar.qy() << 16) | lugar.qx();
        Long antes = ultimoQuadradinho.put(jogador.getUUID(), quadradinho);
        if (antes != null && antes != quadradinho) {
            procurarEmVolta(jogador, lugar, false);
        }
    }

    // ---- Pergaminho de mapeamento mágico ----

    // Revela todas as portas escondidas do andar em que o jogador está; devolve onde elas ficam
    public static List<BlockPos> revelarTodas(ServerPlayer jogador) {
        List<BlockPos> portas = new ArrayList<>();
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            return portas;
        }
        PlantaDoAndar planta = lugar.planta();
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (escondida(lugar, x, y)) {
                    revelar(jogador.serverLevel(), lugar, x, y);
                    portas.add(ConstrutorDeAndar.pontoDeChegada(lugar.origem(), x, y));
                }
            }
        }
        if (!portas.isEmpty()) {
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F);
        }
        return portas;
    }

    // Revela todas as armadilhas escondidas do andar em que o jogador está; devolve quantas
    public static int revelarArmadilhas(ServerPlayer jogador) {
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            return 0;
        }
        int reveladas = 0;
        PlantaDoAndar planta = lugar.planta();
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.temArmadilha(x, y) && Armadilhas.revelar(jogador.serverLevel(), Armadilhas.controle(lugar.origem(), x, y))) {
                    reveladas++;
                }
            }
        }
        return reveladas;
    }

    // ---- Pedra da clarividência ----

    // Revela as portas e as armadilhas escondidas num círculo de "raio" quadradinhos em volta do lugar (atravessa paredes).
    // Devolve quantos segredos achou.
    public static int revelarEmVolta(ServerLevel mundo, Vec3 centro, int raio) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        Integer andar = run.ativa() ? PopulacaoDoAndar.andarEm(run, centro.x, centro.y, centro.z) : null;
        if (andar == null || run.andar(andar) == null) {
            return 0;
        }
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        int qx = Math.floorDiv(Mth.floor(centro.x) - origem.getX(), T);
        int qy = Math.floorDiv(Mth.floor(centro.z) - origem.getZ(), T);
        Lugar lugar = new Lugar(run, andar, PopulacaoDoAndar.planta(run.semente(), andar), origem, qx, qy);
        PlantaDoAndar planta = lugar.planta();
        int achou = 0;
        for (int y = Math.max(0, qy - raio); y <= Math.min(planta.altura() - 1, qy + raio); y++) {
            for (int x = Math.max(0, qx - raio); x <= Math.min(planta.largura() - 1, qx + raio); x++) {
                if ((x - qx) * (x - qx) + (y - qy) * (y - qy) > raio * raio + raio) {
                    continue;
                }
                if (escondida(lugar, x, y)) {
                    revelar(mundo, lugar, x, y);
                    achou++;
                }
                if (planta.temArmadilha(x, y) && Armadilhas.revelar(mundo, Armadilhas.controle(origem, x, y))) {
                    achou++;
                }
            }
        }
        if (achou > 0) {
            mundo.playSound(null, BlockPos.containing(centro), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F);
        }
        return achou;
    }

    // Tem armadilha escondida a até "distancia" quadradinhos, à vista? (o talismã da vidência deixa o herói apreensivo)
    public static boolean armadilhaEscondidaPerto(ServerPlayer jogador, int distancia) {
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            return false;
        }
        for (int dy = -distancia; dy <= distancia; dy++) {
            for (int dx = -distancia; dx <= distancia; dx++) {
                int x = lugar.qx() + dx;
                int y = lugar.qy() + dy;
                if (lugar.planta().temArmadilha(x, y) && Armadilhas.escondidaEm(jogador.serverLevel(), lugar.origem(), x, y)
                        && enxerga(lugar.planta(), lugar.qx(), lugar.qy(), x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- Talismã da vidência ----

    // Os quadradinhos que cada herói já viu com o talismã, por andar (pra não ganhar experiência duas vezes no mesmo)
    private static Map<UUID, Map<Integer, Set<Long>>> vistosPelaVidencia = new HashMap<>();

    // O que a vidência viu: as portas e armadilhas reveladas, quantos quadradinhos novos que o herói não via
    // (atrás de portas ou longe, a mais de 8 quadradinhos), e os quadradinhos do cone (pra achar os inimigos neles)
    public record Videncia(int portas, int armadilhas, int novos, Set<Long> quadradinhos, BlockPos origem) {
        public boolean contem(double x, double z) {
            return quadradinhos.contains(chave(Math.floorDiv(Mth.floor(x) - origem.getX(), T), Math.floorDiv(Mth.floor(z) - origem.getZ(), T)));
        }
    }

    private static long chave(int qx, int qy) {
        return ((long) qy << 32) | (qx & 0xFFFFFFFFL);
    }

    // Um cone saindo do herói pra direção (no chão), até a distância (em quadradinhos), com a abertura em graus.
    // Igual ao Shattered, o cone para nas paredes (mas vê a parede: é assim que acha as portas escondidas) e passa pelas portas.
    @Nullable
    public static Videncia vidente(ServerPlayer jogador, Vec3 direcao, float distancia, float abertura) {
        Lugar lugar = ondeEsta(jogador);
        if (lugar == null) {
            return null;
        }
        Set<Long> vistos = vistosPelaVidencia.computeIfAbsent(jogador.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(lugar.run().numeroDaRun() * 100 + lugar.andar(), andar -> new HashSet<>());
        double cx = (jogador.getX() - lugar.origem().getX()) / T;
        double cy = (jogador.getZ() - lugar.origem().getZ()) / T;
        double angulo = Math.atan2(direcao.z, direcao.x);
        int raio = Mth.ceil(distancia);
        Set<Long> quadradinhos = new HashSet<>();
        int portas = 0;
        int armadilhas = 0;
        int novos = 0;
        ServerLevel mundo = jogador.serverLevel();
        for (int dy = -raio; dy <= raio; dy++) {
            for (int dx = -raio; dx <= raio; dx++) {
                int x = lugar.qx() + dx;
                int y = lugar.qy() + dy;
                double vx = x + 0.5 - cx;
                double vy = y + 0.5 - cy;
                double longe = Math.sqrt(vx * vx + vy * vy);
                if (longe > distancia + 0.5) {
                    continue;
                }
                if (longe > 0.75 && abertura < 360) {
                    double diferenca = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(vy, vx) - angulo)));
                    if (diferenca > abertura / 2) {
                        continue;
                    }
                }
                if (!enxerga(lugar.planta(), lugar.qx(), lugar.qy(), x, y, true)) {
                    continue;
                }
                quadradinhos.add(chave(x, y));
                if (lugar.planta().temArmadilha(x, y) && Armadilhas.revelar(mundo, Armadilhas.controle(lugar.origem(), x, y))) {
                    armadilhas++;
                }
                if (escondida(lugar, x, y)) {
                    revelar(mundo, lugar, x, y);
                    portas++;
                } else if (lugar.planta().aberto(x, y)) {
                    boolean naoVia = longe > 8 || !enxerga(lugar.planta(), lugar.qx(), lugar.qy(), x, y, false);
                    if (naoVia && vistos.add(chave(x, y))) {
                        novos++;
                    }
                    BlockPos meio = ConstrutorDeAndar.pontoDeChegada(lugar.origem(), x, y);
                    mundo.sendParticles(ParticleTypes.ENCHANT, meio.getX(), meio.getY() + 0.2, meio.getZ(), 3, 0.8, 0.1, 0.8, 0.0);
                }
            }
        }
        if (portas + armadilhas > 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.percebeu_algo").withStyle(ChatFormatting.GOLD), false);
            mundo.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F);
        }
        return new Videncia(portas, armadilhas, novos, quadradinhos, lugar.origem());
    }

    // ---- Previsão do ladino ----

    // Chamado sempre que o herói chega num andar. Devolve se avisou.
    public static boolean aoChegarNoAndar(ServerPlayer jogador, int andar) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos = dados.pontos(Talento.PREVISAO_DO_LADINO);
        if (pontos <= 0) {
            return false;
        }
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        // (no andar com segredos, só avisa se tiver pelo menos 2, igual ao Shattered)
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        if (planta.salasSecretas() < (planta.tema() == TemaDoAndar.Tema.SEGREDOS ? 2 : 1)) {
            return false;
        }
        // Igual ao Shattered, o sorteio usa a semente do andar: o resultado é sempre o mesmo naquele andar
        Random sorteio = new Random(run.semente() * 31 + andar + 1);
        if (sorteio.nextInt(4) >= 2 + pontos) {
            return false;
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.sala_secreta_no_andar").withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    // ---- Ajudantes ----

    @Nullable
    private static Lugar ondeEsta(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Integer andar = PopulacaoDoAndar.andarEm(run, jogador.getX(), jogador.getY(), jogador.getZ());
        if (andar == null || run.andar(andar) == null) {
            return null;
        }
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        int qx = Math.floorDiv(Mth.floor(jogador.getX()) - origem.getX(), T);
        int qy = Math.floorDiv(Mth.floor(jogador.getZ()) - origem.getZ(), T);
        return new Lugar(run, andar, PopulacaoDoAndar.planta(run.semente(), andar), origem, qx, qy);
    }

    // Uma porta escondida que ninguém achou ainda
    private static boolean escondida(Lugar lugar, int x, int y) {
        return lugar.planta().tipo(x, y) == PlantaDoAndar.PORTA_ESCONDIDA && !lugar.run().portaAchada(lugar.andar(), x, y);
    }

    private static int procurarEmVolta(ServerPlayer jogador, Lugar lugar, boolean deProposito) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos = dados.pontos(Talento.GRANDE_BUSCA);
        int distancia = dados.classe() == ClasseDoHeroi.LADINO ? 2 : 1;
        if (pontos > 0) {
            distancia++;
        }
        // Igual ao Shattered, o "círculo" é o quadrado sem os 4 cantos
        boolean circular = pontos == 1;
        float chance = deProposito ? 1.0F : 0.2F - lugar.andar() / 100F;
        float chanceDaArmadilha = deProposito ? 1.0F : 0.4F - lugar.andar() / 250F;
        ServerLevel mundo = jogador.serverLevel();
        int achou = 0;
        int armadilhas = 0;
        for (int dy = -distancia; dy <= distancia; dy++) {
            for (int dx = -distancia; dx <= distancia; dx++) {
                if ((dx == 0 && dy == 0) || (circular && Math.abs(dx) == distancia && Math.abs(dy) == distancia)) {
                    continue;
                }
                int x = lugar.qx() + dx;
                int y = lugar.qy() + dy;
                if (!enxerga(lugar.planta(), lugar.qx(), lugar.qy(), x, y, false)) {
                    continue;
                }
                if (deProposito && lugar.planta().aberto(x, y)) {
                    // Cada quadradinho olhado brilha um pouco
                    BlockPos meio = ConstrutorDeAndar.pontoDeChegada(lugar.origem(), x, y);
                    mundo.sendParticles(ParticleTypes.ENCHANT, meio.getX(), meio.getY() + 0.2, meio.getZ(), 4, 0.8, 0.1, 0.8, 0.0);
                }
                if (escondida(lugar, x, y) && jogador.getRandom().nextFloat() < chance) {
                    revelar(mundo, lugar, x, y);
                    achou++;
                }
                if (lugar.planta().temArmadilha(x, y) && Armadilhas.escondidaEm(mundo, lugar.origem(), x, y)
                        && jogador.getRandom().nextFloat() < chanceDaArmadilha) {
                    BlockPos controle = Armadilhas.controle(lugar.origem(), x, y);
                    Armadilhas.revelar(mundo, controle);
                    BlockPos meio = BlockPos.containing(Armadilhas.centro(controle));
                    mundo.sendParticles(ParticleTypes.END_ROD, meio.getX(), meio.getY() + 0.3, meio.getZ(), 12, 1.0, 0.2, 1.0, 0.01);
                    armadilhas++;
                }
            }
        }
        if (achou + armadilhas > 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.percebeu_algo").withStyle(ChatFormatting.GOLD), false);
            mundo.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F);
            // O talismã da vidência equipado ganha 10 de carga por porta e 2 por armadilha achada
            TalismaDaVidencia.aoAcharSegredos(jogador, achou, armadilhas);
        }
        return achou + armadilhas;
    }

    // Dá pra ver o quadradinho daqui? (a linha entre os dois não pode passar por parede, estante ou porta;
    // a vidência passa pelas portas comuns, mas não pelas trancadas)
    private static boolean enxerga(PlantaDoAndar planta, int x0, int y0, int x1, int y1, boolean atravessaPortas) {
        int passos = 8 * Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 1; i < passos; i++) {
            double t = i / (double) passos;
            int x = Mth.floor(x0 + 0.5 + (x1 - x0) * t);
            int y = Mth.floor(y0 + 0.5 + (y1 - y0) * t);
            if ((x == x0 && y == y0) || (x == x1 && y == y1)) {
                continue;
            }
            byte tipo = planta.tipo(x, y);
            boolean bloqueia = !planta.aberto(x, y) || tipo == PlantaDoAndar.ESTANTE || tipo == PlantaDoAndar.PORTA_TRANCADA
                    || (tipo == PlantaDoAndar.PORTA && !atravessaPortas);
            if (bloqueia) {
                return false;
            }
        }
        return true;
    }

    // A parede vira uma porta comum (pra todo mundo), e isso fica salvo
    private static void revelar(ServerLevel mundo, Lugar lugar, int x, int y) {
        lugar.planta().definir(x, y, PlantaDoAndar.PORTA);
        lugar.run().acharPorta(lugar.andar(), x, y);
        ConstrutorDeAndar.revelarPorta(mundo, lugar.planta(), lugar.origem(), x, y);
        BlockPos meio = ConstrutorDeAndar.pontoDeChegada(lugar.origem(), x, y);
        mundo.sendParticles(ParticleTypes.END_ROD, meio.getX(), meio.getY() + 2.5, meio.getZ(), 30, 1.0, 1.5, 1.0, 0.02);
    }
}
