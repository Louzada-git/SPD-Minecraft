package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// O poço secreto (o "SecretWellRoom" do Shattered): uma sala quase toda maciça, com um caminho da porta escondida até um poço
// mágico (de saúde ou de consciência) cercado de abismo.
public class PocoSecreto extends ComodoSecreto {

    // A porta não fica perto dos cantos
    @Override
    public boolean podeLigar(Ponto p) {
        return super.podeLigar(p) && ((p.x > esquerda + 1 && p.x < direita - 1) || (p.y > cima + 1 && p.y < baixo - 1));
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Porta porta = entrada();
        Ponto poco;
        if (porta.x == esquerda) {
            poco = new Ponto(direita - 2, porta.y);
        } else if (porta.x == direita) {
            poco = new Ponto(esquerda + 2, porta.y);
        } else if (porta.y == cima) {
            poco = new Ponto(porta.x, baixo - 2);
        } else {
            poco = new Ponto(porta.x, cima + 2);
        }
        Pincel.preencher(planta, poco.x - 1, poco.y - 1, 3, 3, PlantaDoAndar.ABISMO);
        Pincel.linha(planta, porta, poco, PlantaDoAndar.CHAO);
        planta.adicionarPoco(poco.x, poco.y, Dado.ate(2) == 1);
        esconder(planta);
    }

    @Override
    public boolean podeTerAgua(Ponto p) {
        return false;
    }

    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
