package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O laboratório secreto (o "SecretLaboratoryRoom" do Shattered): chão especial, o pote de alquimia no meio,
// 2 montes de 3 a 5 cristais de energia e de 2 a 3 poções diferentes (as mais fracas são mais comuns).
public class LaboratorioSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        esconder(planta);

        Pincel.definir(planta, centro(), PlantaDoAndar.POTE);

        for (int i = 0; i < 2; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.CRISTAIS_DO_LABORATORIO_SECRETO, false);
        }
        int pocoes = Dado.entre(2, 3);
        for (int i = 0; i < pocoes; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO_ESPECIAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.POCAO_DO_LABORATORIO_SECRETO, false);
        }
    }
}
