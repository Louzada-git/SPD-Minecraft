package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

// Os talentos dos tiers 1 e 2 de cada classe, iguais aos do Shattered.
// Cada talento vai até +2. O tier 1 tem 4 talentos e 5 pontos (níveis 2 a 6);
// o tier 2 tem 5 talentos e 6 pontos (níveis 7 a 12).
//
// Alguns talentos dependem de coisas que o mod ainda não tem (a busca e as salas secretas).
// Eles podem ser escolhidos, mas a dica avisa que ainda não fazem nada ("pendente").
public enum Talento {
    // Guerreiro
    REFEICAO_SAUDAVEL("refeicao_saudavel", ClasseDoHeroi.GUERREIRO, 1, null),
    INTUICAO_DE_VETERANO("intuicao_de_veterano", ClasseDoHeroi.GUERREIRO, 1, null),
    RAIVA_PROVOCADA("raiva_provocada", ClasseDoHeroi.GUERREIRO, 1, null),
    DETERMINACAO_DE_FERRO("determinacao_de_ferro", ClasseDoHeroi.GUERREIRO, 1, null),
    ESTOMAGO_DE_FERRO("estomago_de_ferro", ClasseDoHeroi.GUERREIRO, 2, null),
    TENACIDADE_LIQUIDA("tenacidade_liquida", ClasseDoHeroi.GUERREIRO, 2, null),
    TRANSFERENCIA_RUNICA("transferencia_runica", ClasseDoHeroi.GUERREIRO, 2, null),
    IMPETO_LETAL("impeto_letal", ClasseDoHeroi.GUERREIRO, 2, null),
    PROJETEIS_IMPROVISADOS("projeteis_improvisados", ClasseDoHeroi.GUERREIRO, 2, null),
    // Mago
    REFEICAO_EMPODERADORA("refeicao_empoderadora", ClasseDoHeroi.MAGO, 1, null),
    INTUICAO_DO_ESTUDIOSO("intuicao_do_estudioso", ClasseDoHeroi.MAGO, 1, null),
    MAGIA_PROLONGADA("magia_prolongada", ClasseDoHeroi.MAGO, 1, null),
    BARREIRA_DE_APOIO("barreira_de_apoio", ClasseDoHeroi.MAGO, 1, null),
    REFEICAO_ENERGETICA("refeicao_energetica", ClasseDoHeroi.MAGO, 2, null),
    PODER_INSCRITO("poder_inscrito", ClasseDoHeroi.MAGO, 2, null),
    PRESERVACAO_DE_VARINHA("preservacao_de_varinha", ClasseDoHeroi.MAGO, 2, null),
    VISAO_ARCANA("visao_arcana", ClasseDoHeroi.MAGO, 2, null),
    BATERIA_DE_ESCUDO("bateria_de_escudo", ClasseDoHeroi.MAGO, 2, null),
    // Ladino
    RACOES_GUARDADAS("racoes_guardadas", ClasseDoHeroi.LADINO, 1, null),
    INTUICAO_DO_LADRAO("intuicao_do_ladrao", ClasseDoHeroi.LADINO, 1, null),
    GOLPE_SURPRESA("golpe_surpresa", ClasseDoHeroi.LADINO, 1, null),
    SOMBRAS_PROTETORAS("sombras_protetoras", ClasseDoHeroi.LADINO, 1, null),
    REFEICAO_MISTICA("refeicao_mistica", ClasseDoHeroi.LADINO, 2, null),
    FURTIVIDADE_INSCRITA("furtividade_inscrita", ClasseDoHeroi.LADINO, 2, null),
    GRANDE_BUSCA("grande_busca", ClasseDoHeroi.LADINO, 2, "procurar"),
    PASSOS_SILENCIOSOS("passos_silenciosos", ClasseDoHeroi.LADINO, 2, null),
    PREVISAO_DO_LADINO("previsao_do_ladino", ClasseDoHeroi.LADINO, 2, "segredos"),
    // Caçadora
    RECOMPENSA_DA_NATUREZA("recompensa_da_natureza", ClasseDoHeroi.CACADORA, 1, null),
    INTUICAO_DE_SOBREVIVENTE("intuicao_de_sobrevivente", ClasseDoHeroi.CACADORA, 1, null),
    ATAQUE_CONSECUTIVO("ataque_consecutivo", ClasseDoHeroi.CACADORA, 1, null),
    AUXILIO_DA_NATUREZA("auxilio_da_natureza", ClasseDoHeroi.CACADORA, 1, null),
    REFEICAO_REVIGORANTE("refeicao_revigorante", ClasseDoHeroi.CACADORA, 2, null),
    NATUREZA_LIQUIDA("natureza_liquida", ClasseDoHeroi.CACADORA, 2, null),
    PASSOS_REJUVENESCEDORES("passos_rejuvenescedores", ClasseDoHeroi.CACADORA, 2, null),
    SENTIDOS_APURADOS("sentidos_apurados", ClasseDoHeroi.CACADORA, 2, null),
    PROJETEIS_DURAVEIS("projeteis_duraveis", ClasseDoHeroi.CACADORA, 2, null),
    // Duelista
    REFEICAO_FORTALECEDORA("refeicao_fortalecedora", ClasseDoHeroi.DUELISTA, 1, null),
    INTUICAO_DE_AVENTUREIRO("intuicao_de_aventureiro", ClasseDoHeroi.DUELISTA, 1, null),
    GOLPE_PACIENTE("golpe_paciente", ClasseDoHeroi.DUELISTA, 1, null),
    BARREIRA_AGRESSIVA("barreira_agressiva", ClasseDoHeroi.DUELISTA, 1, null),
    REFEICAO_FOCADA("refeicao_focada", ClasseDoHeroi.DUELISTA, 2, null),
    AGILIDADE_LIQUIDA("agilidade_liquida", ClasseDoHeroi.DUELISTA, 2, null),
    RECARGA_DE_ARMA("recarga_de_arma", ClasseDoHeroi.DUELISTA, 2, null),
    PRESSA_LETAL("pressa_letal", ClasseDoHeroi.DUELISTA, 2, null),
    EMPUNHAR_AGIL("empunhar_agil", ClasseDoHeroi.DUELISTA, 2, null),
    // Clérigo
    MAGIAS_SACIADAS("magias_saciadas", ClasseDoHeroi.CLERIGO, 1, null),
    INTUICAO_DIVINA("intuicao_divina", ClasseDoHeroi.CLERIGO, 1, null),
    LUZ_ABRASADORA("luz_abrasadora", ClasseDoHeroi.CLERIGO, 1, null),
    ESCUDO_DE_LUZ("escudo_de_luz", ClasseDoHeroi.CLERIGO, 1, null),
    REFEICAO_ILUMINADA("refeicao_iluminada", ClasseDoHeroi.CLERIGO, 2, null),
    RECUPERAR_INSCRICAO("recuperar_inscricao", ClasseDoHeroi.CLERIGO, 2, null),
    RAIO_SOLAR("raio_solar", ClasseDoHeroi.CLERIGO, 2, null),
    SENTIDO_DIVINO("sentido_divino", ClasseDoHeroi.CLERIGO, 2, null),
    BENCAO("bencao", ClasseDoHeroi.CLERIGO, 2, null);

