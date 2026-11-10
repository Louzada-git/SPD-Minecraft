package com.louzada.pixeldungeon.dungeon;

import net.minecraft.util.StringRepresentable;

// As armadilhas do Shattered que aparecem no esgoto (andares 1 a 4), com a cor e o desenho de cada uma
// (o mesmo desenho do "terrain_features.png" do Shattered):
//   escondivel: começa escondida (as de dardo e a de desmoronamento ficam sempre à vista)
//   desarmaAoAtivar: depois de ativar fica desarmada (a agarradora e a de portal funcionam pra sempre)
//   evitaLugaresFechados: o sorteio prefere lugares abertos, longe dos corredores (e duas dessas nunca ficam lado a lado)
public enum TipoDeArmadilha implements StringRepresentable {
    // WornDartTrap: um dardo no alvo mais perto
    DARDOS("dardos", Cor.CINZA, Forma.MIRA, false, true, true),
    // PoisonDartTrap: um dardo envenenado no alvo mais perto (só no tesouro secreto, por aqui)
    DARDOS_VENENOSOS("dardos_venenosos", Cor.VERDE, Forma.MIRA, false, true, true),
    // ChillingTrap: ar congelante em volta
    RESFRIAMENTO("resfriamento", Cor.BRANCO, Forma.PONTOS, true, true, false),
    // ShockingTrap: eletricidade em volta
    ELETRICA("eletrica", Cor.AMARELO, Forma.PONTOS, true, true, false),
    // ToxicTrap: uma nuvem de gás tóxico
    GAS_TOXICO("gas_toxico", Cor.VERDE, Forma.GRADE, true, true, false),
    // AlarmTrap: chama todos os inimigos do andar
    ALARME("alarme", Cor.VERMELHO, Forma.PONTOS, true, true, false),
    // OozeTrap: muco cáustico em quem estiver em volta
    MUCO("muco", Cor.VERDE, Forma.PONTOS, true, true, false),
    // ConfusionTrap: uma nuvem de gás de confusão
    CONFUSAO("confusao", Cor.CIANO, Forma.GRADE, true, true, false),
    // FlockTrap: um rebanho de ovelhas mágicas
    REBANHO("rebanho", Cor.BRANCO, Forma.ONDAS, true, true, false),
    // SummoningTrap: invoca inimigos do andar em volta
    INVOCACAO("invocacao", Cor.CIANO, Forma.ONDAS, true, true, false),
    // TeleportationTrap: teletransporta tudo em volta pra lugares aleatórios do andar
    TELETRANSPORTE("teletransporte", Cor.CIANO, Forma.PONTOS, true, true, false),
    // GatewayTrap: teletransporta sempre pro mesmo lugar, quantas vezes for
    PORTAL("portal", Cor.CIANO, Forma.MIRA, true, false, true),
    // GrippingTrap: prende os pés (sangramento e aleijado), quantas vezes for
    AGARRADORA("agarradora", Cor.CINZA, Forma.PONTOS, true, false, true),
    // RockfallTrap: pedras caem na sala inteira (só no tesouro secreto, por aqui)
    DESMORONAMENTO("desmoronamento", Cor.CINZA, Forma.LOSANGO, false, true, true);

    // As cores e os desenhos das armadilhas do Shattered (na ordem da folha de desenhos)
    public enum Cor {
        VERMELHO, LARANJA, AMARELO, VERDE, CIANO, VIOLETA, BRANCO, CINZA, PRETO
    }

    public enum Forma {
        PONTOS, ONDAS, GRADE, ESTRELAS, LOSANGO, MIRA, PONTO_GRANDE
    }

    private final String id;
    private final Cor cor;
    private final Forma forma;
    private final boolean escondivel;
    private final boolean desarmaAoAtivar;
    private final boolean evitaLugaresFechados;

    TipoDeArmadilha(String id, Cor cor, Forma forma, boolean escondivel, boolean desarmaAoAtivar, boolean evitaLugaresFechados) {
        this.id = id;
        this.cor = cor;
        this.forma = forma;
        this.escondivel = escondivel;
        this.desarmaAoAtivar = desarmaAoAtivar;
        this.evitaLugaresFechados = evitaLugaresFechados;
    }

    public String id() {
        return id;
    }

    public Cor cor() {
        return cor;
    }

    public Forma forma() {
        return forma;
    }

    public boolean escondivel() {
        return escondivel;
    }

    public boolean desarmaAoAtivar() {
        return desarmaAoAtivar;
    }

    public boolean evitaLugaresFechados() {
        return evitaLugaresFechados;
    }

    // O nome pra mostrar ("armadilha de dardos")
    public String chaveDoNome() {
        return "armadilha.pixeldungeon." + id;
    }

    // O nome com letras maiúsculas, pra título ("Armadilha de Dardos")
    public String chaveDoTitulo() {
        return "armadilha.pixeldungeon." + id + ".titulo";
    }

    public String chaveDaDescricao() {
        return "armadilha.pixeldungeon." + id + ".descricao";
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    // ---- Quais aparecem em cada andar (igual ao Shattered) ----

    // No andar 1 só tem a de dardos; nos andares 2 a 4, essas com esses pesos
    private static final TipoDeArmadilha[] DO_ESGOTO = { RESFRIAMENTO, ELETRICA, GAS_TOXICO, DARDOS, ALARME, MUCO, CONFUSAO, REBANHO, INVOCACAO,
            TELETRANSPORTE, PORTAL };
    private static final float[] PESOS_DO_ESGOTO = { 4, 4, 4, 4, 2, 2, 1, 1, 1, 1, 1 };

    public static TipoDeArmadilha[] doAndar(int andar) {
        return andar <= 1 ? new TipoDeArmadilha[] { DARDOS } : DO_ESGOTO;
    }

    public static float[] pesosDoAndar(int andar) {
        return andar <= 1 ? new float[] { 1 } : PESOS_DO_ESGOTO;
    }

    // As da sala das armadilhas do esgoto (TrapsRoom): uma delas enche a sala inteira
    public static final TipoDeArmadilha[] DA_SALA_DAS_ARMADILHAS = { AGARRADORA, TELETRANSPORTE, REBANHO };
}
