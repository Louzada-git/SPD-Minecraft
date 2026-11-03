package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Odre (o "waterskin" do Shattered). Todo herói começa com um.
// Guarda até 20 gotas de orvalho. Beber (clique direito) cura 5% da vida máxima por gota,
// e só gasta as gotas que precisa pra encher a vida. Beber gasta 1 turno.
public class Odre extends Item {
    public static final int VOLUME_MAXIMO = 20;
    private static final int COR_DA_BARRA = 0x3F76E4;

    public Odre(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    public static int volume(ItemStack pilha) {
        return Math.min(VOLUME_MAXIMO, pilha.getOrDefault(ModComponentes.VOLUME.get(), 0));
    }

    public static boolean cheio(ItemStack pilha) {
        return volume(pilha) >= VOLUME_MAXIMO;
    }

    public static void adicionar(ItemStack pilha, int gotas) {
        pilha.set(ModComponentes.VOLUME.get(), Math.min(VOLUME_MAXIMO, volume(pilha) + gotas));
    }

    // O odre que o jogador está carregando (ou null)
    @Nullable
    public static ItemStack acharOdre(Player jogador) {
        if (jogador.getOffhandItem().getItem() instanceof Odre) {
            return jogador.getOffhandItem();
        }
        for (ItemStack pilha : jogador.getInventory().items) {
            if (pilha.getItem() instanceof Odre) {
                return pilha;
            }
        }
        return null;
    }

    // Clique direito: beber
    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        int volume = volume(pilha);
        if (mundo.isClientSide()) {
            return volume > 0 && jogador.getHealth() < jogador.getMaxHealth() ? InteractionResultHolder.success(pilha)
                    : InteractionResultHolder.fail(pilha);
        }
        if (volume <= 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.odre_vazio").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(pilha);
        }
        float vidaMaxima = jogador.getMaxHealth();
        float faltando = vidaMaxima - jogador.getHealth();
        if (faltando <= 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.vida_cheia").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(pilha);
        }
        // Igual ao Shattered: cada gota vale 5% da vida, e só bebe o que precisa
        int gotas = (int) Math.ceil(faltando / (vidaMaxima * 0.05F) - 0.01F);
        gotas = Math.max(1, Math.min(gotas, volume));
        int cura = Math.min(Math.round(faltando), Math.round(vidaMaxima * 0.05F * gotas));
        jogador.heal(cura);
        NumerosFlutuantes.mostrar(jogador, "+" + cura, NumerosFlutuantes.VERDE);
        pilha.set(ModComponentes.VOLUME.get(), volume - gotas);
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        return InteractionResultHolder.success(pilha);
    }

    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Math.round(13.0F * volume(pilha) / VOLUME_MAXIMO);
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
        int volume = volume(pilha);
        linhas.add(Component.translatable("dica.pixeldungeon.odre_beber").withStyle(ChatFormatting.AQUA));
        linhas.add(Component.translatable("dica.pixeldungeon.gotas", volume, VOLUME_MAXIMO).withStyle(ChatFormatting.AQUA));
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.odre.descricao", ChatFormatting.GRAY);
        Textos.adicionarQuebrado(linhas, volume == 0 ? "item.pixeldungeon.odre.agua" : "item.pixeldungeon.odre.cura", ChatFormatting.DARK_GRAY);
    }
}
