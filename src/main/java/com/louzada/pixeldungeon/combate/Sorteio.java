package com.louzada.pixeldungeon.combate;

import net.minecraft.util.RandomSource;

// Sorteios do Shattered.
public class Sorteio {

    // "NormalIntRange" do Shattered: um número entre min e max, mais provável no meio do que nas pontas
    // (é a média de dois sorteios). Usado pro dano e pra armadura.
    public static int normal(RandomSource sorteio, int minimo, int maximo) {
        if (maximo <= minimo) {
            return minimo;
        }
        return minimo + (int) ((sorteio.nextFloat() + sorteio.nextFloat()) * (maximo - minimo + 1) / 2f);
    }

    // A mesma curva, com números quebrados (o "NormalFloat" do Shattered)
    public static float normalDecimal(RandomSource sorteio, float minimo, float maximo) {
        return minimo + (maximo - minimo) * (sorteio.nextFloat() + sorteio.nextFloat()) / 2.0F;
    }
}
