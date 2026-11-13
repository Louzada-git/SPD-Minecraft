package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDeFeitico;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePergaminhoExotico;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de feitiço (a página "feitiços" do guia de alquimia), iguais às do Shattered.
// A infusão de maldição, a armadilha recuperada, a energia selvagem (precisam de fragmento de metal) e a invocação de
// elemental (precisa de brasas) ainda não têm receita: os ingredientes só aparecem mais pra frente na masmorra.
public class ReceitasDeFeitico {

    public static List<ReceitaDeAlquimia> todas() {
        List<ReceitaDeAlquimia> lista = new ArrayList<>();
        lista.add(new DeDois(TipoDeFeitico.INSTAVEL, pilha -> pilha.getItem() instanceof ItemPergaminho || pilha.getItem() instanceof ItemPergaminhoExotico));
        lista.add(new DeDois(TipoDeFeitico.ALQUIMIZAR, pilha -> pilha.getItem() instanceof ItemSemente));
        lista.add(feitico(TipoDeFeitico.INFUSAO_MAGICA, Ingrediente.pergaminho(TipoDePergaminho.MELHORIA), 1));
        lista.add(feitico(TipoDeFeitico.MUDANCA_DE_FASE, Ingrediente.pergaminho(TipoDePergaminho.TELETRANSPORTE), 1));
        lista.add(feitico(TipoDeFeitico.RECICLAR, Ingrediente.pergaminho(TipoDePergaminho.TRANSMUTACAO), 1));
        lista.add(feitico(TipoDeFeitico.FAROL_DE_RETORNO, Ingrediente.pergaminhoExotico(TipoDePergaminhoExotico.PASSAGEM), 1));
        lista.add(feitico(TipoDeFeitico.AGARRAO_TELECINETICO, Ingrediente.item(ModItens.METAL_LIQUIDO), 10));
        return lista;
    }

    private static ReceitaSimples feitico(TipoDeFeitico tipo, Ingrediente entrada, int quantos) {
        return new ReceitaSimples(new Ingrediente[] { entrada }, new int[] { quantos }, tipo.custo(),
                () -> new ItemStack(ModItens.feitico(tipo), tipo.quantas()));
    }

    // Uma pedra rúnica qualquer + mais um ingrediente (o feitiço instável: um pergaminho qualquer, comum ou exótico,
    // conhecido ou não; o alquimizar: uma semente)
    public static class DeDois implements ReceitaDeAlquimia {
        private final TipoDeFeitico tipo;
        private final Predicate<ItemStack> outro;

        public DeDois(TipoDeFeitico tipo, Predicate<ItemStack> outro) {
            this.tipo = tipo;
            this.outro = outro;
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            if (ingredientes.quantos() != 2) {
                return false;
            }
            ItemStack a = ingredientes.get(0);
            ItemStack b = ingredientes.get(1);
            return (a.getItem() instanceof ItemPedraRunica && outro.test(b)) || (b.getItem() instanceof ItemPedraRunica && outro.test(a));
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return tipo.custo();
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.feitico(tipo), tipo.quantas());
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            for (ItemStack pilha : ingredientes.pilhas()) {
                pilha.shrink(1);
            }
            return exemplo(ingredientes);
        }
    }
}
