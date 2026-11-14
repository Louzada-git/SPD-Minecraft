package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.item.Acessorios;

// O campo minado (o "MinefieldRoom" do Shattered): armadilhas explosivas escondidas espalhadas pela sala
// (a raiz da área: 3 a menos nas normais, 3 a mais nas grandes e 9 a mais nas gigantes), com brasas em volta de cada uma.
// O mecanismo de armadilha às vezes deixa uma delas à vista.
public class CampoMinado extends ComodoComum {
    private static final int[][] VIZINHOS = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 } };

    @Override
    public float[] chancesDoTamanho() {
        return new float[] { 4, 1, 0 };
    }

    @Override
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        Ponto dentro = pontoDentro(p, 1);
        return planta.tipo(dentro.x, dentro.y) == PlantaDoAndar.CHAO;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        portasComuns();
        int minas = (int) Math.round(Math.sqrt(largura() * altura()));
        minas += switch (tamanho) {
            case NORMAL -> -3;
            case GRANDE -> 3;
            case GIGANTE -> 9;
        };
        float chanceAVista = Acessorios.chanceDeArmadilhaAVista(TemaDoAndar.atual().mecanismo());
        float somaAVista = 0;
        for (int i = 0; i < minas; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio(1);
            } while (planta.temArmadilha(lugar.x, lugar.y));
            // Umas brasas em volta da mina
            for (int j = 0; j < 8; j++) {
                int[] v = VIZINHOS[Dado.ate(8)];
                int x = lugar.x + v[0];
                int y = lugar.y + v[1];
                if (!planta.temArmadilha(x, y) && planta.tipo(x, y) == PlantaDoAndar.CHAO) {
                    planta.definir(x, y, PlantaDoAndar.BRASAS);
                }
            }
            somaAVista += chanceAVista;
            boolean aVista = somaAVista >= 1;
            if (aVista) {
                somaAVista--;
            }
            planta.definir(lugar.x, lugar.y, PlantaDoAndar.CHAO);
            planta.colocarArmadilha(lugar.x, lugar.y, TipoDeArmadilha.EXPLOSIVA, aVista);
        }
    }
}
