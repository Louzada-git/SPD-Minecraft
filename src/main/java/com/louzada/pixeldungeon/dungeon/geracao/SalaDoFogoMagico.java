package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A sala do fogo mágico (o "MagicalFireRoom" do Shattered): uma parede de fogo eterno corta a sala no meio, e do outro lado
// ficam 3 ou 4 itens (às vezes um pote de mel). O fogo eterno não apaga sozinho: só com gelo (poção de gelo, bomba de gelo,
// nevasca) ou água. O andar ganha uma poção de gelo. Os itens (ver SaqueDoAndar): iguais aos do depósito.
public class SalaDoFogoMagico extends ComodoEspecial {

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
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Porta porta = entrada();
        porta.definir(Porta.Tipo.COMUM);

        Ponto fogo = centro();
        SubSala atras = new SubSala();
        if (porta.x == esquerda || porta.x == direita) {
            for (fogo.y = cima + 1; fogo.y != baixo; fogo.y++) {
                planta.adicionarFogoEterno(fogo.x, fogo.y);
                Pincel.definir(planta, fogo, PlantaDoAndar.CHAO_ESPECIAL);
            }
            if (porta.x == esquerda) {
                atras.definir(fogo.x + 1, cima + 1, direita - 1, baixo - 1);
            } else {
                atras.definir(esquerda + 1, cima + 1, fogo.x - 1, baixo - 1);
            }
        } else {
            for (fogo.x = esquerda + 1; fogo.x != direita; fogo.x++) {
                planta.adicionarFogoEterno(fogo.x, fogo.y);
                Pincel.definir(planta, fogo, PlantaDoAndar.CHAO_ESPECIAL);
            }
            if (porta.y == cima) {
                atras.definir(esquerda + 1, fogo.y + 1, direita - 1, baixo - 1);
            } else {
                atras.definir(esquerda + 1, cima + 1, direita - 1, fogo.y - 1);
            }
        }
        atras.preencher(planta, PlantaDoAndar.CHAO_ESPECIAL);

        boolean poteDeMel = Dado.ate(2) == 0;
        int quantos = Dado.entre(3, 4);
        for (int i = 0; i < quantos; i++) {
            Ponto lugar;
            do {
                lugar = atras.sorteado(0);
            } while (planta.temPremioEm(lugar.x, lugar.y));
            planta.adicionarPremio(lugar.x, lugar.y, poteDeMel ? Premio.POTE_DE_MEL : Premio.PREMIO_DO_DEPOSITO, false);
            poteDeMel = false;
        }
        planta.pedir(PlantaDoAndar.ItemPedido.GELO);
    }

    @Override
    public boolean podeTerGrama(Ponto p) {
        return false;
    }
}
