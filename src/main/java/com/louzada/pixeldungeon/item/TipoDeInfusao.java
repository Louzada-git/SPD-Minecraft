package com.louzada.pixeldungeon.item;

// As infusões do Shattered (os "Brews"): poções de arremessar feitas no pote (ver EfeitosDeInfusao).
//   sprite: a posição na folha de itens do Shattered; custo: a energia da receita; quantas: quantas saem
public enum TipoDeInfusao {
    CAUSTICA("caustica", 403, 1, 1, 12),
    NEVASCA("nevasca", 401, 8, 1, 12),
    INFERNAL("infernal", 400, 12, 1, 12),
    CHOQUE("choque", 402, 10, 1, 12),
    AQUATICA("aquatica", 404, 8, 8, 12),
    INSTAVEL("instavel", 405, 1, 1, 8);

    private String id;
    private int sprite;
    private int custo;
    private int quantas;
    private int energia;

    TipoDeInfusao(String id, int sprite, int custo, int quantas, int energia) {
        this.id = id;
        this.sprite = sprite;
        this.custo = custo;
        this.quantas = quantas;
        this.energia = energia;
    }

    public String id() {
        return id;
    }

    public String nomeDoItem() {
        return "infusao_" + id;
    }

    public int sprite() {
        return sprite;
    }

    public int custo() {
        return custo;
    }

    public int quantas() {
        return quantas;
    }

    // Quanta energia a receita inteira vale quando é energizada (a aquática: 12 a cada 8)
    public int energia() {
        return energia;
    }
}
