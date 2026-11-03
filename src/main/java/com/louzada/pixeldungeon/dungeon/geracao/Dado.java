package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.Collections;
import java.util.List;
import java.util.Random;

// O sorteio da geração dos andares (o "Random" do Shattered).
// A geração inteira usa um só sorteador, escolhido no começo: a mesma semente sempre gera o mesmo andar.
public final class Dado {
    private static Random atual = new Random();

    private Dado() {
    }

    static void usar(Random sorteio) {
        atual = sorteio;
    }

    static Random atual() {
        return atual;
    }

    // De 0 (incluído) até 1 (fora)
    public static float decimal() {
        return atual.nextFloat();
    }

    public static float decimal(float maximo) {
        return decimal() * maximo;
    }

    public static float decimal(float minimo, float maximo) {
        return minimo + decimal(maximo - minimo);
    }

    // De 0 até maximo - 1 (o "Random.Int(max)" do Shattered)
    public static int ate(int maximo) {
        return maximo > 0 ? (int) (decimal() * maximo) : 0;
    }

    // De minimo até maximo - 1 (o "Random.Int(min, max)" do Shattered)
    public static int de(int minimo, int maximo) {
        return minimo + ate(maximo - minimo);
    }

    // De minimo até maximo, os dois incluídos
    public static int entre(int minimo, int maximo) {
        return minimo + ate(maximo - minimo + 1);
    }

    // De minimo até maximo, mais provável perto do meio
    public static int normalEntre(int minimo, int maximo) {
        return minimo + (int) ((decimal() + decimal()) * (maximo - minimo + 1) / 2f);
    }

    // Sorteia uma posição da lista, com chance proporcional ao peso de cada uma (-1 se todas forem 0)
    public static int chances(float[] pesos) {
        float soma = 0;
        for (float peso : pesos) {
            soma += Math.max(0, peso);
        }
        if (soma <= 0) {
            return -1;
        }
        float valor = decimal(soma);
        float acumulado = 0;
        int ultimo = -1;
        for (int i = 0; i < pesos.length; i++) {
            if (pesos[i] <= 0) {
                continue;
            }
            acumulado += pesos[i];
            ultimo = i;
            if (valor < acumulado) {
                return i;
            }
        }
        return ultimo;
    }

    public static <T> T elemento(List<T> lista) {
        return lista.isEmpty() ? null : lista.get(ate(lista.size()));
    }

    public static void embaralhar(List<?> lista) {
        Collections.shuffle(lista, atual);
    }

    public static long numeroGrande() {
        return atual.nextLong();
    }
}
