package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// Todas as receitas do pote de alquimia (as do esgoto do Shattered: as 5 primeiras páginas do guia de alquimia),
// e quanta energia cada item dá quando é energizado.
public class ReceitasDaAlquimia {
    private static final List<ReceitaDeAlquimia> RECEITAS = new ArrayList<>();
    static {
        RECEITAS.add(new ReceitasDePocao.DeSementes());
        RECEITAS.add(new ReceitasDePedra.DePergaminho());
        RECEITAS.add(new ReceitasDePocao.ParaExotica());
        RECEITAS.add(new ReceitasDePedra.ParaExotico());
        RECEITAS.add(new ReceitasDeComida.Ensopado(1, 1));
        RECEITAS.add(new ReceitasDeComida.Ensopado(2, 2));
        RECEITAS.add(new ReceitasDeComida.Ensopado(3, 2));
        RECEITAS.add(new ReceitasDeComida.TortaDeCarne());
        RECEITAS.add(new ReceitasDeComida.CozinharFruta());
    }

    public static List<ReceitaDeAlquimia> todas() {
        return RECEITAS;
    }

    // Igual ao Shattered, os mesmos ingredientes podem servir pra mais de uma receita (um pergaminho vira pedras
    // ou o pergaminho exótico): o pote mostra todas (até 3) e o herói escolhe qual misturar
    public static final int RECEITAS_POR_VEZ = 3;

    public static List<ReceitaDeAlquimia> achar(Ingredientes ingredientes) {
        List<ReceitaDeAlquimia> achadas = new ArrayList<>();
        if (ingredientes.quantos() == 0) {
            return achadas;
        }
        for (ReceitaDeAlquimia receita : RECEITAS) {
            if (receita.serve(ingredientes) && achadas.size() < RECEITAS_POR_VEZ) {
                achadas.add(receita);
            }
        }
        return achadas;
    }

    // ---- Energizar ----

    // Quanta energia a pilha inteira dá (0 = não dá pra energizar), igual ao "energyVal" do Shattered:
    // poção e pergaminho 6 (a poção de força, a de experiência, o pergaminho de melhoria e o de transmutação dão 10,
    // se o herói já souber o que são), poção exótica 4 a mais que a comum dela, pergaminho exótico 6 a mais, pedra rúnica 3 (a de encantamento e a de
    // alteração, 5), a bolha de gosma 3
    public static int energia(ServerPlayer jogador, ItemStack pilha) {
        return energiaDeUm(jogador, pilha) * pilha.getCount();
    }

    public static int energiaDeUm(ServerPlayer jogador, ItemStack pilha) {
        if (pilha.isEmpty()) {
            return 0;
        }
        if (pilha.getItem() instanceof ItemPocao pocao) {
            TipoDePocao tipo = Identificacao.tipoDa(jogador, pocao.cor());
            boolean valiosa = tipo == TipoDePocao.FORCA || tipo == TipoDePocao.EXPERIENCIA;
            return valiosa && Identificacao.conhece(jogador, pocao.cor()) ? 10 : 6;
        }
        if (pilha.getItem() instanceof ItemPocaoExotica exotica) {
            TipoDePocao tipo = Identificacao.tipoDa(jogador, exotica.cor());
            boolean valiosa = tipo == TipoDePocao.FORCA || tipo == TipoDePocao.EXPERIENCIA;
            return (valiosa && Identificacao.conhece(jogador, exotica.cor()) ? 10 : 6) + 4;
        }
        if (pilha.getItem() instanceof ItemPergaminhoExotico exotico) {
            TipoDePergaminho tipo = Identificacao.tipoDa(jogador, exotico.runa());
            boolean valioso = tipo == TipoDePergaminho.MELHORIA || tipo == TipoDePergaminho.TRANSMUTACAO;
            return (valioso && Identificacao.conhece(jogador, exotico.runa()) ? 10 : 6) + 6;
        }
        if (pilha.getItem() instanceof ItemPergaminho pergaminho) {
            TipoDePergaminho tipo = Identificacao.tipoDa(jogador, pergaminho.runa());
            boolean valioso = tipo == TipoDePergaminho.MELHORIA || tipo == TipoDePergaminho.TRANSMUTACAO;
            return valioso && Identificacao.conhece(jogador, pergaminho.runa()) ? 10 : 6;
        }
        if (pilha.getItem() instanceof ItemPedraRunica pedra) {
            return pedra.tipo().energia();
        }
        if (pilha.is(ModItens.BOLHA_DE_GOSMA.get())) {
            return 3;
        }
        return 0;
    }

    // Energiza a pilha inteira: devolve a energia (quem chama soma no herói). Igual ao Shattered,
    // energizar uma poção ou um pergaminho identifica o tipo.
    public static int energizar(ServerPlayer jogador, ItemStack pilha) {
        int energia = energia(jogador, pilha);
        if (energia <= 0) {
            return 0;
        }
        Identificacao.identificarItem(jogador, pilha);
        pilha.setCount(0);
        return energia;
    }
}
