package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

// Os 4 primeiros talentos (tier 1) de cada classe, iguais aos do Shattered.
// Cada talento vai até +2. O herói ganha 1 ponto por nível do 2 ao 6 (5 pontos no total).
//
// Alguns talentos dependem de coisas que o mod ainda não tem (itens não identificados, plantas).
// Eles podem ser escolhidos, mas a dica avisa que ainda não fazem nada ("pendente").
public enum Talento {
    // Guerreiro
    REFEICAO_SAUDAVEL("refeicao_saudavel", ClasseDoHeroi.GUERREIRO, null),
    INTUICAO_DE_VETERANO("intuicao_de_veterano", ClasseDoHeroi.GUERREIRO, "identificar"),
    RAIVA_PROVOCADA("raiva_provocada", ClasseDoHeroi.GUERREIRO, null),
    DETERMINACAO_DE_FERRO("determinacao_de_ferro", ClasseDoHeroi.GUERREIRO, null),
    // Mago
    REFEICAO_EMPODERADORA("refeicao_empoderadora", ClasseDoHeroi.MAGO, null),
    INTUICAO_DO_ESTUDIOSO("intuicao_do_estudioso", ClasseDoHeroi.MAGO, "identificar"),
    MAGIA_PROLONGADA("magia_prolongada", ClasseDoHeroi.MAGO, null),
    BARREIRA_DE_APOIO("barreira_de_apoio", ClasseDoHeroi.MAGO, null),
    // Ladino
    RACOES_GUARDADAS("racoes_guardadas", ClasseDoHeroi.LADINO, null),
    INTUICAO_DO_LADRAO("intuicao_do_ladrao", ClasseDoHeroi.LADINO, "identificar"),
    GOLPE_SURPRESA("golpe_surpresa", ClasseDoHeroi.LADINO, null),
    SOMBRAS_PROTETORAS("sombras_protetoras", ClasseDoHeroi.LADINO, null),
    // Caçadora
    RECOMPENSA_DA_NATUREZA("recompensa_da_natureza", ClasseDoHeroi.CACADORA, null),
    INTUICAO_DE_SOBREVIVENTE("intuicao_de_sobrevivente", ClasseDoHeroi.CACADORA, "identificar"),
    ATAQUE_CONSECUTIVO("ataque_consecutivo", ClasseDoHeroi.CACADORA, null),
    AUXILIO_DA_NATUREZA("auxilio_da_natureza", ClasseDoHeroi.CACADORA, "plantas"),
    // Duelista
    REFEICAO_FORTALECEDORA("refeicao_fortalecedora", ClasseDoHeroi.DUELISTA, null),
    INTUICAO_DE_AVENTUREIRO("intuicao_de_aventureiro", ClasseDoHeroi.DUELISTA, "identificar"),
    GOLPE_PACIENTE("golpe_paciente", ClasseDoHeroi.DUELISTA, null),
    BARREIRA_AGRESSIVA("barreira_agressiva", ClasseDoHeroi.DUELISTA, null),
    // Clérigo
    MAGIAS_SACIADAS("magias_saciadas", ClasseDoHeroi.CLERIGO, null),
    INTUICAO_DIVINA("intuicao_divina", ClasseDoHeroi.CLERIGO, null),
    LUZ_ABRASADORA("luz_abrasadora", ClasseDoHeroi.CLERIGO, null),
    ESCUDO_DE_LUZ("escudo_de_luz", ClasseDoHeroi.CLERIGO, null);

    public static final int PONTOS_MAXIMOS = 2;
    // Igual ao Shattered: os pontos do tier 1 vêm nos níveis 2, 3, 4, 5 e 6
    public static final int PONTOS_DO_TIER_1 = 5;

    private String id;
    private ClasseDoHeroi classe;
    @Nullable
    private String pendente;

    Talento(String id, ClasseDoHeroi classe, @Nullable String pendente) {
        this.id = id;
        this.classe = classe;
        this.pendente = pendente;
    }

    public String id() {
        return id;
    }

    public ClasseDoHeroi classe() {
        return classe;
    }

    public String chaveDoNome() {
        return "talento.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "talento.pixeldungeon." + id + ".descricao";
    }

    // Aviso de que o talento ainda não faz nada (ou null se ele já funciona)
    @Nullable
    public String chaveDoPendente() {
        return pendente == null ? null : "talento.pixeldungeon.pendente." + pendente;
    }

    public static List<Talento> daClasse(ClasseDoHeroi classe) {
        List<Talento> lista = new ArrayList<>();
        for (Talento talento : values()) {
            if (talento.classe == classe) {
                lista.add(talento);
            }
        }
        return lista;
    }

    @Nullable
    public static Talento peloId(String id) {
        for (Talento talento : values()) {
            if (talento.id.equals(id)) {
                return talento;
            }
        }
        return null;
    }

    // Pontos que o herói já ganhou nesse nível (1 por nível a partir do 2, até 5)
    public static int pontosGanhos(int nivel) {
        return Math.max(0, Math.min(PONTOS_DO_TIER_1, nivel - 1));
    }
}
