package com.louzada.pixeldungeon.item;

// Os feitiços do Shattered que dá pra fazer com o que se acha no esgoto (ver EfeitosDosFeiticos).
//   sprite: a posição na folha de itens do Shattered; custo: a energia da receita; quantas: quantos saem;
//   energia: quanto a receita inteira vale quando é energizada
public enum TipoDeFeitico {
    INSTAVEL("instavel", 419, 1, 1, 8),
    ALQUIMIZAR("alquimizar", 422, 2, 8, 4),
    INFUSAO_MAGICA("infusao_magica", 421, 12, 1, 12),
    MUDANCA_DE_FASE("mudanca_de_fase", 417, 10, 6, 12),
    RECICLAR("reciclar", 423, 12, 12, 12),
    FAROL_DE_RETORNO("farol_de_retorno", 425, 12, 5, 12),
    AGARRAO_TELECINETICO("agarrao_telecinetico", 418, 10, 8, 10);

    private String id;
    private int sprite;
    private int custo;
    private int quantas;
    private int energia;

    TipoDeFeitico(String id, int sprite, int custo, int quantas, int energia) {
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
        return "feitico_" + id;
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

    public int energia() {
        return energia;
    }
}
