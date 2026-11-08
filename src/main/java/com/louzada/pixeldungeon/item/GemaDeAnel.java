package com.louzada.pixeldungeon.item;

// As 12 gemas dos anéis do Shattered. Em cada run, cada gema esconde um tipo de anel diferente.
//   desenho: posição do desenho do anel no items.png do Shattered (linha dos anéis)
public enum GemaDeAnel {
    GRANADA("granada", 0),
    RUBI("rubi", 1),
    TOPAZIO("topazio", 2),
    ESMERALDA("esmeralda", 3),
    ONIX("onix", 4),
    OPALA("opala", 5),
    TURMALINA("turmalina", 6),
    SAFIRA("safira", 7),
    AMETISTA("ametista", 8),
    QUARTZO("quartzo", 9),
    AGATA("agata", 10),
    DIAMANTE("diamante", 11);

    private String id;
    private int desenho;

    GemaDeAnel(String id, int desenho) {
        this.id = id;
        this.desenho = desenho;
    }

    public String id() {
        return id;
    }

    public int desenho() {
        return desenho;
    }

    // Nome do item no registro, tipo "anel_granada"
    public String nomeDoItem() {
        return "anel_" + id;
    }
}
