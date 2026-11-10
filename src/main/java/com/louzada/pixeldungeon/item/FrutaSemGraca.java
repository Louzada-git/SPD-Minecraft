package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.entidade.PocaoArremessada;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// A fruta-sem-graça do Shattered (cai do arbusto dela). Crua não dá pra comer; cozida no pote com uma semente,
// ela pega o efeito da poção daquela semente (a "fruta-solar" cura, a "fruta-de-fogo" pega fogo...):
//   comer: mata a fome de 450 turnos (15) e faz o efeito de beber a poção
//   agachado + clique direito: arremessa; as de chama, gelo, gases, levitação e pureza quebram igual à poção
//   e deixam pedaços de fruta no chão (as outras caem inteiras)
// A poção fica no componente FRUTA_COZIDA (o id do TipoDePocao).
public class FrutaSemGraca extends Item {

    public FrutaSemGraca(Properties propriedades) {
        super(propriedades);
    }

    @Nullable
    public static TipoDePocao pocao(ItemStack pilha) {
        String id = pilha.get(ModComponentes.FRUTA_COZIDA.get());
        if (id == null) {
            return null;
        }
        for (TipoDePocao tipo : TipoDePocao.values()) {
            if (tipo.id().equals(id)) {
                return tipo;
            }
        }
        return null;
    }

    // A fruta cozida com o efeito dessa poção (vira comida)
    public static ItemStack cozida(TipoDePocao tipo, int quantas) {
        ItemStack fruta = new ItemStack(ModItens.FRUTA_SEM_GRACA.get(), quantas);
        fruta.set(ModComponentes.FRUTA_COZIDA.get(), tipo.id());
        fruta.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(15).saturationModifier(0.6F).build());
        return fruta;
    }

    // As que quebram ao serem arremessadas
    public static boolean quebra(@Nullable TipoDePocao tipo) {
        return tipo == TipoDePocao.CHAMA_LIQUIDA || tipo == TipoDePocao.GAS_TOXICO || tipo == TipoDePocao.GAS_PARALISANTE || tipo == TipoDePocao.GELO
                || tipo == TipoDePocao.LEVITACAO || tipo == TipoDePocao.PUREZA;
    }

    @Override
    public Component getName(ItemStack pilha) {
        TipoDePocao tipo = pocao(pilha);
        return tipo == null ? super.getName(pilha) : Component.translatable("item.pixeldungeon.fruta_sem_graca." + tipo.id());
    }

    @Override
    public boolean isFoil(ItemStack pilha) {
        return pocao(pilha) != null;
    }

    // Crua: não dá pra comer. Cozida e agachado: arremessa (comer é com o clique direito normal, ver Comida)
    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (pocao(pilha) == null) {
            if (!mundo.isClientSide()) {
                jogador.displayClientMessage(Component.translatable("item.pixeldungeon.fruta_sem_graca.crua").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(pilha);
        }
        if (!mundo.isClientSide()) {
            PocaoArremessada fruta = new PocaoArremessada(mundo, jogador);
            fruta.setItem(pilha.copyWithCount(1));
            fruta.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), -10.0F, 0.9F, 1.0F);
            mundo.addFreshEntity(fruta);
        }
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.5F);
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // Comeu a cozida: o efeito de beber a poção dela
    @Override
    public ItemStack finishUsingItem(ItemStack pilha, Level mundo, LivingEntity quem) {
        TipoDePocao tipo = pocao(pilha);
        ItemStack resto = super.finishUsingItem(pilha, mundo, quem);
        if (tipo != null && quem instanceof ServerPlayer jogador) {
            EfeitosDePocao.beber(jogador, tipo, IdentidadesDaRun.corDoTipo(Identificacao.sementeDaRun(jogador), tipo));
        }
        return resto;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDePocao tipo = pocao(pilha);
        if (tipo == null) {
            Textos.adicionarQuebrado(linhas, "item.pixeldungeon.fruta_sem_graca.descricao", ChatFormatting.GRAY);
            return;
        }
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.fruta_sem_graca.cozida", ChatFormatting.GRAY);
        Textos.adicionarQuebrado(linhas, quebra(tipo) ? "item.pixeldungeon.fruta_sem_graca.arremessar" : "item.pixeldungeon.fruta_sem_graca.comer",
                ChatFormatting.GRAY);
    }
}
