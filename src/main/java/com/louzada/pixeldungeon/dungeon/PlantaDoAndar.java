package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import javax.annotation.Nullable;

// A "planta baixa" de um andar: uma grade de quadradinhos, igual ao mapa do Shattered.
// Cada quadradinho tem um tipo (parede, chão, água, porta, escada...).
// Ela ainda não é blocos no mundo: quem transforma em blocos é o ConstrutorDeAndar.
public class PlantaDoAndar {
    public static final byte PAREDE = 0;
    public static final byte CHAO = 1;
    public static final byte AGUA = 2;
    public static final byte ENTRADA = 3;
    public static final byte SAIDA = 4;
    // Porta gigante (ocupa o quadradinho inteiro): abre sozinha quando alguém chega perto
    public static final byte PORTA = 5;
    // Porta trancada: só abre com a chave de ferro do andar
    public static final byte PORTA_TRANCADA = 6;
    // Chão especial (as pontes, o chão da biblioteca): o "EMPTY_SP" do Shattered
    public static final byte CHAO_ESPECIAL = 7;
    // Estátua: não dá pra passar
    public static final byte ESTATUA = 8;
    // Estante de livros (a biblioteca)
    public static final byte ESTANTE = 9;
    // Os barris de armazenamento do esgoto (o "REGION_DECO" do Shattered): não dá pra passar
    public static final byte BARRIL = 10;
    // Porta escondida (o "SECRET_DOOR" do Shattered): é construída como parede e vira porta comum quando alguém procura
    public static final byte PORTA_ESCONDIDA = 11;
    // O pote de alquimia (o laboratório): fica no meio do quadradinho, ninguém passa
    public static final byte POTE = 12;
    // Abismo (o "CHASM" do Shattered): um buraco; quem cai vai parar no andar de baixo
    public static final byte ABISMO = 13;
    // Poço mágico (água da saúde ou da consciência) e o poço que já foi usado (o "EMPTY_WELL")
    public static final byte POCO = 14;
    public static final byte POCO_VAZIO = 15;
    // Pedestal: o chão especial onde fica o prêmio de algumas salas
    public static final byte PEDESTAL = 16;
    // Porta de cristal: só abre com a chave de cristal do andar (e dá pra ver através dela)
    public static final byte PORTA_DE_CRISTAL = 17;
    // Barricada de madeira: não abre, só queima
    public static final byte BARRICADA = 18;
    // Brasas (em volta do altar do fogo de sacrifício)
    public static final byte BRASAS = 19;
    // Respiradouro de gás tóxico (a sala de gás tóxico): solta gás o tempo todo
    public static final byte RESPIRADOURO = 20;

    // Grama por cima do chão (uma camada separada, o quadradinho continua sendo chão)
    public static final byte SEM_GRAMA = 0;
    public static final byte GRAMA = 1;
    public static final byte GRAMA_ALTA = 2;

