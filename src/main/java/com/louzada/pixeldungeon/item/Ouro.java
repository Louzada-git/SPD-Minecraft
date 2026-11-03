package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

// Ouro do Shattered: uma pilha de moedas. Quando o herói encosta nela, as moedas vão direto pra bolsa dele
// (não ocupam espaço no inventário), sobe um "+N" dourado e o total aparece do lado da barra de itens.
// Cada herói tem o seu ouro; ele vai servir pras lojas.
// Igual ao Shattered, uma pilha tem de 30 + 10 por andar até 60 + 20 por andar moedas.
public class Ouro extends Item {
    // Onde fica o ouro do herói (no estado do DadosDoHeroi; zera numa run nova)
    public static final String OURO = "ouro";

    public Ouro(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    public static ItemStack pilha(int quantidade) {
        ItemStack pilha = new ItemStack(ModItens.OURO.get());
        pilha.set(ModComponentes.QUANTIDADE.get(), Math.max(1, quantidade));
        return pilha;
    }

    // Uma pilha do tamanho certo pro andar
    public static ItemStack sortear(Random sorteio, int andar) {
        int minimo = 30 + andar * 10;
        int maximo = 60 + andar * 20;
        return pilha(minimo + sorteio.nextInt(maximo - minimo + 1));
    }

    public static int quantidade(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.QUANTIDADE.get(), 1) * pilha.getCount();
    }

    public static int ouroDe(DadosDoHeroi dados) {
        return dados.inteiro(OURO);
    }

    // O herói pegou moedas
    public static void pegar(ServerPlayer jogador, int quantidade) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(OURO, ouroDe(dados) + quantidade);
        NumerosFlutuantes.mostrar(jogador, "+" + quantidade, NumerosFlutuantes.DOURADO);
        jogador.level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS,
                0.8F, 0.9F + jogador.getRandom().nextFloat() * 0.2F);
        RedeDoHeroi.enviarEstado(jogador, false);
    }

    // O herói encostou numa pilha de ouro no chão: as moedas vão pra bolsa, não pro inventário
    public static void aoEncostar(ItemEntityPickupEvent.Pre evento) {
        ItemEntity noChao = evento.getItemEntity();
        ItemStack pilha = noChao.getItem();
        if (!pilha.is(ModItens.OURO.get()) || !(evento.getPlayer() instanceof ServerPlayer jogador) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        evento.setCanPickup(TriState.FALSE);
        if (noChao.hasPickUpDelay() || noChao.isRemoved()) {
            return;
        }
        pegar(jogador, quantidade(pilha));
        // A animação do item voando até o jogador
        jogador.take(noChao, 1);
        noChao.discard();
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("dica.pixeldungeon.moedas", quantidade(pilha)).withStyle(ChatFormatting.GOLD));
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.ouro.descricao", ChatFormatting.GRAY);
    }
}
