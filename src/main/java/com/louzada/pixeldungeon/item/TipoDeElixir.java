package com.louzada.pixeldungeon.item;

// Os elixires do Shattered: poções de beber feitas no pote (ver EfeitosDeElixir).
//   sprite: a posição na folha de itens do Shattered; custo: a energia da receita
public enum TipoDeElixir {
    SANGUE_DE_DRAGAO("sangue_de_dragao", 411, 10),
    TOQUE_GELIDO("toque_gelido", 413, 6),
    ESSENCIA_TOXICA("essencia_toxica", 412, 8),
    PODER("poder", 410, 16),
    QUEDA_SUAVE("queda_suave", 415, 10),
    ARMADURA_ARCANA("armadura_arcana", 414, 8),
    REJUVENESCIMENTO_AQUATICO("rejuvenescimento_aquatico", 409, 6);

    private String id;
    private int sprite;
    private int custo;

    TipoDeElixir(String id, int sprite, int custo) {
        this.id = id;
        this.sprite = sprite;
        this.custo = custo;
    }

    public String id() {
        return id;
    }

    public String nomeDoItem() {
        return "elixir_" + id;
    }

    public int sprite() {
        return sprite;
    }

    public int custo() {
        return custo;
    }
}
