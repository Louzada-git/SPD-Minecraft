package com.louzada.pixeldungeon.item;

import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

// Nível, identificação e maldição das armas e armaduras, igual ao Shattered:
//   - o pergaminho de melhoria dá +1 de nível (mais dano ou mais defesa, e precisa de menos força)
//   - armas achadas na dungeon vêm sem identificar: não dá pra saber o nível nem se tem maldição
//   - a arma se identifica sozinha depois de 20 golpes (a intuição do Guerreiro e da Duelista acelera)
//   - segurar a arma revela se ela é amaldiçoada
//   - 30% das armas achadas vêm com uma maldição
public class NivelDoItem {
    public static final float USOS_PARA_IDENTIFICAR = 20;

    // ---- O que o item é ----

    public static boolean podeIdentificar(ItemStack pilha) {
        return pilha.getItem() instanceof ArmaDoShattered;
    }

    // O pergaminho de melhoria funciona em armas (o cajado também) e armaduras
    public static boolean podeMelhorar(ItemStack pilha) {
        return pilha.getItem() instanceof ArmaDoShattered || pilha.getItem() instanceof ArmaduraDoShattered;
    }

    public static int nivel(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.NIVEL.get(), 0);
    }

    public static void definirNivel(ItemStack pilha, int nivel) {
        if (nivel <= 0) {
            pilha.remove(ModComponentes.NIVEL.get());
        } else {
            pilha.set(ModComponentes.NIVEL.get(), nivel);
        }
    }

    // Sem o dado, a arma é conhecida (as armas iniciais das classes)
    public static boolean identificado(ItemStack pilha) {
        return !Boolean.FALSE.equals(pilha.get(ModComponentes.IDENTIFICADO.get()));
    }

    public static boolean maldicaoConhecida(ItemStack pilha) {
        return identificado(pilha) || !Boolean.FALSE.equals(pilha.get(ModComponentes.MALDICAO_CONHECIDA.get()));
    }

    @Nullable
    public static Maldicao maldicao(ItemStack pilha) {
        return Maldicao.peloId(pilha.get(ModComponentes.MALDICAO.get()));
    }

    public static boolean amaldicoado(ItemStack pilha) {
        return maldicao(pilha) != null;
    }

    // Força que a arma ou armadura pede no nível dela: cada melhoria vai tirando um pouco, igual ao Shattered
    public static int forcaNecessaria(int tier, int nivel) {
        return 8 + 2 * tier - (int) (Math.sqrt(8 * Math.max(0, nivel) + 1) - 1) / 2;
    }

    // ---- Armas achadas na dungeon ----

    // Igual ao Shattered: +0 (75%), +1 (20%), +2 (5%); 30% amaldiçoadas. E vem sem identificar.
    public static ItemStack sortearDoChao(ItemStack pilha, Random sorteio, boolean sortearNivel) {
        if (!podeIdentificar(pilha)) {
            return pilha;
        }
        int nivel = 0;
        if (sortearNivel && sorteio.nextInt(4) == 0) {
            nivel++;
            if (sorteio.nextInt(5) == 0) {
                nivel++;
            }
        }
        definirNivel(pilha, nivel);
        if (sorteio.nextFloat() < 0.3F) {
            Maldicao[] todas = Maldicao.values();
            pilha.set(ModComponentes.MALDICAO.get(), todas[sorteio.nextInt(todas.length)].id());
        }
        pilha.set(ModComponentes.IDENTIFICADO.get(), false);
        pilha.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), USOS_PARA_IDENTIFICAR);
        return pilha;
    }

    // ---- Descobrir ----

    public static void identificar(@Nullable ServerPlayer jogador, ItemStack pilha) {
        if (identificado(pilha)) {
            return;
        }
        pilha.remove(ModComponentes.IDENTIFICADO.get());
        pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        pilha.remove(ModComponentes.USOS_PARA_IDENTIFICAR.get());
        if (jogador != null) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arma_identificada", pilha.getHoverName())
                    .withStyle(ChatFormatting.AQUA), false);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    // Segurou a arma: descobre se é amaldiçoada (a Duelista com intuição de aventureiro +2 identifica na hora)
    public static void aoSegurar(ServerPlayer jogador, ItemStack pilha) {
        if (!podeIdentificar(pilha) || identificado(pilha)) {
            return;
        }
        if (RegrasDoHeroi.dados(jogador).pontos(Talento.INTUICAO_DE_AVENTUREIRO) >= 2) {
            identificar(jogador, pilha);
            return;
        }
        if (!maldicaoConhecida(pilha)) {
            pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
            if (amaldicoado(pilha)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arma_amaldicoada").withStyle(ChatFormatting.DARK_PURPLE), false);
                jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
        }
    }

    // Cada golpe que acerta deixa o herói mais acostumado com a arma
    public static void aoGolpear(ServerPlayer jogador, ItemStack pilha) {
        if (!podeIdentificar(pilha) || identificado(pilha)) {
            return;
        }
        float faltando = pilha.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), USOS_PARA_IDENTIFICAR);
        faltando -= fatorDosTalentos(RegrasDoHeroi.dados(jogador));
        if (faltando <= 0) {
            identificar(jogador, pilha);
        } else {
            pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), faltando);
        }
    }

    // Intuição de veterano (Guerreiro): 1,75x / 2,5x. Intuição de aventureiro (Duelista): 2,5x / na hora.
    public static float fatorDosTalentos(DadosDoHeroi dados) {
        return (1 + 1.5F * dados.pontos(Talento.INTUICAO_DE_AVENTUREIRO)) * (1 + 0.75F * dados.pontos(Talento.INTUICAO_DE_VETERANO));
    }

    // ---- Melhorar e tirar maldição ----

    // +1 de nível. Igual ao Shattered, melhorar uma arma amaldiçoada tem 1 chance em 3 de tirar a maldição.
    public static void melhorar(@Nullable ServerPlayer jogador, ItemStack pilha, RandomSource sorteio) {
        definirNivel(pilha, nivel(pilha) + 1);
        if (amaldicoado(pilha) && sorteio.nextInt(3) == 0) {
            pilha.remove(ModComponentes.MALDICAO.get());
            if (jogador != null && maldicaoConhecida(pilha)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.maldicao_sumiu").withStyle(ChatFormatting.AQUA), false);
            }
        }
    }

    public static boolean removerMaldicao(ItemStack pilha) {
        boolean tinha = amaldicoado(pilha);
        pilha.remove(ModComponentes.MALDICAO.get());
        pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        return tinha;
    }

    // ---- Nome e dica ----

    // "Espada +1", "Espada irritante +2"... (o que ainda não se sabe não aparece)
    public static Component nome(ItemStack pilha, Component nomeDaArma) {
        MutableComponent nome = nomeDaArma.copy();
        Maldicao maldicao = maldicao(pilha);
        if (maldicao != null && maldicaoConhecida(pilha)) {
            nome = Component.translatable(maldicao.chaveDoNome(), nomeDaArma);
        }
        int nivel = nivel(pilha);
        if (identificado(pilha) && nivel > 0) {
            nome = nome.append(" +" + nivel);
        }
        return nome;
    }
}