    // O que as salas especiais pedem pra deixar no chão (quem sorteia o item de verdade é o SaqueDoAndar)
    public enum Premio {
        ARMA, ARMADURA, OURO, OURO_POUCO, PERGAMINHO, PERGAMINHO_DE_IDENTIFICAR_OU_REMOVER, ARMADURA_AMALDICOADA,
        // Das salas secretas: a comida da despensa, o ouro miúdo do tesouro, o prêmio do labirinto e as armas de arremesso do arsenal
        RACAO_DE_SUPRIMENTOS, OURO_DO_TESOURO, PREMIO_DO_LABIRINTO, ARREMESSO,
        // O baú do fundo da sala das armadilhas, e o item qualquer da sala de invocação secreta
        PREMIO_DAS_ARMADILHAS, ITEM_ALEATORIO,
        // Plantas já plantadas (não são itens)
        PLANTA_ALEATORIA, PLANTA_GRAMA_SOLAR, PLANTA_ESTRELA, ARBUSTO_DE_FRUTA,
        // A carne grelhada da despensa secreta
        CARNE_GRELHADA,
        // Do laboratório: os cristais de energia (5; no secreto, de 3 a 5), o prêmio (a poção de força do andar, ou uma poção ou uma
        // pedra rúnica), a página do guia de alquimia e as poções do laboratório secreto (todas diferentes)
        CRISTAIS_DO_LABORATORIO, CRISTAIS_DO_LABORATORIO_SECRETO, PREMIO_DO_LABORATORIO, PAGINA_DO_GUIA, POCAO_DO_LABORATORIO_SECRETO,
        // Bombas: a do arsenal (1 em 4 vezes, duas) e a dupla do arsenal secreto
        BOMBA, BOMBA_DUPLA,
        // Das salas especiais da prisão e do resto do esgoto:
        //   o depósito e a sala do fogo mágico (um prêmio guardado, ou uma poção, pergaminho, comida ou ouro), o pote de mel,
        //   a pedra rúnica da sala das pedras, o equipamento da piscina e da sentinela, o do cofre de cristal (varinha, anel
        //   ou artefato), as poções e pergaminhos da sala da escolha, o ouro (dobrado) e os baús da sala de gás tóxico,
        //   e o que tem na ossada da sala do poço
        PREMIO_DO_DEPOSITO, POTE_DE_MEL, PEDRA_DA_SALA, EQUIPAMENTO_DA_SALA, EQUIPAMENTO_DA_SENTINELA, PREMIO_DE_CRISTAL, PREMIO_ESCONDIDO,
        POCAO_OU_PERGAMINHO, OURO_DOBRADO,
        OURO_OU_CATALISADOR, PREMIO_DA_OSSADA, MIUDEZA_DA_OSSADA, CHAVE_DE_CRISTAL,
        // A sala do caminho de cristal: as poções e os pergaminhos, das mais comuns (as primeiras salinhas) às mais raras (os pedestais)
        CAMINHO_1, CAMINHO_2, CAMINHO_3, CAMINHO_4, CAMINHO_5, CAMINHO_6
    }

    // Onde o prêmio fica: no chão, num baú, num baú de cristal (só abre com a chave de cristal) ou numa ossada (o "SKELETON")
    public enum Recipiente {
        CHAO, BAU, BAU_DE_CRISTAL, OSSADA
    }

    // Um prêmio num quadradinho
    public record PremioNoChao(int x, int y, Premio premio, Recipiente recipiente) {
        public boolean noBau() {
            return recipiente == Recipiente.BAU;
        }
    }

    // Os itens que uma sala pede pro andar (largados numa sala comum): a sala das armadilhas pede a poção de levitação,
    // o depósito a de chama líquida, a piscina a de invisibilidade, a do fogo mágico a gélida, a de gás tóxico a de pureza,
    // a da sentinela a de rapidez, e as salas de cristal as chaves de cristal
    public enum ItemPedido {
        LEVITACAO, CHAMA_LIQUIDA, INVISIBILIDADE, GELO, PUREZA, RAPIDEZ, CHAVE_DE_CRISTAL
    }

    // Os inimigos que já nascem com a sala: as piranhas da piscina, a estátua animada e a sentinela
    public enum InimigoDaSala {
        PIRANHA, ESTATUA_ANIMADA, SENTINELA,
        // A abelha do esconderijo do mel (nasce com o pote quebrado dela do lado)
        ABELHA
    }

    // extra: a sala da sentinela (onde ela vigia) e quantos turnos ela demora pra atirar a primeira vez
    public record InimigoNaPlanta(int x, int y, InimigoDaSala tipo, @Nullable Sala sala, float extra) {
    }

    // O poço mágico de uma sala (true: água da saúde; false: da consciência)
    public record PocoNaPlanta(int x, int y, boolean saude) {
    }

    // Uma sala retangular (só o lado de dentro, sem as paredes). x, y = canto; largura e altura em quadradinhos.
    // especial: as salas especiais do Shattered (arsenal, biblioteca...), onde não nascem inimigos nem itens soltos.
    public record Sala(int x, int y, int largura, int altura, boolean especial) {
        public Sala(int x, int y, int largura, int altura) {
            this(x, y, largura, altura, false);
        }

