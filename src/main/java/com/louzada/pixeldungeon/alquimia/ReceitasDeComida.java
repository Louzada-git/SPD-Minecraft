package com.louzada.pixeldungeon.alquimia;

import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.ItemCarneMisteriosa;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de comida do pote de alquimia (a página "comida" do guia de alquimia do Shattered)
public class ReceitasDeComida {

    // Carne misteriosa vira ensopado (sem os riscos de comer a carne): 1 carne custa 1 de energia, 2 custam 2,
    // e 3 também custam só 2. Com 3 carnes, o pote mostra as 3 receitas.
    public static class Ensopado implements ReceitaDeAlquimia {
        private int carnes;
        private int custo;

        public Ensopado(int carnes, int custo) {
            this.carnes = carnes;
            this.custo = custo;
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            int total = 0;
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (!(pilha.getItem() instanceof ItemCarneMisteriosa)) {
                    return false;
                }
                total += pilha.getCount();
            }
            return total >= carnes;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return custo;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.ENSOPADO.get(), carnes);
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            int faltam = carnes;
            for (ItemStack pilha : ingredientes.pilhas()) {
                int tira = Math.min(faltam, pilha.getCount());
                pilha.shrink(tira);
                faltam -= tira;
            }
            return new ItemStack(ModItens.ENSOPADO.get(), carnes);
        }
    }

    // Pastel + ração + carne (misteriosa, ensopado ou grelhada): torta de carne, por 6 de energia
    public static class TortaDeCarne implements ReceitaDeAlquimia {

        private static boolean carne(ItemStack pilha) {
            return pilha.getItem() instanceof ItemCarneMisteriosa || pilha.is(ModItens.ENSOPADO.get()) || pilha.is(ModItens.CARNE_GRELHADA.get());
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            if (ingredientes.quantos() != 3) {
                return false;
            }
            boolean pastel = false;
            boolean racao = false;
            boolean comCarne = false;
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (pilha.is(ModItens.PASTEL.get())) {
                    pastel = true;
                } else if (pilha.is(ModItens.RACAO.get())) {
                    racao = true;
                } else if (carne(pilha)) {
                    comCarne = true;
                }
            }
            return pastel && racao && comCarne;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 6;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.TORTA_DE_CARNE.get());
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            for (ItemStack pilha : ingredientes.pilhas()) {
                pilha.shrink(1);
            }
            return new ItemStack(ModItens.TORTA_DE_CARNE.get());
        }
    }

    // Fruta-sem-graça (crua) + semente: a fruta cozida com o efeito da poção daquela semente, por 2 de energia
    public static class CozinharFruta implements ReceitaDeAlquimia {

        private static ItemStack semente(Ingredientes ingredientes) {
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (pilha.getItem() instanceof ItemSemente) {
                    return pilha;
                }
            }
            return ItemStack.EMPTY;
        }

        private static ItemStack fruta(Ingredientes ingredientes) {
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (pilha.getItem() instanceof FrutaSemGraca && FrutaSemGraca.pocao(pilha) == null) {
                    return pilha;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            ItemStack semente = semente(ingredientes);
            return ingredientes.quantos() == 2 && !fruta(ingredientes).isEmpty() && !semente.isEmpty()
                    && ReceitasDePocao.POCAO_DA_SEMENTE.containsKey(((ItemSemente) semente.getItem()).planta());
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 2;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            ItemSemente semente = (ItemSemente) semente(ingredientes).getItem();
            return FrutaSemGraca.cozida(ReceitasDePocao.POCAO_DA_SEMENTE.get(semente.planta()), 1);
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack cozida = exemplo(ingredientes);
            fruta(ingredientes).shrink(1);
            semente(ingredientes).shrink(1);
            return cozida;
        }
    }
}
