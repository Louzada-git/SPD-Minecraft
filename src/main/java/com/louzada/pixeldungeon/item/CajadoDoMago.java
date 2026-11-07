package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;

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

// Cajado do Mago: bate como arma (1-6) e leva uma varinha dentro (a de míssil mágico). Igual ao Shattered:
//   - o cajado dá 1 carga a mais que a varinha sozinha (3 + 1 = 4)
//   - clique direito lança a varinha (gasta 1 carga e 1 turno)
//   - cada nível (pergaminho de melhoria) melhora o cajado e a varinha juntos: +1 carga (até 10) e um míssil mais forte
public class CajadoDoMago extends ArmaDoShattered {
    // A varinha que vai dentro do cajado
    private static final Varinha VARINHA = new VarinhaDeMisselMagico();
    private static final int CARGA_DO_CAJADO = 1;
    private static final int COR_DA_BARRA = 0xB07CFF;

    public CajadoDoMago(Properties propriedades) {
        super(propriedades, "cajado_do_mago", Numeros.doTier(1).dano(1, 6));
    }

    // ---- Cargas ----

    public static int cargasMaximas(ItemStack pilha) {
        return VARINHA.cargasMaximas(NivelDoItem.nivel(pilha), CARGA_DO_CAJADO);
    }

    public static int cargas(ItemStack pilha) {
        return Cargas.cargas(pilha, cargasMaximas(pilha));
    }

    private static void recarregar(ItemStack pilha, long agora) {
        Cargas.recarregar(pilha, agora, cargasMaximas(pilha), Varinha::ticksParaCarregar);
    }

    // Pergaminho de recarga: enche tudo na hora
    public static void encher(ItemStack pilha) {
        Cargas.somar(pilha, cargasMaximas(pilha), cargasMaximas(pilha));
    }

    // Uma vez por turno, no servidor, vê se encheu alguma carga
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && Turno.virou(dono)) {
            recarregar(pilha, mundo.getGameTime());
        }
    }

    // ---- Lançar ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return cargas(pilha) > 0 ? InteractionResultHolder.success(pilha) : InteractionResultHolder.fail(pilha);
        }
        long agora = mundo.getGameTime();
        recarregar(pilha, agora);
        int cargas = cargas(pilha);
        if (cargas <= 0) {
            heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_sem_carga").withStyle(ChatFormatting.GRAY), true);
            mundo.playSound(null, heroi.getX(), heroi.getY(), heroi.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.fail(pilha);
        }

        // Bateria de escudo: agachado, as cargas viram escudo
        if (TalentosDoTier2.bateriaDeEscudo(heroi, pilha, cargas)) {
            heroi.getCooldowns().addCooldown(this, Turno.TICKS);
            return InteractionResultHolder.success(pilha);
        }

        // Talentos do Mago (magia prolongada, barreira de apoio) e a invisibilidade acaba
        Talentos.aoUsarVarinha(heroi, cargas);
        Furtividade.revelar(heroi);
        // Poder inscrito: depois de ler um pergaminho, alguns disparos saem com +2 níveis
        VARINHA.lancar(heroi, NivelDoItem.nivel(pilha) + TalentosDoTier2.niveisInscritos(heroi));
        Cargas.gastar(pilha, agora, cargasMaximas(pilha), 1);
        // Lançar gasta um turno
        heroi.getCooldowns().addCooldown(this, Turno.TICKS);
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
        VARINHA.dica(linhas, NivelDoItem.nivel(pilha));
        linhas.add(Component.translatable("dica.pixeldungeon.cargas", cargas(pilha), cargasMaximas(pilha)).withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(pilha, contexto, linhas, opcoes);
    }
}
