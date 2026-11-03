package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A cripta (o "CryptRoom" do Shattered): duas estátuas no fundo e um túmulo com uma armadura melhor
// que as do andar, +1, mas amaldiçoada (e já se sabe disso). No mod, o túmulo é um baú.
public class Cripta extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);

        Ponto c = centro();
        int cx = c.x;
        int cy = c.y;
        Porta porta = entrada();
        trancar(planta);
        if (porta.x == esquerda) {
            Pincel.definir(planta, direita - 1, cima + 1, PlantaDoAndar.ESTATUA);
            Pincel.definir(planta, direita - 1, baixo - 1, PlantaDoAndar.ESTATUA);
            cx = direita - 2;
        } else if (porta.x == direita) {
            Pincel.definir(planta, esquerda + 1, cima + 1, PlantaDoAndar.ESTATUA);
            Pincel.definir(planta, esquerda + 1, baixo - 1, PlantaDoAndar.ESTATUA);
            cx = esquerda + 2;
        } else if (porta.y == cima) {
            Pincel.definir(planta, esquerda + 1, baixo - 1, PlantaDoAndar.ESTATUA);
            Pincel.definir(planta, direita - 1, baixo - 1, PlantaDoAndar.ESTATUA);
            cy = baixo - 2;
        } else if (porta.y == baixo) {
            Pincel.definir(planta, esquerda + 1, cima + 1, PlantaDoAndar.ESTATUA);
            Pincel.definir(planta, direita - 1, cima + 1, PlantaDoAndar.ESTATUA);
            cy = cima + 2;
        }
        planta.adicionarPremio(cx, cy, Premio.ARMADURA_AMALDICOADA, true);
    }
}
