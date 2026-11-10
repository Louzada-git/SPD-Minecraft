package com.louzada.pixeldungeon.item;

import net.minecraft.server.level.ServerPlayer;

// Os 12 pergaminhos exóticos do Shattered: cada um é a versão alquímica (pergaminho + 6 de energia no pote) de um
// pergaminho comum, na mesma ordem do TipoDePergaminho. O exótico tem a mesma runa do pergaminho dele e fica
// conhecido quando o comum é.
public enum TipoDePergaminhoExotico {
    ENCANTAMENTO("encantamento"),
    ADIVINHACAO("adivinhacao"),
    ANTIMAGIA("antimagia"),
    IMAGEM_PRISMATICA("imagem_prismatica"),
    ENERGIA_MISTICA("energia_mistica"),
    PASSAGEM("passagem"),
    CANTO_DA_SEREIA("canto_da_sereia"),
    VIDENCIA("videncia"),
    DESAFIO("desafio"),
    EXPLOSAO_PSIONICA("explosao_psionica"),
    PAVOR("pavor"),
    METAMORFOSE("metamorfose");

    private String id;

    TipoDePergaminhoExotico(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    // O pergaminho comum dele
    public TipoDePergaminho base() {
        return TipoDePergaminho.values()[ordinal()];
    }

    public static TipoDePergaminhoExotico doBase(TipoDePergaminho base) {
        return values()[base.ordinal()];
    }

    // O exótico dessa runa nesta run
    public static TipoDePergaminhoExotico daRuna(ServerPlayer jogador, RunaDePergaminho runa) {
        return doBase(Identificacao.tipoDa(jogador, runa));
    }

    public String chaveDoNome() {
        return "item.pixeldungeon.pergaminho_exotico." + id;
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon.pergaminho_exotico." + id + ".descricao";
    }
}
