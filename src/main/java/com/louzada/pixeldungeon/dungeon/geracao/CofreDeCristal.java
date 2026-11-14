package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// O cofre de cristal (o "CrystalVaultRoom" do Shattered): uma sala 7x7 com 2 baús de cristal em pedestais, um de frente
// pro outro. Cada um guarda uma varinha, um anel ou um artefato (de tipos diferentes), mas o andar só tem uma chave de
// cristal: dá pra abrir um só. A porta fica trancada (chave de ferro).
public class CofreDeCristal extends ComodoEspecial {
    // As 8 casas em volta do meio, em círculo (o "CIRCLE8" do Shattered)
    private static final int[][] CIRCULO = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 } };

    @Override
    public int minLargura() {
        return 7;
    }

    @Override
    public int maxLargura() {
        return 7;
    }

    @Override
    public int minAltura() {
        return 7;
    }

    @Override
    public int maxAltura() {
        return 7;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        Pincel.preencher(planta, this, 2, PlantaDoAndar.CHAO);
        Ponto c = centro();
        Porta porta = entrada();
        Ponto primeiro;
        Ponto segundo;
        do {
            int indice = Dado.ate(CIRCULO.length);
            primeiro = new Ponto(c.x + CIRCULO[indice][0], c.y + CIRCULO[indice][1]);
            segundo = new Ponto(c.x + CIRCULO[(indice + 4) % 8][0], c.y + CIRCULO[(indice + 4) % 8][1]);
        } while (encosta(primeiro, porta) || encosta(segundo, porta));
        for (Ponto lugar : new Ponto[] { primeiro, segundo }) {
            Pincel.definir(planta, lugar, PlantaDoAndar.PEDESTAL);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.PREMIO_DE_CRISTAL, Recipiente.BAU_DE_CRISTAL);
        }
        planta.pedir(PlantaDoAndar.ItemPedido.CHAVE_DE_CRISTAL);
        trancar(planta);
    }

    private static boolean encosta(Ponto a, Ponto b) {
        return Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1;
    }
}
