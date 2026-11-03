package com.louzada.pixeldungeon.item;

// As 12 runas de pergaminho do Shattered. Em cada run, cada runa esconde um tipo de pergaminho diferente.
public enum RunaDePergaminho {
    KAUNAN("kaunan"),
    SOWILO("sowilo"),
    LAGUZ("laguz"),
    YNGVI("yngvi"),
    GYFU("gyfu"),
    RAIDO("raido"),
    ISAZ("isaz"),
    MANNAZ("mannaz"),
    NAUDIZ("naudiz"),
    BERKANAN("berkanan"),
    ODAL("odal"),
    TIWAZ("tiwaz");

    private String id;

    RunaDePergaminho(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    // Nome do item no registro, tipo "pergaminho_kaunan"
    public String nomeDoItem() {
        return "pergaminho_" + id;
    }
}
