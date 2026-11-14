package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;

// A sala do poço (o "PitRoom" do Shattered): aparece no andar logo abaixo de um piso frágil, e é onde cai quem cai de lá.
// Um poço seco num canto e, no meio, a ossada de quem caiu antes: um anel, um artefato ou um equipamento, mais 1 ou 2 miudezas
// e a chave de cristal que abre a porta (de cristal) da sala.
public class SalaDoPoco extends ComodoEspecial {

    // Um pouco maior que o mínimo, e não muito grande (pra dar pra ver o poço da porta)
    @Override
    public int minLargura() {
        return 6;
    }

    @Override
    public int minAltura() {
        return 6;
    }

    @Override
    public int maxLargura() {
        return 9;
    }

    @Override
    public int maxAltura() {
        return 9;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        Porta porta = entrada();
        porta.definir(Porta.Tipo.CRISTAL);
        Ponto poco = null;
        if (porta.x == esquerda) {
            poco = new Ponto(direita - 1, Dado.ate(2) == 0 ? cima + 1 : baixo - 1);
        } else if (porta.x == direita) {
            poco = new Ponto(esquerda + 1, Dado.ate(2) == 0 ? cima + 1 : baixo - 1);
        } else if (porta.y == cima) {
            poco = new Ponto(Dado.ate(2) == 0 ? esquerda + 1 : direita - 1, baixo - 1);
        } else if (porta.y == baixo) {
            poco = new Ponto(Dado.ate(2) == 0 ? esquerda + 1 : direita - 1, cima + 1);
        }
        Pincel.definir(planta, poco, PlantaDoAndar.POCO_VAZIO);
        Ponto ossada = centro();
        planta.adicionarPremio(ossada.x, ossada.y, Premio.PREMIO_DA_OSSADA, Recipiente.OSSADA);
        int miudezas = Dado.entre(1, 2);
        for (int i = 0; i < miudezas; i++) {
            planta.adicionarPremio(ossada.x, ossada.y, Premio.MIUDEZA_DA_OSSADA, Recipiente.CHAO);
        }
        planta.adicionarPremio(ossada.x, ossada.y, Premio.CHAVE_DE_CRISTAL, Recipiente.CHAO);
    }

    @Override
    public void registrada(PlantaDoAndar planta, PlantaDoAndar.Sala sala) {
        planta.definirSalaDoPoco(sala);
    }

    // Quem cai aqui já chega machucado: sem armadilhas
    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }

    // Pra ver o poço da porta
    @Override
    public boolean podeTerGrama(Ponto p) {
        return false;
    }
}
