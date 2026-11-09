package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.entidade.FantasmaTriste;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.Glifo;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.rede.PacoteDoFantasma;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// A missão do fantasma triste, igual ao Shattered:
//   - o fantasma aparece na sala da saída de um andar do esgoto: 1 em 3 no andar 2, 1 em 2 no 3, e com certeza no 4
//     (se não apareceu antes). Só um por run.
//   - conversando com ele, nasce num canto do andar o inimigo que o matou: o rato fétido (andar 2), o gnoll trapaceiro (3)
//     ou o caranguejo grande (4). Ele vaga pelo andar indo pro lado do herói.
//   - morto o inimigo (naquele andar), o fantasma agradece; conversando de novo, dá pra escolher a recompensa:
//     uma arma ou uma armadura do tier 2 a 5 (50%, 30%, 15%, 5%), +0 a +3 (50%, 30%, 15%, 5%), e 20% de chance de vir
//     com um encantamento (na arma) ou um glifo (na armadura). Escolhida a recompensa, o fantasma vai embora.
//   - completar a missão deixa a rosa seca funcionar (ver RosaSeca), e as pétalas dela começam a aparecer pelos andares
// O que se sabe da missão fica salvo nos dados da run.
public class MissaoDoFantasma {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    // Onde o inimigo da missão pode nascer: longe dos heróis
    private static final double LONGE_DOS_HEROIS = 24;
    private static final double PERTO_DO_FANTASMA = 16;

    // ---- O estado da missão ----

    public static boolean apareceu(DadosDaRun run) {
        return run.fantasma().getBoolean("apareceu");
    }

    public static boolean dada(DadosDaRun run) {
        return run.fantasma().getBoolean("dada");
    }

    public static boolean cumprida(DadosDaRun run) {
        return run.fantasma().getBoolean("cumprida");
    }

    // Recompensa escolhida: o fantasma foi embora (a rosa seca já funciona)
    public static boolean completa(DadosDaRun run) {
        return run.fantasma().getBoolean("completa");
    }

    // 1 = rato fétido, 2 = gnoll trapaceiro, 3 = caranguejo grande
    public static int tipo(DadosDaRun run) {
        return run.fantasma().getInt("tipo");
    }

    public static int andar(DadosDaRun run) {
        return run.fantasma().getInt("andar");
    }

    private static String nomeDoTipo(int tipo) {
        return switch (tipo) {
            case 2 -> "gnoll";
            case 3 -> "caranguejo";
            default -> "rato";
        };
    }

    // ---- Aparecer (quando o andar é construído) ----

    public static void talvezCriar(ServerLevel mundo, PlantaDoAndar planta, BlockPos origem, int andar, DadosDaRun run) {
        if (andar < 2 || andar > 4 || apareceu(run) || !planta.temSaida()) {
            return;
        }
        // Igual ao Shattered: 1 em (5 - andar). A semente da run decide.
        Random sorteio = new Random(run.semente() * 97 + andar * 13L);
        if (sorteio.nextInt(5 - andar) != 0) {
            return;
        }
        BlockPos lugar = lugarNaSalaDaSaida(planta, origem, sorteio);
        FantasmaTriste fantasma = ModEntidades.FANTASMA_TRISTE.get().create(mundo);
        if (lugar == null || fantasma == null) {
            return;
        }
        fantasma.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, sorteio.nextFloat() * 360.0F, 0.0F);
        Sala sala = planta.salaEm(planta.saidaX(), planta.saidaY());
        int raio = sala == null ? 8 : Math.max(sala.largura(), sala.altura()) * T / 2;
        fantasma.restrictTo(lugar, raio);
        mundo.addFreshEntity(fantasma);

