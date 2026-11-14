package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// A sala do gás tóxico (o "ToxicGasRoom" do Shattered): uma estátua no meio, respiradouros soltando gás tóxico sem parar
// e a sala já cheia dele. Lá dentro, uma ossada com ouro dobrado (longe da porta) e 2 baús com ouro (ou o catalisador mágico,
// se ele estiver pra cair). A porta é comum, e o andar ganha uma poção de pureza (que limpa o gás).
public class SalaDeGasToxico extends ComodoEspecial {

    @Override
    public int minLargura() {
        return 7;
    }

    @Override
    public int minAltura() {
        return 7;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Pincel.definir(planta, centro(), PlantaDoAndar.ESTATUA);

        int respiradouros = Math.min(largura() - 2, altura() - 2);
        for (int i = 0; i < respiradouros; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio(2);
            } while (planta.tipo(lugar.x, lugar.y) != PlantaDoAndar.CHAO);
            Pincel.definir(planta, lugar, PlantaDoAndar.RESPIRADOURO);
        }

        // Sobram lugares de propósito, pra ossada ficar longe da porta
        List<Ponto> lugares = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Ponto lugar;
            do {
                lugar = aleatorio(2);
            } while (planta.tipo(lugar.x, lugar.y) == PlantaDoAndar.ESTATUA || lugares.contains(lugar));
            lugares.add(lugar);
        }
        Porta porta = entrada();
        Ponto maisLonge = null;
        for (Ponto lugar : lugares) {
            if (maisLonge == null || distancia(porta, lugar) > distancia(porta, maisLonge)) {
                maisLonge = lugar;
            }
        }
        lugares.remove(maisLonge);
        planta.adicionarPremio(maisLonge.x, maisLonge.y, Premio.OURO_DOBRADO, Recipiente.OSSADA);
        for (int i = 0; i < 2; i++) {
            Ponto lugar = lugares.remove(0);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.OURO_OU_CATALISADOR, Recipiente.BAU);
        }
        planta.pedir(PlantaDoAndar.ItemPedido.PUREZA);
        porta.definir(Porta.Tipo.COMUM);
    }

    private static double distancia(Ponto a, Ponto b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    // A sala já está cheia de perigo; as armadilhas do andar não caem aqui
    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
