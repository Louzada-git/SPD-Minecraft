package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDeElixir;
import com.louzada.pixeldungeon.item.TipoDeInfusao;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDePocaoExotica;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de infusão e de elixir (a página "infusões e elixires" do guia de alquimia), com as energias do Shattered.
// O elixir de cura com mel ainda não tem receita (o mod ainda não tem o pote de mel).
public class ReceitasDeInfusao {

    public static List<ReceitaDeAlquimia> todas() {
        List<ReceitaDeAlquimia> lista = new ArrayList<>();
        // Infusões
        lista.add(infusao(TipoDeInfusao.CAUSTICA, Ingrediente.pocao(TipoDePocao.GAS_TOXICO), Ingrediente.item(ModItens.BOLHA_DE_GOSMA)));
        lista.add(infusao(TipoDeInfusao.NEVASCA, Ingrediente.pocao(TipoDePocao.GELO)));
        lista.add(infusao(TipoDeInfusao.INFERNAL, Ingrediente.pocao(TipoDePocao.CHAMA_LIQUIDA)));
        lista.add(infusao(TipoDeInfusao.CHOQUE, Ingrediente.pocao(TipoDePocao.GAS_PARALISANTE)));
        lista.add(infusao(TipoDeInfusao.AQUATICA, Ingrediente.pocaoExotica(TipoDePocaoExotica.NUVENS_TEMPESTUOSAS)));
        lista.add(new Instavel());
        // Elixires
        lista.add(elixir(TipoDeElixir.SANGUE_DE_DRAGAO, Ingrediente.pocaoExotica(TipoDePocaoExotica.BAFO_DE_DRAGAO)));
        lista.add(elixir(TipoDeElixir.TOQUE_GELIDO, Ingrediente.pocaoExotica(TipoDePocaoExotica.CONGELAMENTO_INSTANTANEO)));
        lista.add(elixir(TipoDeElixir.ESSENCIA_TOXICA, Ingrediente.pocaoExotica(TipoDePocaoExotica.GAS_CORROSIVO)));
        lista.add(elixir(TipoDeElixir.PODER, Ingrediente.pocao(TipoDePocao.FORCA)));
        lista.add(elixir(TipoDeElixir.QUEDA_SUAVE, Ingrediente.pocao(TipoDePocao.LEVITACAO)));
        lista.add(elixir(TipoDeElixir.ARMADURA_ARCANA, Ingrediente.pocaoExotica(TipoDePocaoExotica.ARMADURA_DE_BARRO), Ingrediente.item(ModItens.BOLHA_DE_GOSMA)));
        lista.add(elixir(TipoDeElixir.REJUVENESCIMENTO_AQUATICO, Ingrediente.pocao(TipoDePocao.CURA), Ingrediente.item(ModItens.BOLHA_DE_GOSMA)));
        return lista;
    }

    private static ReceitaSimples infusao(TipoDeInfusao tipo, Ingrediente... entradas) {
        return new ReceitaSimples(entradas, uns(entradas.length), tipo.custo(), () -> new ItemStack(ModItens.infusao(tipo), tipo.quantas()));
    }

    private static ReceitaSimples elixir(TipoDeElixir tipo, Ingrediente... entradas) {
        return new ReceitaSimples(entradas, uns(entradas.length), tipo.custo(), () -> new ItemStack(ModItens.elixir(tipo)));
    }

    private static int[] uns(int quantos) {
        int[] lista = new int[quantos];
        Arrays.fill(lista, 1);
        return lista;
    }

    // Infusão instável: uma poção qualquer (comum ou exótica, conhecida ou não) + uma semente, por 1 de energia
    public static class Instavel implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            if (ingredientes.quantos() != 2) {
                return false;
            }
            boolean pocao = false;
            boolean semente = false;
            for (ItemStack pilha : ingredientes.pilhas()) {
                pocao |= pilha.getItem() instanceof ItemPocao || pilha.getItem() instanceof ItemPocaoExotica;
                semente |= pilha.getItem() instanceof ItemSemente;
            }
            return pocao && semente;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return TipoDeInfusao.INSTAVEL.custo();
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.infusao(TipoDeInfusao.INSTAVEL));
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
