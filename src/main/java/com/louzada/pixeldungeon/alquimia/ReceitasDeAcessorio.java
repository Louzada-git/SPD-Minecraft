package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.item.ItemAcessorio;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.world.item.ItemStack;

// As receitas dos acessórios (a página "acessórios" do guia de alquimia), iguais às do Shattered:
//   - o catalisador mágico, sozinho, por 6 de energia, vira um acessório: aparecem 3 pra escolher (no Shattered, 4;
//     o pote daqui mostra até 3 receitas de cada vez). Os 3 saem da semente da run (o mesmo catalisador mostra sempre os mesmos).
//   - um acessório sozinho sobe um nível (até o 3): 6 + 2 x nível de energia (o pedaço de papiro, a moita de musgo e a
//     resina maravilhosa custam 10 + 5 x nível)
public class ReceitasDeAcessorio {
    public static final int OPCOES = 3;
    public static final int CUSTO_DO_CATALISADOR = 6;
    private static final String CATALISADORES_USADOS = "catalisadores_usados";

    public static List<ReceitaDeAlquimia> todas() {
        List<ReceitaDeAlquimia> lista = new ArrayList<>();
        for (int i = 0; i < OPCOES; i++) {
            lista.add(new Catalisador(i));
        }
        lista.add(new Aprimorar());
        return lista;
    }

    // Os 3 acessórios que o catalisador oferece agora (diferentes entre si)
    public static List<TipoDeAcessorio> opcoes(long semente, DadosDaRun run) {
        List<TipoDeAcessorio> tipos = new ArrayList<>(Arrays.asList(TipoDeAcessorio.values()));
        Collections.shuffle(tipos, new Random(semente * 97 + 13 + run.quedas(CATALISADORES_USADOS)));
        return tipos.subList(0, OPCOES);
    }

    public static class Catalisador implements ReceitaDeAlquimia {
        private final int opcao;

        public Catalisador(int opcao) {
            this.opcao = opcao;
        }

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).is(ModItens.CATALISADOR_MAGICO.get());
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return CUSTO_DO_CATALISADOR;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.acessorio(opcoes(ingredientes.semente(), ingredientes.run()).get(opcao)));
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack acessorio = exemplo(ingredientes);
            ingredientes.get(0).shrink(1);
            ingredientes.run().contarQueda(CATALISADORES_USADOS);
            return acessorio;
        }
    }

    public static class Aprimorar implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ItemAcessorio
                    && NivelDoItem.nivel(ingredientes.get(0)) < TipoDeAcessorio.NIVEL_MAXIMO;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            ItemStack pilha = ingredientes.get(0);
            return ((ItemAcessorio) pilha.getItem()).tipo().custoParaAprimorar(NivelDoItem.nivel(pilha));
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            ItemStack aprimorado = ingredientes.get(0).copyWithCount(1);
            NivelDoItem.definirNivel(aprimorado, NivelDoItem.nivel(aprimorado) + 1);
            return aprimorado;
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack aprimorado = exemplo(ingredientes);
            ingredientes.get(0).shrink(1);
            return aprimorado;
        }
    }
}
