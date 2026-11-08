package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// O jardim secreto (o "SecretGardenRoom" do Shattered): grama com manchas de grama alta, uma flor-estrela
// e mais 3 plantas (no Shattered, as vagens e os orvalheiros da varinha de crescimento; o mod ainda não tem: viram plantas quaisquer).
public class JardimSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        int largura = largura() - 2;
        boolean[] alta = Mancha.gerar(largura, altura() - 2, 0.5f, 0, true);
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                boolean ehAlta = alta[(x - esquerda - 1) + (y - cima - 1) * largura];
                planta.definirGrama(x, y, ehAlta ? PlantaDoAndar.GRAMA_ALTA : PlantaDoAndar.GRAMA);
            }
        }
        esconder(planta);
        plantar(planta, Premio.PLANTA_ESTRELA);
        for (int i = 0; i < 3; i++) {
            plantar(planta, Premio.PLANTA_ALEATORIA);
        }
    }

    private void plantar(PlantaDoAndar planta, Premio premio) {
        Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
        planta.definirGrama(lugar.x, lugar.y, PlantaDoAndar.GRAMA);
        planta.adicionarPremio(lugar.x, lugar.y, premio, false);
    }
}
