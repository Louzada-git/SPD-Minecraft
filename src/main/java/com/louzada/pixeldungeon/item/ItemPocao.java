package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.PocaoArremessada;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Uma poção de uma cor. O que ela faz depende da run (veja IdentidadesDaRun).
//   clique direito: bebe na hora
//   agachado + clique direito: arremessa
// Igual ao Shattered, quem já sabe que a poção é ruim (gelo, chama, gases) arremessa com o clique direito.
public class ItemPocao extends Item {
    // Um tempinho entre uma poção e outra, pra não beber duas sem querer
    private static final int INTERVALO = 10;
    private CorDePocao cor;

    public ItemPocao(Properties propriedades, CorDePocao cor) {
        super(propriedades);
        this.cor = cor;
    }

    public CorDePocao cor() {
        return cor;
    }

    public static Item doTipo(CorDePocao cor) {
        return ModItens.POCOES.get(cor.ordinal()).get();
    }

    // O tipo, se o jogador já descobriu (no servidor vale o que ele sabe; na tela, o que o servidor contou)
    private TipoDePocao tipoConhecido(Player jogador) {
        if (jogador instanceof ServerPlayer noServidor) {
            return Identificacao.conhece(noServidor, cor) ? Identificacao.tipoDa(noServidor, cor) : null;
        }
        return ConhecimentoDoCliente.pocao(cor);
    }

    // Se o jogador já sabe o que é, mostra o nome de verdade; senão, só a cor
    @Override
    public Component getName(ItemStack pilha) {
        TipoDePocao conhecido = ConhecimentoDoCliente.pocao(cor);
        if (conhecido != null) {
            return Component.translatable(conhecido.chaveDoNome());
        }
        return super.getName(pilha);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        TipoDePocao conhecido = tipoConhecido(jogador);
        if (jogador.isShiftKeyDown() || (conhecido != null && conhecido.ruim())) {
            arremessar(mundo, jogador, pilha);
            return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
        }
        // Bebe na hora (sem a animação de beber do Minecraft)
        if (!mundo.isClientSide()) {
            finishUsingItem(pilha, mundo, jogador);
        }
        jogador.getCooldowns().addCooldown(this, INTERVALO);
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    private void arremessar(Level mundo, Player jogador, ItemStack pilha) {
        if (!mundo.isClientSide()) {
            PocaoArremessada pocao = new PocaoArremessada(mundo, jogador);
            pocao.setItem(pilha.copyWithCount(1));
            pocao.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), -10.0F, 0.9F, 1.0F);
            mundo.addFreshEntity(pocao);
            // Arremessar também conta pros talentos de poção (natureza líquida, tenacidade líquida, agilidade líquida)
            if (jogador instanceof ServerPlayer heroi) {
                TalentosDoTier2.aoUsarPocao(heroi, Identificacao.tipoDa(heroi, cor));
            }
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.5F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
    }

    // Bebeu: o efeito e, igual ao Shattered, descobre o tipo
    @Override
    public ItemStack finishUsingItem(ItemStack pilha, Level mundo, LivingEntity quem) {
        if (quem instanceof ServerPlayer jogador) {
            TipoDePocao tipo = Identificacao.tipoDa(jogador, cor);
            Identificacao.identificar(jogador, cor);
            EfeitosDePocao.beber(jogador, tipo, cor);
            TalentosDoTier2.aoUsarPocao(jogador, tipo);
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.2F);
            if (!jogador.getAbilities().instabuild) {
                pilha.shrink(1);
            }
        }
        return pilha;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDePocao conhecido = ConhecimentoDoCliente.pocao(cor);
        String descricao = conhecido != null ? conhecido.chaveDaDescricao() : "item.pixeldungeon.pocao.desconhecida.descricao";
        Textos.adicionarQuebrado(linhas, descricao, ChatFormatting.GRAY);
        String comoUsar = conhecido != null && conhecido.ruim() ? "dica.pixeldungeon.pocao_ruim_como_usar" : "dica.pixeldungeon.pocao_como_usar";
        linhas.add(Component.translatable(comoUsar).withStyle(ChatFormatting.DARK_GRAY));
    }
}
