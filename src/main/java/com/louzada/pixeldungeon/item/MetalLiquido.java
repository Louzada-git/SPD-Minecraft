package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
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

// Metal líquido (o "LiquidMetal" do Shattered): sai de armas de arremesso no pote. Clique direito escolhe um conjunto
// de armas de arremesso e conserta: cada gota recupera um pouco da durabilidade (menos nas armas de tier e nível maiores);
// com o conjunto inteiro, completa uma arma que faltar nele (até 3).
public class MetalLiquido extends Item {

    public MetalLiquido(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi) {
            boolean abriu = EscolhaDeItem.pedirProArtefato(heroi, "metal_liquido", item -> item.getItem() instanceof ArmaDeArremesso,
                    (quem, espaco) -> aplicar(quem, quem.getInventory().getItem(espaco)));
            if (!abriu) {
                heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.metal_liquido.nada").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // Quanto metal conserta a arma inteira (o "maxToUse" do Shattered)
    public static float metalPorArma(ItemStack arma) {
        int tier = ((ArmaDeArremesso) arma.getItem()).tier();
        return (float) (5 * (tier + 1) * Math.pow(1.35, NivelDoItem.nivel(arma)));
    }

    // Conserta a arma com o metal líquido do inventário. Devolve quanto metal gastou.
    public static int aplicar(ServerPlayer jogador, ItemStack arma) {
        if (!(arma.getItem() instanceof ArmaDeArremesso tipo)) {
            return 0;
        }
        int metal = jogador.getInventory().countItem(ModItens.METAL_LIQUIDO.get());
        float porArma = metalPorArma(arma);
        float durabilidadePorGota = ArmaDeArremesso.DURABILIDADE_MAXIMA / porArma;
        float durabilidade = ArmaDeArremesso.durabilidade(arma);
        int gastou;
        if (durabilidade >= ArmaDeArremesso.DURABILIDADE_MAXIMA - 0.01F) {
            // Inteira: completa uma arma que faltar no conjunto
            if (arma.getCount() >= ArmaDeArremesso.QUANTIDADE_PADRAO || metal * durabilidadePorGota < tipo.gastoPorUso(arma, RegrasDoHeroi.dados(jogador))) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.metal_liquido.inteira").withStyle(ChatFormatting.GRAY), true);
                return 0;
            }
            gastou = Math.min(metal, (int) Math.ceil(porArma));
            arma.grow(1);
            if (gastou < porArma) {
                ArmaDeArremesso.definirDurabilidade(arma, gastou * durabilidadePorGota);
            }
        } else {
            int precisa = (int) Math.ceil(porArma * (0.999F - durabilidade / ArmaDeArremesso.DURABILIDADE_MAXIMA));
            gastou = Math.min(metal, Math.max(1, precisa));
            ArmaDeArremesso.definirDurabilidade(arma, Math.min(ArmaDeArremesso.DURABILIDADE_MAXIMA, durabilidade + gastou * durabilidadePorGota));
        }
        tirar(jogador, gastou);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.2F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.metal_liquido.usou", gastou).withStyle(ChatFormatting.GRAY), true);
        return gastou;
    }

    private static void tirar(ServerPlayer jogador, int quanto) {
        for (int i = 0; i < jogador.getInventory().getContainerSize() && quanto > 0; i++) {
            ItemStack pilha = jogador.getInventory().getItem(i);
            if (pilha.is(ModItens.METAL_LIQUIDO.get())) {
                int tira = Math.min(quanto, pilha.getCount());
                pilha.shrink(tira);
                quanto -= tira;
            }
        }
    }

    // Quanto metal sai de um conjunto de armas de arremesso no pote (igual ao Shattered)
    public static int metalDoConjunto(ItemStack arma) {
        int tier = ((ArmaDeArremesso) arma.getItem()).tier();
        double porArma = 5 * (tier + 1) * Math.pow(1.35, Math.min(5, NivelDoItem.nivel(arma)));
        double armas = arma.getCount() - 1 + 0.25 + 0.0075 * ArmaDeArremesso.durabilidade(arma);
        return (int) Math.round(armas * porArma);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.metal_liquido.descricao", ChatFormatting.GRAY);
    }
}
