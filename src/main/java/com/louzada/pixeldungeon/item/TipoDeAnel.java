package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

// Os 12 anéis do Shattered. O bônus de um anel é o nível dele + 1 (amaldiçoado: de 0 a -2), e dois anéis iguais somam.
//   precisão: acerta 1,3x por bônus            evasão: desvia 1,125x por bônus
//   fúria: ataca 1,09x mais rápido por bônus   pressa: anda 1,15x mais rápido por bônus
//   poder: +1 de força e +3,5% de vida máxima por bônus
//   força: soco de uma arma do tier da força do herói (e +1 de dano com arma por bônus)
//   tiro certeiro: armas de arremesso +1 nível de dano e 1,2x mais usos por bônus
//   energia: varinhas e artefatos recarregam 1,175x mais rápido por bônus
//   elementos: o dano de fogo, gelo, raio, gás e veneno cai pra 0,825x por bônus
//   tenacidade: quanto mais ferido, menos dano (até 0,85x por bônus com a vida quase no fim)
//   fortuna: os inimigos às vezes deixam itens a mais
//   arcano: deixa os encantamentos e glifos mais fortes (o mod ainda não tem)
public enum TipoDeAnel {
    PRECISAO("precisao"),
    ARCANO("arcano"),
    ELEMENTOS("elementos"),
    ENERGIA("energia"),
    EVASAO("evasao"),
    FORCA("forca"),
    FURIA("furia"),
    PRESSA("pressa"),
    PODER("poder"),
    TIRO_CERTEIRO("tiro_certeiro"),
    TENACIDADE("tenacidade"),
    FORTUNA("fortuna");

    private String id;

    TipoDeAnel(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String chaveDoNome() {
        return "anel.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "anel.pixeldungeon." + id + ".descricao";
    }

    // Como aparece no que o herói já sabe (ex.: "anel:precisao")
    public String chaveDeConhecimento() {
        return "anel:" + id;
    }

    @Nullable
    public static TipoDeAnel peloId(@Nullable String id) {
        for (TipoDeAnel tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }
}
