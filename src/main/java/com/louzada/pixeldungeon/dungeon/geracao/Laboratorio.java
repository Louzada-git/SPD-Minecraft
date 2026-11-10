package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O laboratório (o "LaboratoryRoom" do Shattered): chão especial, o pote de alquimia num canto da parede
// do lado oposto à porta, 5 cristais de energia, 1 ou 2 prêmios (a poção de força do andar, se ele tiver uma;
// senão, uma poção ou uma pedra rúnica) e uma página do guia de alquimia. A porta fica trancada.
public class Laboratorio extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);

        Porta porta = entrada();
        boolean primeiro = Dado.ate(2) == 0;
        Ponto pote;
        if (porta.x == esquerda) {
            pote = new Ponto(direita - 1, primeiro ? cima + 1 : baixo - 1);
        } else if (porta.x == direita) {
            pote = new Ponto(esquerda + 1, primeiro ? cima + 1 : baixo - 1);
        } else if (porta.y == cima) {
            pote = new Ponto(primeiro ? esquerda + 1 : direita - 1, baixo - 1);
        } else {
            pote = new Ponto(primeiro ? esquerda + 1 : direita - 1, cima + 1);
        }
        Pincel.definir(planta, pote, PlantaDoAndar.POTE);

        Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
        planta.adicionarPremio(lugar.x, lugar.y, Premio.CRISTAIS_DO_LABORATORIO, false);
        int premios = Dado.normalEntre(1, 2);
        for (int i = 0; i < premios; i++) {
            lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.PREMIO_DO_LABORATORIO, false);
        }
        lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
        planta.adicionarPremio(lugar.x, lugar.y, Premio.PAGINA_DO_GUIA, false);

        trancar(planta);
    }
}
