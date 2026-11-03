package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A "planta baixa" de um andar: uma grade de quadradinhos, igual ao mapa do Shattered.
// Cada quadradinho tem um tipo (parede, chão, água, entrada ou saída).
// Ela ainda não é blocos no mundo: quem transforma em blocos é o ConstrutorDeAndar.
public class PlantaDoAndar {
    public static final byte PAREDE = 0;
    public static final byte CHAO = 1;
    public static final byte AGUA = 2;
    public static final byte ENTRADA = 3;
    public static final byte SAIDA = 4;

    // Grama por cima do chão (uma camada separada, o quadradinho continua sendo chão)
    public static final byte SEM_GRAMA = 0;
    public static final byte GRAMA = 1;
    public static final byte GRAMA_ALTA = 2;

    // Uma sala retangular. x, y = canto; largura e altura em quadradinhos.
    public record Sala(int x, int y, int largura, int altura) {
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

    public List<Sala> salas() {
        return salas;
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

    // Dá pra andar em cima? (tudo que não é parede)
    public boolean aberto(int x, int y) {
        return tipo(x, y) != PAREDE;
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
