package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// O cemitério gramado (o "GrassyGraveRoom" do Shattered): uma sala de grama com uma fileira de túmulos (um a cada 2 quadradinhos,
// no sentido do lado mais comprido). Um dos túmulos guarda um item qualquer; os outros, ouro. Quem abre um túmulo solta
// os espectros em volta. Junto com outro cemitério ou com a sala das plantas, a parede que some vira grama.
public class CemiterioGramado extends ComodoComum {

    @Override
    public void juntar(PlantaDoAndar planta, Comodo outro, Retangulo pedaco, byte tipo) {
        super.juntar(planta, outro, pedaco, tipo);
        if (tipo == PlantaDoAndar.CHAO && (outro instanceof CemiterioGramado || outro instanceof SalaDasPlantas)) {
            for (int y = pedaco.cima; y < pedaco.baixo; y++) {
                for (int x = pedaco.esquerda; x < pedaco.direita; x++) {
                    planta.definirGrama(x, y, PlantaDoAndar.GRAMA);
                }
            }
        }
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        portasComuns();
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        for (int y = cima + 1; y < baixo; y++) {
            for (int x = esquerda + 1; x < direita; x++) {
                planta.definirGrama(x, y, PlantaDoAndar.GRAMA);
            }
        }
        int l = largura() - 2;
        int a = altura() - 2;
        int tumulos = Math.max(l, a) / 2;
        int doItem = Dado.ate(tumulos);
        int deslocamento = Dado.ate(2);
        for (int i = 0; i < tumulos; i++) {
            int x;
            int y;
            if (l > a) {
                x = esquerda + 1 + deslocamento + i * 2;
                y = cima + 2 + Dado.ate(a - 2);
            } else {
                x = esquerda + 2 + Dado.ate(l - 2);
                y = cima + 1 + deslocamento + i * 2;
            }
            planta.adicionarPremio(x, y, i == doItem ? Premio.ITEM_ALEATORIO : Premio.OURO, Recipiente.TUMULO);
        }
    }
}