        public int centroX() {
            return x + largura / 2;
        }

        public int centroY() {
            return y + altura / 2;
        }

        // Verdadeiro se as duas salas se encostam (deixando "folga" quadradinhos de parede entre elas)
        public boolean encostaEm(Sala outra, int folga) {
            return x - folga < outra.x + outra.largura
                    && outra.x - folga < x + largura
                    && y - folga < outra.y + outra.altura
                    && outra.y - folga < y + altura;
        }
    }

    private int largura;
    private int altura;
    private byte[] quadradinhos;
    private byte[] grama;
    // Número da sala de cada quadradinho (-1 = não é sala, ou seja, corredor ou parede)
    private int[] salaDoQuadradinho;
    private List<Sala> salas = new ArrayList<>();
    // As armadilhas (outra camada por cima do chão, igual à grama): o tipo de cada uma (null = nenhuma)
    // e se ela começa à vista. Depois de construída, quem sabe se ela está escondida ou desarmada é o bloco no mundo.
    private TipoDeArmadilha[] armadilhas;
    private boolean[] armadilhaVisivel;

    private List<PremioNoChao> premios = new ArrayList<>();
    private EnumMap<ItemPedido, Integer> pedidos = new EnumMap<>(ItemPedido.class);
    private List<InimigoNaPlanta> inimigos = new ArrayList<>();
    private List<PocoNaPlanta> pocos = new ArrayList<>();
    // O fogo mágico (eterno) e o fogo de sacrifício
    private List<int[]> fogoEterno = new ArrayList<>();
    @Nullable
    private int[] fogoDeSacrificio;
    // O piso frágil (quem cai nele vai pra sala do poço do andar de baixo) e a sala do poço (onde se cai)
    @Nullable
    private Sala pisoFragil;
    @Nullable
    private Sala salaDoPoco;
    // Quantas chaves de ferro o andar precisa (uma por porta trancada); elas ficam em salas comuns
    private int chavesDeFerro = 0;
    // Quantas salas secretas o andar tem (a previsão do ladino avisa)
    private int salasSecretas = 0;

    private int entradaX = -1;
    private int entradaY = -1;
    private int saidaX = -1;
    private int saidaY = -1;

    // Começa tudo como parede
    public PlantaDoAndar(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
        this.quadradinhos = new byte[largura * altura];
        this.grama = new byte[largura * altura];
        this.salaDoQuadradinho = new int[largura * altura];
        Arrays.fill(salaDoQuadradinho, -1);
        this.armadilhas = new TipoDeArmadilha[largura * altura];
        this.armadilhaVisivel = new boolean[largura * altura];
    }

