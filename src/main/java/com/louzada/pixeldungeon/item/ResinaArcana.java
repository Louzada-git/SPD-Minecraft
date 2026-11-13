package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModComponentes;
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

// Resina arcana (o "ArcaneResin" do Shattered): sai de varinhas no pote. Clique direito escolhe uma varinha identificada
// e aprimora (+1 nível e +1 carga), gastando nível + 1 de resina. Só funciona até o nível 3.
public class ResinaArcana extends Item {
    public static final int NIVEL_MAXIMO = 3;

    public ResinaArcana(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi) {
            boolean abriu = EscolhaDeItem.pedirProArtefato(heroi, "resina_arcana",
                    item -> item.getItem() instanceof ItemVarinha && NivelDoItem.identificado(item),
                    (quem, espaco) -> aplicar(quem, quem.getInventory().getItem(espaco)));
            if (!abriu) {
                heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.resina_arcana.nada").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // Aprimora a varinha com a resina do inventário. Devolve true se deu.
    public static boolean aplicar(ServerPlayer jogador, ItemStack varinha) {
        if (!(varinha.getItem() instanceof ItemVarinha)) {
            return false;
        }
        int nivel = NivelDoItem.nivel(varinha);
        if (nivel >= NIVEL_MAXIMO) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.resina_arcana.nivel_alto").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int precisa = nivel + 1;
        if (jogador.getInventory().countItem(ModItens.RESINA_ARCANA.get()) < precisa) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.resina_arcana.pouca", precisa).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        for (int i = 0; i < jogador.getInventory().getContainerSize() && precisa > 0; i++) {
            ItemStack pilha = jogador.getInventory().getItem(i);
            if (pilha.is(ModItens.RESINA_ARCANA.get())) {
                int tira = Math.min(precisa, pilha.getCount());
                pilha.shrink(tira);
                precisa -= tira;
            }
        }
        NivelDoItem.definirNivel(varinha, nivel + 1);
        varinha.set(ModComponentes.RESINA.get(), varinha.getOrDefault(ModComponentes.RESINA.get(), 0) + 1);
        Cargas.somar(varinha, ItemVarinha.cargasMaximas(varinha), 1);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.resina_arcana.usou").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // Quanta resina sai de uma varinha no pote: 2 × (nível sem a resina + 1), mais os pontos de preservação de varinha
    // se o herói não for mago (o talento veio pela metamorfose)
    public static int resinaDaVarinha(ServerPlayer jogador, ItemStack varinha) {
        int nivel = NivelDoItem.nivel(varinha) - varinha.getOrDefault(ModComponentes.RESINA.get(), 0);
        int resina = 2 * (Math.max(0, nivel) + 1);
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.classe() != ClasseDoHeroi.MAGO && dados.temTalento(Talento.PRESERVACAO_DE_VARINHA)) {
            resina += dados.pontos(Talento.PRESERVACAO_DE_VARINHA);
        }
        return resina;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.resina_arcana.descricao", ChatFormatting.GRAY);
    }
}
