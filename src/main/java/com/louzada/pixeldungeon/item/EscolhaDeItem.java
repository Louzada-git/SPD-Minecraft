package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.rede.PacoteEscolherItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// Os pergaminhos que pedem um item (melhoria, identificação, remover maldição, transmutação):
// o servidor manda a lista de itens que servem, a tela do jogador mostra, e ele escolhe um.
// O pergaminho só é gasto quando o item é escolhido. Igual ao Shattered, se o pergaminho era desconhecido
// e o jogador desistir, ele se gasta mesmo assim.
public class EscolhaDeItem {
    // runa null: o pergaminho foi repetido pela magia "recuperar inscrição" do Clérigo (não tem pergaminho pra gastar)
    private record Pendente(TipoDePergaminho tipo, @Nullable RunaDePergaminho runa, boolean eraDesconhecido) {
    }

    private static Map<UUID, Pendente> pendentes = new HashMap<>();

    // Abre a tela. Devolve false se não tem nenhum item que sirva.
    public static boolean pedir(ServerPlayer jogador, TipoDePergaminho tipo, RunaDePergaminho runa, boolean eraDesconhecido) {
        List<Integer> espacos = elegiveis(jogador, tipo, runa);
        if (espacos.isEmpty()) {
            return false;
        }
        pendentes.put(jogador.getUUID(), new Pendente(tipo, runa, eraDesconhecido));
        PacketDistributor.sendToPlayer(jogador, new PacoteEscolherItem(tipo.id(), espacos));
        return true;
    }

    // Recuperar inscrição (Clérigo): repete um pergaminho que pede item, sem gastar pergaminho nenhum
    public static boolean pedirSemPergaminho(ServerPlayer jogador, TipoDePergaminho tipo) {
        List<Integer> espacos = elegiveis(jogador, tipo, null);
        if (espacos.isEmpty()) {
            return false;
        }
        pendentes.put(jogador.getUUID(), new Pendente(tipo, null, false));
        PacketDistributor.sendToPlayer(jogador, new PacoteEscolherItem(tipo.id(), espacos));
        return true;
    }

    // Os espaços do inventário com itens que servem pra esse pergaminho
    public static List<Integer> elegiveis(ServerPlayer jogador, TipoDePergaminho tipo, @Nullable RunaDePergaminho runa) {
        List<Integer> espacos = new ArrayList<>();
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack pilha = inventario.getItem(i);
            if (pilha.isEmpty() || (pilha.getItem() instanceof ItemPergaminho pergaminho && pergaminho.runa() == runa)) {
                continue;
            }
            if (serve(jogador, tipo, pilha)) {
                espacos.add(i);
            }
        }
        return espacos;
    }

    private static boolean serve(ServerPlayer jogador, TipoDePergaminho tipo, ItemStack pilha) {
        return switch (tipo) {
            case MELHORIA -> NivelDoItem.podeMelhorar(pilha);
            case IDENTIFICACAO -> Identificacao.desconhecido(jogador, pilha);
            case REMOVER_MALDICAO -> NivelDoItem.podeIdentificar(pilha) && (!NivelDoItem.maldicaoConhecida(pilha) || NivelDoItem.amaldicoado(pilha));
            case TRANSMUTACAO -> EfeitosDePergaminho.podeTransmutar(jogador, pilha);
            default -> false;
        };
    }

    // O jogador escolheu (ou desistiu, com espaço -1)
    public static void escolheu(ServerPlayer jogador, int espaco) {
        Pendente pendente = pendentes.remove(jogador.getUUID());
        if (pendente == null) {
            return;
        }
        if (espaco < 0 || !elegiveis(jogador, pendente.tipo(), pendente.runa()).contains(espaco)) {
            if (pendente.eraDesconhecido() && pendente.runa() != null) {
                gastarPergaminho(jogador, pendente.runa());
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_desperdicado").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        boolean repetido = pendente.runa() == null;
        if (!repetido && !gastarPergaminho(jogador, pendente.runa())) {
            return;
        }
        EfeitosDePergaminho.aplicarNoItem(jogador, pendente.tipo(), espaco);
        TalentosDoTier2.aoLerPergaminho(jogador, pendente.tipo(), repetido);
    }

    // Tira um pergaminho dessa runa do inventário
    private static boolean gastarPergaminho(ServerPlayer jogador, RunaDePergaminho runa) {
        if (jogador.getAbilities().instabuild) {
            return true;
        }
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack pilha = inventario.getItem(i);
            if (pilha.getItem() instanceof ItemPergaminho pergaminho && pergaminho.runa() == runa) {
                pilha.shrink(1);
                return true;
            }
        }
        return false;
    }
}
