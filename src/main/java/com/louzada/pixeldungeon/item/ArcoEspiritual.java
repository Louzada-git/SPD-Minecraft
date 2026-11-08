package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.entidade.FlechaEspiritual;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Arco espiritual da Caçadora: flechas infinitas que aparecem quando a corda é puxada.
// Igual ao Shattered:
//   - dano: (1 + nível/5) a (6 + nível/2,5), cresce com o nível da Caçadora (não precisa de melhoria)
//   - força necessária 10; força sobrando soma dano
//   - colado no inimigo, acerta pior (precisão pela metade)
//   - atirar gasta 1 turno
public class ArcoEspiritual extends Item implements Arma {
    private static final int FORCA_NECESSARIA = 10;

    public ArcoEspiritual(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    public static int danoMinimo(int nivelDoHeroi) {
        return 1 + nivelDoHeroi / 5;
    }

    public static int danoMaximo(int nivelDoHeroi) {
        return 6 + (int) (nivelDoHeroi / 2.5F);
    }

    // Anel do tiro certeiro: +1 no mínimo e +2 no máximo por bônus
    @Override
    public int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean surpresa) {
        int tiroCerteiro = heroi.bonusDoAnel(TipoDeAnel.TIRO_CERTEIRO);
        int minimo = Math.max(0, danoMinimo(heroi.nivel()) + tiroCerteiro);
        int dano = Sorteio.normal(sorteio, minimo, Math.max(minimo, danoMaximo(heroi.nivel()) + 2 * tiroCerteiro));
        int forcaSobrando = heroi.forca() - FORCA_NECESSARIA;
        if (forcaSobrando > 0) {
            dano += Sorteio.normal(sorteio, 0, forcaSobrando);
        }
        return dano;
    }

    @Override
    public float precisao(ItemStack pilha, DadosDoHeroi heroi) {
        return Equipamento.divisaoPelaForca(FORCA_NECESSARIA - heroi.forca());
    }

    // Clique direito: atira uma flecha espiritual
    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!mundo.isClientSide()) {
            FlechaEspiritual flecha = new FlechaEspiritual(mundo, jogador);
            flecha.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 3.0F, 0.0F);
            mundo.addFreshEntity(flecha);
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.3F);
            jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("dica.pixeldungeon.arco_atirar").withStyle(ChatFormatting.AQUA));
        linhas.add(Component.translatable("dica.pixeldungeon.dano_do_arco", danoMinimo(1), danoMaximo(1)).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dica.pixeldungeon.forca", FORCA_NECESSARIA).withStyle(ChatFormatting.GRAY));
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.arco_espiritual.descricao", ChatFormatting.DARK_GRAY);
    }
}
