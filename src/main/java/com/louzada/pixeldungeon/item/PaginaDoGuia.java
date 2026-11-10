package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.alquimia.GuiaDeAlquimia;
import com.louzada.pixeldungeon.rede.PacoteGuiaDeAlquimia;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

// Uma página rasgada do guia de alquimia (o "AlchemyPage" do Shattered): fica nos laboratórios.
// Clique direito pra ler: ela vira a primeira página do guia que o herói ainda não tem, e o guia abre nela.
// Quem já tem o guia inteiro não gasta a página (pode dar pra um amigo).
public class PaginaDoGuia extends Item {

    public PaginaDoGuia(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi) {
            ler(heroi, pilha);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // Devolve a página que o herói aprendeu (-1 = ele já tinha todas)
    public static int ler(ServerPlayer jogador, ItemStack pilha) {
        int pagina = GuiaDeAlquimia.lerProxima(jogador);
        if (pagina >= 0) {
            pilha.shrink(1);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pagina_do_guia.leu",
                    Component.translatable(GuiaDeAlquimia.chaveDoTitulo(pagina))).withStyle(ChatFormatting.YELLOW), false);
        } else {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pagina_do_guia.completo"), true);
        }
        // O estado (com as páginas) vai antes, pra tela já abrir com a página nova
        RedeDoHeroi.enviarEstado(jogador, false);
        PacketDistributor.sendToPlayer(jogador, new PacoteGuiaDeAlquimia(Math.max(0, pagina)));
        return pagina;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.pagina_do_guia.descricao", ChatFormatting.GRAY);
    }
}
