package com.louzada.pixeldungeon.item;

// As 12 cores de poção do Shattered. Em cada run, cada cor esconde um tipo de poção diferente.
//   desenho: posição do desenho da poção no items.png do Shattered
//   cor: a cor do líquido (pras partículas quando a poção quebra)
public enum CorDePocao {
    ESCARLATE("escarlate", 0xCC1111),
    AMBAR("ambar", 0xFFAA22),
    DOURADA("dourada", 0xFFDD33),
    JADE("jade", 0x22AA44),
    TURQUESA("turquesa", 0x22CCCC),
    AZUL("azul", 0x2266DD),
    ANIL("anil", 0x5522AA),
    MAGENTA("magenta", 0xDD22CC),
    MARROM("marrom", 0x885533),
    GRAFITE("grafite", 0x444444),
    PRATEADA("prateada", 0xBBBBCC),
    MARFIM("marfim", 0xEEEEDD);

    private String id;
    private int cor;

    CorDePocao(String id, int cor) {
        this.id = id;
        this.cor = cor;
    }

    public String id() {
        return id;
    }

    public int cor() {
        return cor;
    }

    // Nome do item no registro, tipo "pocao_escarlate"
    public String nomeDoItem() {
        return "pocao_" + id;
    }
}
