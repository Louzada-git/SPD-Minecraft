package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.rede.PacoteConhecimento;
import com.louzada.pixeldungeon.run.DadosDaRun;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// Quem sabe o quê, igual ao Shattered:
//   - cada cor de poção e cada runa de pergaminho esconde um tipo, sorteado pela run
//   - beber, ler ou ver o efeito revela o tipo (pra sempre, nesta run)
//   - o que um herói descobre vai também pra quem ele escolheu compartilhar (ver DadosDoHeroi)
// A tela de cada jogador fica sabendo pelo PacoteConhecimento (é ela que mostra o nome certo).
public class Identificacao {

    public static long sementeDaRun(ServerPlayer jogador) {
        return sementeDaRun(jogador.serverLevel());
    }

    public static long sementeDaRun(ServerLevel mundo) {
        return DadosDaRun.pegar(mundo.getServer()).semente();
    }

    public static TipoDePocao tipoDa(ServerPlayer jogador, CorDePocao cor) {
        return IdentidadesDaRun.tipoDaCor(sementeDaRun(jogador), cor);
    }

    public static TipoDePergaminho tipoDa(ServerPlayer jogador, RunaDePergaminho runa) {
        return IdentidadesDaRun.tipoDaRuna(sementeDaRun(jogador), runa);
    }

    public static boolean conhece(ServerPlayer jogador, CorDePocao cor) {
        return RegrasDoHeroi.dados(jogador).conhece(tipoDa(jogador, cor).chaveDeConhecimento());
    }

    public static boolean conhece(ServerPlayer jogador, RunaDePergaminho runa) {
        return RegrasDoHeroi.dados(jogador).conhece(tipoDa(jogador, runa).chaveDeConhecimento());
    }

    // O item ainda tem alguma coisa pra descobrir? (poção ou pergaminho desconhecido, arma sem identificar)
    public static boolean desconhecido(ServerPlayer jogador, ItemStack pilha) {
        if (pilha.getItem() instanceof ItemPocao pocao) {
            return !conhece(jogador, pocao.cor());
        }
        if (pilha.getItem() instanceof ItemPergaminho pergaminho) {
            return !conhece(jogador, pergaminho.runa());
        }
        return NivelDoItem.podeIdentificar(pilha) && !NivelDoItem.identificado(pilha);
    }

    // Descobre tudo do item (pergaminho de identificação)
    public static void identificarItem(ServerPlayer jogador, ItemStack pilha) {
        if (pilha.getItem() instanceof ItemPocao pocao) {
            identificar(jogador, pocao.cor());
        } else if (pilha.getItem() instanceof ItemPergaminho pergaminho) {
            identificar(jogador, pergaminho.runa());
        } else if (NivelDoItem.podeIdentificar(pilha)) {
            NivelDoItem.identificar(jogador, pilha);
        }
    }

    public static void identificar(ServerPlayer jogador, CorDePocao cor) {
        TipoDePocao tipo = tipoDa(jogador, cor);
        aprender(jogador, tipo.chaveDeConhecimento(), Component.translatable("item.pixeldungeon." + cor.nomeDoItem()),
                Component.translatable(tipo.chaveDoNome()));
    }

    public static void identificar(ServerPlayer jogador, RunaDePergaminho runa) {
        TipoDePergaminho tipo = tipoDa(jogador, runa);
        aprender(jogador, tipo.chaveDeConhecimento(), Component.translatable("item.pixeldungeon." + runa.nomeDoItem()),
                Component.translatable(tipo.chaveDoNome()));
    }

    // Descobre um tipo direto (o de força que caiu do teste, por exemplo)
    public static void identificar(ServerPlayer jogador, TipoDePocao tipo) {
        identificar(jogador, IdentidadesDaRun.corDoTipo(sementeDaRun(jogador), tipo));
    }

    public static void identificar(ServerPlayer jogador, TipoDePergaminho tipo) {
        identificar(jogador, IdentidadesDaRun.runaDoTipo(sementeDaRun(jogador), tipo));
    }

    // Aprende e compartilha com quem o jogador escolheu
    private static void aprender(ServerPlayer quem, String chave, Component nomeAntes, Component nomeDepois) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(quem);
        if (!dados.aprender(chave)) {
            return;
        }
        quem.displayClientMessage(Component.translatable("mensagem.pixeldungeon.identificou", nomeAntes, nomeDepois)
                .withStyle(ChatFormatting.AQUA), false);
        sincronizar(quem);

        for (ServerPlayer outro : quem.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (outro == quem || !GerenciadorDaRun.estaNaRun(outro) || !dados.compartilhaCom(outro.getGameProfile().getName())) {
                continue;
            }
            if (RegrasDoHeroi.dados(outro).aprender(chave)) {
                outro.displayClientMessage(Component.translatable("mensagem.pixeldungeon.compartilhou", quem.getDisplayName(), nomeAntes, nomeDepois)
                        .withStyle(ChatFormatting.AQUA), false);
                sincronizar(outro);
            }
        }
    }

    // Manda pra tela do jogador o que ele já sabe (fora da run, não sabe nada)
    public static void sincronizar(ServerPlayer jogador) {
        boolean naRun = GerenciadorDaRun.estaNaRun(jogador);
        long semente = sementeDaRun(jogador);
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);

        List<String> pocoes = new ArrayList<>();
        for (CorDePocao cor : CorDePocao.values()) {
            TipoDePocao tipo = IdentidadesDaRun.tipoDaCor(semente, cor);
            pocoes.add(naRun && dados.conhece(tipo.chaveDeConhecimento()) ? tipo.id() : "");
        }
        List<String> pergaminhos = new ArrayList<>();
        for (RunaDePergaminho runa : RunaDePergaminho.values()) {
            TipoDePergaminho tipo = IdentidadesDaRun.tipoDaRuna(semente, runa);
            pergaminhos.add(naRun && dados.conhece(tipo.chaveDeConhecimento()) ? tipo.id() : "");
        }
        PacketDistributor.sendToPlayer(jogador, new PacoteConhecimento(pocoes, pergaminhos));
    }

    // O item (poção ou pergaminho) de um tipo, pra colocar no chão ou dar de prêmio
    public static ItemStack pocao(long semente, TipoDePocao tipo) {
        return new ItemStack(ItemPocao.doTipo(IdentidadesDaRun.corDoTipo(semente, tipo)));
    }

    public static ItemStack pergaminho(long semente, TipoDePergaminho tipo) {
        return new ItemStack(ItemPergaminho.daRuna(IdentidadesDaRun.runaDoTipo(semente, tipo)));
    }

    // O tipo de uma poção ou pergaminho do inventário (ou null)
    @Nullable
    public static TipoDePocao tipoDaPocao(long semente, ItemStack pilha) {
        return pilha.getItem() instanceof ItemPocao pocao ? IdentidadesDaRun.tipoDaCor(semente, pocao.cor()) : null;
    }

    @Nullable
    public static TipoDePergaminho tipoDoPergaminho(long semente, ItemStack pilha) {
        return pilha.getItem() instanceof ItemPergaminho pergaminho ? IdentidadesDaRun.tipoDaRuna(semente, pergaminho.runa()) : null;
    }
}
