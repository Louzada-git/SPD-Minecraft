package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.run.DadosDaRun;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Os inimigos de cada andar, com as regras do Shattered:
//   - quando o andar é criado, nascem vários inimigos dormindo (8 no andar 1)
//   - a cada 50 turnos (50 s), se o andar tiver menos inimigos que o limite, nasce mais um longe dos heróis
//   - no andar 1 não nasce mais nenhum depois do começo
//   - quanto mais jogadores, mais vida os inimigos têm (+50% por jogador a mais)
public class PopulacaoDoAndar {
    private static final int TICKS_PARA_RENASCER = 50 * 20;
    private static final double DISTANCIA_MINIMA_DOS_HEROIS = 24;
    private static final int RAIO_LIVRE_EM_VOLTA_DA_ENTRADA = 3;

    private static int contadorDeTicks = 0;
    // Plantas já geradas (gerar de novo sai igual, mas assim é mais rápido)
    private static Map<String, PlantaDoAndar> plantas = new HashMap<>();

    // ---- Quantidade e tipos ----

    public static int limite(long semente, int andar) {
        if (andar <= 1) {
            return 0;
        }
        Random sorteio = new Random(semente * 7919 + andar);
        return 3 + andar % 5 + sorteio.nextInt(3);
    }

    public static int quantidadeInicial(long semente, int andar) {
        return andar == 1 ? 8 : limite(semente, andar);
    }

    // Quem aparece em cada andar (igual ao Shattered)
    private static List<EntityType<? extends MobDoShattered>> rotacao(int andar) {
        EntityType<? extends MobDoShattered> rato = ModEntidades.RATO_MARSUPIAL.get();
        EntityType<? extends MobDoShattered> cobra = ModEntidades.COBRA_DE_ESGOTO.get();
        EntityType<? extends MobDoShattered> gnoll = ModEntidades.GNOLL_EXPLORADOR.get();
        EntityType<? extends MobDoShattered> caranguejo = ModEntidades.CARANGUEJO_DE_ESGOTO.get();
        EntityType<? extends MobDoShattered> slime = ModEntidades.SLIME.get();
        EntityType<? extends MobDoShattered> enxame = ModEntidades.ENXAME_DE_MOSCAS.get();
        return switch (andar) {
            case 1 -> List.of(rato, rato, rato, cobra);
            case 2 -> List.of(rato, rato, cobra, gnoll, gnoll);
            case 3 -> List.of(rato, cobra, gnoll, gnoll, gnoll, enxame, caranguejo);
            default -> List.of(gnoll, enxame, caranguejo, caranguejo, slime, slime);
        };
    }

    // Igual ao Shattered: 1 em cada 50 ratos é um rato albino
    private static EntityType<? extends MobDoShattered> talvezRaro(EntityType<? extends MobDoShattered> tipo, Random sorteio) {
        if (tipo == ModEntidades.RATO_MARSUPIAL.get() && sorteio.nextInt(50) == 0) {
            return ModEntidades.RATO_ALBINO.get();
        }
        return tipo;
    }

    // ---- Começo do andar ----

    public static void povoarNoInicio(ServerLevel mundo, PlantaDoAndar planta, BlockPos origem, int andar, long semente) {
        if (Andares.ehAndarDeChefe(andar)) {
            return;
        }
        Random sorteio = new Random(semente * 31 + andar * 7L);
        List<int[]> lugares = lugaresLivres(planta);
        List<EntityType<? extends MobDoShattered>> fila = new ArrayList<>();
        int quantidade = quantidadeInicial(semente, andar);
        int criados = 0;

        for (int i = 0; i < quantidade && !lugares.isEmpty(); i++) {
            // Igual ao Shattered: pega da rotação embaralhada; quando acaba, embaralha de novo
            if (fila.isEmpty()) {
                fila.addAll(rotacao(andar));
                Collections.shuffle(fila, sorteio);
            }
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            MobDoShattered inimigo = criar(mundo, talvezRaro(fila.remove(0), sorteio), origem, lugar);
            if (inimigo != null) {
                inimigo.dormir();
                criados++;
            }
        }
        PixelDungeon.LOGGER.info("Andar {}: {} inimigos", andar, criados);
    }

    // ---- Renascimento ----

