package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A bacia redonda (o "CircleBasinRoom" do Shattered): uma sala redonda (sempre grande ou gigante, com lados ímpares)
// com um abismo redondo no meio, cortado por duas pontes de chão especial em cruz. Metade do chão em volta vira água.
// Nas gigantes, o meio das pontes é uma plataforma em volta de um pilar. Também pode ser a entrada ou a saída do andar
// (a partir do andar 3 e 2): aí a escada fica bem no meio.
public class BaciaRedonda extends SalaComMancha {

    @Override
    public float[] chancesDoTamanho() {
        return escada != Escada.NENHUMA ? new float[] { 0, 1, 0 } : new float[] { 0, 3, 1 };
    }

    @Override
    public int minLargura() {
        return Math.max(super.minLargura(), tamanho.minimo + 1);
    }

    @Override
    public int minAltura() {
        return minLargura();
    }

    // Os lados são sempre ímpares (pras pontes ficarem bem no meio)
    @Override
    public Retangulo redimensionar(int largura, int altura) {
        super.redimensionar(largura, altura);
        if (largura() % 2 == 0) {
            direita--;
        }
        if (altura() % 2 == 0) {
            baixo--;
        }
        return this;
    }

    @Override
    protected float preenchimento() {
        return 0.5f;
    }

    @Override
    protected int agrupamento() {
        return 5;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.elipse(planta, this, 1, PlantaDoAndar.CHAO);
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.COMUM);
            int ate = porta.x == esquerda || porta.x == direita ? largura() / 2 : altura() / 2;
            Pincel.desenharDentro(planta, this, porta, ate, PlantaDoAndar.CHAO);
        }
        Pincel.elipse(planta, this, 3, PlantaDoAndar.ABISMO);
        Pincel.linha(planta, new Ponto(esquerda + largura() / 2, cima + 3), new Ponto(esquerda + largura() / 2, baixo - 3), PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.linha(planta, new Ponto(esquerda + 3, cima + altura() / 2), new Ponto(direita - 3, cima + altura() / 2), PlantaDoAndar.CHAO_ESPECIAL);
        Ponto meio = centro();
        if (largura() > 11 || altura() > 11) {
            Pincel.preencher(planta, meio.x - 1, meio.y - 1, 3, 3, PlantaDoAndar.CHAO_ESPECIAL);
            Pincel.definir(planta, meio, PlantaDoAndar.PAREDE);
        }
        sortearMancha();
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                if (planta.tipo(x, y) == PlantaDoAndar.CHAO && naMancha(x, y)) {
                    planta.definir(x, y, PlantaDoAndar.AGUA);
                }
            }
        }
        porEscadaEm(planta, meio);
    }
}
