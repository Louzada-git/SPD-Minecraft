package com.louzada.pixeldungeon.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.louzada.pixeldungeon.rede.PacoteOpcoes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// Uma janelinha com botões pra escolher uma opção (o "WndOptions" do Shattered): a pedra de alteração pergunta
// o que aumentar, a pedra de intuição pergunta o palpite. O servidor guarda o que fazer com a escolha;
// fechar a janela é desistir (a escolha chega como -1).
public class EscolhaDeOpcao {
    private record Pedido(int quantas, BiConsumer<ServerPlayer, Integer> aoEscolher) {
    }

    private static Map<UUID, Pedido> pedidos = new HashMap<>();

    public static void pedir(ServerPlayer jogador, Component titulo, Component texto, ItemStack icone, List<Component> opcoes,
            BiConsumer<ServerPlayer, Integer> aoEscolher) {
        pedidos.put(jogador.getUUID(), new Pedido(opcoes.size(), aoEscolher));
        PacketDistributor.sendToPlayer(jogador, new PacoteOpcoes(titulo, texto, icone, opcoes));
    }

    // Chegou a escolha do jogador (-1: desistiu)
    public static void escolheu(ServerPlayer jogador, int indice) {
        Pedido pedido = pedidos.remove(jogador.getUUID());
        if (pedido != null) {
            pedido.aoEscolher().accept(jogador, indice >= 0 && indice < pedido.quantas() ? indice : -1);
        }
    }
}
