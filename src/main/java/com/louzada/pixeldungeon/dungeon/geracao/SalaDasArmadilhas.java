package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.Arrays;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;

// A sala das armadilhas (o "TrapsRoom" do Shattered): o chão inteiro coberto de armadilhas à vista, todas do mesmo tipo
// (no esgoto: agarradora, de teletransporte ou de rebanho), e no fundo, do outro lado, um baú com o prêmio
// (às vezes em cima de um pedestal). A porta é comum, e o andar ganha uma poção de levitação pra passar voando.
// O prêmio (ver SaqueDoAndar): 2 em 3 vezes, um dos itens garantidos do andar (a ração, a poção de força ou o pergaminho
// de melhoria); senão, uma arma ou armadura da região seguinte, nunca amaldiçoada, às vezes +1.
// No Shattered, 1 em 4 dessas salas é um abismo em vez de armadilhas; o mod ainda não tem abismos.
public class SalaDasArmadilhas extends ComodoEspecial {

    // Do tamanho limitado, pra não ter armadilhas demais nem de menos
    @Override
    public int minLargura() {
        return 6;
    }

    @Override
    public int maxLargura() {
        return 8;
    }

    @Override
    public int minAltura() {
        return 6;
    }

    @Override
    public int maxAltura() {
        return 8;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        // (o sorteio do abismo do Shattered fica, pra sala sair do mesmo jeito quando os abismos chegarem)
        Dado.ate(4);
        TipoDeArmadilha tipo = Dado.elemento(Arrays.asList(TipoDeArmadilha.DA_SALA_DAS_ARMADILHAS));

        Porta porta = entrada();
        porta.definir(Porta.Tipo.COMUM);

        // A fileira do fundo (do lado oposto à porta) fica sem armadilhas, com o baú no meio
        int x = -1;
        int y = -1;
        int fileiraX0 = esquerda + 1;
        int fileiraX1 = direita - 1;
        int fileiraY0 = cima + 1;
        int fileiraY1 = baixo - 1;
        if (porta.x == esquerda) {
            x = direita - 1;
            y = cima + altura() / 2;
            fileiraX0 = x;
            fileiraX1 = x;
        } else if (porta.x == direita) {
            x = esquerda + 1;
            y = cima + altura() / 2;
            fileiraX0 = x;
            fileiraX1 = x;
        } else if (porta.y == cima) {
            x = esquerda + largura() / 2;
            y = baixo - 1;
            fileiraY0 = y;
            fileiraY1 = y;
        } else if (porta.y == baixo) {
            x = esquerda + largura() / 2;
            y = cima + 1;
            fileiraY0 = y;
            fileiraY1 = y;
        }

        for (int qy = cima + 1; qy < baixo; qy++) {
            for (int qx = esquerda + 1; qx < direita; qx++) {
                boolean naFileira = qx >= fileiraX0 && qx <= fileiraX1 && qy >= fileiraY0 && qy <= fileiraY1;
                if (!naFileira) {
                    planta.colocarArmadilha(qx, qy, tipo, true);
                }
            }
        }

        // O baú: 1 em 3 direto no chão, senão em cima de um pedestal (o chão entalhado)
        if (x >= 0) {
            if (Dado.ate(3) != 0) {
                Pincel.definir(planta, x, y, PlantaDoAndar.CHAO_ESPECIAL);
            }
            planta.adicionarPremio(x, y, Premio.PREMIO_DAS_ARMADILHAS, true);
        }
        planta.pedirPocaoDeLevitacao();
    }

    // As armadilhas comuns do andar não caem aqui (o chão já está cheio)
    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
