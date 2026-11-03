package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Capa das sombras do Ladino (artefato). Clique direito liga e desliga a invisibilidade.
// Igual ao Shattered:
//   - ligar gasta 1 carga, e depois mais 1 a cada 4 turnos invisível; sem cargas, desliga sozinha
//   - não recarrega enquanto está ligada
//   - começa com 3 cargas e ganha mais conforme é usada (sobe de nível até o 10)
//   - atacar (ou usar magia) desliga a capa
public class CapaDasSombras extends Item {
    private static final String ATIVA = "capa_ativa";
    private static final String TURNOS = "capa_turnos";
    private static final int TURNOS_POR_CARGA = 4;
    private static final int COR_DA_BARRA = 0x8866CC;

    public CapaDasSombras(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    public static boolean ativa(Player jogador) {
        return RegrasDoHeroi.dados(jogador).sim(ATIVA);
    }

    // A capa que o jogador está carregando (ou null)
    @Nullable
    public static ItemStack acharCapa(Player jogador) {
        for (ItemStack pilha : jogador.getInventory().items) {
            if (pilha.getItem() instanceof CapaDasSombras) {
                return pilha;
            }
        }
        ItemStack outraMao = jogador.getOffhandItem();
        return outraMao.getItem() instanceof CapaDasSombras ? outraMao : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return InteractionResultHolder.success(pilha);
        }
        if (ativa(heroi)) {
            desativar(heroi, true);
            return InteractionResultHolder.success(pilha);
        }
        long agora = mundo.getGameTime();
        Cargas.recarregar(pilha, agora, Artefato.cargasMaximas(pilha), Artefato::ticksParaCarregar);
        if (Cargas.cargas(pilha, Artefato.cargasMaximas(pilha)) <= 0) {
            heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.capa_sem_carga").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(pilha);
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.definir(ATIVA, true);
        dados.definir(TURNOS, 0);
        Cargas.pausar(pilha, agora);
        gastarUmaCarga(heroi, dados, pilha);
        Furtividade.ficarInvisivel(heroi, 30);
        Particulas.explosao(heroi.serverLevel(), ParticleTypes.LARGE_SMOKE, heroi.getX(), heroi.getY(0.5), heroi.getZ(), 12, 0.3, 0.02);
        mundo.playSound(null, heroi.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.success(pilha);
    }

    // Desliga a capa (manual = o jogador desligou; senão foi um ataque ou acabou a carga)
    public static void desativar(ServerPlayer jogador, boolean manual) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(ATIVA, false);
        jogador.removeEffect(MobEffects.INVISIBILITY);
        ItemStack capa = acharCapa(jogador);
        if (capa != null) {
            Cargas.retomar(capa, jogador.level().getGameTime());
        }
        if (manual) {
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    private static void gastarUmaCarga(ServerPlayer jogador, DadosDoHeroi dados, ItemStack capa) {
        int maximo = Artefato.cargasMaximas(capa);
        Cargas.gastar(capa, jogador.level().getGameTime(), maximo, 1);
        Artefato.ganharExperiencia(jogador, capa, dados.nivel(), 1, "mensagem.pixeldungeon.capa_subiu");
        dados.definir(TURNOS, TURNOS_POR_CARGA);
    }

    // Todo tick do herói: mantém a invisibilidade e gasta as cargas
    public static void aoTickDoHeroi(ServerPlayer jogador, DadosDoHeroi dados, boolean umTurno) {
        if (!dados.sim(ATIVA)) {
            return;
        }
        ItemStack capa = acharCapa(jogador);
        if (capa == null) {
            desativar(jogador, false);
            return;
        }
        if (jogador.getEffect(MobEffects.INVISIBILITY) == null || jogador.getEffect(MobEffects.INVISIBILITY).getDuration() < 10) {
            Furtividade.ficarInvisivel(jogador, 30);
        }
        if (!umTurno) {
            return;
        }
        // Sombras protetoras: blindagem enquanto invisível (1 a cada 2 turnos até 3, ou 1 por turno até 5)
        int pontos = dados.pontos(Talento.SOMBRAS_PROTETORAS);
        if (pontos > 0) {
            Escudos.somarBarreira(jogador, 0.5F * pontos, 1 + 2 * pontos);
        }
        int turnos = dados.inteiro(TURNOS) - 1;
        dados.definir(TURNOS, turnos);
        if (turnos <= 0) {
            if (Cargas.cargas(capa, Artefato.cargasMaximas(capa)) <= 0) {
                desativar(jogador, false);
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.capa_acabou").withStyle(ChatFormatting.GRAY), false);
            } else {
                gastarUmaCarga(jogador, dados, capa);
            }
        }
    }

    // Recarrega sozinha (menos enquanto está ligada)
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && dono.tickCount % 20 == 0 && dono instanceof Player jogador && !ativa(jogador)) {
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
        linhas.add(Component.translatable("dica.pixeldungeon.capa_usar").withStyle(ChatFormatting.LIGHT_PURPLE));
        linhas.add(Component.translatable("dica.pixeldungeon.cargas", Cargas.cargas(pilha, maximo), maximo).withStyle(ChatFormatting.LIGHT_PURPLE));
        linhas.add(Component.translatable("dica.pixeldungeon.nivel_do_artefato", Artefato.nivel(pilha)).withStyle(ChatFormatting.GRAY));
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.capa_das_sombras.descricao", ChatFormatting.DARK_GRAY);
    }
}
