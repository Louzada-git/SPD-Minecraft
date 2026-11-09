package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Sorteio;

import net.minecraft.util.RandomSource;

// As 13 varinhas do Shattered, com os números de lá (dano por nível, cargas).
// O que cada uma faz quando é lançada fica no EfeitosDeVarinha.
//   míssil mágico: 2-8 (+1/+2), 3 cargas
//   relâmpago: 5-10 (+1/+5), pula pros inimigos perto (mais longe na água)
//   desintegração: 2-8 (+1/+4), atravessa tudo (paredes também); mais forte pra cada inimigo e parede no caminho
//   rajada de fogo: cone de fogo que gasta 1 a 3 cargas (quanto mais, maior e mais forte)
//   corrosão: nuvem de gás corrosivo
//   onda de choque: 1-3 (+1/+3), explode e empurra todo mundo em volta
//   geada: 2-8 (+1/+5), deixa o alvo resfriado (mais devagar)
//   luz prismática: 1-5 (+1/+3), pode cegar
//   transfusão: cura aliados com a vida de quem lança; em inimigos, encanta e dá blindagem
//   maturação: cone de grama alta que enraíza; gasta 1 a 3 cargas
//   terra viva: 4-6 (+0/+2), e o dano vira armadura de pedra no herói; com pedra bastante, nasce um guardião da terra
//   corrupção: sem dano; corrompe o inimigo (vira aliado) ou deixa ele com um efeito ruim, pela vida e pelo XP dele
//   proteção: sem dano; põe guardas no chão (ou melhora as que já existem), que disparam sozinhas nos inimigos
public enum TipoDeVarinha {
    MISSEL_MAGICO("missel_magico", 0xFFFFFF, 3, 0),
    RAJADA_DE_FOGO("rajada_de_fogo", 0xFF8833, 2, 1),
    GEADA("geada", 0x99DDFF, 2, 2),
    RELAMPAGO("relampago", 0xBBDDFF, 2, 3),
    DESINTEGRACAO("desintegracao", 0xCC66FF, 2, 4),
    LUZ_PRISMATICA("luz_prismatica", 0xFFFFCC, 2, 5),
    CORROSAO("corrosao", 0xDD8833, 2, 6),
    ONDA_DE_CHOQUE("onda_de_choque", 0xDDCC88, 2, 8),
    MATURACAO("maturacao", 0x66CC33, 2, 11),
    TRANSFUSAO("transfusao", 0xFF44AA, 2, 12),
    TERRA_VIVA("terra_viva", 0x996633, 2, 7),
    CORRUPCAO("corrupcao", 0x441144, 2, 9),
    PROTECAO("protecao", 0x8822FF, 2, 10);

    private String id;
    private int cor;
    private int cargasIniciais;
    // A posição do desenho na folha de itens do Shattered (linha das varinhas)
    private int desenho;

    TipoDeVarinha(String id, int cor, int cargasIniciais, int desenho) {
        this.id = id;
        this.cor = cor;
        this.cargasIniciais = cargasIniciais;
        this.desenho = desenho;
    }

    public String id() {
        return id;
    }

    public int cor() {
        return cor;
    }

    public int cargasIniciais() {
        return cargasIniciais;
    }

    public int desenho() {
        return desenho;
    }

    public String chaveDoNome() {
        return "item.pixeldungeon.varinha_" + id;
    }

    public String chaveDaDescricao() {
        return "item.pixeldungeon.varinha_" + id + ".descricao";
    }

    public String chaveDoEfeito() {
        return "dica.pixeldungeon.varinha." + id;
    }

    @Nullable
    public static TipoDeVarinha peloId(@Nullable String id) {
        for (TipoDeVarinha tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }

    // Cargas no nível (+1 por nível, até 10); quem segura pode dar mais (o cajado dá 1)
    public int cargasMaximas(int nivel, int extra) {
        return Math.min(Varinha.CARGAS_NO_MAXIMO, cargasIniciais + Math.max(0, nivel) + extra);
    }

    // A rajada de fogo e a maturação gastam 30% das cargas (de 1 a 3); as outras, 1
    public int cargasPorDisparo(int cargas) {
        if (this == RAJADA_DE_FOGO || this == MATURACAO) {
            return Math.max(1, Math.min(3, (int) Math.ceil(cargas * 0.3)));
        }
        return 1;
    }

    // Varinhas que causam dano direto (a corrosão, a transfusão, a maturação, a corrupção e a proteção não)
    public boolean causaDano() {
        return this != CORROSAO && this != TRANSFUSAO && this != MATURACAO && this != CORRUPCAO && this != PROTECAO;
    }

    public int danoMinimo(int nivel, int cargasUsadas) {
        return switch (this) {
            case MISSEL_MAGICO -> 2 + nivel;
            case RELAMPAGO -> 5 + nivel;
            case DESINTEGRACAO -> 2 + nivel;
            case RAJADA_DE_FOGO -> (1 + nivel) * cargasUsadas;
            case ONDA_DE_CHOQUE -> 1 + nivel;
            case GEADA -> 2 + nivel;
            case LUZ_PRISMATICA -> 1 + nivel;
            case TRANSFUSAO -> 3 + nivel;
            case TERRA_VIVA -> 4;
            default -> 0;
        };
    }

    public int danoMaximo(int nivel, int cargasUsadas) {
        return switch (this) {
            case MISSEL_MAGICO -> 8 + 2 * nivel;
            case RELAMPAGO -> 10 + 5 * nivel;
            case DESINTEGRACAO -> 8 + 4 * nivel;
            case RAJADA_DE_FOGO -> switch (cargasUsadas) {
                case 2 -> 2 * (4 + 2 * nivel);
                case 3 -> 3 * (6 + 2 * nivel);
                default -> 2 + 2 * nivel;
            };
            case ONDA_DE_CHOQUE -> 3 + 3 * nivel;
            case GEADA -> 8 + 5 * nivel;
            case LUZ_PRISMATICA -> 5 + 3 * nivel;
            case TRANSFUSAO -> 6 + 2 * nivel;
            case TERRA_VIVA -> 6 + 2 * nivel;
            default -> 0;
        };
    }

    public int sortearDano(RandomSource sorteio, int nivel, int cargasUsadas) {
        return Sorteio.normal(sorteio, danoMinimo(nivel, cargasUsadas), danoMaximo(nivel, cargasUsadas));
    }
}
