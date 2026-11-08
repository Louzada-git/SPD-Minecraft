package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Grama por cima do chão (uma camada separada, o quadradinho continua sendo chão)
    public static final byte SEM_GRAMA = 0;
    public static final byte GRAMA = 1;
    public static final byte GRAMA_ALTA = 2;

    // O que as salas especiais pedem pra deixar no chão (quem sorteia o item de verdade é o SaqueDoAndar)
    public enum Premio {
        ARMA, ARMADURA, OURO, OURO_POUCO, PERGAMINHO, PERGAMINHO_DE_IDENTIFICAR_OU_REMOVER, ARMADURA_AMALDICOADA,
        // Das salas secretas: a comida da despensa, o ouro miúdo do tesouro, o prêmio do labirinto e as armas de arremesso do arsenal
        RACAO_DE_SUPRIMENTOS, OURO_DO_TESOURO, PREMIO_DO_LABIRINTO, ARREMESSO,
        // Plantas já plantadas (não são itens)
        PLANTA_ALEATORIA, PLANTA_GRAMA_SOLAR, PLANTA_ESTRELA
    }

    // Um prêmio num quadradinho, no chão ou dentro de um baú
    public record PremioNoChao(int x, int y, Premio premio, boolean noBau) {
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

    private List<PremioNoChao> premios = new ArrayList<>();
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
        premios.add(new PremioNoChao(x, y, premio, noBau));
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
        return tipo != PAREDE && tipo != PORTA_ESCONDIDA && tipo != ESTATUA && tipo != ESTANTE && tipo != BARRIL;
    }

    // As portas gigantes (as comuns e as trancadas)
    public boolean ehPorta(int x, int y) {
        byte tipo = tipo(x, y);
        return tipo == PORTA || tipo == PORTA_TRANCADA;
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

    // Onde inimigos e itens podem aparecer, igual ao Shattered: chão (sem água, escada ou porta) das salas comuns,
    // longe da entrada e sem nada já posto ali. Nas salas especiais e nos túneis não aparece nada.
    // (O andar do chefe não tem salas comuns de verdade: lá vale qualquer chão.)
    public List<int[]> lugaresLivres(int raioEmVoltaDaEntrada) {
        List<int[]> lugares = new ArrayList<>();
        for (int qy = 0; qy < altura; qy++) {
            for (int qx = 0; qx < largura; qx++) {
                if (tipo(qx, qy) != CHAO || temPremioEm(qx, qy)) {
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