    public static void aoTickDoServidor(ServerTickEvent.Post evento) {
        contadorDeTicks++;
        if (contadorDeTicks % TICKS_PARA_RENASCER != 0) {
            return;
        }
        MinecraftServer servidor = evento.getServer();
        DadosDaRun dados = DadosDaRun.pegar(servidor);
        if (!dados.ativa()) {
            return;
        }

        // Só renasce em andares onde tem herói vivo
        Set<Integer> andaresComHerois = new HashSet<>();
        List<ServerPlayer> herois = new ArrayList<>();
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            Integer andar = andarDoJogador(dados, jogador);
            if (andar != null && GerenciadorDaRun.estaVivoNaRun(jogador)) {
                andaresComHerois.add(andar);
                herois.add(jogador);
            }
        }
        for (int andar : andaresComHerois) {
            renascerUm(servidor.overworld(), dados, andar, herois);
        }
    }

    private static void renascerUm(ServerLevel mundo, DadosDaRun dados, int andar, List<ServerPlayer> herois) {
        int limite = limite(dados.semente(), andar);
        if (limite <= 0 || Andares.ehAndarDeChefe(andar)) {
            return;
        }
        BlockPos origem = Andares.origemDoAndar(dados.numeroDaRun(), andar);
        int vivos = mundo.getEntitiesOfClass(MobDoShattered.class, caixaDoAndar(origem)).size();
        if (vivos >= limite) {
            return;
        }

        PlantaDoAndar planta = planta(dados.semente(), andar);
        List<int[]> lugares = new ArrayList<>();
        for (int[] lugar : lugaresLivres(planta)) {
            BlockPos posicao = ConstrutorDeAndar.pontoDeChegada(origem, lugar[0], lugar[1]);
            boolean longeDeTodos = true;
            for (ServerPlayer heroi : herois) {
                if (heroi.position().distanceTo(posicao.getCenter()) < DISTANCIA_MINIMA_DOS_HEROIS) {
                    longeDeTodos = false;
                    break;
                }
            }
            if (longeDeTodos) {
                lugares.add(lugar);
            }
        }
        if (lugares.isEmpty()) {
            return;
        }
        Random sorteio = new Random();
        List<EntityType<? extends MobDoShattered>> tipos = rotacao(andar);
        // Quem renasce já nasce acordado, andando pelo andar
        criar(mundo, talvezRaro(tipos.get(sorteio.nextInt(tipos.size())), sorteio), origem, lugares.get(sorteio.nextInt(lugares.size())));
    }

    // ---- Ajudantes ----

    @Nullable
    private static MobDoShattered criar(ServerLevel mundo, EntityType<? extends MobDoShattered> tipo, BlockPos origem, int[] lugar) {
        MobDoShattered inimigo = tipo.create(mundo);
        if (inimigo == null) {
            return null;
        }
        BlockPos posicao = ConstrutorDeAndar.pontoDeChegada(origem, lugar[0], lugar[1]);
        inimigo.moveTo(posicao.getX() + 0.5, posicao.getY(), posicao.getZ() + 0.5, mundo.getRandom().nextFloat() * 360.0F, 0.0F);
        inimigo.ajustarVida(mundo, multiplicadorDeVida(mundo.getServer()));
        inimigo.setPersistenceRequired();
        mundo.addFreshEntity(inimigo);
        return inimigo;
    }

    // +50% de vida por jogador vivo a mais na run
    private static double multiplicadorDeVida(MinecraftServer servidor) {
        int herois = 0;
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            if (GerenciadorDaRun.estaVivoNaRun(jogador)) {
                herois++;
            }
        }
        return 1.0 + 0.5 * Math.max(0, herois - 1);
    }

    // Quadradinhos de chão (sem água, sem escada) que não ficam colados na entrada
    private static List<int[]> lugaresLivres(PlantaDoAndar planta) {
        List<int[]> lugares = new ArrayList<>();
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                if (planta.tipo(qx, qy) != PlantaDoAndar.CHAO) {
                    continue;
                }
                boolean pertoDaEntrada = Math.abs(qx - planta.entradaX()) <= RAIO_LIVRE_EM_VOLTA_DA_ENTRADA
                        && Math.abs(qy - planta.entradaY()) <= RAIO_LIVRE_EM_VOLTA_DA_ENTRADA;
                if (!pertoDaEntrada) {
                    lugares.add(new int[] { qx, qy });
                }
            }
        }
        return lugares;
    }

    private static PlantaDoAndar planta(long semente, int andar) {
        return plantas.computeIfAbsent(semente + ":" + andar, chave -> GeradorDeAndar.gerar(semente, andar));
    }

    private static AABB caixaDoAndar(BlockPos origem) {
        int tamanho = Andares.TAMANHO_DA_PLANTA * Andares.TAMANHO_DO_QUADRADINHO;
        return new AABB(origem.getX(), origem.getY() - 1, origem.getZ(),
                origem.getX() + tamanho, origem.getY() + Andares.ALTURA_DA_PAREDE + 2, origem.getZ() + tamanho);
    }

    // Em que andar da run atual o jogador está (ou null se está fora da dungeon dessa run)
    @Nullable
    private static Integer andarDoJogador(DadosDaRun dados, ServerPlayer jogador) {
        BlockPos origem = Andares.origemDoAndar(dados.numeroDaRun(), 1);
        int tamanho = Andares.TAMANHO_DA_PLANTA * Andares.TAMANHO_DO_QUADRADINHO;
        boolean dentro = jogador.getX() >= origem.getX() && jogador.getX() < origem.getX() + tamanho
                && jogador.getZ() >= origem.getZ() && jogador.getZ() < origem.getZ() + tamanho;
        if (!dentro) {
            return null;
        }
        int andar = Andares.andarPelaAltura((int) Math.floor(jogador.getY()));
        if (andar < 1 || andar > Andares.ULTIMO_ANDAR_LIBERADO) {
            return null;
        }
        return andar;
    }
}
