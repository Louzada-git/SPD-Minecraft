package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// O pote quebrado (o "ShatteredPot" do Shattered): a casa da abelha que saiu dele. Não serve pra nada além de dizer à abelha
// onde ele está e com quem: ela defende o pote, e ataca quem estiver com ele.
public class PoteQuebrado extends Item {

    public PoteQuebrado(Properties propriedades) {
        super(propriedades);
    }

    public static ItemStack criar(int id) {
        ItemStack pote = new ItemStack(ModItens.POTE_QUEBRADO.get());
        pote.set(ModComponentes.ID_DO_POTE.get(), id);
        return pote;
    }

    // O número do pote (0 se não for um pote quebrado)
    public static int id(ItemStack pilha) {
        return pilha.getItem() instanceof PoteQuebrado ? pilha.getOrDefault(ModComponentes.ID_DO_POTE.get(), 0) : 0;
    }

    public static boolean temNoInventario(Player jogador, int id) {
        for (ItemStack pilha : jogador.getInventory().items) {
            if (id(pilha) == id) {
                return true;
            }
        }
        return id(jogador.getOffhandItem()) == id;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag bandeira) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.pote_quebrado.descricao", ChatFormatting.GRAY);
    }
}
