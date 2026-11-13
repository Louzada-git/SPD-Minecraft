package com.louzada.pixeldungeon.item;

// As bombas do Shattered: a comum e as melhoradas no pote de alquimia (bomba + um ingrediente conhecido).
//   alcance: até quantos quadradinhos a explosão chega (a comum, 1; as melhoradas, 2; a de brotamento, 3)
//   destrutiva: faz a explosão comum (dano, quebra os itens frágeis do chão, queima a grama); a de brotamento e a arcana não
//   custo: quanta energia a receita gasta
public enum TipoDeBomba {
    COMUM("bomba", 0, 1, true, 0),
    GELO("bomba_de_gelo", 3, 2, true, 0),
    LA("bomba_de_la", 8, 2, true, 0),
    FOGO("bomba_de_fogo", 2, 2, true, 1),
    BARULHO("bomba_de_barulho", 9, 2, true, 1),
    FUMACA("bomba_de_fumaca", 5, 2, true, 2),
    CLARAO("bomba_de_clarao", 6, 2, true, 2),
    BROTAMENTO("bomba_de_brotamento", 4, 3, false, 3),
    SAGRADA("bomba_sagrada", 7, 2, true, 3),
    ARCANA("bomba_arcana", 10, 2, false, 6);

    private String id;
    private int sprite;
    private int alcance;
    private boolean destrutiva;
    private int custo;

    TipoDeBomba(String id, int sprite, int alcance, boolean destrutiva, int custo) {
        this.id = id;
        this.sprite = sprite;
        this.alcance = alcance;
        this.destrutiva = destrutiva;
        this.custo = custo;
    }

    public String id() {
        return id;
    }

    // A posição do desenho na folha de itens do Shattered (a partir da primeira bomba)
    public int sprite() {
        return sprite;
    }

    public int alcance() {
        return alcance;
    }

    public boolean destrutiva() {
        return destrutiva;
    }

    public int custo() {
        return custo;
    }
}
