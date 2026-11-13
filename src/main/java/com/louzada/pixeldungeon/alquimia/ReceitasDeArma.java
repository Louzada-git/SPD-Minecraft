package com.louzada.pixeldungeon.alquimia;

import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.MetalLiquido;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.ResinaArcana;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de arma (a página "armas" do guia de alquimia): armas de arremesso viram metal líquido
// e varinhas viram resina arcana. Igual ao Shattered, só servem as que já se sabe que não são amaldiçoadas;
// se o nível ainda é desconhecido, o pote não mostra quanto vai sair.
public class ReceitasDeArma {

    private static boolean semMaldicao(ItemStack pilha) {
        return NivelDoItem.maldicaoConhecida(pilha) && !NivelDoItem.amaldicoado(pilha);
    }

    // Um conjunto de armas de arremesso + 3 de energia: metal líquido
    public static class MetalDeArremesso implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ArmaDeArremesso && semMaldicao(ingredientes.get(0));
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 3;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            ItemStack arma = ingredientes.get(0);
            return new ItemStack(ModItens.METAL_LIQUIDO.get(), NivelDoItem.identificado(arma) ? MetalLiquido.metalDoConjunto(arma) : 1);
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack arma = ingredientes.get(0);
            ItemStack metal = new ItemStack(ModItens.METAL_LIQUIDO.get(), Math.max(1, MetalLiquido.metalDoConjunto(arma)));
            arma.setCount(0);
            return metal;
        }
    }

    // Uma varinha + 5 de energia: resina arcana
    public static class ResinaDeVarinha implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ItemVarinha && semMaldicao(ingredientes.get(0));
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 5;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            ItemStack varinha = ingredientes.get(0);
            return new ItemStack(ModItens.RESINA_ARCANA.get(),
                    NivelDoItem.identificado(varinha) ? ResinaArcana.resinaDaVarinha(ingredientes.jogador(), varinha) : 1);
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack varinha = ingredientes.get(0);
            ItemStack resina = new ItemStack(ModItens.RESINA_ARCANA.get(), ResinaArcana.resinaDaVarinha(ingredientes.jogador(), varinha));
            varinha.setCount(0);
            return resina;
        }
    }
}
