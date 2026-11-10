package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.EventosDeCombate;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.Mira;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// O que cada pergaminho exótico do Shattered faz (1 turno = 2 segundos, 1 quadradinho = 4 blocos):
//   encantamento: escolhe uma arma ou armadura e um de 3 encantamentos (ou glifos): um comum, um incomum e um qualquer
//   adivinhação: o herói descobre 4 tipos de poção, pergaminho ou anel que ainda não conhecia
//   antimagia: 20 turnos imune à magia (não machuca nem pega), mas os glifos e encantamentos do herói param
//   imagem prismática: uma imagem colorida do herói que luta do lado dele até morrer (se já tem uma, ela se cura)
//   energia mística: recarrega os artefatos por 30 turnos
//   passagem: leva o herói pro primeiro andar da região (no esgoto, o andar 1)
//   canto da sereia: o inimigo na mira vira aliado pra sempre (os chefes só ficam encantados); os outros que o herói
//                    vê ficam encantados por 10 turnos
//   vidência: por 400 turnos, o herói acha sozinho as portas e armadilhas escondidas até 8 quadradinhos
//   desafio: todos os inimigos do andar vêm atrás do herói, e em volta dele aparece uma arena (1 a 3 quadradinhos,
//            maior em lugar aberto) por 100 turnos: dentro dela ele toma 67% do dano e não fica com fome
//   explosão psiônica: todos os inimigos à vista tomam metade da vida máxima + metade da que têm (quase sempre morrem)
//                      e ficam cegos; o herói toma metade da vida máxima (menos 10% por inimigo), fica cego e fraco
//   pavor: os inimigos à vista fogem de pavor por 20 turnos, e somem da dungeon se ficarem longe e fora da vista
//          (os chefes só ficam aterrorizados)
//   metamorfose: troca um talento do herói por um de mesmo tier de outra classe (os pontos vão junto)
// Igual ao Shattered, os que pedem uma escolha só são gastos na escolha, se o herói já conhecia o pergaminho.
public class EfeitosDePergaminhoExotico {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    public static final int TURNOS_DA_ANTIMAGIA = 20;
    public static final int TURNOS_DA_ENERGIA_MISTICA = 30;
    public static final int TURNOS_DA_VIDENCIA = 400;
    public static final int DISTANCIA_DA_VIDENCIA = 8;
    public static final int TURNOS_DO_DESAFIO = 100;
    public static final int TURNOS_DO_PAVOR = 20;
    public static final int TURNOS_DO_ENCANTO = 10;
    // No estado do herói: o quadradinho do meio da arena do desafio e o tamanho dela
    public static final String ARENA_X = "arena_x";
    public static final String ARENA_Z = "arena_z";
    public static final String ARENA_RAIO = "arena_raio";

