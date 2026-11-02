package com.louzada.pixeldungeon.dungeon;

// A "planta baixa" de um andar: uma grade de quadradinhos, igual ao mapa do Shattered.
// Cada quadradinho tem um tipo (parede, chão, água, entrada ou saída).
// Ela ainda não é blocos no mundo: quem transforma em blocos é o ConstrutorDeAndar.
public class PlantaDoAndar {
    public static final byte PAREDE = 0;
    public static final byte CHAO = 1;
    public static final byte AGUA = 2;
    public static final byte ENTRADA = 3;
    public static final byte SAIDA = 4;

    private int largura;
    private int altura;
    private byte[] quadradinhos;

    private int entradaX = -1;
    private int entradaY = -1;
    private int saidaX = -1;
    private int saidaY = -1;

    // Começa tudo como parede
    public PlantaDoAndar(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
        this.quadradinhos = new byte[largura * altura];
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
