package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala da ponte na água (o "WaterBridgeRoom" do Shattered): uma faixa de água corta a sala
// entre as portas, e uma ponte de chão especial atravessa a faixa.
public class SalaDaPonte extends ComodoComum {
    // A faixa de água (sem as paredes) e a ponte
    private Retangulo faixa;
    private Retangulo ponte;

    @Override
    public int minLargura() {
        return Math.max(5, super.minLargura());
    }

    @Override
    public int minAltura() {
        return Math.max(5, super.minAltura());
    }

    @Override
    public boolean podeTerAgua(Ponto p) {
        return false;
    }

    // Só junta com a vizinha onde não tem água encostada na parede
    @Override
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        if (ehEntrada() && andar <= 2) {
            return false;
        }
        Ponto dentro = pontoDentro(p, 1);
        return planta.tipo(dentro.x, dentro.y) != PlantaDoAndar.AGUA;
    }

    // A ponte tem 2 de largura nas salas menores que 8, e 3 nas outras
    private int larguraMaximaDaFaixa(int ladoDaSala) {
        return ladoDaSala >= 8 ? 3 : 2;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);

        // A faixa corta a sala no sentido em que ela atrapalha menos as portas
        int portasXY = 0;
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.COMUM);
            if (porta.x == esquerda || porta.x == direita) {
                portasXY++;
            } else {
                portasXY--;
            }
        }
        portasXY += (largura() - altura()) / 2;

        if (portasXY > 0 || (portasXY == 0 && Dado.ate(2) == 0)) {
            // Faixa de cima a baixo (a ponte atravessa de lado)
            List<Ponto> pontos = new ArrayList<>();
            for (Porta porta : ligados.values()) {
                if (porta.y == cima || porta.y == baixo) {
                    pontos.add(porta);
                }
            }
            pontos.add(new Ponto(esquerda + 1, 0));
            pontos.add(new Ponto(direita - 1, 0));
            pontos.sort(Comparator.comparingInt(p -> p.x));
            int inicio = -1;
            int fim = -1;
            for (int i = 0; i < pontos.size() - 1; i++) {
                if (fim - inicio < pontos.get(i + 1).x - pontos.get(i).x) {
                    inicio = pontos.get(i).x;
                    fim = pontos.get(i + 1).x;
                }
            }
            while (fim - inicio > larguraMaximaDaFaixa(largura()) + 1) {
                if (Dado.ate(2) == 0) {
                    inicio++;
                } else {
                    fim--;
                }
            }
            faixa = new Retangulo(inicio + 1, cima + 1, fim, baixo);
            int yDaPonte = Dado.normalEntre(faixa.cima + 1, faixa.baixo - 2);
            ponte = new Retangulo(faixa.esquerda, yDaPonte, faixa.direita, yDaPonte + 1);
        } else {
            // Faixa de lado a lado (a ponte atravessa de cima a baixo)
            List<Ponto> pontos = new ArrayList<>();
            for (Porta porta : ligados.values()) {
                if (porta.x == esquerda || porta.x == direita) {
                    pontos.add(porta);
                }
            }
            pontos.add(new Ponto(0, cima + 1));
            pontos.add(new Ponto(0, baixo - 1));
            pontos.sort(Comparator.comparingInt(p -> p.y));
            int inicio = -1;
            int fim = -1;
            for (int i = 0; i < pontos.size() - 1; i++) {
                if (fim - inicio < pontos.get(i + 1).y - pontos.get(i).y) {
                    inicio = pontos.get(i).y;
                    fim = pontos.get(i + 1).y;
                }
            }
            while (fim - inicio > larguraMaximaDaFaixa(altura()) + 1) {
                if (Dado.ate(2) == 0) {
                    inicio++;
                } else {
                    fim--;
                }
            }
            faixa = new Retangulo(esquerda + 1, inicio + 1, direita, fim);
            int xDaPonte = Dado.normalEntre(faixa.esquerda + 1, faixa.direita - 2);
            ponte = new Retangulo(xDaPonte, faixa.cima, xDaPonte + 1, faixa.baixo);
        }
        Pincel.preencher(planta, faixa, PlantaDoAndar.AGUA);
        Pincel.preencher(planta, ponte, PlantaDoAndar.CHAO_ESPECIAL);

        porEscada(planta);
    }

    // A escada fica fora da faixa de água
    @Override
    protected boolean podeTerEscada(PlantaDoAndar planta, Ponto lugar) {
        return faixa == null || !faixa.contem(lugar);
    }
}
