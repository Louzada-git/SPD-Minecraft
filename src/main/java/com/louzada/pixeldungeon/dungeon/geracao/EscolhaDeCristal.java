package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// A escolha de cristal (o "CrystalChoiceRoom" do Shattered): logo depois da porta, duas portas de cristal, mas o andar só tem
// uma chave de cristal. Atrás de uma, 3 ou 4 poções e pergaminhos; atrás da outra, um baú com uma varinha, um anel ou um
// artefato. A porta da sala fica trancada (chave de ferro).
public class EscolhaDeCristal extends ComodoEspecial {

    @Override
    public int minLargura() {
        return 7;
    }

    @Override
    public int minAltura() {
        return 7;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Porta porta = entrada();
        SubSala entrada = new SubSala();
        SubSala sala1 = new SubSala();
        SubSala sala2 = new SubSala();
        if (porta.x == esquerda) {
            entrada.definir(esquerda + 1, cima + 1, esquerda + 2, baixo - 1);
            sala1.definir(entrada.direita + 2, cima + 1, direita - 1, centro().y - 1);
            sala2.definir(entrada.direita + 2, sala1.baixo + 2, direita - 1, baixo - 1);
            Pincel.definir(planta, entrada.direita + 1, (sala1.cima + sala1.baixo + 1) / 2, PlantaDoAndar.PORTA_DE_CRISTAL);
            Pincel.definir(planta, entrada.direita + 1, (sala2.cima + sala2.baixo) / 2, PlantaDoAndar.PORTA_DE_CRISTAL);
        } else if (porta.y == cima) {
            entrada.definir(esquerda + 1, cima + 1, direita - 1, cima + 2);
            sala1.definir(esquerda + 1, entrada.baixo + 2, centro().x - 1, baixo - 1);
            sala2.definir(sala1.direita + 2, entrada.baixo + 2, direita - 1, baixo - 1);
            Pincel.definir(planta, (sala1.esquerda + sala1.direita + 1) / 2, entrada.baixo + 1, PlantaDoAndar.PORTA_DE_CRISTAL);
            Pincel.definir(planta, (sala2.esquerda + sala2.direita) / 2, entrada.baixo + 1, PlantaDoAndar.PORTA_DE_CRISTAL);
        } else if (porta.x == direita) {
            entrada.definir(direita - 2, cima + 1, direita - 1, baixo - 1);
            sala1.definir(esquerda + 1, cima + 1, entrada.esquerda - 2, centro().y - 1);
            sala2.definir(esquerda + 1, sala1.baixo + 2, entrada.esquerda - 2, baixo - 1);
            Pincel.definir(planta, entrada.esquerda - 1, (sala1.cima + sala1.baixo + 1) / 2, PlantaDoAndar.PORTA_DE_CRISTAL);
            Pincel.definir(planta, entrada.esquerda - 1, (sala2.cima + sala2.baixo) / 2, PlantaDoAndar.PORTA_DE_CRISTAL);
        } else if (porta.y == baixo) {
            entrada.definir(esquerda + 1, baixo - 2, direita - 1, baixo - 1);
            sala1.definir(esquerda + 1, cima + 1, centro().x - 1, entrada.cima - 2);
            sala2.definir(sala1.direita + 2, cima + 1, direita - 1, entrada.cima - 2);
            Pincel.definir(planta, (sala1.esquerda + sala1.direita + 1) / 2, entrada.cima - 1, PlantaDoAndar.PORTA_DE_CRISTAL);
            Pincel.definir(planta, (sala2.esquerda + sala2.direita) / 2, entrada.cima - 1, PlantaDoAndar.PORTA_DE_CRISTAL);
        }
        entrada.preencher(planta, PlantaDoAndar.CHAO);
        sala1.preencher(planta, PlantaDoAndar.CHAO_ESPECIAL);
        sala2.preencher(planta, PlantaDoAndar.CHAO_ESPECIAL);
        if (Dado.ate(2) == 0) {
            SubSala troca = sala1;
            sala1 = sala2;
            sala2 = troca;
        }

        int quantos = Dado.normalEntre(3, 4);
        for (int i = 0; i < quantos; i++) {
            Ponto lugar;
            do {
                lugar = sala1.sorteado(sala1.quadradinhos() >= 16 ? 1 : 0);
            } while (planta.temPremioEm(lugar.x, lugar.y));
            planta.adicionarPremio(lugar.x, lugar.y, Premio.POCAO_OU_PERGAMINHO, false);
        }
        Ponto meio = sala2.meio();
        planta.adicionarPremio(meio.x, meio.y, Premio.PREMIO_ESCONDIDO, Recipiente.BAU);
        planta.pedir(PlantaDoAndar.ItemPedido.CHAVE_DE_CRISTAL);
        trancar(planta);
    }
}
