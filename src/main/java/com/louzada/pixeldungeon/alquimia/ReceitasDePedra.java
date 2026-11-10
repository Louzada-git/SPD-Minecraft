package com.louzada.pixeldungeon.alquimia;

import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.TipoDePedra;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de pergaminho do pote de alquimia (as pedras rúnicas e o pergaminho exótico)
public class ReceitasDePedra {

    // 1 pergaminho: 2 pedras rúnicas da mesma "família" (o "ScrollToStone" do Shattered, ver TipoDePedra.pergaminho).
    // De graça. Igual ao Shattered, o pergaminho fica identificado; antes de misturar, se ele é desconhecido,
    // o pote só mostra "2 pedras rúnicas".
    public static class DePergaminho implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ItemPergaminho;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 0;
        }

        private TipoDePedra pedra(Ingredientes ingredientes) {
            return TipoDePedra.doPergaminho(Identificacao.tipoDoPergaminho(ingredientes.semente(), ingredientes.get(0)));
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            ItemPergaminho pergaminho = (ItemPergaminho) ingredientes.get(0).getItem();
            if (!Identificacao.conhece(ingredientes.jogador(), pergaminho.runa())) {
                return new ItemStack(ModItens.PEDRA_ALEATORIA.get(), 2);
            }
            return new ItemStack(ModItens.pedra(pedra(ingredientes)), 2);
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack pergaminho = ingredientes.get(0);
            TipoDePedra pedra = pedra(ingredientes);
            Identificacao.identificar(ingredientes.jogador(), ((ItemPergaminho) pergaminho.getItem()).runa());
            pergaminho.shrink(1);
            return new ItemStack(ModItens.pedra(pedra), 2);
        }
    }

    // 1 pergaminho + 6 de energia: o pergaminho exótico da mesma runa (o "ScrollToExotic" do Shattered).
    // Ele fica conhecido se o pergaminho comum já é.
    public static class ParaExotico implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ItemPergaminho;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 6;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ItemPergaminhoExotico.daRuna(((ItemPergaminho) ingredientes.get(0).getItem()).runa()));
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack exotico = exemplo(ingredientes);
            ingredientes.get(0).shrink(1);
            return exotico;
        }
    }
}
