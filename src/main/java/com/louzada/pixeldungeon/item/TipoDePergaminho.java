package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// Os 12 tipos de pergaminho do Shattered.
//   peso: chance de aparecer no chão da dungeon, igual ao Shattered
//         (o de melhoria tem peso 0: caem 3 por região, de propósito)
//   escolheItem: depois de ler, o jogador escolhe um item do inventário
public enum TipoDePergaminho {
    MELHORIA("melhoria", 0, true),
    IDENTIFICACAO("identificacao", 3, true),
    REMOVER_MALDICAO("remover_maldicao", 2, true),
    IMAGEM_ESPELHADA("imagem_espelhada", 1, false),
    RECARGA("recarga", 2, false),
    TELETRANSPORTE("teletransporte", 1, false),
    CANCAO_DE_NINAR("cancao_de_ninar", 1, false),
    MAPEAMENTO_MAGICO("mapeamento_magico", 1, false),
    FURIA("furia", 1, false),
    RETRIBUICAO("retribuicao", 1, false),
    TERROR("terror", 1, false),
    TRANSMUTACAO("transmutacao", 1, true);

    private String id;
    private int peso;
    private boolean escolheItem;

    TipoDePergaminho(String id, int peso, boolean escolheItem) {
        this.id = id;
        this.peso = peso;
        this.escolheItem = escolheItem;
    }

    public String id() {
        return id;
    }

    public int peso() {
        return peso;
    }

    public boolean escolheItem() {
        return escolheItem;
    }

    public String chaveDeConhecimento() {
        return "pergaminho:" + id;
    }

    public String chaveDoNome() {
        return "item.pixeldungeon.pergaminho." + id;
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon.pergaminho." + id + ".descricao";
    }

    @Nullable
    public static TipoDePergaminho peloId(String id) {
        for (TipoDePergaminho tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }
}
