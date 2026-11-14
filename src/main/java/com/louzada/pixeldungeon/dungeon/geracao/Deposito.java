package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O depósito (o "StorageRoom" do Shattered): chão especial e 3 ou 4 itens (às vezes um pote de mel), atrás de uma
// barricada de madeira. A barricada não abre: só queimando. O andar ganha uma poção de chama líquida.
// Os itens (ver SaqueDoAndar): 2 em 3 vezes, um dos itens que o andar ia ter; senão, uma poção, um pergaminho, comida ou ouro.
public class Deposito extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        boolean poteDeMel = Dado.ate(2) == 0;
        int quantos = Dado.entre(3, 4);
        for (int i = 0; i < quantos; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, poteDeMel ? Premio.POTE_DE_MEL : Premio.PREMIO_DO_DEPOSITO, false);
            poteDeMel = false;
        }
        entrada().definir(Porta.Tipo.BARRICADA);
        planta.pedir(PlantaDoAndar.ItemPedido.CHAMA_LIQUIDA);
    }
}
