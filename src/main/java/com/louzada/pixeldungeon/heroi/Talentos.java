package com.louzada.pixeldungeon.heroi;

import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

// O que cada talento faz (os números são os do Shattered).
// O combate, a comida e o cajado chamam estes métodos na hora certa.
public class Talentos {
    // Nomes dos efeitos guardados no estado do herói
    public static final String RAIVA = "raiva_provocada";
    public static final String MAGIA = "magia_prolongada";
    public static final String CONSECUTIVO = "ataque_consecutivo";
    public static final String CONSECUTIVO_ALVO = "ataque_consecutivo_alvo";
    public static final String PACIENTE = "golpe_paciente";
    public static final String POSICAO_X = "posicao_x";
    public static final String POSICAO_Z = "posicao_z";
    public static final String FORTALECER_VEZES = "fortalecer_vezes";
    public static final String FORTALECER_BONUS = "fortalecer_bonus";
    public static final String EMPODERAR_VEZES = "empoderar_vezes";
    public static final String EMPODERAR_BONUS = "empoderar_bonus";
    public static final String SACIADO = "magias_saciadas";
    public static final String RACOES = "racoes_guardadas";
    public static final String ANDAR_MAIS_FUNDO = "andar_mais_fundo";

    // ---- Escolher talentos ----

