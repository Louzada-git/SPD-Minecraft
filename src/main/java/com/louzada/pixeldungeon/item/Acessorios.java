package com.louzada.pixeldungeon.item;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// O que cada acessório faz, com os números do Shattered (nível -1 = ninguém tem).
// Os que mexem no herói valem pra quem carrega. Os que mexem na masmorra (os andares, os inimigos, os itens achados)
// valem pra run toda se algum herói carregar (vale o maior nível): é o "da run".
//   crânio de rato: inimigos raros (o rato albino) 2x a 5x mais comuns
//   pedaço de papiro: encantamentos e glifos 2x a 10x mais comuns; maldições em armas e armaduras 1,5x, 2x, 1x, 0x
//   semente petrificada: a grama pisoteada solta pedra rúnica em vez de semente (25% a 80%) e solta mais coisas
//   cristais exóticos: 20% a 80% das poções e pergaminhos achados vêm exóticos (menos força e melhoria)
//   moita de musgo: 25% a 100% dos andares sem tema (metade deles) ficam cheios de água ou de grama
//   relógio de sol dimensional: inimigos renascem 5% a 20% menos de dia (8h às 20h, no relógio de verdade) e 25% a 100% mais de noite
//   trevo de treze folhas: 25% a 100% dos golpes do herói saem com o dano máximo (60%) ou o mínimo (40%)
//   mecanismo de armadilha: 25% a 100% dos andares sem tema ficam cheios de armadilhas (5x, as extras à vista);
//     10% a 40% das armadilhas escondidas ficam à vista (o mod ainda não tem abismos, então é sempre armadilha)
//   dente de mímico: mímicos 1,5x a 3x mais comuns
//   resina maravilhosa: o disparo amaldiçoado das varinhas sai neutro ou bom 25% a 100% das vezes, e as varinhas normais
//     soltam um disparo amaldiçoado bom 12,5% a 50% das vezes
//   olho de salamandra: vê menos longe (12,5% a 50%), mas vê os inimigos a 2 a 5 quadradinhos através das paredes
//   cubo de sal: a fome demora 25% a 100% mais, mas a vida volta 16% a 40% mais devagar
//   frasco de sangue: a poção de cura cura 12,5% a 50% mais, mas devagar (no máximo um tanto por turno)
//   fragmento do esquecimento: os equipamentos não se identificam sozinhos (ficam "prontos" e o fragmento identifica);
//     os inimigos soltam itens 20% mais por equipamento sem identificar no corpo (até 1 a 4)
//   incensário caótico: a cada 300 / (nível + 1) turnos, mais ou menos, solta um gás perto de um inimigo
//   tufo de furão: todo mundo desvia 12,5% a 50% mais
//   luneta trincada: 37,5% a 150% de chance de um item a mais em cada andar
public class Acessorios {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int[] DA_RUN = new int[TipoDeAcessorio.values().length];
    private static final String EXAUSTAO = "pixeldungeon_exaustao_antes";
    private static final String INCENSARIO = "pixeldungeon_incensario_falta";
    // Se a cura que está acontecendo agora é a da fome (a "regeneração" do Minecraft): ver mixin.RegeneracaoDaFome
    private static boolean curaDaFome = false;

    static {
        Arrays.fill(DA_RUN, -1);
    }

    // ---- Níveis ----

