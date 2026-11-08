package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoComum.Escada;

// Gera um andar comum do esgoto igual ao Shattered (o "RegularLevel" + "SewerLevel"):
//   - a entrada, a saída, de 4 a 6 salas comuns (as grandes valem por 2 ou 3), 1 ou 2 salas especiais e as secretas
//   - metade das vezes o andar é uma volta, metade é um oito
//   - o arquiteto posiciona tudo (tenta de novo até dar certo) e o pintor desenha
public final class GeracaoDoShattered {
    private GeracaoDoShattered() {
    }

    // null se não coube no tamanho máximo (aí quem chamou tenta de novo)
    @Nullable
    public static PlantaDoAndar gerar(Random sorteio, int andar, List<ComodoEspecial.Tipo> especiais, List<ComodoSecreto.Tipo> secretas,
            int tamanhoMaximo) {
        Random anterior = Dado.atual();
        Dado.usar(sorteio);
        try {
            Arquiteto arquiteto = Dado.ate(2) == 0
                    ? new ArquitetoEmLaco().formaDaVolta(2, Dado.decimal(0f, 0.65f), Dado.decimal(0f, 0.50f))
                    : new ArquitetoEmOito().formaDaVolta(2, Dado.decimal(0.3f, 0.8f), 0f);
            List<Comodo> iniciais = comodosIniciais(andar, especiais, secretas);
            Dado.embaralhar(iniciais);

            List<Comodo> comodos = null;
            for (int tentativa = 0; tentativa < 200 && comodos == null; tentativa++) {
                for (Comodo comodo : iniciais) {
                    comodo.vizinhos.clear();
                    comodo.ligados.clear();
                }
                comodos = arquiteto.construir(new ArrayList<>(iniciais));
            }
            if (comodos == null) {
                return null;
            }
            PlantaDoAndar planta = PintorDoEsgoto.pintar(comodos, tamanhoMaximo, andar);
            if (planta == null || planta.entradaX() < 0 || !planta.temSaida()) {
                return null;
            }
            return planta;
        } finally {
            Dado.usar(anterior);
        }
    }

    private static List<Comodo> comodosIniciais(int andar, List<ComodoEspecial.Tipo> especiais, List<ComodoSecreto.Tipo> secretas) {
        List<Comodo> comodos = new ArrayList<>();
        comodos.add(ComodoComum.sortearComEscada(Escada.ENTRADA, andar));
        comodos.add(ComodoComum.sortearComEscada(Escada.SAIDA, andar));

        // De 4 a 6 salas comuns (5 na média)
        int comuns = 4 + Dado.chances(new float[] { 1, 3, 1 });
        for (int i = 0; i < comuns; i++) {
            ComodoComum sala;
            do {
                sala = ComodoComum.sortear();
            } while (!sala.sortearTamanho(comuns - i));
            i += sala.valor() - 1;
            comodos.add(sala);
        }

        for (ComodoEspecial.Tipo tipo : especiais) {
            comodos.add(tipo.criar());
        }
        for (ComodoSecreto.Tipo tipo : secretas) {
            comodos.add(tipo.criar());
        }
        return comodos;
    }
}
