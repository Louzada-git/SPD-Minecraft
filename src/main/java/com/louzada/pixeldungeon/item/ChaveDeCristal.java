package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// A chave de cristal do Shattered: abre uma porta de cristal ou um baú de cristal do andar (e da run) dela, e é gasta.
// Aparece largada numa sala comum do andar que tem as salas de cristal (no cofre e na sala da escolha, uma; no caminho, três),
// ou na ossada da sala do poço.
public class ChaveDeCristal extends Item {

    public ChaveDeCristal(Properties propriedades) {
        super(propriedades.stacksTo(16));
    }

    public static ItemStack criar(int run, int andar) {
        ItemStack chave = new ItemStack(ModItens.CHAVE_DE_CRISTAL.get());
        chave.set(ModComponentes.RUN_DA_CHAVE.get(), run);
        chave.set(ModComponentes.ANDAR_DA_CHAVE.get(), andar);
        return chave;
    }

    // A chave desse andar no inventário do jogador (ou vazio se não tiver)
    public static ItemStack procurar(ServerPlayer jogador, int run, int andar) {
        for (ItemStack pilha : jogador.getInventory().items) {
            if (pilha.getItem() instanceof ChaveDeCristal && pilha.getOrDefault(ModComponentes.ANDAR_DA_CHAVE.get(), -1) == andar
                    && pilha.getOrDefault(ModComponentes.RUN_DA_CHAVE.get(), -1) == run) {
                return pilha;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Integer andar = pilha.get(ModComponentes.ANDAR_DA_CHAVE.get());
        if (andar != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.chave_do_andar", andar).withStyle(ChatFormatting.YELLOW));
        }
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.chave_de_cristal.descricao", ChatFormatting.GRAY);
    }
}
