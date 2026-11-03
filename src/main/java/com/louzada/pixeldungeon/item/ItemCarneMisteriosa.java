package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Carne misteriosa: cai de caranguejos e ratos albinos. Mata metade da fome de uma ração,
// mas igual ao Shattered, pode dar ruim: queimar, travar as pernas, envenenar ou deixar lento.
public class ItemCarneMisteriosa extends Item {

    public ItemCarneMisteriosa(Properties propriedades) {
        super(propriedades.food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.3F).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pilha, Level mundo, LivingEntity quem) {
        ItemStack resto = super.finishUsingItem(pilha, mundo, quem);
        if (quem instanceof ServerPlayer jogador) {
            switch (jogador.getRandom().nextInt(5)) {
                case 0 -> {
                    jogador.setRemainingFireTicks(5 * Turno.TICKS);
                    avisar(jogador, "item.pixeldungeon.carne_misteriosa.quente");
                }
                case 1 -> {
                    // Raízes: não consegue andar por um tempo
                    jogador.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * Turno.TICKS, 9));
                    avisar(jogador, "item.pixeldungeon.carne_misteriosa.pernas");
                }
                case 2 -> {
                    // Veneno de 1/5 da vida máxima (o veneno tira 1 de vida a cada 25 ticks)
                    int dano = Math.max(1, Math.round(jogador.getMaxHealth() / 5));
                    jogador.addEffect(new MobEffectInstance(MobEffects.POISON, dano * 25));
                    avisar(jogador, "item.pixeldungeon.carne_misteriosa.mal");
                }
                case 3 -> {
                    jogador.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * Turno.TICKS, 1));
                    avisar(jogador, "item.pixeldungeon.carne_misteriosa.estufado");
                }
                default -> avisar(jogador, "item.pixeldungeon.carne_misteriosa.esquisito");
            }
        }
        return resto;
    }

    private static void avisar(ServerPlayer jogador, String chave) {
        jogador.displayClientMessage(Component.translatable(chave), true);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("item.pixeldungeon.carne_misteriosa.descricao").withStyle(ChatFormatting.GRAY));
    }
}
