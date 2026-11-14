package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.item.Acessorios;

// A sala queimada (o "BurnedRoom" do Shattered): manchas de chão queimado pela sala (quanto maior a sala, menos mancha).
// Em cada quadradinho da mancha, 1 em 5 de cada: chão comum, brasas, armadilha de fogo à vista, armadilha de fogo
// escondida (o mecanismo de armadilha às vezes deixa à vista) e armadilha de fogo já gasta.
// A água, a grama e as armadilhas do andar não caem na mancha.
public class SalaQueimada extends SalaComMancha {

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 4, 1, 0 };
    }

    // Só se junta onde o lado de dentro é chão comum
    @Override
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        Ponto dentro = pontoDentro(p, 1);
        return planta.tipo(dentro.x, dentro.y) == PlantaDoAndar.CHAO;
    }

    // Acima de 8x8, cada quadradinho a mais de largura ou altura tira 3% (uma de 14x14 fica com 54%)
    @Override
    protected float preenchimento() {
        return Math.min(1f, 1.48f - (largura() + altura()) * 0.03f);
    }

    @Override
    protected int agrupamento() {
        return 2;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        portasComuns();
        sortearMancha();
        float chanceAVista = Acessorios.chanceDeArmadilhaAVista(TemaDoAndar.atual().mecanismo());
        float somaAVista = 0;
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                if (!naMancha(x, y)) {
                    continue;
                }
                switch (Dado.ate(5)) {
                    case 1 -> planta.definir(x, y, PlantaDoAndar.BRASAS);
                    case 2 -> planta.colocarArmadilha(x, y, TipoDeArmadilha.FOGO, true);
                    case 3 -> {
                        somaAVista += chanceAVista;
                        boolean aVista = somaAVista >= 1;
                        if (aVista) {
                            somaAVista--;
                        }
                        planta.colocarArmadilha(x, y, TipoDeArmadilha.FOGO, aVista);
                    }
                    case 4 -> planta.colocarArmadilhaDesarmada(x, y, TipoDeArmadilha.FOGO);
                    default -> {
                    }
                }
            }
        }
    }

    @Override
    public boolean podeTerAgua(Ponto p) {
        return !naMancha(p);
    }

    @Override
    public boolean podeTerGrama(Ponto p) {
        return !naMancha(p);
    }

    @Override
    public boolean podePorArmadilha(Ponto p) {
        return !naMancha(p);
    }
}
