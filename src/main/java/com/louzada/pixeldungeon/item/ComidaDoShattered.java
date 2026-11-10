package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Uma comida simples do Shattered (o quanto mata a fome segue a "energy" de lá: a ração, 300, é 10 de fome aqui):
//   carne grelhada (a carne misteriosa que pegou fogo) e ensopado de carne: 150 (5)
//   pastel: 450 (15)
//   torta de carne: 900 (30), e deixa bem alimentado
//   pedaços de fruta-sem-graça (sobra da fruta cozida arremessada): 450 (15)
public class ComidaDoShattered extends Item {
    // Bem alimentado (torta de carne): 450 turnos sem fome, curando 1 de vida a cada 18 turnos
    public static final int TURNOS_BEM_ALIMENTADO = 450;

    private String id;
    private boolean bemAlimentado;

    public ComidaDoShattered(Properties propriedades, String id, int fome, boolean bemAlimentado) {
        super(propriedades.food(new FoodProperties.Builder().nutrition(fome).saturationModifier(0.6F).build()));
        this.id = id;
        this.bemAlimentado = bemAlimentado;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pilha, Level mundo, LivingEntity quem) {
        ItemStack resto = super.finishUsingItem(pilha, mundo, quem);
        if (bemAlimentado && quem instanceof ServerPlayer jogador) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.BEM_ALIMENTADO, TURNOS_BEM_ALIMENTADO * Turno.TICKS));
        }
        return resto;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + id + ".descricao", ChatFormatting.GRAY);
    }
}
