package com.louzada.pixeldungeon.item;

// Os acessórios ("trinkets") do Shattered: saem do catalisador mágico no pote de alquimia e mudam um pouco a masmorra
// ou quem vive nela. Valem enquanto estão no inventário (qualquer lugar dele) e sobem até o nível 3 com energia no pote.
// (id, sprite, chave do Shattered, caro: os que custam 10 + 5 x nível pra aprimorar em vez de 6 + 2 x nível)
// O que cada um faz está em Acessorios.
public enum TipoDeAcessorio {
    CRANIO_DE_RATO("cranio_de_rato", 272, "ratskull", false),
    PEDACO_DE_PAPIRO("pedaco_de_papiro", 273, "parchmentscrap", true),
    SEMENTE_PETRIFICADA("semente_petrificada", 274, "petrifiedseed", false),
    CRISTAIS_EXOTICOS("cristais_exoticos", 275, "exoticcrystals", false),
    MOITA_DE_MUSGO("moita_de_musgo", 276, "mossyclump", true),
    RELOGIO_DE_SOL("relogio_de_sol", 277, "dimensionalsundial", false),
    TREVO_DE_TREZE_FOLHAS("trevo_de_treze_folhas", 278, "thirteenleafclover", false),
    MECANISMO_DE_ARMADILHA("mecanismo_de_armadilha", 279, "trapmechanism", false),
    DENTE_DE_MIMICO("dente_de_mimico", 280, "mimictooth", false),
    RESINA_MARAVILHOSA("resina_maravilhosa", 281, "wondrousresin", true),
    OLHO_DE_SALAMANDRA("olho_de_salamandra", 282, "eyeofnewt", false),
    CUBO_DE_SAL("cubo_de_sal", 283, "saltcube", false),
    FRASCO_DE_SANGUE("frasco_de_sangue", 284, "vialofblood", false),
    FRAGMENTO_DO_ESQUECIMENTO("fragmento_do_esquecimento", 285, "shardofoblivion", false),
    INCENSARIO_CAOTICO("incensario_caotico", 286, "chaoticcenser", false),
    TUFO_DE_FURAO("tufo_de_furao", 287, "ferrettuft", false),
    LUNETA_TRINCADA("luneta_trincada", 288, "crackedspyglass", false);

    public static final int NIVEL_MAXIMO = 3;

    private final String id;
    private final int sprite;
    private final String chaveDoShattered;
    private final boolean caro;

    TipoDeAcessorio(String id, int sprite, String chaveDoShattered, boolean caro) {
        this.id = id;
        this.sprite = sprite;
        this.chaveDoShattered = chaveDoShattered;
        this.caro = caro;
    }

    public String id() {
        return id;
    }

    public int sprite() {
        return sprite;
    }

    public String chaveDoShattered() {
        return chaveDoShattered;
    }

    public String nomeDoItem() {
        return "acessorio_" + id;
    }

    // Energia pra subir do nível atual pro próximo (igual ao Shattered)
    public int custoParaAprimorar(int nivel) {
        return caro ? 10 + 5 * nivel : 6 + 2 * nivel;
    }
}
