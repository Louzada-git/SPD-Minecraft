package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModItens;

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

// Um pergaminho de uma runa. O que ele faz depende da run (veja IdentidadesDaRun).
// Clique direito: lê (1 turno). Ler sempre revela o tipo do pergaminho, igual ao Shattered.
// Alguns (melhoria, identificação, remover maldição, transmutação) abrem uma tela pra escolher o item.
public class ItemPergaminho extends Item {
    private RunaDePergaminho runa;

    public ItemPergaminho(Properties propriedades, RunaDePergaminho runa) {
        super(propriedades);
        this.runa = runa;
    }

    public RunaDePergaminho runa() {
        return runa;
    }

    public static Item daRuna(RunaDePergaminho runa) {
        return ModItens.PERGAMINHOS.get(runa.ordinal()).get();
    }

    @Override
    public Component getName(ItemStack pilha) {
        TipoDePergaminho conhecido = ConhecimentoDoCliente.pergaminho(runa);
        if (conhecido != null) {
            return Component.translatable(conhecido.chaveDoNome());
        }
        return super.getName(pilha);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer leitor)) {
            return InteractionResultHolder.success(pilha);
        }
        if (EfeitosDePergaminhoExotico.bloqueado(leitor, "pergaminho")) {
            return InteractionResultHolder.fail(pilha);
        }
        TipoDePergaminho tipo = Identificacao.tipoDa(leitor, runa);
        boolean eraDesconhecido = !Identificacao.conhece(leitor, runa);
        Identificacao.identificar(leitor, runa);
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        // Ler tira a invisibilidade (igual a usar qualquer item no Shattered)
        Furtividade.revelar(leitor);

        if (tipo.escolheItem()) {
            if (EscolhaDeItem.pedir(leitor, tipo, runa, eraDesconhecido)) {
                // O pergaminho só é gasto quando o item for escolhido
                return InteractionResultHolder.success(pilha);
            }
            leitor.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_sem_item").withStyle(ChatFormatting.GRAY), true);
            // Sem nada pra escolher: se o pergaminho era desconhecido, ele se gasta mesmo assim
            if (!eraDesconhecido) {
                return InteractionResultHolder.fail(pilha);
            }
        } else {
            EfeitosDePergaminho.ler(leitor, tipo);
            TalentosDoTier2.aoLerPergaminho(leitor, tipo, false);
        }
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.success(pilha);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDePergaminho conhecido = ConhecimentoDoCliente.pergaminho(runa);
        String descricao = conhecido != null ? conhecido.chaveDaDescricao() : "item.pixeldungeon.pergaminho.desconhecido.descricao";
        Textos.adicionarQuebrado(linhas, descricao, ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.pergaminho_como_usar").withStyle(ChatFormatting.DARK_GRAY));
    }
}
