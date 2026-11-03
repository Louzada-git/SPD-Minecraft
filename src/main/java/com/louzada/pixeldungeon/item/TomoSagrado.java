package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.cliente.ClienteDoMod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Tomo sagrado do Clérigo (artefato). Clique direito abre a lista de magias.
// Igual ao Shattered: começa com 3 cargas, recarrega sozinho e sobe de nível com o uso (até o 10).
public class TomoSagrado extends Item {
    private static final int COR_DA_BARRA = 0xFFDD44;

    public TomoSagrado(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    // O tomo que o jogador está carregando (ou null)
    @Nullable
    public static ItemStack acharTomo(Player jogador) {
        if (jogador.getMainHandItem().getItem() instanceof TomoSagrado) {
            return jogador.getMainHandItem();
        }
        if (jogador.getOffhandItem().getItem() instanceof TomoSagrado) {
            return jogador.getOffhandItem();
        }
        for (ItemStack pilha : jogador.getInventory().items) {
            if (pilha.getItem() instanceof TomoSagrado) {
                return pilha;
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (mundo.isClientSide()) {
            // Só no jogo do jogador: abre a tela das magias
            ClienteDoMod.abrirTomo();
        }
        return InteractionResultHolder.success(pilha);
    }

    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && Turno.virou(dono)) {
            Cargas.recarregar(pilha, mundo.getGameTime(), Artefato.cargasMaximas(pilha), Artefato::ticksParaCarregar);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Cargas.larguraDaBarra(pilha, Artefato.cargasMaximas(pilha));
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return COR_DA_BARRA;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack depois, boolean trocouDeEspaco) {
        return trocouDeEspaco || !ItemStack.isSameItem(antes, depois);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        int maximo = Artefato.cargasMaximas(pilha);
        linhas.add(Component.translatable("dica.pixeldungeon.tomo_usar").withStyle(ChatFormatting.YELLOW));
        linhas.add(Component.translatable("dica.pixeldungeon.cargas", Cargas.cargas(pilha, maximo), maximo).withStyle(ChatFormatting.YELLOW));
        linhas.add(Component.translatable("dica.pixeldungeon.nivel_do_artefato", Artefato.nivel(pilha)).withStyle(ChatFormatting.GRAY));
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.tomo_sagrado.descricao", ChatFormatting.DARK_GRAY);
    }
}
