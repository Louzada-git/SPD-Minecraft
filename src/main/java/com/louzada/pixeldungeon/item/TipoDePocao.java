package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// Os 12 tipos de poção do Shattered.
//   peso: chance de aparecer no chão da dungeon, igual ao Shattered
//         (a de força tem peso 0: caem 2 por região, de propósito)
//   ruim: faz mal a quem bebe (explode em volta); o certo é arremessar nos inimigos
public enum TipoDePocao {
    FORCA("forca", 0, false),
    CURA("cura", 3, false),
    VISAO_MENTAL("visao_mental", 2, false),
    GELO("gelo", 1, true),
    CHAMA_LIQUIDA("chama_liquida", 2, true),
    GAS_TOXICO("gas_toxico", 1, true),
    PRESSA("pressa", 1, false),
    INVISIBILIDADE("invisibilidade", 1, false),
    LEVITACAO("levitacao", 1, false),
    GAS_PARALISANTE("gas_paralisante", 1, true),
    PUREZA("pureza", 1, false),
    EXPERIENCIA("experiencia", 1, false);

    private String id;
    private int peso;
    private boolean ruim;

    TipoDePocao(String id, int peso, boolean ruim) {
        this.id = id;
        this.peso = peso;
        this.ruim = ruim;
    }

    public String id() {
        return id;
    }

    public int peso() {
        return peso;
    }

    public boolean ruim() {
        return ruim;
    }

    // Chave usada pra lembrar quem já conhece este tipo
    public String chaveDeConhecimento() {
        return "pocao:" + id;
    }

    public String chaveDoNome() {
        return "item.pixeldungeon.pocao." + id;
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon.pocao." + id + ".descricao";
    }

    @Nullable
    public static TipoDePocao peloId(String id) {
        for (TipoDePocao tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }
}
