package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// Maldições das armas do Shattered (as que o mod já tem). Cada uma tem um nome que vai junto
// com o nome da arma ("espada irritante") e só aparece depois que a maldição é descoberta.
// O que cada uma faz fica no Maldicoes.
public enum Maldicao {
    // 1 em 20 golpes: grita e chama todos os inimigos do andar
    IRRITANTE("irritante"),
    // 1 em 12 golpes: teletransporta o inimigo pra outro lugar do andar
    DESLOCAMENTO("deslocamento"),
    // 1 em 10 golpes: um clarão cega quem bateu (e quem estiver vendo)
    OFUSCANTE("ofuscante"),
    // Metade dos golpes dá 50% a mais de dano; a outra metade não dá nada
    POLARIZANTE("polarizante"),
    // 1 em 10 golpes: quem bate sangra (mais, quanto mais vida tiver)
    SACRIFICANTE("sacrificante"),
    // 1 em 4 golpes: fica muito imprecisa por 10 turnos (o próximo acerto desfaz)
    INCONSTANTE("inconstante");

    private String id;

    Maldicao(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String chaveDoNome() {
        return "maldicao.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "maldicao.pixeldungeon." + id + ".descricao";
    }

    @Nullable
    public static Maldicao peloId(@Nullable String id) {
        for (Maldicao maldicao : values()) {
            if (maldicao.id.equals(id)) {
                return maldicao;
            }
        }
        return null;
    }
}