        CompoundTag missao = run.fantasma();
        missao.putBoolean("apareceu", true);
        missao.putInt("tipo", andar - 1);
        missao.putInt("andar", andar);
        sortearRecompensas(missao, sorteio, mundo.registryAccess());
        run.mudouOFantasma();
        PixelDungeon.LOGGER.info("Andar {}: o fantasma triste apareceu (missão {})", andar, nomeDoTipo(andar - 1));
    }

    @Nullable
    private static BlockPos lugarNaSalaDaSaida(PlantaDoAndar planta, BlockPos origem, Random sorteio) {
        Sala sala = planta.salaEm(planta.saidaX(), planta.saidaY());
        List<int[]> lugares = new ArrayList<>();
        if (sala != null) {
            for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
                for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                    if (planta.tipo(x, y) == PlantaDoAndar.CHAO && (x != planta.saidaX() || y != planta.saidaY())) {
                        lugares.add(new int[] { x, y });
                    }
                }
            }
        }
        if (lugares.isEmpty()) {
            return ConstrutorDeAndar.pontoDeChegada(origem, planta.saidaX(), planta.saidaY());
        }
        int[] lugar = lugares.get(sorteio.nextInt(lugares.size()));
        return ConstrutorDeAndar.pontoDeChegada(origem, lugar[0], lugar[1]);
    }

    // As duas recompensas (arma e armadura), sorteadas quando o fantasma aparece, igual ao Shattered
    private static void sortearRecompensas(CompoundTag missao, Random sorteio, HolderLookup.Provider registros) {
        float[] tiers = { 10, 6, 3, 1 };
        int tierDaArmadura = 2 + SaqueDoAndar.sortearPeso(sorteio, tiers);
        int tierDaArma = 2 + SaqueDoAndar.sortearPeso(sorteio, tiers);
        ItemStack arma = SaqueDoAndar.armaDoTier(sorteio, tierDaArma);
        ItemStack armadura = ItemStack.EMPTY;
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaduraDoShattered tipo && tipo.tier() == tierDaArmadura) {
                armadura = new ItemStack(tipo);
                break;
            }
        }
        float rolagem = sorteio.nextFloat();
        int nivel = rolagem < 0.5F ? 0 : rolagem < 0.8F ? 1 : rolagem < 0.95F ? 2 : 3;
        NivelDoItem.definirNivel(arma, nivel);
        NivelDoItem.definirNivel(armadura, nivel);
        // O encantamento e o glifo são sorteados sempre (pra não mudar o resto do sorteio), mas só ficam 20% das vezes
        Encantamento encantamento = Encantamento.sortear(sorteio);
        Glifo glifo = Glifo.sortear(sorteio);
        if (sorteio.nextFloat() < 0.2F) {
            missao.putString("encantamento", encantamento.id());
            missao.putString("glifo", glifo.id());
        }
        missao.put("arma", arma.save(registros));
        if (!armadura.isEmpty()) {
            missao.put("armadura", armadura.save(registros));
        }
    }

    public static ItemStack arma(DadosDaRun run, HolderLookup.Provider registros) {
        return ItemStack.parseOptional(registros, run.fantasma().getCompound("arma"));
    }

    public static ItemStack armadura(DadosDaRun run, HolderLookup.Provider registros) {
        return ItemStack.parseOptional(registros, run.fantasma().getCompound("armadura"));
    }

    // ---- Conversar ----

    public static void conversar(ServerPlayer jogador, FantasmaTriste fantasma) {
        ServerLevel mundo = jogador.serverLevel();
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        mundo.playSound(null, fantasma.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1.0F, 0.6F);
        if (completa(run)) {
            return;
        }
        String tipo = nomeDoTipo(tipo(run));
        if (!dada(run)) {
            // A primeira conversa: conta a história e o inimigo aparece num canto do andar
            if (criarOInimigo(mundo, run, fantasma) == null) {
                return;
            }
            run.fantasma().putBoolean("dada", true);
            run.mudouOFantasma();
            Component nome = Component.translatable(RegrasDoHeroi.dados(jogador).classe().chaveDoNome());
            mostrar(jogador, Component.translatable("fantasma.pixeldungeon." + tipo + "_1", nome), ItemStack.EMPTY, ItemStack.EMPTY);
        } else if (!cumprida(run)) {
            mostrar(jogador, Component.translatable("fantasma.pixeldungeon." + tipo + "_2"), ItemStack.EMPTY, ItemStack.EMPTY);
        } else {
            // Cumprida: escolher a recompensa
            Component texto = Component.translatable("fantasma.pixeldungeon." + tipo + "_obrigado").append("\n\n")
                    .append(Component.translatable("fantasma.pixeldungeon.recompensa"));
            mostrar(jogador, texto, arma(run, mundo.registryAccess()), armadura(run, mundo.registryAccess()));
        }
    }

    private static void mostrar(ServerPlayer jogador, Component texto, ItemStack arma, ItemStack armadura) {
        PacketDistributor.sendToPlayer(jogador, new PacoteDoFantasma(texto, arma, armadura));
    }

    // O inimigo da missão nasce acordado num lugar livre do andar, longe dos heróis
    @Nullable
    public static MobDoShattered criarOInimigo(ServerLevel mundo, DadosDaRun run, FantasmaTriste fantasma) {
        EntityType<? extends MobDoShattered> tipo = switch (tipo(run)) {
            case 2 -> ModEntidades.GNOLL_TRAPACEIRO.get();
            case 3 -> ModEntidades.CARANGUEJO_GRANDE.get();
            default -> ModEntidades.RATO_FETIDO.get();
        };
        MobDoShattered inimigo = tipo.create(mundo);
        if (inimigo == null) {
            return null;
        }
        int andar = andar(run);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        List<BlockPos> longe = new ArrayList<>();
        List<BlockPos> todos = new ArrayList<>();
        for (int[] lugar : PopulacaoDoAndar.lugaresLivres(planta)) {
            BlockPos posicao = ConstrutorDeAndar.pontoDeChegada(origem, lugar[0], lugar[1]);
            todos.add(posicao);
            boolean longeDeTodos = posicao.getCenter().distanceTo(fantasma.position()) >= LONGE_DOS_HEROIS;
            for (ServerPlayer heroi : mundo.players()) {
                if (heroi.position().distanceTo(posicao.getCenter()) < LONGE_DOS_HEROIS) {
                    longeDeTodos = false;
                }
            }
            if (longeDeTodos) {
                longe.add(posicao);
            }
        }
        List<BlockPos> opcoes = longe.isEmpty() ? todos : longe;
        BlockPos lugar = opcoes.isEmpty() ? fantasma.blockPosition() : opcoes.get(mundo.getRandom().nextInt(opcoes.size()));
        inimigo.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, mundo.getRandom().nextFloat() * 360.0F, 0.0F);
        inimigo.ajustarVida(mundo, PopulacaoDoAndar.multiplicadorDeVida(mundo.getServer()));
        inimigo.setPersistenceRequired();
        mundo.addFreshEntity(inimigo);
        return inimigo;
    }

    // ---- O inimigo morreu ----

    public static void aoMorrerOChefe(MobDoShattered chefe) {
        if (!(chefe.level() instanceof ServerLevel mundo)) {
            return;
        }
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        int andar = Andares.andarPelaAltura(chefe.getBlockY());
        if (!dada(run) || cumprida(run) || andar != andar(run)) {
            return;
        }
        run.fantasma().putBoolean("cumprida", true);
        run.mudouOFantasma();
        // Igual ao Shattered: "Obrigado... venha me encontrar..."
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        for (ServerPlayer heroi : mundo.players()) {
            if (PopulacaoDoAndar.caixaDoAndar(origem).contains(heroi.position())) {
                heroi.displayClientMessage(Component.translatable("fantasma.pixeldungeon.venha_me_encontrar").withStyle(ChatFormatting.RED), false);
                mundo.playSound(null, heroi.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1.0F, 0.5F);
            }
        }
    }

    // ---- A recompensa ----

    // qual: 0 = arma, 1 = armadura. O herói tem que estar perto do fantasma.
    public static boolean escolherRecompensa(ServerPlayer jogador, int qual) {
        ServerLevel mundo = jogador.serverLevel();
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        if (!cumprida(run) || completa(run)) {
            return false;
        }
        List<FantasmaTriste> perto = mundo.getEntitiesOfClass(FantasmaTriste.class, jogador.getBoundingBox().inflate(PERTO_DO_FANTASMA));
        if (perto.isEmpty()) {
            return false;
        }
        ItemStack recompensa = qual == 0 ? arma(run, mundo.registryAccess()) : armadura(run, mundo.registryAccess());
        if (recompensa.isEmpty()) {
            return false;
        }
        CompoundTag missao = run.fantasma();
        if (qual == 0 && missao.contains("encantamento")) {
            NivelDoItem.definirEncantamento(recompensa, Encantamento.peloId(missao.getString("encantamento")));
        } else if (qual == 1 && missao.contains("glifo")) {
            NivelDoItem.definirGlifo(recompensa, Glifo.peloId(missao.getString("glifo")));
        }
        NivelDoItem.identificar(null, recompensa);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.agora_voce_tem", recompensa.getHoverName()).withStyle(ChatFormatting.GREEN),
                false);
        if (!jogador.getInventory().add(recompensa)) {
            jogador.drop(recompensa, false);
        }
        missao.putBoolean("completa", true);
        missao.remove("arma");
        missao.remove("armadura");
        run.mudouOFantasma();
        // O fantasma se despede e some
        FantasmaTriste fantasma = perto.get(0);
        jogador.displayClientMessage(Component.translatable("fantasma.pixeldungeon.adeus").withStyle(ChatFormatting.AQUA), false);
        Particulas.explosao(mundo, ParticleTypes.SOUL, fantasma.getX(), fantasma.getY() + 0.8, fantasma.getZ(), 20, 0.4, 0.05);
        mundo.playSound(null, fantasma.blockPosition(), SoundEvents.ALLAY_DEATH, SoundSource.NEUTRAL, 1.0F, 0.6F);
        fantasma.discard();
        return true;
    }
}
