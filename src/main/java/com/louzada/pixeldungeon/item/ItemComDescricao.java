package com.louzada.pixeldungeon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// Um item simples que só mostra a descrição do Shattered na dica ("item.pixeldungeon.<nome>.descricao")
public class ItemComDescricao extends Item {
    private String nome;

    public ItemComDescricao(Properties propriedades, String nome) {
        super(propriedades);
        this.nome = nome;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + nome + ".descricao", ChatFormatting.GRAY);
    }
}
