package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A plataforma (o "PlatformRoom" do Shattered): a sala é um abismo com plataformas de chão especial, ligadas por pontes
// de 1 quadradinho; cada porta tem um caminho até a plataforma. As plataformas grandes se dividem em duas
// (de 0% de chance com 25 quadradinhos até 100% com 36). Duas plataformas juntas ficam separadas por abismo.
public class Plataforma extends ComodoComum {

    @Override
    public int minLargura() {
        return Math.max(super.minLargura(), 6);
    }

    @Override
    public int minAltura() {
        return Math.max(super.minAltura(), 6);
    }

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 6, 3, 1 };
    }

    @Override
    public void juntar(PlantaDoAndar planta, Comodo outro, Retangulo pedaco, byte tipo) {
        if (tipo != PlantaDoAndar.ABISMO && ligados.containsKey(outro) && outro instanceof Plataforma) {
            super.juntar(planta, outro, pedaco, PlantaDoAndar.ABISMO);
            Porta porta = ligados.get(outro);
            if (porta != null) {
                Pincel.definir(planta, porta, PlantaDoAndar.CHAO_ESPECIAL);
            }
        } else {
            super.juntar(planta, outro, pedaco, tipo);
        }
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ABISMO);
        List<Retangulo> plataformas = new ArrayList<>();
        dividir(new Retangulo(esquerda + 2, cima + 2, direita - 2, baixo - 2), plataformas);
        for (Retangulo plataforma : plataformas) {
            Pincel.preencher(planta, plataforma.esquerda, plataforma.cima, plataforma.largura() + 1, plataforma.altura() + 1,
                    PlantaDoAndar.CHAO_ESPECIAL);
        }
        for (Porta porta : ligados.values()) {
            porta.definir(Porta.Tipo.COMUM);
            Pincel.desenharDentro(planta, this, porta, 2, PlantaDoAndar.CHAO_ESPECIAL);
        }
    }

    private void dividir(Retangulo atual, List<Retangulo> todas) {
        int area = (atual.largura() + 1) * (atual.altura() + 1);
        if (Dado.decimal() < (area - 25) / 11f) {
            if (atual.largura() > atual.altura() || (atual.largura() == atual.altura() && Dado.ate(2) == 0)) {
                int corteX = Dado.entre(atual.esquerda + 2, atual.direita - 2);
                dividir(new Retangulo(atual.esquerda, atual.cima, corteX - 1, atual.baixo), todas);
                dividir(new Retangulo(corteX + 1, atual.cima, atual.direita, atual.baixo), todas);
                int ponteY = Dado.normalEntre(atual.cima, atual.baixo);
                todas.add(new Retangulo(corteX - 1, ponteY, corteX + 1, ponteY));
            } else {
                int corteY = Dado.entre(atual.cima + 2, atual.baixo - 2);
                dividir(new Retangulo(atual.esquerda, atual.cima, atual.direita, corteY - 1), todas);
                dividir(new Retangulo(atual.esquerda, corteY + 1, atual.direita, atual.baixo), todas);
                int ponteX = Dado.normalEntre(atual.esquerda, atual.direita);
                todas.add(new Retangulo(ponteX, corteY - 1, ponteX, corteY + 1));
            }
        } else {
            todas.add(atual);
        }
    }
}
