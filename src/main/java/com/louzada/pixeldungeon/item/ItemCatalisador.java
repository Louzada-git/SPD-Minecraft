package com.louzada.pixeldungeon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// O catalisador mágico (o "TrinketCatalyst" do Shattered): um por run, nos andares 1 a 4 (o laboratório guarda ele,
// se o andar tiver um). No pote de alquimia, com 6 de energia, vira um acessório: o pote mostra 3 pra escolher.
public class ItemCatalisador extends Item {

    public ItemCatalisador(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag bandeira) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.catalisador_magico.descricao", ChatFormatting.GRAY);
    }
}
