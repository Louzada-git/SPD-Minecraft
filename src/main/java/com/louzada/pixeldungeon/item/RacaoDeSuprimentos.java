package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Ração de suprimentos (talento rações guardadas do Ladino). Igual ao Shattered:
// come rápido, mata um pouco menos de fome que a ração, cura 5 de vida e dá 1 carga pra capa das sombras.
public class RacaoDeSuprimentos extends Item {
    public RacaoDeSuprimentos(Properties propriedades) {
        super(propriedades.food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.6F).fast().build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pilha, Level mundo, LivingEntity quemComeu) {
        if (quemComeu instanceof ServerPlayer jogador) {
            jogador.heal(5);
            NumerosFlutuantes.mostrar(jogador, "+5", NumerosFlutuantes.VERDE);
            ItemStack capa = CapaDasSombras.acharCapa(jogador);
            if (capa != null) {
                Cargas.somar(capa, Artefato.cargasMaximas(capa), 1);
            }
        }
        return super.finishUsingItem(pilha, mundo, quemComeu);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.racao_de_suprimentos.descricao", ChatFormatting.GRAY);
    }
}
