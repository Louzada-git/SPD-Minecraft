package com.louzada.pixeldungeon.item;

import net.minecraft.server.level.ServerPlayer;

// As 12 poções exóticas do Shattered: cada uma é a versão alquímica (poção + 4 de energia no pote) de uma poção comum,
// na mesma ordem do TipoDePocao. A exótica é da mesma cor da poção dela e fica conhecida quando a poção comum é.
//   ruim: faz mal a quem bebe (o certo é arremessar)
public enum TipoDePocaoExotica {
    MAESTRIA("maestria", false),
    BLINDAGEM("blindagem", false),
    VISAO_MAGICA("visao_magica", false),
    CONGELAMENTO_INSTANTANEO("congelamento_instantaneo", true),
    BAFO_DE_DRAGAO("bafo_de_dragao", false),
    GAS_CORROSIVO("gas_corrosivo", true),
    VIGOR("vigor", false),
    NEBLINA("neblina", true),
    NUVENS_TEMPESTUOSAS("nuvens_tempestuosas", true),
    ARMADURA_DE_BARRO("armadura_de_barro", false),
    LIMPEZA("limpeza", false),
    INSPIRACAO_DIVINA("inspiracao_divina", false);

    private String id;
    private boolean ruim;

    TipoDePocaoExotica(String id, boolean ruim) {
        this.id = id;
        this.ruim = ruim;
    }

    public String id() {
        return id;
    }

    public boolean ruim() {
        return ruim;
    }

    // A poção comum dela
    public TipoDePocao base() {
        return TipoDePocao.values()[ordinal()];
    }

    public static TipoDePocaoExotica doBase(TipoDePocao base) {
        return values()[base.ordinal()];
    }

    // A exótica dessa cor nesta run
    public static TipoDePocaoExotica daCor(ServerPlayer jogador, CorDePocao cor) {
        return doBase(Identificacao.tipoDa(jogador, cor));
    }

    public String chaveDoNome() {
        return "item.pixeldungeon.pocao_exotica." + id;
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon.pocao_exotica." + id + ".descricao";
    }
}