    // Cava a sala (vira chão) e lembra que esses quadradinhos são dela
    public void adicionarSala(Sala sala) {
        int numero = salas.size();
        salas.add(sala);
        for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
            for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                if (dentro(x, y)) {
                    quadradinhos[y * largura + x] = CHAO;
                    salaDoQuadradinho[y * largura + x] = numero;
                }
            }
        }
    }

    // Lembra que esse pedaço é uma sala sem mudar o que já foi desenhado (a geração do Shattered desenha antes)
    public int registrarSala(Sala sala) {
        int numero = salas.size();
        salas.add(sala);
        for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
            for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                marcarDaSala(x, y, numero);
            }
        }
        return numero;
    }

    // Esse quadradinho conta como parte da sala (por exemplo, a parede que sumiu quando duas salas se juntaram)
    public void marcarDaSala(int x, int y, int numero) {
        if (dentro(x, y)) {
            salaDoQuadradinho[y * largura + x] = numero;
        }
    }

    public List<Sala> salas() {
        return salas;
    }

    // A sala desse quadradinho (null se for corredor ou parede)
    @Nullable
    public Sala salaEm(int x, int y) {
        if (!dentro(x, y) || salaDoQuadradinho[y * largura + x] < 0) {
            return null;
        }
        return salas.get(salaDoQuadradinho[y * largura + x]);
    }

    public void adicionarPremio(int x, int y, Premio premio, boolean noBau) {
        premios.add(new PremioNoChao(x, y, premio, noBau ? Recipiente.BAU : Recipiente.CHAO));
    }

    public void adicionarPremio(int x, int y, Premio premio, Recipiente recipiente) {
        premios.add(new PremioNoChao(x, y, premio, recipiente));
    }

    // ---- O que as salas especiais pedem ----

    public void pedir(ItemPedido item) {
        pedidos.merge(item, 1, Integer::sum);
    }

    public int pedidos(ItemPedido item) {
        return pedidos.getOrDefault(item, 0);
    }

    public void adicionarInimigo(int x, int y, InimigoDaSala tipo, @Nullable Sala sala, float extra) {
        inimigos.add(new InimigoNaPlanta(x, y, tipo, sala, extra));
    }

    public List<InimigoNaPlanta> inimigos() {
        return inimigos;
    }

    public void adicionarPoco(int x, int y, boolean saude) {
        definir(x, y, POCO);
        pocos.add(new PocoNaPlanta(x, y, saude));
    }

    // O poço mágico desse quadradinho (ou null)
    @Nullable
    public PocoNaPlanta pocoEm(int x, int y) {
        for (PocoNaPlanta poco : pocos) {
            if (poco.x() == x && poco.y() == y) {
                return poco;
            }
        }
        return null;
    }

    public List<PocoNaPlanta> pocos() {
        return pocos;
    }

    public void adicionarFogoEterno(int x, int y) {
        fogoEterno.add(new int[] { x, y });
    }

    public List<int[]> fogoEterno() {
        return fogoEterno;
    }

    public void definirFogoDeSacrificio(int x, int y) {
        fogoDeSacrificio = new int[] { x, y };
    }

    @Nullable
    public int[] fogoDeSacrificio() {
        return fogoDeSacrificio;
    }

    public void definirPisoFragil(Sala sala) {
        pisoFragil = sala;
    }

    @Nullable
    public Sala pisoFragil() {
        return pisoFragil;
    }

    public void definirSalaDoPoco(Sala sala) {
        salaDoPoco = sala;
    }

    @Nullable
    public Sala salaDoPoco() {
        return salaDoPoco;
    }

    public List<PremioNoChao> premios() {
        return premios;
    }

    public boolean temPremioEm(int x, int y) {
        for (PremioNoChao premio : premios) {
            if (premio.x() == x && premio.y() == y) {
                return true;
            }
        }
        return false;
    }

    public void adicionarChaveDeFerro() {
        chavesDeFerro++;
    }

    public int chavesDeFerro() {
        return chavesDeFerro;
    }

    // ---- Armadilhas ----

    public void colocarArmadilha(int x, int y, TipoDeArmadilha tipo, boolean visivel) {
        if (dentro(x, y)) {
            armadilhas[y * largura + x] = tipo;
            armadilhaVisivel[y * largura + x] = visivel || !tipo.escondivel();
        }
    }

    @Nullable
    public TipoDeArmadilha armadilha(int x, int y) {
        return dentro(x, y) ? armadilhas[y * largura + x] : null;
    }

    public boolean temArmadilha(int x, int y) {
        return armadilha(x, y) != null;
    }

    // Começa à vista? (as escondidas só aparecem procurando, pisando ou com o mapeamento mágico)
    public boolean armadilhaVisivel(int x, int y) {
        return dentro(x, y) && armadilhaVisivel[y * largura + x];
    }

    public int quantasArmadilhas() {
        int total = 0;
        for (TipoDeArmadilha tipo : armadilhas) {
            if (tipo != null) {
                total++;
            }
        }
        return total;
    }


    public void contarSalaSecreta() {
        salasSecretas++;
    }

    public int salasSecretas() {
        return salasSecretas;
    }

    // Lugar aberto que não pertence a nenhuma sala = corredor
    public boolean ehCorredor(int x, int y) {
        return aberto(x, y) && salaDoQuadradinho[y * largura + x] < 0;
    }

    public int largura() {
        return largura;
    }

    public int altura() {
        return altura;
    }

    public boolean dentro(int x, int y) {
        return x >= 0 && y >= 0 && x < largura && y < altura;
    }

    // Fora da planta é sempre parede
    public byte tipo(int x, int y) {
        if (!dentro(x, y)) {
            return PAREDE;
        }
        return quadradinhos[y * largura + x];
    }

    public void definir(int x, int y, byte tipo) {
        if (dentro(x, y)) {
            quadradinhos[y * largura + x] = tipo;
        }
    }

    public byte grama(int x, int y) {
        return dentro(x, y) ? grama[y * largura + x] : SEM_GRAMA;
    }

    public void definirGrama(int x, int y, byte tipo) {
        if (dentro(x, y)) {
            grama[y * largura + x] = tipo;
        }
    }

    // Tem espaço aberto (chão e teto)? Tudo que não é parede: até as estátuas e estantes ficam dentro da sala.
    // A porta escondida conta como parede até ser achada.
    public boolean aberto(int x, int y) {
        byte tipo = tipo(x, y);
        return tipo != PAREDE && tipo != PORTA_ESCONDIDA;
    }

    // Dá pra andar em cima?
    public boolean passavel(int x, int y) {
        byte tipo = tipo(x, y);
        return tipo != PAREDE && tipo != PORTA_ESCONDIDA && tipo != ESTATUA && tipo != ESTANTE && tipo != BARRIL && tipo != POTE && tipo != ABISMO
                && tipo != POCO && tipo != POCO_VAZIO && tipo != PORTA_DE_CRISTAL && tipo != BARRICADA;
    }

    // As portas (as gigantes, comuns e trancadas, a de cristal e a barricada): têm a altura dos corredores
    public boolean ehPorta(int x, int y) {
        byte tipo = tipo(x, y);
        return tipo == PORTA || tipo == PORTA_TRANCADA || tipo == PORTA_DE_CRISTAL || tipo == BARRICADA;
    }

    // Uma parede só precisa ser construída se encostar em algum lugar aberto (inclusive na diagonal)
    public boolean encostaEmAberto(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && aberto(x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Onde inimigos e itens podem aparecer, igual ao Shattered: chão (sem água, escada, porta ou armadilha) das salas comuns,
    // longe da entrada e sem nada já posto ali. Nas salas especiais e nos túneis não aparece nada.
    // (O andar do chefe não tem salas comuns de verdade: lá vale qualquer chão.)
    public List<int[]> lugaresLivres(int raioEmVoltaDaEntrada) {
        List<int[]> lugares = new ArrayList<>();
        for (int qy = 0; qy < altura; qy++) {
            for (int qx = 0; qx < largura; qx++) {
                if (tipo(qx, qy) != CHAO || temPremioEm(qx, qy) || temArmadilha(qx, qy)) {
                    continue;
                }
                Sala sala = salaEm(qx, qy);
                if (sala == null || sala.especial()) {
                    continue;
                }
                boolean pertoDaEntrada = Math.abs(qx - entradaX) <= raioEmVoltaDaEntrada && Math.abs(qy - entradaY) <= raioEmVoltaDaEntrada;
                if (!pertoDaEntrada) {
                    lugares.add(new int[] { qx, qy });
                }
            }
        }
        return lugares;
    }

    public void definirEntrada(int x, int y) {
        definir(x, y, ENTRADA);
        entradaX = x;
        entradaY = y;
    }

    public void definirSaida(int x, int y) {
        definir(x, y, SAIDA);
        saidaX = x;
        saidaY = y;
    }

    public int entradaX() {
        return entradaX;
    }

    public int entradaY() {
        return entradaY;
    }

    public int saidaX() {
        return saidaX;
    }

    public int saidaY() {
        return saidaY;
    }

    // O andar do chefe não tem saída (por enquanto)
    public boolean temSaida() {
        return saidaX >= 0;
    }
}
