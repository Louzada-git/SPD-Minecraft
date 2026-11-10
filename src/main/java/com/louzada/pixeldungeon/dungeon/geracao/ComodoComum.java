package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// As salas comuns (o "StandardRoom" do Shattered). Têm 3 tamanhos: normal (4 a 10 quadradinhos, com as paredes),
// grande (10 a 14) e gigante (14 a 18). As grandes valem por 2 ou 3 salas na conta do andar.
// A entrada e a saída do andar também são salas comuns, com a escada dentro.
public abstract class ComodoComum extends Comodo {
    public enum Tamanho {
        NORMAL(4, 10, 1), GRANDE(10, 14, 2), GIGANTE(14, 18, 3);

        public final int minimo;
        public final int maximo;
        public final int valor;

        Tamanho(int minimo, int maximo, int valor) {
            this.minimo = minimo;
            this.maximo = maximo;
            this.valor = valor;
        }
    }

    // O que a sala tem de escada
    public enum Escada {
        NENHUMA, ENTRADA, SAIDA
    }

    public Tamanho tamanho;
    protected Escada escada = Escada.NENHUMA;
    // O andar (a entrada dos andares 1 e 2 não se junta com as vizinhas)
    protected int andar = 1;

    protected ComodoComum() {
        sortearTamanho();
    }

    public ComodoComum comEscada(Escada escada, int andar) {
        this.escada = escada;
        this.andar = andar;
        return this;
    }

    // A chance de cada tamanho (normal, grande, gigante)
    public float[] chancesDoTamanho() {
        return new float[] { 1, 0, 0 };
    }

    public boolean sortearTamanho() {
        return sortearTamanho(0, Tamanho.values().length - 1);
    }

    // Sorteia um tamanho que valha no máximo "valorMaximo" salas
    public boolean sortearTamanho(int valorMaximo) {
        return sortearTamanho(0, valorMaximo - 1);
    }

    public boolean sortearTamanho(int minimo, int maximo) {
        float[] chances = chancesDoTamanho();
        Tamanho[] tamanhos = Tamanho.values();
        for (int i = 0; i < minimo; i++) {
            chances[i] = 0;
        }
        for (int i = maximo + 1; i < tamanhos.length; i++) {
            chances[i] = 0;
        }
        int sorteado = Dado.chances(chances);
        if (sorteado == -1) {
            return false;
        }
        tamanho = tamanhos[sorteado];
        return true;
    }

    @Override
    public int minLargura() {
        int minimo = tamanho.minimo;
        // A sala com escada precisa de pelo menos 7 (os tipos de entrada e saída do Shattered)
        return escada == Escada.NENHUMA ? minimo : Math.max(minimo, 7);
    }

    @Override
    public int maxLargura() {
        return tamanho.maximo;
    }

    @Override
    public int minAltura() {
        return minLargura();
    }

    @Override
    public int maxAltura() {
        return tamanho.maximo;
    }

    public int valor() {
        return tamanho.valor;
    }

    // As salas grandes atraem mais ligações
    public int pesoDasLigacoes() {
        return valor() * valor();
    }

    @Override
    public boolean ehEntrada() {
        return escada == Escada.ENTRADA;
    }

    @Override
    public boolean ehSaida() {
        return escada == Escada.SAIDA;
    }

    // Igual ao Shattered: a sala da entrada do andar 1 não tem armadilhas
    @Override
    public boolean podePorArmadilha(Ponto p) {
        return !(ehEntrada() && andar <= 1);
    }

    // Duas salas comuns encostadas podem virar uma só se o lado de dentro perto da parede não for sólido.
    // A entrada dos andares 1 e 2 nunca se junta (igual ao Shattered).
    @Override
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        if (ehEntrada() && andar <= 2) {
            return false;
        }
        Ponto dentro = pontoDentro(p, 1);
        return planta.passavel(dentro.x, dentro.y);
    }

    // Põe a escada num quadradinho sorteado longe das paredes (onde o "podeTerEscada" deixar),
    // com chão livre em volta
    protected void porEscada(PlantaDoAndar planta) {
        if (escada == Escada.NENHUMA) {
            return;
        }
        Ponto lugar = aleatorio(2);
        for (int tentativa = 0; tentativa < 100 && !podeTerEscada(planta, lugar); tentativa++) {
            lugar = aleatorio(2);
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                planta.definir(lugar.x + dx, lugar.y + dy, PlantaDoAndar.CHAO);
            }
        }
        if (escada == Escada.ENTRADA) {
            planta.definirEntrada(lugar.x, lugar.y);
        } else {
            planta.definirSaida(lugar.x, lugar.y);
        }
    }

    protected boolean podeTerEscada(PlantaDoAndar planta, Ponto lugar) {
        return planta.passavel(lugar.x, lugar.y);
    }

    // Todas as portas das salas comuns são portas de verdade (a sala vizinha pode pedir outra coisa)
    protected void portasComuns() {
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.COMUM);
        }
    }

    // ---- As salas do esgoto ----

    // Igual ao Shattered nos andares 1 a 4: cano (16), anel (8), ponte na água (8), barris (4) e plantas (1).
    // (O Shattered também tem a bacia redonda, a plataforma e a fenda, que precisam de abismo; o mod ainda não tem.)
    public static ComodoComum sortear() {
        return switch (Dado.chances(new float[] { 16, 8, 8, 4, 1 })) {
            case 0 -> new SalaDosCanos();
            case 1 -> new SalaDoAnel();
            case 2 -> new SalaDaPonte();
            case 3 -> new SalaDosBarris();
            default -> new SalaDasPlantas();
        };
    }

    // A entrada e a saída: ponte na água (4) ou barris (3)
    public static ComodoComum sortearComEscada(Escada escada, int andar) {
        ComodoComum sala = Dado.chances(new float[] { 4, 3 }) == 0 ? new SalaDaPonte() : new SalaDosBarris();
        sala.comEscada(escada, andar);
        sala.sortearTamanho();
        return sala;
    }
}
