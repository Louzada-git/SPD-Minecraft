package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// A sala da estátua (o "StatueRoom" do Shattered): uma fileira de estátuas no fundo e, na frente delas, uma estátua animada
// segurando uma arma encantada. Ela fica parada até alguém mexer com ela; derrotada, deixa a arma. A porta fica trancada.
public class SalaDaEstatua extends ComodoEspecial {

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
            Pincel.preencher(planta, direita - 1, cima + 1, 1, altura() - 2, PlantaDoAndar.ESTATUA);
            cx = direita - 2;
        } else if (porta.x == direita) {
            Pincel.preencher(planta, esquerda + 1, cima + 1, 1, altura() - 2, PlantaDoAndar.ESTATUA);
            cx = esquerda + 2;
        } else if (porta.y == cima) {
            Pincel.preencher(planta, esquerda + 1, baixo - 1, largura() - 2, 1, PlantaDoAndar.ESTATUA);
            cy = baixo - 2;
        } else if (porta.y == baixo) {
            Pincel.preencher(planta, esquerda + 1, cima + 1, largura() - 2, 1, PlantaDoAndar.ESTATUA);
            cy = cima + 2;
        }
        planta.adicionarInimigo(cx, cy, PlantaDoAndar.InimigoDaSala.ESTATUA_ANIMADA, null, 0);
    }
}
