package com.louzada.pixeldungeon.alquimia;

import java.util.LinkedHashMap;
import java.util.Map;

import com.louzada.pixeldungeon.item.ItemBomba;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de bomba (a página "bombas" do guia de alquimia): uma bomba comum + um ingrediente conhecido
// = uma bomba melhorada (o "EnhanceBomb" do Shattered). A energia depende da bomba (ver TipoDeBomba).
public class ReceitasDeBomba {
    // Igual ao Shattered: poção de gelo = gelo, imagem espelhada = lã, chama líquida = fogo, fúria = barulho,
    // invisibilidade = fumaça, recarga = clarão, cura = brotamento, remover maldição = sagrada, bolha de gosma = arcana
    public static final Map<Ingrediente, TipoDeBomba> INGREDIENTES = new LinkedHashMap<>();
    static {
        INGREDIENTES.put(Ingrediente.pocao(TipoDePocao.GELO), TipoDeBomba.GELO);
        INGREDIENTES.put(Ingrediente.pergaminho(TipoDePergaminho.IMAGEM_ESPELHADA), TipoDeBomba.LA);
        INGREDIENTES.put(Ingrediente.pocao(TipoDePocao.CHAMA_LIQUIDA), TipoDeBomba.FOGO);
        INGREDIENTES.put(Ingrediente.pergaminho(TipoDePergaminho.FURIA), TipoDeBomba.BARULHO);
        INGREDIENTES.put(Ingrediente.pocao(TipoDePocao.INVISIBILIDADE), TipoDeBomba.FUMACA);
        INGREDIENTES.put(Ingrediente.pergaminho(TipoDePergaminho.RECARGA), TipoDeBomba.CLARAO);
        INGREDIENTES.put(Ingrediente.pocao(TipoDePocao.CURA), TipoDeBomba.BROTAMENTO);
        INGREDIENTES.put(Ingrediente.pergaminho(TipoDePergaminho.REMOVER_MALDICAO), TipoDeBomba.SAGRADA);
        INGREDIENTES.put(Ingrediente.item(ModItens.BOLHA_DE_GOSMA), TipoDeBomba.ARCANA);
    }

    public static class Melhorar implements ReceitaDeAlquimia {

        // A bomba que sai (null: os ingredientes não servem)
        private TipoDeBomba resultado(Ingredientes ingredientes) {
            if (ingredientes.quantos() != 2) {
                return null;
            }
            boolean bomba = false;
            TipoDeBomba melhorada = null;
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (pilha.getItem() instanceof ItemBomba item && item.tipo() == TipoDeBomba.COMUM) {
                    bomba = true;
                    continue;
                }
                for (Map.Entry<Ingrediente, TipoDeBomba> ingrediente : INGREDIENTES.entrySet()) {
                    if (ingrediente.getKey().serve(ingredientes.jogador(), pilha)) {
                        melhorada = ingrediente.getValue();
                    }
                }
            }
            return bomba ? melhorada : null;
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return resultado(ingredientes) != null;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return resultado(ingredientes).custo();
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.bomba(resultado(ingredientes)));
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack saida = exemplo(ingredientes);
            for (ItemStack pilha : ingredientes.pilhas()) {
                pilha.shrink(1);
            }
            return saida;
        }
    }
}
