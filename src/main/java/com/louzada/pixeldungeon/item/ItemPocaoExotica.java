package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.PocaoArremessada;
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

// Uma poção exótica de uma cor (o frasco redondo). O que ela faz depende da poção comum da mesma cor nesta run
// (ver TipoDePocaoExotica e EfeitosDePocaoExotica).
//   clique direito: bebe (as que pedem alguma coisa abrem a escolha)
//   agachado + clique direito: arremessa
// Igual às poções comuns, quem já sabe que ela é ruim arremessa com o clique direito.
public class ItemPocaoExotica extends Item {
    private static final int INTERVALO = 10;
    private CorDePocao cor;

    public ItemPocaoExotica(Properties propriedades, CorDePocao cor) {
        super(propriedades);
        this.cor = cor;
    }

    public CorDePocao cor() {
        return cor;
    }

    public static Item daCor(CorDePocao cor) {
        return ModItens.POCOES_EXOTICAS.get(cor.ordinal()).get();
    }

    // A exótica, se o jogador já conhece a poção comum dessa cor (no servidor vale o que ele sabe; na tela, o que o servidor contou)
    private TipoDePocaoExotica tipoConhecido(Player jogador) {
        if (jogador instanceof ServerPlayer noServidor) {
            return Identificacao.conhece(noServidor, cor) ? TipoDePocaoExotica.daCor(noServidor, cor) : null;
        }
        TipoDePocao base = ConhecimentoDoCliente.pocao(cor);
        return base == null ? null : TipoDePocaoExotica.doBase(base);
    }

    @Override
    public Component getName(ItemStack pilha) {
        TipoDePocao base = ConhecimentoDoCliente.pocao(cor);
        if (base != null) {
            return Component.translatable(TipoDePocaoExotica.doBase(base).chaveDoNome());
        }
        return super.getName(pilha);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        TipoDePocaoExotica conhecido = tipoConhecido(jogador);
        if (jogador.isShiftKeyDown() || (conhecido != null && conhecido.ruim())) {
            if (!mundo.isClientSide()) {
                PocaoArremessada pocao = new PocaoArremessada(mundo, jogador);
                pocao.setItem(pilha.copyWithCount(1));
                pocao.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), -10.0F, 0.9F, 1.0F);
                mundo.addFreshEntity(pocao);
            }
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.5F);
            jogador.getCooldowns().addCooldown(this, Turno.TICKS);
            if (!jogador.getAbilities().instabuild) {
                pilha.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
        }
        if (jogador instanceof ServerPlayer heroi) {
            EfeitosDePocaoExotica.beber(heroi, cor);
        }
        jogador.getCooldowns().addCooldown(this, INTERVALO);
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDePocao base = ConhecimentoDoCliente.pocao(cor);
        TipoDePocaoExotica conhecido = base == null ? null : TipoDePocaoExotica.doBase(base);
        Textos.adicionarQuebrado(linhas, conhecido != null ? conhecido.chaveDaDescricao() : "item.pixeldungeon.pocao_exotica.desconhecida.descricao",
                ChatFormatting.GRAY);
        String comoUsar = conhecido != null && conhecido.ruim() ? "dica.pixeldungeon.pocao_ruim_como_usar" : "dica.pixeldungeon.pocao_como_usar";
        linhas.add(Component.translatable(comoUsar).withStyle(ChatFormatting.DARK_GRAY));
    }
}
