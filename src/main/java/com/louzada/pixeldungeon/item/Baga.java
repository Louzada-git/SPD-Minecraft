package com.louzada.pixeldungeon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// Baga da masmorra (talento recompensa da natureza da Caçadora): come rápido e mata um pouco de fome
public class Baga extends Item {
    public Baga(Properties propriedades) {
        super(propriedades.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).fast().build()));
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.baga.descricao", ChatFormatting.GRAY);
    }
}
