package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// As plantas do Shattered (menos a baga-podre, que é de missão). Cada uma vem de uma semente.
//   peso: chance de a semente aparecer (a flor-estrela é mais rara), igual ao Shattered
public enum TipoDePlanta {
    GRAMA_SOLAR("grama_solar", 2),
    FOLHA_FANTASMA("folha_fantasma", 2),
    MUDA_DE_GELO("muda_de_gelo", 2),
    FLOR_DE_FOGO("flor_de_fogo", 2),
    SOFRIMUSGO("sofrimusgo", 2),
    CARDO_LIGEIRO("cardo_ligeiro", 2),
    CEGUERVA("ceguerva", 2),
    VINHA_TEMPESTUOSA("vinha_tempestuosa", 2),
    TERRAIZ("terraiz", 2),
    MAGI_REAL("magi_real", 2),
    FLOR_ESTRELA("flor_estrela", 1);

    private String id;
    private int peso;

    TipoDePlanta(String id, int peso) {
        this.id = id;
        this.peso = peso;
    }

    public String id() {
        return id;
    }

    public int peso() {
        return peso;
    }

    public String nomeDaSemente() {
        return "semente_" + id;
    }

    public String nomeDaPlanta() {
        return "planta_" + id;
    }

    @Nullable
    public static TipoDePlanta peloId(String id) {
        for (TipoDePlanta tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }
}
