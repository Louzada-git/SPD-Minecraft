package com.louzada.pixeldungeon.alquimia;

import java.util.function.Supplier;

import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePergaminhoExotico;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDePocaoExotica;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Um ingrediente de receita. Igual ao Shattered, as poções e os pergaminhos só servem se o herói já souber o que são.
public interface Ingrediente {

    boolean serve(ServerPlayer jogador, ItemStack pilha);

    static Ingrediente pocao(TipoDePocao tipo) {
        return (jogador, pilha) -> pilha.getItem() instanceof ItemPocao pocao && Identificacao.conhece(jogador, pocao.cor())
                && Identificacao.tipoDa(jogador, pocao.cor()) == tipo;
    }

    static Ingrediente pergaminho(TipoDePergaminho tipo) {
        return (jogador, pilha) -> pilha.getItem() instanceof ItemPergaminho pergaminho && Identificacao.conhece(jogador, pergaminho.runa())
                && Identificacao.tipoDa(jogador, pergaminho.runa()) == tipo;
    }

    static Ingrediente pocaoExotica(TipoDePocaoExotica tipo) {
        return (jogador, pilha) -> pilha.getItem() instanceof ItemPocaoExotica exotica && Identificacao.conhece(jogador, exotica.cor())
                && TipoDePocaoExotica.doBase(Identificacao.tipoDa(jogador, exotica.cor())) == tipo;
    }

    static Ingrediente pergaminhoExotico(TipoDePergaminhoExotico tipo) {
        return (jogador, pilha) -> pilha.getItem() instanceof ItemPergaminhoExotico exotico && Identificacao.conhece(jogador, exotico.runa())
                && TipoDePergaminhoExotico.doBase(Identificacao.tipoDa(jogador, exotico.runa())) == tipo;
    }

    static Ingrediente item(Supplier<? extends Item> item) {
        return (jogador, pilha) -> pilha.is(item.get());
    }
}