    // O jogador apertou "aprimorar" na tela de talentos
    public static void aprimorar(ServerPlayer jogador, Talento talento) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (!dados.temTalento(talento) || dados.pontosLivres(talento.tier()) <= 0 || dados.pontos(talento) >= Talento.PONTOS_MAXIMOS) {
            return;
        }
        int pontos = dados.pontos(talento) + 1;
        dados.definirPontos(talento, pontos);
        // Rações guardadas: 2 rações no primeiro ponto, mais 1 no segundo
        if (talento == Talento.RACOES_GUARDADAS) {
            dados.definir(RACOES, dados.inteiro(RACOES) + (pontos == 1 ? 2 : 1));
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.talento_aprimorado",
                Component.translatable(talento.chaveDoNome()), pontos).withStyle(ChatFormatting.YELLOW), true);
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 12, 0.1);
    }

    // ---- Comida ----

    // Igual ao Shattered: os talentos de refeição
    public static void aoComer(ServerPlayer jogador, DadosDoHeroi dados) {
        int pontos;
        // Refeição saudável: cura 4/6 com 33% de vida ou menos
        pontos = dados.pontos(Talento.REFEICAO_SAUDAVEL);
        if (pontos > 0 && jogador.getHealth() / jogador.getMaxHealth() < 0.334F) {
            int cura = 2 + 2 * pontos;
            jogador.heal(cura);
            NumerosFlutuantes.mostrar(jogador, "+" + cura, NumerosFlutuantes.VERDE);
        }
        // Refeição empoderadora: +2/+3 de dano nos próximos 3 disparos da varinha
        pontos = dados.pontos(Talento.REFEICAO_EMPODERADORA);
        if (pontos > 0) {
            dados.definir(EMPODERAR_BONUS, 1 + pontos);
            dados.definir(EMPODERAR_VEZES, 3);
            Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 12, 0.3);
        }
        // Refeição fortalecedora: +3 de dano nos próximos 2/3 golpes físicos
        pontos = dados.pontos(Talento.REFEICAO_FORTALECEDORA);
        if (pontos > 0) {
            dados.definir(FORTALECER_BONUS, 3);
            dados.definir(FORTALECER_VEZES, 1 + pontos);
            Particulas.subindoEmVolta(jogador, ParticleTypes.CRIT, 12, 0.2);
        }
        // Magias saciadas: a próxima magia dá 3/5 de blindagem
        if (dados.pontos(Talento.MAGIAS_SACIADAS) > 0) {
            dados.definir(SACIADO, true);
        }
    }

    // ---- Golpes ----

    // Dano a mais que os talentos dão num golpe físico que acertou (soma depois da armadura, igual ao Shattered)
    public static float bonusDoGolpe(ServerPlayer jogador, DadosDoHeroi dados, LivingEntity alvo, boolean surpresa, boolean aDistancia, long agora) {
        RandomSource sorteio = jogador.getRandom();
        float bonus = 0;
        int pontos;

        // Raiva provocada: depois de perder a blindagem, +3/+5
        pontos = dados.pontos(Talento.RAIVA_PROVOCADA);
        if (pontos > 0 && dados.ativo(RAIVA, agora)) {
            bonus += 1 + 2 * pontos;
            dados.desativar(RAIVA);
        }
        // Magia prolongada: depois de usar a varinha, +1-2/+2
        pontos = dados.pontos(Talento.MAGIA_PROLONGADA);
        if (pontos > 0 && dados.ativo(MAGIA, agora)) {
            bonus += pontos + sorteio.nextInt(3 - pontos);
            dados.desativar(MAGIA);
        }
        // Golpe surpresa: o primeiro ataque surpresa em cada inimigo, +1-2/+2
        pontos = dados.pontos(Talento.GOLPE_SURPRESA);
        if (pontos > 0 && surpresa && alvo instanceof MobDoShattered inimigo && !inimigo.levouGolpeSurpresa()) {
            bonus += pontos + sorteio.nextInt(3 - pontos);
            inimigo.marcarGolpeSurpresa();
        }
        // Ataque consecutivo: acertou com o arco, o próximo golpe de perto nesse inimigo dá +2/+3
        pontos = dados.pontos(Talento.ATAQUE_CONSECUTIVO);
        if (pontos > 0) {
            if (aDistancia) {
                dados.definirAlvo(CONSECUTIVO_ALVO, alvo.getUUID());
                dados.ativarPor(CONSECUTIVO, agora, Turno.ticks(5));
            } else if (dados.ativo(CONSECUTIVO, agora) && alvo.getUUID().equals(dados.alvo(CONSECUTIVO_ALVO))) {
                bonus += 1 + pontos;
                dados.desativar(CONSECUTIVO);
            }
        }
        // Golpe paciente: esperou parada antes de bater, +1-2/+2
        pontos = dados.pontos(Talento.GOLPE_PACIENTE);
        if (pontos > 0 && !aDistancia && dados.sim(PACIENTE)) {
            bonus += pontos + sorteio.nextInt(3 - pontos);
            dados.definir(PACIENTE, false);
        }
        // Refeição fortalecedora
        int vezes = dados.inteiro(FORTALECER_VEZES);
        if (vezes > 0) {
            bonus += dados.inteiro(FORTALECER_BONUS);
            dados.definir(FORTALECER_VEZES, vezes - 1);
        }
        return bonus;
    }

    // ---- Varinha (cajado do Mago) ----

    // Chamado antes de disparar: magia prolongada e barreira de apoio
    public static void aoUsarVarinha(ServerPlayer jogador, int cargasAntes, int cargasUsadas, boolean doCajado) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.pontos(Talento.MAGIA_PROLONGADA) > 0) {
            dados.ativarPor(MAGIA, jogador.level().getGameTime(), Turno.ticks(5));
        }
        // Barreira de apoio: gastou a última carga do cajado, ganha 3/5 de blindagem
        int pontos = dados.pontos(Talento.BARREIRA_DE_APOIO);
        if (pontos > 0 && doCajado && cargasAntes - cargasUsadas <= 0) {
            Escudos.darBarreira(jogador, 1 + 2 * pontos);
        }
    }

    // Refeição empoderadora: dano a mais no disparo da varinha (gasta uma das 3 vezes)
    public static int bonusDaVarinha(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int vezes = dados.inteiro(EMPODERAR_VEZES);
        if (vezes <= 0) {
            return 0;
        }
        dados.definir(EMPODERAR_VEZES, vezes - 1);
        return dados.inteiro(EMPODERAR_BONUS);
    }

    // ---- Magias do Clérigo ----

    // Magias saciadas: a primeira magia depois de comer dá blindagem
    public static void aoLancarMagia(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos = dados.pontos(Talento.MAGIAS_SACIADAS);
        if (pontos > 0 && dados.sim(SACIADO)) {
            Escudos.darBarreira(jogador, 1 + 2 * pontos);
            dados.definir(SACIADO, false);
        }
    }

    // ---- Todo tick ----

    // Golpe paciente: igual ao Shattered, esperar (o botão de esperar, ver Espera) prepara o golpe; sair do lugar desfaz
    public static void aoEsperar(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.pontos(Talento.GOLPE_PACIENTE) <= 0) {
            return;
        }
        dados.definir(PACIENTE, true);
        dados.definir(POSICAO_X, (float) jogador.getX());
        dados.definir(POSICAO_Z, (float) jogador.getZ());
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0x8000FF, 0.8F), 6, 0.0);
    }

    public static void aoTick(ServerPlayer jogador, DadosDoHeroi dados) {
        if (!dados.sim(PACIENTE)) {
            return;
        }
        double dx = jogador.getX() - dados.decimal(POSICAO_X);
        double dz = jogador.getZ() - dados.decimal(POSICAO_Z);
        if (dx * dx + dz * dz > 0.0025) {
            dados.definir(PACIENTE, false);
        }
    }

    // ---- Andares ----

    // Rações guardadas: num andar novo, o Ladino acha um baú com uma ração de suprimentos
    public static void aoChegarNoAndar(ServerPlayer jogador, int andar) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (andar <= dados.inteiro(ANDAR_MAIS_FUNDO)) {
            return;
        }
        dados.definir(ANDAR_MAIS_FUNDO, andar);
        if (dados.inteiro(RACOES) <= 0 || Andares.ehAndarDeChefe(andar)) {
            return;
        }
        ServerLevel mundo = jogador.serverLevel();
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        BlockPos lugar = PopulacaoDoAndar.lugarAleatorio(run.semente(), Andares.origemDoAndar(run.numeroDaRun(), andar), andar,
                new Random(run.semente() * 13 + andar * 101L + jogador.getUUID().hashCode()));
        if (lugar == null) {
            return;
        }
        Direction frente = Direction.Plane.HORIZONTAL.getRandomDirection(jogador.getRandom());
        mundo.setBlock(lugar, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, frente), 3);
        if (mundo.getBlockEntity(lugar) instanceof ChestBlockEntity bau) {
            bau.setItem(13, new ItemStack(ModItens.RACAO_DE_SUPRIMENTOS.get()));
            dados.definir(RACOES, dados.inteiro(RACOES) - 1);
            PixelDungeon.LOGGER.info("Baú com ração de suprimentos no andar {} em {}", andar, lugar);
        }
    }
}