    // O nível do acessório no inventário (-1: não tem). Com mais de um igual, vale o maior.
    public static int nivel(Player jogador, TipoDeAcessorio tipo) {
        int nivel = -1;
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack pilha = inventario.getItem(i);
            if (pilha.getItem() instanceof ItemAcessorio acessorio && acessorio.tipo() == tipo) {
                nivel = Math.max(nivel, NivelDoItem.nivel(pilha));
            }
        }
        return nivel;
    }

    // O maior nível entre os heróis no servidor (atualizado a cada segundo, e antes de construir um andar)
    public static int daRun(TipoDeAcessorio tipo) {
        return DA_RUN[tipo.ordinal()];
    }

    // Só pros testes
    public static void definirDaRun(TipoDeAcessorio tipo, int nivel) {
        DA_RUN[tipo.ordinal()] = nivel;
    }

    public static void atualizar(MinecraftServer servidor) {
        int[] niveis = new int[DA_RUN.length];
        Arrays.fill(niveis, -1);
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            if (!GerenciadorDoHeroi.emJogo(jogador)) {
                continue;
            }
            for (TipoDeAcessorio tipo : TipoDeAcessorio.values()) {
                niveis[tipo.ordinal()] = Math.max(niveis[tipo.ordinal()], nivel(jogador, tipo));
            }
        }
        System.arraycopy(niveis, 0, DA_RUN, 0, niveis.length);
    }

    public static void aoTickDoServidor(ServerTickEvent.Post evento) {
        if (evento.getServer().getTickCount() % 20 == 0) {
            atualizar(evento.getServer());
        }
    }

    // ---- Os números (iguais aos do Shattered) ----

    public static float multiplicadorDeRaros(int nivel) {
        return nivel < 0 ? 1 : 2 + nivel;
    }

    public static float multiplicadorDeEncantos(int nivel) {
        return switch (nivel) {
            case 0 -> 2;
            case 1 -> 4;
            case 2 -> 7;
            case 3 -> 10;
            default -> 1;
        };
    }

    public static float multiplicadorDeMaldicoes(int nivel) {
        return switch (nivel) {
            case 0 -> 1.5F;
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 0;
            default -> 1;
        };
    }

    public static float multiplicadorDaGrama(int nivel) {
        return nivel <= 0 ? 1 : 1 + 0.25F * nivel / 3F;
    }

    public static float chanceDePedraNaGrama(int nivel) {
        return switch (nivel) {
            case 0 -> 0.25F;
            case 1 -> 0.46F;
            case 2 -> 0.65F;
            case 3 -> 0.8F;
            default -> 0;
        };
    }

    public static float chanceDeExotico(int nivel) {
        return nivel < 0 ? 0 : 0.2F + 0.2F * nivel;
    }

    // A moita de musgo e o mecanismo de armadilha: a chance de um andar sem tema ganhar o tema deles
    public static float chanceDeTema(int nivel) {
        return nivel < 0 ? 0 : 0.25F + 0.25F * nivel;
    }

    public static float renascerDeDia(int nivel) {
        return nivel < 0 ? 1 : 0.95F - 0.05F * nivel;
    }

    public static float renascerDeNoite(int nivel) {
        return nivel < 0 ? 1 : 1.25F + 0.25F * nivel;
    }

    // Igual ao Shattered: pela hora de verdade (de noite: das 20h às 7h59)
    public static float renascerAgora(int nivel) {
        if (nivel < 0) {
            return 1;
        }
        int hora = LocalTime.now().getHour();
        return hora >= 20 || hora <= 7 ? renascerDeNoite(nivel) : renascerDeDia(nivel);
    }

    public static float chanceDoTrevo(int nivel) {
        return nivel < 0 ? 0 : 0.25F + 0.25F * nivel;
    }

    public static float chanceDeArmadilhaAVista(int nivel) {
        return nivel < 0 ? 0 : 0.1F + 0.1F * nivel;
    }

    public static float multiplicadorDeMimicos(int nivel) {
        return nivel < 0 ? 1 : 1.5F + 0.5F * nivel;
    }

    public static float chanceDeDisparoBom(int nivel) {
        return nivel < 0 ? 0 : 0.25F + 0.25F * nivel;
    }

    public static float chanceDeDisparoExtra(int nivel) {
        return nivel < 0 ? 0 : 0.125F + 0.125F * nivel;
    }

    public static float multiplicadorDaVisao(int nivel) {
        return nivel < 0 ? 1 : 0.875F - 0.125F * nivel;
    }

    public static int alcanceDaVisaoMental(int nivel) {
        return nivel < 0 ? 0 : 2 + nivel;
    }

    public static float multiplicadorDaFome(int nivel) {
        return nivel < 0 ? 1 : 1F / (1F + 0.25F * (nivel + 1));
    }

    public static float multiplicadorDaRegeneracao(int nivel) {
        return switch (nivel) {
            case 0 -> 0.84F;
            case 1 -> 0.73F;
            case 2 -> 0.66F;
            case 3 -> 0.6F;
            default -> 1;
        };
    }

    public static float multiplicadorDaCura(int nivel) {
        return nivel < 0 ? 1 : 1 + 0.125F * (nivel + 1);
    }

    public static int curaPorTurno(int nivel, float vidaMaxima) {
        return switch (nivel) {
            case 0 -> 4 + Math.round(0.15F * vidaMaxima);
            case 1 -> 3 + Math.round(0.10F * vidaMaxima);
            case 2 -> 2 + Math.round(0.07F * vidaMaxima);
            case 3 -> 1 + Math.round(0.05F * vidaMaxima);
            default -> Integer.MAX_VALUE;
        };
    }

    public static int maximoDoFragmento(int nivel) {
        return nivel + 1;
    }

    public static int turnosDoIncensario(int nivel) {
        return nivel < 0 ? -1 : 300 / (nivel + 1);
    }

    public static float multiplicadorDeEsquiva(int nivel) {
        return nivel < 0 ? 1 : 1 + 0.125F * (nivel + 1);
    }

    public static float chanceDaLuneta(int nivel) {
        return nivel < 0 ? 0 : 0.375F * (nivel + 1);
    }

    // ---- Trevo de treze folhas ----

    // O trevo de quem está batendo agora (o HeroiEmCombate marca antes de sortear o dano)
    private static int trevoDoGolpe = -1;

    public static void marcarTrevo(int nivel) {
        trevoDoGolpe = nivel;
    }

    // O dano do herói entre o mínimo e o máximo (Sorteio.normal), ou, com o trevo, às vezes o máximo ou o mínimo
    public static int danoDoHeroi(RandomSource sorteio, int minimo, int maximo) {
        if (trevoDoGolpe >= 0 && sorteio.nextFloat() < chanceDoTrevo(trevoDoGolpe)) {
            return sorteio.nextFloat() < 0.6F ? maximo : minimo;
        }
        return com.louzada.pixeldungeon.combate.Sorteio.normal(sorteio, minimo, maximo);
    }

    // ---- Fragmento do esquecimento ----

    // Quantos equipamentos sem identificar o herói está usando (a mão, a armadura e os anéis)
    public static int desconhecidosNoCorpo(Player jogador) {
        int quantos = 0;
        for (ItemStack pilha : List.of(jogador.getMainHandItem(), jogador.getOffhandItem(), jogador.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                jogador.getItemBySlot(com.louzada.pixeldungeon.heroi.Aneis.PRIMEIRO), jogador.getItemBySlot(com.louzada.pixeldungeon.heroi.Aneis.SEGUNDO))) {
            if (NivelDoItem.podeIdentificar(pilha) && !NivelDoItem.identificado(pilha)) {
                quantos++;
            }
        }
        return quantos;
    }

    public static float multiplicadorDeSaque(Player jogador) {
        int nivel = nivel(jogador, TipoDeAcessorio.FRAGMENTO_DO_ESQUECIMENTO);
        if (nivel < 0) {
            return 1;
        }
        return 1 + 0.2F * Math.min(desconhecidosNoCorpo(jogador), maximoDoFragmento(nivel));
    }

    // ---- O que acontece com o tempo ----

    public static void aoTickDoJogador(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        fome(jogador);
        if (Turno.virou(jogador)) {
            visaoMental(jogador);
            incensario(jogador);
        }
    }

    // Cubo de sal: a exaustão (o que gasta a fome) cresce mais devagar
    public static void fome(ServerPlayer jogador) {
        FoodData comida = jogador.getFoodData();
        CompoundTag dados = jogador.getPersistentData();
        float agora = comida.getExhaustionLevel();
        float antes = dados.contains(EXAUSTAO) ? dados.getFloat(EXAUSTAO) : agora;
        int nivel = nivel(jogador, TipoDeAcessorio.CUBO_DE_SAL);
        if (nivel >= 0 && agora > antes) {
            agora = antes + (agora - antes) * multiplicadorDaFome(nivel);
            comida.setExhaustion(agora);
        }
        dados.putFloat(EXAUSTAO, agora);
    }

    // Cubo de sal: a cura da fome (a regeneração natural) fica mais fraca
    public static void marcarCuraDaFome(boolean sim) {
        curaDaFome = sim;
    }

    public static void aoCurar(LivingHealEvent evento) {
        if (curaDaFome && evento.getEntity() instanceof Player jogador) {
            evento.setAmount(evento.getAmount() * multiplicadorDaRegeneracao(nivel(jogador, TipoDeAcessorio.CUBO_DE_SAL)));
        }
    }

    // Olho de salamandra: os inimigos perto brilham através das paredes
    public static void visaoMental(ServerPlayer jogador) {
        int nivel = nivel(jogador, TipoDeAcessorio.OLHO_DE_SALAMANDRA);
        if (nivel < 0) {
            return;
        }
        double alcance = alcanceDaVisaoMental(nivel) * T + T / 2.0;
        for (MobDoShattered inimigo : jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(alcance))) {
            if (!inimigo.aliado() && inimigo.distanceTo(jogador) <= alcance) {
                inimigo.addEffect(new MobEffectInstance(MobEffects.GLOWING, 2 * Turno.TICKS, 0, false, false));
            }
        }
    }

    // Incensário caótico: conta os turnos e, na hora, solta um gás perto de um inimigo à vista
    public static void incensario(ServerPlayer jogador) {
        CompoundTag dados = jogador.getPersistentData();
        int media = turnosDoIncensario(nivel(jogador, TipoDeAcessorio.INCENSARIO_CAOTICO));
        if (media < 0) {
            dados.remove(INCENSARIO);
            return;
        }
        RandomSource sorteio = jogador.getRandom();
        int falta = dados.contains(INCENSARIO) ? dados.getInt(INCENSARIO) : Integer.MAX_VALUE;
        if (falta > media * 1.2F) {
            falta = entre(sorteio, (int) (media * 0.833F), (int) (media * 1.2F));
        }
        falta--;
        if (falta <= 0) {
            LivingEntity alvo = alvoDoIncensario(jogador);
            if (alvo != null && soltarGas(jogador, alvo) != null) {
                falta += entre(sorteio, (int) (media * 0.9F), (int) (media * 1.1F));
            }
        }
        dados.putInt(INCENSARIO, Math.max(falta, (int) (-media / 3F)));
    }

    private static int entre(RandomSource sorteio, int minimo, int maximo) {
        return minimo + sorteio.nextInt(Math.max(1, maximo - minimo + 1));
    }

    // Quem o herói atacou por último, se ainda estiver à vista; senão, o inimigo à vista mais perto
    @Nullable
    public static LivingEntity alvoDoIncensario(ServerPlayer jogador) {
        List<MobDoShattered> visiveis = EfeitosDePergaminho.visiveis(jogador).stream().filter(inimigo -> !inimigo.aliado()).toList();
        if (jogador.getLastHurtMob() instanceof MobDoShattered ultimo && visiveis.contains(ultimo)) {
            return ultimo;
        }
        MobDoShattered maisPerto = null;
        for (MobDoShattered inimigo : visiveis) {
            if (maisPerto == null || inimigo.distanceTo(jogador) < maisPerto.distanceTo(jogador)) {
                maisPerto = inimigo;
            }
        }
        return maisPerto;
    }

    // As chances de cada tipo de gás em cada nível (comum, incomum, raro), iguais às do Shattered
    private static final float[][] CHANCES_DO_GAS = { { 80, 10, 10 }, { 67, 13, 20 }, { 54, 16, 30 }, { 40, 20, 40 } };

    // Solta o gás entre 2 e 6 quadradinhos do herói, o mais perto possível do alvo. Devolve o nome do gás (null: não soltou).
    @Nullable
    public static String soltarGas(ServerPlayer jogador, LivingEntity alvo) {
        int nivel = Math.min(TipoDeAcessorio.NIVEL_MAXIMO, Math.max(0, nivel(jogador, TipoDeAcessorio.INCENSARIO_CAOTICO)));
        ServerLevel mundo = jogador.serverLevel();
        RandomSource sorteio = jogador.getRandom();
        Vec3 deOnde = jogador.position();
        Vec3 direcao = new Vec3(alvo.getX() - deOnde.x, 0, alvo.getZ() - deOnde.z);
        double distancia = direcao.length();
        if (distancia < 0.01) {
            direcao = new Vec3(1, 0, 0);
        }
        Vec3 lugar = deOnde.add(direcao.normalize().scale(Math.max(2 * T, Math.min(6 * T, distancia))));
        float[] chances = CHANCES_DO_GAS[nivel];
        float dado = sorteio.nextFloat() * (chances[0] + chances[1] + chances[2]);
        int categoria = dado < chances[0] ? 0 : dado < chances[0] + chances[1] ? 1 : 2;
        int qual = sorteio.nextInt(3);
        String gas;
        switch (categoria * 3 + qual) {
            case 0 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.TOXICO, 5.0F, 10);
                gas = "toxico";
            }
            case 1 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.CONFUSAO, 5.0F, 10);
                gas = "confusao";
            }
            case 2 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.FEDOR, 4.0F, 10);
                gas = "fedor";
            }
            case 3 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.TEMPESTADE, 5.0F, 10);
                gas = "tempestade";
            }
            case 4 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.NEBLINA, 5.0F, 10);
                gas = "fumaca";
            }
            case 5 -> {
                // A rebrota: a grama cresce em volta
                net.minecraft.core.BlockPos pe = net.minecraft.core.BlockPos.containing(lugar);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Grama.crescer(mundo, (Math.floorDiv(pe.getX(), T) + dx) * T, pe.getY(), (Math.floorDiv(pe.getZ(), T) + dz) * T);
                    }
                }
                gas = "rebrota";
            }
            case 6 -> {
                EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.INFERNAL, lugar, jogador);
                gas = "inferno";
            }
            case 7 -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.CONGELANTE, 5.0F, 10);
                gas = "nevasca";
            }
            default -> {
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.CORROSIVO, 4.0F, 10);
                gas = "corrosivo";
            }
        }
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 0.6F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.incensario",
                Component.translatable("gas.pixeldungeon." + gas)).withStyle(ChatFormatting.YELLOW), false);
        return gas;
    }

    // ---- Frasco de sangue ----

    // A cura total da poção de cura (com o frasco, maior)
    public static int curaDoFrasco(LivingEntity alvo, int quantidade) {
        return alvo instanceof Player jogador ? Math.round(quantidade * multiplicadorDaCura(nivel(jogador, TipoDeAcessorio.FRASCO_DE_SANGUE))) : quantidade;
    }

    // Quanto a poção de cura pode curar por turno (sem o frasco, sem limite)
    public static int limiteDoFrasco(LivingEntity alvo) {
        return alvo instanceof Player jogador ? curaPorTurno(nivel(jogador, TipoDeAcessorio.FRASCO_DE_SANGUE), jogador.getMaxHealth()) : Integer.MAX_VALUE;
    }

    // ---- Cristais exóticos ----

    // Uma poção achada vira exótica? (não a de força)
    public static ItemStack talvezExotica(ItemStack pocao, java.util.Random sorteio, long semente) {
        int nivel = daRun(TipoDeAcessorio.CRISTAIS_EXOTICOS);
        if (nivel < 0 || !(pocao.getItem() instanceof ItemPocao comum)
                || IdentidadesDaRun.tipoDaCor(semente, comum.cor()) == TipoDePocao.FORCA || sorteio.nextFloat() >= chanceDeExotico(nivel)) {
            return pocao;
        }
        return new ItemStack(ItemPocaoExotica.daCor(comum.cor()), pocao.getCount());
    }

    // Um pergaminho achado vira exótico? (não o de melhoria)
    public static ItemStack talvezExotico(ItemStack pergaminho, java.util.Random sorteio, long semente) {
        int nivel = daRun(TipoDeAcessorio.CRISTAIS_EXOTICOS);
        if (nivel < 0 || !(pergaminho.getItem() instanceof ItemPergaminho comum)
                || IdentidadesDaRun.tipoDaRuna(semente, comum.runa()) == TipoDePergaminho.MELHORIA || sorteio.nextFloat() >= chanceDeExotico(nivel)) {
            return pergaminho;
        }
        return new ItemStack(ItemPergaminhoExotico.daRuna(comum.runa()), pergaminho.getCount());
    }
}
