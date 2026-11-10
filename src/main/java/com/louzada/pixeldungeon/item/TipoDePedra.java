package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// As 12 pedras rúnicas do Shattered (na ordem do "Generator" dele, que é a ordem dos pesos).
//   peso: chance de aparecer no chão da dungeon (a de encantamento e a de alteração têm peso 0: só saem da alquimia)
//   energia: quanto dá energizar uma no pote de alquimia (3; as duas raras, 5)
//   deInventario: usada num item da mochila (clique direito abre a escolha); as outras são arremessadas
//   pergaminho: o pergaminho que vira 2 dessas pedras no pote de alquimia
//   sprite: a posição dela na folha de itens do Shattered (a linha das pedras)
public enum TipoDePedra {
    ENCANTAMENTO("encantamento", 0, 5, true, TipoDePergaminho.MELHORIA, 8),
    INTUICAO("intuicao", 2, 3, true, TipoDePergaminho.IDENTIFICACAO, 10),
    DETECCAO_MAGICA("deteccao_magica", 2, 3, true, TipoDePergaminho.REMOVER_MALDICAO, 7),
    REBANHO("rebanho", 2, 3, false, TipoDePergaminho.IMAGEM_ESPELHADA, 9),
    CHOQUE("choque", 2, 3, false, TipoDePergaminho.RECARGA, 11),
    TELETRANSPORTE("teletransporte", 2, 3, false, TipoDePergaminho.TELETRANSPORTE, 4),
    SONO_PROFUNDO("sono_profundo", 2, 3, false, TipoDePergaminho.CANCAO_DE_NINAR, 6),
    CLARIVIDENCIA("clarividencia", 2, 3, false, TipoDePergaminho.MAPEAMENTO_MAGICO, 5),
    AGRESSAO("agressao", 2, 3, false, TipoDePergaminho.FURIA, 0),
    EXPLOSAO("explosao", 2, 3, false, TipoDePergaminho.RETRIBUICAO, 3),
    MEDO("medo", 2, 3, false, TipoDePergaminho.TERROR, 2),
    ALTERACAO("alteracao", 0, 5, true, TipoDePergaminho.TRANSMUTACAO, 1);

    private String id;
    private int peso;
    private int energia;
    private boolean deInventario;
    private TipoDePergaminho pergaminho;
    private int sprite;

    TipoDePedra(String id, int peso, int energia, boolean deInventario, TipoDePergaminho pergaminho, int sprite) {
        this.id = id;
        this.peso = peso;
        this.energia = energia;
        this.deInventario = deInventario;
        this.pergaminho = pergaminho;
        this.sprite = sprite;
    }

    public String id() {
        return id;
    }

    public int peso() {
        return peso;
    }

    public int energia() {
        return energia;
    }

    public boolean deInventario() {
        return deInventario;
    }

    public TipoDePergaminho pergaminho() {
        return pergaminho;
    }

    public int sprite() {
        return sprite;
    }

    // O nome do item registrado (pedra_choque, pedra_medo...)
    public String nomeDoItem() {
        return "pedra_" + id;
    }

    public String chaveDoNome() {
        return "item.pixeldungeon." + nomeDoItem();
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon." + nomeDoItem() + ".descricao";
    }

    // A pedra que esse pergaminho vira no pote
    @Nullable
    public static TipoDePedra doPergaminho(@Nullable TipoDePergaminho pergaminho) {
        for (TipoDePedra tipo : values()) {
            if (tipo.pergaminho == pergaminho) {
                return tipo;
            }
        }
        return null;
    }
}
