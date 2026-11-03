package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A biblioteca (o "LibraryRoom" do Shattered): chão especial, estantes na parede do fundo
// e de 1 a 3 pergaminhos (o primeiro é de identificação ou de remover maldição).
public class Biblioteca extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);

        Porta porta = entrada();
        Pincel.preencher(planta, esquerda + 1, cima + 1, largura() - 2, 1, PlantaDoAndar.ESTANTE);
        // Se a porta for na parede das estantes, a da frente dela sai
        Pincel.desenharDentro(planta, this, porta, 1, PlantaDoAndar.CHAO_ESPECIAL);

        int quantos = Dado.normalEntre(1, 3);
        for (int i = 0; i < quantos; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, i == 0 ? Premio.PERGAMINHO_DE_IDENTIFICAR_OU_REMOVER : Premio.PERGAMINHO, false);
        }
        trancar(planta);
    }
}
