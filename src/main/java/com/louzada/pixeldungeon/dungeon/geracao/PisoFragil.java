package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// O piso frágil (o "WeakFloorRoom" do Shattered): quase toda a sala já desabou num abismo, e só sobrou um pouco de chão
// junto à porta. Quem cair aqui vai parar na sala do poço do andar de baixo (que sempre aparece depois desta sala), trancada
// por uma porta de cristal, com uma ossada que guarda um bom prêmio e a chave pra sair.
public class PisoFragil extends ComodoEspecial {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.ABISMO);
        Porta porta = entrada();
        porta.definir(Porta.Tipo.COMUM);
        if (porta.x == esquerda) {
            for (int y = cima + 1; y < baixo; y++) {
                Pincel.desenharDentro(planta, this, new Ponto(esquerda, y), Dado.entre(1, largura() - 4), PlantaDoAndar.CHAO_ESPECIAL);
            }
        } else if (porta.x == direita) {
            for (int y = cima + 1; y < baixo; y++) {
                Pincel.desenharDentro(planta, this, new Ponto(direita, y), Dado.entre(1, largura() - 4), PlantaDoAndar.CHAO_ESPECIAL);
            }
        } else if (porta.y == cima) {
            for (int x = esquerda + 1; x < direita; x++) {
                Pincel.desenharDentro(planta, this, new Ponto(x, cima), Dado.entre(1, altura() - 4), PlantaDoAndar.CHAO_ESPECIAL);
            }
        } else if (porta.y == baixo) {
            for (int x = esquerda + 1; x < direita; x++) {
                Pincel.desenharDentro(planta, this, new Ponto(x, baixo), Dado.entre(1, altura() - 4), PlantaDoAndar.CHAO_ESPECIAL);
            }
        }
        // (o Shattered ainda sorteia onde se vê o poço lá embaixo; o sorteio fica pra sala sair igual)
        Dado.ate(2);
    }

    @Override
    public void registrada(PlantaDoAndar planta, PlantaDoAndar.Sala sala) {
        planta.definirPisoFragil(sala);
    }
}
