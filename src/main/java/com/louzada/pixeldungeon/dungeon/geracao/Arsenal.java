package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O arsenal (o "ArmoryRoom" do Shattered): uma estátua no canto do fundo e, no chão, 2 ou 3 prêmios diferentes
// entre bomba, arma, armadura e arma de arremesso.
public class Arsenal extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);

        Porta porta = entrada();
        Ponto estatua = null;
        if (porta.x == esquerda) {
            estatua = new Ponto(direita - 1, Dado.ate(2) == 0 ? cima + 1 : baixo - 1);
        } else if (porta.x == direita) {
            estatua = new Ponto(esquerda + 1, Dado.ate(2) == 0 ? cima + 1 : baixo - 1);
        } else if (porta.y == cima) {
            estatua = new Ponto(Dado.ate(2) == 0 ? esquerda + 1 : direita - 1, baixo - 1);
        } else if (porta.y == baixo) {
            estatua = new Ponto(Dado.ate(2) == 0 ? esquerda + 1 : direita - 1, cima + 1);
        }
        if (estatua != null) {
            Pincel.definir(planta, estatua, PlantaDoAndar.ESTATUA);
        }

        List<Premio> premios = new ArrayList<>(List.of(Premio.BOMBA, Premio.ARMA, Premio.ARMADURA, Premio.ARREMESSO));
        Dado.embaralhar(premios);
        int quantos = Dado.entre(2, 3);
        for (Premio premio : premios.subList(0, quantos)) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
            planta.adicionarPremio(lugar.x, lugar.y, premio, false);
        }
        trancar(planta);
    }
}
