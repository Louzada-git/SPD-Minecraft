package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.entidade.MisselMagico;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.Talentos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Cajado do Mago: bate como arma (1-6) e já vem com a varinha de míssil mágico dentro.
// Clique direito lança um míssil mágico (2-8 de dano). Igual ao Shattered:
//   - a varinha de míssil mágico tem 3 cargas, e o cajado dá +1 (4 no total)
//   - as cargas voltam sozinhas com o tempo: quanto mais vazio, mais rápido (de ~33 a ~45 turnos por carga)
//   - lançar gasta 1 turno (1 segundo)
//   - cada nível (pergaminho de melhoria) dá +1 carga (até 10) e +1/+2 no dano do míssil
public class CajadoDoMago extends ArmaDoShattered {
    public static final int CARGAS_INICIAIS = 4;
    private static final int COR_DA_BARRA = 0xB07CFF;

    public CajadoDoMago(Properties propriedades) {
        super(propriedades, "cajado_do_mago", 1, 1, 6, 1.0f, 1.0f, 0, 0.0f, null, 0);
    }

    // ---- Cargas ----

    // Igual ao recarregador das varinhas do Shattered: 10 + 40 * 0,875^(cargas que faltam) turnos por carga
    private static long ticksParaCarregar(int faltando) {
        return Math.round((10 + 40 * Math.pow(0.875, faltando)) * 20);
    }

    public static int cargasMaximas(ItemStack pilha) {
        return Math.min(10, CARGAS_INICIAIS + NivelDoItem.nivel(pilha));
    }

    public static int cargas(ItemStack pilha) {
        return Cargas.cargas(pilha, cargasMaximas(pilha));
    }

    private static void recarregar(ItemStack pilha, long agora) {
        Cargas.recarregar(pilha, agora, cargasMaximas(pilha), CajadoDoMago::ticksParaCarregar);
    }

    // Pergaminho de recarga: enche tudo na hora
    public static void encher(ItemStack pilha) {
        Cargas.somar(pilha, cargasMaximas(pilha), cargasMaximas(pilha));
    }

    // Uma vez por segundo, no servidor, vê se encheu alguma carga
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && dono.tickCount % 20 == 0) {
            recarregar(pilha, mundo.getGameTime());
        }
    }

    // ---- Lançar ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (mundo.isClientSide()) {
            return cargas(pilha) > 0 ? InteractionResultHolder.success(pilha) : InteractionResultHolder.fail(pilha);
        }
        long agora = mundo.getGameTime();
        recarregar(pilha, agora);
        int cargas = cargas(pilha);
        if (cargas <= 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_sem_carga").withStyle(ChatFormatting.GRAY), true);
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.fail(pilha);
        }

        // Talentos do Mago (magia prolongada, barreira de apoio) e a invisibilidade acaba
        if (jogador instanceof ServerPlayer heroi) {
            Talentos.aoUsarVarinha(heroi, cargas);
            Furtividade.revelar(heroi);
        }
        MisselMagico missil = new MisselMagico(mundo, jogador, NivelDoItem.nivel(pilha));
        missil.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 2.0F, 0.0F);
        mundo.addFreshEntity(missil);
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.4F);

        Cargas.gastar(pilha, agora, cargasMaximas(pilha), 1);
        // Lançar gasta um turno
        jogador.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(pilha);
    }

    // ---- Aparência ----

    // A barrinha embaixo do ícone mostra as cargas (como o "3/4" do Shattered)
    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Cargas.larguraDaBarra(pilha, cargasMaximas(pilha));
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return COR_DA_BARRA;
    }

    // Mudar as cargas não faz o cajado "piscar" na mão
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack depois, boolean trocouDeEspaco) {
        return trocouDeEspaco || !ItemStack.isSameItem(antes, depois);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        int nivel = NivelDoItem.nivel(pilha);
        linhas.add(Component.translatable("dica.pixeldungeon.missel_magico", 2 + nivel, 8 + 2 * nivel).withStyle(ChatFormatting.LIGHT_PURPLE));
        linhas.add(Component.translatable("dica.pixeldungeon.cargas", cargas(pilha), cargasMaximas(pilha)).withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(pilha, contexto, linhas, opcoes);
    }
}