    public static void ler(ServerPlayer jogador, RunaDePergaminho runa) {
        TipoDePergaminhoExotico tipo = TipoDePergaminhoExotico.daRuna(jogador, runa);
        boolean conhecia = Identificacao.conhece(jogador, runa);
        ServerLevel mundo = jogador.serverLevel();
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        // Ler tira a invisibilidade (igual a usar qualquer item no Shattered)
        Furtividade.revelar(jogador);
        switch (tipo) {
            case ENCANTAMENTO -> {
                encantamento(jogador, runa, conhecia);
                return;
            }
            case METAMORFOSE -> {
                metamorfose(jogador, runa, conhecia);
                return;
            }
            case CANTO_DA_SEREIA -> {
                cantoDaSereia(jogador, runa, conhecia);
                return;
            }
            default -> {
            }
        }
        Identificacao.identificar(jogador, runa);
        gastar(jogador, runa);
        TalentosDoTier2.aoLerPergaminho(jogador, tipo.base(), false);
        switch (tipo) {
            case ADIVINHACAO -> adivinhacao(jogador);
            case ANTIMAGIA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.ANTIMAGIA, TURNOS_DA_ANTIMAGIA * Turno.TICKS));
                Particulas.subindoEmVolta(jogador, Particulas.poeira(0x00FF00, 1.5F), 24, 0.1);
                avisar(jogador, "efeito.pixeldungeon.antimagia", ChatFormatting.GREEN);
            }
            case IMAGEM_PRISMATICA -> ImagemEspelhada.criarPrismatica(mundo, jogador);
            case ENERGIA_MISTICA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.RECARGA_DE_ARTEFATO, TURNOS_DA_ENERGIA_MISTICA * Turno.TICKS));
                Particulas.subindoEmVolta(jogador, ParticleTypes.ELECTRIC_SPARK, 20, 0.1);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.2F);
                avisar(jogador, "efeito.pixeldungeon.energia_mistica", ChatFormatting.YELLOW);
            }
            case PASSAGEM -> passagem(jogador);
            case VIDENCIA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.VIDENCIA, TURNOS_DA_VIDENCIA * Turno.TICKS));
                Busca.revelarEmVolta(mundo, jogador.position(), DISTANCIA_DA_VIDENCIA);
                avisar(jogador, "efeito.pixeldungeon.videncia", ChatFormatting.AQUA);
            }
            case DESAFIO -> desafio(jogador);
            case EXPLOSAO_PSIONICA -> explosaoPsionica(jogador);
            case PAVOR -> pavor(jogador);
            default -> {
            }
        }
    }

    // ---- Adivinhação ----

    // 4 tipos desconhecidos (poção, pergaminho ou anel, com chances iguais); devolve quantos descobriu
    public static int adivinhacao(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        List<TipoDePocao> pocoes = new ArrayList<>();
        for (TipoDePocao tipo : TipoDePocao.values()) {
            if (!dados.conhece(tipo.chaveDeConhecimento())) {
                pocoes.add(tipo);
            }
        }
        List<TipoDePergaminho> pergaminhos = new ArrayList<>();
        for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
            if (!dados.conhece(tipo.chaveDeConhecimento())) {
                pergaminhos.add(tipo);
            }
        }
        List<TipoDeAnel> aneis = new ArrayList<>();
        for (TipoDeAnel tipo : TipoDeAnel.values()) {
            if (!dados.conhece(tipo.chaveDeConhecimento())) {
                aneis.add(tipo);
            }
        }
        Random sorteio = new Random(jogador.getRandom().nextLong());
        List<Component> descobertos = new ArrayList<>();
        float[] pesos = { 3, 3, 3 };
        while (descobertos.size() < 4 && pocoes.size() + pergaminhos.size() + aneis.size() > 0) {
            if (pesos[0] + pesos[1] + pesos[2] <= 0) {
                pesos = new float[] { 3, 3, 3 };
            }
            int qual = SaqueDoAndar.sortearPeso(sorteio, pesos);
            if (qual == 0) {
                if (pocoes.isEmpty()) {
                    pesos[0] = 0;
                    continue;
                }
                pesos[0]--;
                TipoDePocao tipo = pocoes.remove(sorteio.nextInt(pocoes.size()));
                Identificacao.identificar(jogador, tipo);
                descobertos.add(Component.translatable(tipo.chaveDoNome()));
            } else if (qual == 1) {
                if (pergaminhos.isEmpty()) {
                    pesos[1] = 0;
                    continue;
                }
                pesos[1]--;
                TipoDePergaminho tipo = pergaminhos.remove(sorteio.nextInt(pergaminhos.size()));
                Identificacao.identificar(jogador, tipo);
                descobertos.add(Component.translatable(tipo.chaveDoNome()));
            } else {
                if (aneis.isEmpty()) {
                    pesos[2] = 0;
                    continue;
                }
                pesos[2]--;
                TipoDeAnel tipo = aneis.remove(sorteio.nextInt(aneis.size()));
                Identificacao.identificar(jogador, tipo);
                descobertos.add(Component.translatable(tipo.chaveDoNome()));
            }
        }
        if (descobertos.isEmpty()) {
            avisar(jogador, "mensagem.pixeldungeon.pergaminho_adivinhacao.nada", ChatFormatting.GRAY);
            return 0;
        }
        jogador.sendSystemMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_adivinhacao").withStyle(ChatFormatting.AQUA));
        for (Component nome : descobertos) {
            jogador.sendSystemMessage(Component.literal(" - ").append(nome).withStyle(ChatFormatting.AQUA));
        }
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 16, 0.05);
        return descobertos.size();
    }

    // ---- Passagem ----

    // O primeiro andar da região mais perto acima (no esgoto, o 1), igual ao Shattered
    public static int andarDaPassagem(int andar) {
        return Math.max(1, andar - 1 - (andar - 2) % 5);
    }

    private static void passagem(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Integer andar = run.ativa() ? PopulacaoDoAndar.andarEm(run, jogador.getX(), jogador.getY(), jogador.getZ()) : null;
        if (andar == null) {
            avisar(jogador, "mensagem.pixeldungeon.sem_teletransporte", ChatFormatting.GRAY);
            return;
        }
        Particulas.subindoEmVolta(jogador, ParticleTypes.PORTAL, 30, 0.2);
        GerenciadorDaRun.levarPara(jogador, andarDaPassagem(andar), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ---- Desafio ----

    private static void desafio(ServerPlayer jogador) {
        ServerLevel mundo = jogador.serverLevel();
        for (MobDoShattered inimigo : EfeitosDePergaminho.inimigosDoAndar(jogador)) {
            if (!inimigo.aliado()) {
                inimigo.chamar(jogador.position());
            }
        }
        BlockPos controle = EfeitosDasPedras.quadradinhoEm(mundo, jogador.position());
        // O tamanho da arena: quanto mais lugar aberto em volta, maior (igual ao Shattered)
        int abertos = EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 8).size();
        int raio = abertos < 30 ? 1 : abertos >= 100 ? 3 : 2;
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(ARENA_X, controle.getX());
        dados.definir(ARENA_Z, controle.getZ());
        dados.definir(ARENA_RAIO, raio);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.DESAFIO, TURNOS_DO_DESAFIO * Turno.TICKS));
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 1.0F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ANGRY_VILLAGER, 8, 0.1);
        mostrarArena(mundo, dados, jogador.getY());
    }

    // O herói está dentro da arena do desafio?
    public static boolean naArena(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int raio = dados.inteiro(ARENA_RAIO);
        int qx = Math.floorDiv(Mth.floor(jogador.getX()) - dados.inteiro(ARENA_X), T);
        int qz = Math.floorDiv(Mth.floor(jogador.getZ()) - dados.inteiro(ARENA_Z), T);
        return Math.abs(qx) <= raio && Math.abs(qz) <= raio;
    }

    // A névoa vermelha na beirada da arena
    private static void mostrarArena(ServerLevel mundo, DadosDoHeroi dados, double y) {
        int raio = dados.inteiro(ARENA_RAIO);
        double x0 = dados.inteiro(ARENA_X) - raio * T;
        double z0 = dados.inteiro(ARENA_Z) - raio * T;
        double lado = (2 * raio + 1) * T;
        for (double d = 0; d <= lado; d += 1.0) {
            mundo.sendParticles(Particulas.poeira(0xFF0000, 1.5F), x0 + d, y + 0.3, z0, 1, 0, 0.2, 0, 0);
            mundo.sendParticles(Particulas.poeira(0xFF0000, 1.5F), x0 + d, y + 0.3, z0 + lado, 1, 0, 0.2, 0, 0);
            mundo.sendParticles(Particulas.poeira(0xFF0000, 1.5F), x0, y + 0.3, z0 + d, 1, 0, 0.2, 0, 0);
            mundo.sendParticles(Particulas.poeira(0xFF0000, 1.5F), x0 + lado, y + 0.3, z0 + d, 1, 0, 0.2, 0, 0);
        }
    }

    // Dentro da arena: 67% do dano (depois das outras reduções)
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador && jogador.hasEffect(ModEfeitos.DESAFIO) && naArena(jogador)) {
            evento.setAmount(evento.getAmount() * 0.67F);
        }
    }

    // A cada turno: a vidência acha os segredos em volta; a arena some se o herói sair dela
    public static void aoTurno(ServerPlayer jogador) {
        if (jogador.hasEffect(ModEfeitos.VIDENCIA)) {
            Busca.revelarEmVolta(jogador.serverLevel(), jogador.position(), DISTANCIA_DA_VIDENCIA);
        }
        if (jogador.hasEffect(ModEfeitos.DESAFIO)) {
            if (!naArena(jogador)) {
                jogador.removeEffect(ModEfeitos.DESAFIO);
                avisar(jogador, "mensagem.pixeldungeon.pergaminho_desafio.saiu", ChatFormatting.GRAY);
            } else {
                // Dentro da arena, a fome não aumenta
                jogador.getFoodData().setExhaustion(0);
                mostrarArena(jogador.serverLevel(), RegrasDoHeroi.dados(jogador), jogador.getY());
            }
        }
    }

    // ---- Explosão psiônica e pavor ----

    // Devolve quantos inimigos atingiu
    public static int explosaoPsionica(ServerPlayer jogador) {
        ServerLevel mundo = jogador.serverLevel();
        List<MobDoShattered> alvos = EfeitosDePergaminho.visiveis(jogador);
        DamageSource magia = mundo.damageSources().indirectMagic(jogador, jogador);
        for (MobDoShattered inimigo : alvos) {
            Combate.aplicar(inimigo, Math.round(inimigo.getMaxHealth() / 2F + inimigo.getHealth() / 2F), magia);
            if (inimigo.isAlive()) {
                inimigo.addEffect(new MobEffectInstance(ModEfeitos.CEGO, 10 * Turno.TICKS));
            }
        }
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.2F);
        mundo.sendParticles(ParticleTypes.FLASH, jogador.getX(), jogador.getEyeY(), jogador.getZ(), 1, 0, 0, 0, 0);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_explosao_psionica").withStyle(ChatFormatting.WHITE), true);
        float dano = Math.round(jogador.getMaxHealth() * 0.5F * (float) Math.pow(0.9, alvos.size()));
        Combate.aplicar(jogador, dano, mundo.damageSources().magic());
        if (jogador.isAlive()) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.CEGO, 10 * Turno.TICKS));
            jogador.addEffect(new MobEffectInstance(ModEfeitos.FRACO, 100 * Turno.TICKS));
        }
        return alvos.size();
    }

    // Devolve quantos inimigos ficaram com pavor
    public static int pavor(ServerPlayer jogador) {
        int amedrontados = 0;
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.6F, 1.6F);
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFF0000, 2.0F), 30, 0.2);
        for (MobDoShattered inimigo : EfeitosDePergaminho.visiveis(jogador)) {
            if (inimigo.aliado()) {
                continue;
            }
            inimigo.acordar();
            inimigo.setTarget(null);
            inimigo.addEffect(new MobEffectInstance(inimigo.chefe() ? ModEfeitos.ATERRORIZADO : ModEfeitos.PAVOR, TURNOS_DO_PAVOR * Turno.TICKS));
            amedrontados++;
        }
        return amedrontados;
    }

    // Com pavor, longe do herói (6 quadradinhos) e fora da vista dele: some da dungeon (sem dar XP)
    public static boolean sumiuDePavor(MobDoShattered inimigo) {
        Player heroi = inimigo.level().getNearestPlayer(inimigo, 64);
        if (heroi != null && (heroi.distanceTo(inimigo) < 6 * T || heroi.hasLineOfSight(inimigo))) {
            return false;
        }
        if (inimigo.level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.POOF, inimigo.getX(), inimigo.getY(0.5), inimigo.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
        inimigo.discard();
        return true;
    }

    // ---- Antimagia ----

    // Imune à magia (o "MagicImmune" do Shattered): varinhas, pergaminhos, pedras rúnicas e artefatos não funcionam,
    // e os anéis não fazem nada (os encantamentos, os glifos e os efeitos mágicos ficam de fora em outros lugares)
    public static boolean semMagia(LivingEntity alvo) {
        return alvo.hasEffect(ModEfeitos.ANTIMAGIA);
    }

    // Avisa e devolve true se a antimagia não deixa usar o item ("pergaminho", "varinha" ou "item")
    public static boolean bloqueado(ServerPlayer jogador, String qual) {
        if (!semMagia(jogador)) {
            return false;
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.antimagia." + qual).withStyle(ChatFormatting.GRAY), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
        return true;
    }

    // Os efeitos ruins que vêm de magia (o "AntiMagic.RESISTS" do Shattered)
    public static boolean efeitoMagico(MobEffectInstance efeito) {
        return efeito.is(ModEfeitos.SONO_MAGICO) || efeito.is(ModEfeitos.ENCANTADO) || efeito.is(ModEfeitos.FRACO)
                || efeito.is(ModEfeitos.VULNERAVEL) || efeito.is(ModEfeitos.FEITICO) || efeito.is(ModEfeitos.CONDENADO);
    }

    public static boolean danoMagico(DamageSource fonte) {
        return fonte.is(DamageTypes.MAGIC) || fonte.is(DamageTypes.INDIRECT_MAGIC) || fonte.is(DamageTypeTags.IS_LIGHTNING);
    }

    // ---- Os que pedem uma escolha ----

    // Encantamento: a arma ou armadura, depois um de 3 encantamentos
    private static void encantamento(ServerPlayer jogador, RunaDePergaminho runa, boolean conhecia) {
        if (!conhecia) {
            Identificacao.identificar(jogador, runa);
            gastar(jogador, runa);
        }
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, "pergaminho_encantamento", EfeitosDasPedras::encantavel, (quem, espaco) -> {
            ItemStack pilha = quem.getInventory().getItem(espaco);
            ItemStack copia = pilha.copy();
            Random sorteio = new Random(quem.getRandom().nextLong());
            boolean armadura = NivelDoItem.ehArmadura(pilha);
            List<Object> opcoes = armadura ? new ArrayList<>(opcoesDeGlifo(NivelDoItem.glifo(pilha), sorteio))
                    : new ArrayList<>(opcoesDeEncantamento(NivelDoItem.encantamento(pilha), sorteio));
            List<Component> botoes = new ArrayList<>();
            for (Object opcao : opcoes) {
                botoes.add(Component.translatable(opcao instanceof Glifo glifo ? glifo.chaveDoNome() : ((Encantamento) opcao).chaveDoNome()));
            }
            EscolhaDeOpcao.pedir(quem, Component.translatable(TipoDePergaminhoExotico.ENCANTAMENTO.chaveDoNome()),
                    Component.translatable(armadura ? "item.pixeldungeon.pergaminho_exotico.encantamento.armadura"
                            : "item.pixeldungeon.pergaminho_exotico.encantamento.arma"),
                    pilha.copyWithCount(1), botoes, (quemEscolheu, indice) -> {
                        ItemStack agora = quemEscolheu.getInventory().getItem(espaco);
                        if (indice < 0 || !ItemStack.isSameItemSameComponents(agora, copia) || (conhecia && !gastar(quemEscolheu, runa))) {
                            return;
                        }
                        encantarCom(quemEscolheu, agora, opcoes.get(indice));
                    });
        });
        if (!abriu) {
            avisar(jogador, "mensagem.pixeldungeon.pedra_sem_item", ChatFormatting.GRAY);
        }
    }

    // Um comum, um incomum e um qualquer (todos diferentes do que o item já tem), igual ao Shattered
    public static List<Encantamento> opcoesDeEncantamento(@Nullable Encantamento atual, Random sorteio) {
        List<Encantamento> opcoes = new ArrayList<>();
        opcoes.add(sortearEncantamento(atual, Encantamento.Raridade.COMUM, opcoes, sorteio));
        opcoes.add(sortearEncantamento(atual, Encantamento.Raridade.INCOMUM, opcoes, sorteio));
        opcoes.add(sortearEncantamento(atual, null, opcoes, sorteio));
        return opcoes;
    }

    private static Encantamento sortearEncantamento(@Nullable Encantamento atual, @Nullable Encantamento.Raridade raridade, List<Encantamento> jaSairam,
            Random sorteio) {
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            Encantamento sorteado = raridade == null ? Encantamento.sortear(sorteio) : daRaridade(raridade, sorteio);
            if (sorteado != atual && !jaSairam.contains(sorteado)) {
                return sorteado;
            }
        }
        return Encantamento.sortear(sorteio);
    }

    private static Encantamento daRaridade(Encantamento.Raridade raridade, Random sorteio) {
        List<Encantamento> lista = new ArrayList<>();
        for (Encantamento encantamento : Encantamento.values()) {
            if (encantamento.raridade() == raridade) {
                lista.add(encantamento);
            }
        }
        return lista.get(sorteio.nextInt(lista.size()));
    }

    public static List<Glifo> opcoesDeGlifo(@Nullable Glifo atual, Random sorteio) {
        List<Glifo> opcoes = new ArrayList<>();
        opcoes.add(sortearGlifo(atual, Glifo.Raridade.COMUM, opcoes, sorteio));
        opcoes.add(sortearGlifo(atual, Glifo.Raridade.INCOMUM, opcoes, sorteio));
        opcoes.add(sortearGlifo(atual, null, opcoes, sorteio));
        return opcoes;
    }

    private static Glifo sortearGlifo(@Nullable Glifo atual, @Nullable Glifo.Raridade raridade, List<Glifo> jaSairam, Random sorteio) {
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            Glifo sorteado;
            if (raridade == null) {
                sorteado = Glifo.sortear(sorteio);
            } else {
                List<Glifo> lista = new ArrayList<>();
                for (Glifo glifo : Glifo.values()) {
                    if (glifo.raridade() == raridade) {
                        lista.add(glifo);
                    }
                }
                sorteado = lista.get(sorteio.nextInt(lista.size()));
            }
            if (sorteado != atual && !jaSairam.contains(sorteado)) {
                return sorteado;
            }
        }
        return Glifo.sortear(sorteio);
    }

    // Põe o encantamento (ou glifo) escolhido; a maldição, se tiver, dá lugar a ele
    public static void encantarCom(ServerPlayer jogador, ItemStack pilha, Object escolhido) {
        if (NivelDoItem.amaldicoado(pilha)) {
            NivelDoItem.removerMaldicao(pilha);
        }
        if (escolhido instanceof Glifo glifo) {
            NivelDoItem.definirGlifo(pilha, glifo);
        } else {
            NivelDoItem.definirEncantamento(pilha, (Encantamento) escolhido);
        }
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 30, 0.2);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.displayClientMessage(Component.translatable(escolhido instanceof Glifo ? "mensagem.pixeldungeon.pedra_encantamento.armadura"
                : "mensagem.pixeldungeon.pedra_encantamento.arma").withStyle(ChatFormatting.GREEN), false);
        TalentosDoTier2.aoLerPergaminho(jogador, TipoDePergaminho.MELHORIA, false);
    }

    // Metamorfose: primeiro o talento que sai, depois o que entra (um de cada outra classe, sorteado)
    private static void metamorfose(ServerPlayer jogador, RunaDePergaminho runa, boolean conhecia) {
        if (!conhecia) {
            Identificacao.identificar(jogador, runa);
            gastar(jogador, runa);
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        List<Talento> meus = new ArrayList<>();
        for (int tier = 1; tier <= Talento.TIERS; tier++) {
            meus.addAll(dados.talentos(tier));
        }
        if (meus.isEmpty()) {
            return;
        }
        List<Component> botoes = new ArrayList<>();
        for (Talento talento : meus) {
            botoes.add(Component.translatable(talento.chaveDoNome()).append(" (").append(Component.translatable("dica.pixeldungeon.tier", talento.tier()))
                    .append(")"));
        }
        ItemStack icone = new ItemStack(ItemPergaminhoExotico.daRuna(runa));
        EscolhaDeOpcao.pedir(jogador, Component.translatable(TipoDePergaminhoExotico.METAMORFOSE.chaveDoNome()),
                Component.translatable("item.pixeldungeon.pergaminho_exotico.metamorfose.escolha"), icone, botoes, (quem, indice) -> {
                    if (indice < 0) {
                        return;
                    }
                    // Igual ao Shattered: o pergaminho conhecido se gasta quando o talento que sai é escolhido
                    if (conhecia && !gastar(quem, runa)) {
                        return;
                    }
                    Talento antigo = meus.get(indice);
                    List<Talento> novos = opcoesDeMetamorfose(RegrasDoHeroi.dados(quem), antigo, new Random(quem.getRandom().nextLong()));
                    List<Component> nomes = new ArrayList<>();
                    for (Talento novo : novos) {
                        nomes.add(Component.translatable(novo.chaveDoNome()).append(" (").append(Component.translatable(novo.classe().chaveDoNome()))
                                .append(")"));
                    }
                    EscolhaDeOpcao.pedir(quem, Component.translatable(antigo.chaveDoNome()),
                            Component.translatable("item.pixeldungeon.pergaminho_exotico.metamorfose.troca"), icone, nomes, (quemTroca, qual) -> {
                                if (qual >= 0) {
                                    metamorfosear(quemTroca, antigo, novos.get(qual));
                                }
                            });
                });
    }

    // Um talento sorteado de cada outra classe (no mesmo tier, que o herói ainda não tem; de preferência um que já funciona)
    public static List<Talento> opcoesDeMetamorfose(DadosDoHeroi dados, Talento antigo, Random sorteio) {
        List<Talento> opcoes = new ArrayList<>();
        for (ClasseDoHeroi classe : ClasseDoHeroi.values()) {
            List<Talento> daClasse = Talento.daClasse(classe, antigo.tier());
            if (daClasse.isEmpty() || daClasse.contains(antigo)) {
                continue;
            }
            List<Talento> podem = new ArrayList<>();
            List<Talento> funcionam = new ArrayList<>();
            for (Talento talento : daClasse) {
                if (!dados.temTalento(talento)) {
                    podem.add(talento);
                    if (talento.chaveDoPendente() == null) {
                        funcionam.add(talento);
                    }
                }
            }
            List<Talento> lista = funcionam.isEmpty() ? podem : funcionam;
            if (!lista.isEmpty()) {
                opcoes.add(lista.get(sorteio.nextInt(lista.size())));
            }
        }
        return opcoes;
    }

    public static void metamorfosear(ServerPlayer jogador, Talento antigo, Talento novo) {
        RegrasDoHeroi.dados(jogador).metamorfosear(antigo, novo);
        Particulas.subindoEmVolta(jogador, ParticleTypes.WITCH, 24, 0.1);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_metamorfose", Component.translatable(antigo.chaveDoNome()),
                Component.translatable(novo.chaveDoNome())).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        TalentosDoTier2.aoLerPergaminho(jogador, TipoDePergaminho.TRANSMUTACAO, false);
        RedeDoHeroi.enviarEstado(jogador, true);
    }

    // Canto da sereia: o inimigo na mira vira aliado (ele tem que ser escolhido se o pergaminho já era conhecido)
    private static void cantoDaSereia(ServerPlayer jogador, RunaDePergaminho runa, boolean conhecia) {
        LivingEntity naMira = Mira.inimigo(jogador, 48);
        MobDoShattered alvo = naMira instanceof MobDoShattered inimigo && !inimigo.aliado() ? inimigo : null;
        if (alvo == null && conhecia) {
            avisar(jogador, "mensagem.pixeldungeon.pergaminho_canto_da_sereia.mirar", ChatFormatting.GRAY);
            return;
        }
        Identificacao.identificar(jogador, runa);
        gastar(jogador, runa);
        TalentosDoTier2.aoLerPergaminho(jogador, TipoDePergaminho.CANCAO_DE_NINAR, false);
        cantar(jogador, alvo);
    }

    public static void cantar(ServerPlayer jogador, @Nullable MobDoShattered alvo) {
        ServerLevel mundo = jogador.serverLevel();
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 1.0F, 0.8F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.HEART, 6, 0.1);
        for (MobDoShattered inimigo : EfeitosDePergaminho.visiveis(jogador)) {
            if (inimigo != alvo && !inimigo.aliado()) {
                inimigo.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, TURNOS_DO_ENCANTO * Turno.TICKS));
                Particulas.subindoEmVolta(inimigo, ParticleTypes.HEART, 4, 0.0);
            }
        }
        if (alvo == null) {
            avisar(jogador, "mensagem.pixeldungeon.pergaminho_canto_da_sereia.sem_alvo", ChatFormatting.GRAY);
        } else if (alvo.chefe()) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, TURNOS_DO_ENCANTO * Turno.TICKS));
        } else {
            // Igual a corromper: o herói ganha o XP e os itens dele
            alvo.dominar(jogador);
            EventosDeCombate.darRecompensa(alvo, jogador);
        }
    }

    // ---- Ajudantes ----

    // Tira um pergaminho exótico dessa runa do inventário (no criativo, não tira)
    public static boolean gastar(ServerPlayer jogador, RunaDePergaminho runa) {
        if (jogador.getAbilities().instabuild) {
            return true;
        }
        ItemStack naMao = jogador.getMainHandItem();
        if (naMao.getItem() instanceof ItemPergaminhoExotico pergaminho && pergaminho.runa() == runa) {
            naMao.shrink(1);
            return true;
        }
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            if (inventario.getItem(i).getItem() instanceof ItemPergaminhoExotico pergaminho && pergaminho.runa() == runa) {
                inventario.getItem(i).shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void avisar(ServerPlayer jogador, String chave, ChatFormatting cor) {
        jogador.displayClientMessage(Component.translatable(chave).withStyle(cor), true);
    }
}
