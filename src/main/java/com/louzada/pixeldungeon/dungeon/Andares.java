package com.louzada.pixeldungeon.dungeon;

import net.minecraft.core.BlockPos;

// Medidas da dungeon e contas de "em que lugar do mundo fica cada andar".
//
// Os andares ficam empilhados: o andar 1 é o mais alto e cada andar fica 12 blocos abaixo do anterior.
// Cada run nova é construída longe da anterior (no eixo X), então nunca precisa apagar nada.
public class Andares {
    // 1 quadradinho do Shattered = 4x4 blocos
    public static final int TAMANHO_DO_QUADRADINHO = 4;
    // Altura do espaço livre entre o chão e o teto
    public static final int ALTURA_DA_PAREDE = 8;
    // Distância (em blocos) entre o chão de um andar e o chão do próximo
    public static final int ESPACO_ENTRE_ANDARES = 12;
    // Altura (Y) do chão do andar 1
    public static final int Y_DO_ANDAR_1 = 300;
    // Tamanho da planta de um andar, em quadradinhos (32x32 = 128x128 blocos)
    public static final int TAMANHO_DA_PLANTA = 32;
    // Distância (em blocos) entre uma run e outra
    public static final int DISTANCIA_ENTRE_RUNS = 4096;
    // Por enquanto só existe o esgoto (andares 1 a 5)
    public static final int ULTIMO_ANDAR_LIBERADO = 5;

    // Altura (Y) do chão de um andar
    public static int yDoAndar(int andar) {
        return Y_DO_ANDAR_1 - (andar - 1) * ESPACO_ENTRE_ANDARES;
    }

    // Descobre o andar a partir de uma altura que esteja entre o chão e o teto dele
    public static int andarPelaAltura(int y) {
        return (Y_DO_ANDAR_1 - y + ESPACO_ENTRE_ANDARES - 1) / ESPACO_ENTRE_ANDARES + 1;
    }

    // Canto do andar no mundo (onde fica o quadradinho 0,0)
    public static BlockPos origemDoAndar(int numeroDaRun, int andar) {
        return new BlockPos(numeroDaRun * DISTANCIA_ENTRE_RUNS, yDoAndar(andar), 0);
    }

    // No Shattered, os andares 5, 10, 15, 20 e 25 são de chefe
    public static boolean ehAndarDeChefe(int andar) {
        return andar % 5 == 0;
    }

    // Chave de tradução do nome da região do andar
    public static String regiaoDoAndar(int andar) {
        if (andar <= 5) {
            return "regiao.pixeldungeon.esgoto";
        } else if (andar <= 10) {
            return "regiao.pixeldungeon.prisao";
        } else if (andar <= 15) {
            return "regiao.pixeldungeon.cavernas";
        } else if (andar <= 20) {
            return "regiao.pixeldungeon.cidade_dos_anoes";
        } else {
            return "regiao.pixeldungeon.saloes_demoniacos";
        }
    }
}