    public static final int PONTOS_MAXIMOS = 2;
    public static final int TIERS = 2;
    // Igual ao Shattered: o tier 1 começa no nível 2 e o tier 2 no nível 7
    private static final int[] NIVEL_DO_TIER = { 2, 7 };
    // Pontos de cada tier: 5 (níveis 2 a 6) e 6 (níveis 7 a 12)
    private static final int[] PONTOS_DO_TIER = { 5, 6 };

    private String id;
    private ClasseDoHeroi classe;
    private int tier;
    @Nullable
    private String pendente;

    Talento(String id, ClasseDoHeroi classe, int tier, @Nullable String pendente) {
        this.id = id;
        this.classe = classe;
        this.tier = tier;
        this.pendente = pendente;
    }

    public int tier() {
        return tier;
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

    public static List<Talento> daClasse(ClasseDoHeroi classe, int tier) {
        List<Talento> lista = new ArrayList<>();
        for (Talento talento : daClasse(classe)) {
            if (talento.tier == tier) {
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

    // O nível em que o tier começa
    public static int nivelDoTier(int tier) {
        return NIVEL_DO_TIER[tier - 1];
    }

    // Pontos do tier que o herói já ganhou nesse nível (1 por nível a partir do começo do tier)
    public static int pontosGanhos(int tier, int nivel) {
        return Math.max(0, Math.min(PONTOS_DO_TIER[tier - 1], nivel - NIVEL_DO_TIER[tier - 1] + 1));
    }
}
