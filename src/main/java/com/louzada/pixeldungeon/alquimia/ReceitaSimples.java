package com.louzada.pixeldungeon.alquimia;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;

// Uma receita de ingredientes fixos (o "SimpleRecipe" do Shattered): uma casinha pra cada ingrediente, cada uma com
// pelo menos a quantidade pedida; gasta a energia e sai o resultado.
public class ReceitaSimples implements ReceitaDeAlquimia {
    private final Ingrediente[] entradas;
    private final int[] quantidades;
    private final int custo;
    private final Supplier<ItemStack> saida;

    public ReceitaSimples(Ingrediente[] entradas, int[] quantidades, int custo, Supplier<ItemStack> saida) {
        this.entradas = entradas;
        this.quantidades = quantidades;
        this.custo = custo;
        this.saida = saida;
    }

    // Um ingrediente só, 1 de cada
    public ReceitaSimples(Ingrediente entrada, int custo, Supplier<ItemStack> saida) {
        this(new Ingrediente[] { entrada }, new int[] { 1 }, custo, saida);
    }

    // Qual casinha serve pra cada ingrediente (null: não serve)
    private int[] casar(Ingredientes ingredientes) {
        if (ingredientes.quantos() != entradas.length) {
            return null;
        }
        int[] casinhas = new int[entradas.length];
        boolean[] usada = new boolean[ingredientes.quantos()];
        for (int e = 0; e < entradas.length; e++) {
            casinhas[e] = -1;
            for (int c = 0; c < ingredientes.quantos(); c++) {
                ItemStack pilha = ingredientes.get(c);
                if (!usada[c] && pilha.getCount() >= quantidades[e] && entradas[e].serve(ingredientes.jogador(), pilha)) {
                    casinhas[e] = c;
                    usada[c] = true;
                    break;
                }
            }
            if (casinhas[e] < 0) {
                return null;
            }
        }
        return casinhas;
    }

    @Override
    public boolean serve(Ingredientes ingredientes) {
        return casar(ingredientes) != null;
    }

    @Override
    public int custo(Ingredientes ingredientes) {
        return custo;
    }

    @Override
    public ItemStack exemplo(Ingredientes ingredientes) {
        return saida.get();
    }

    @Override
    public ItemStack misturar(Ingredientes ingredientes) {
        int[] casinhas = casar(ingredientes);
        for (int e = 0; e < entradas.length; e++) {
            ingredientes.get(casinhas[e]).shrink(quantidades[e]);
        }
        return saida.get();
    }
}
