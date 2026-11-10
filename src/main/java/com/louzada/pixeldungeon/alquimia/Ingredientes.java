package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// O que está nas casinhas do pote de alquimia (só as que têm alguma coisa, na ordem), e de quem é.
// As pilhas são as de verdade: quando uma receita gasta um ingrediente, ele sai da casinha.
public class Ingredientes {
    private final List<ItemStack> pilhas = new ArrayList<>();
    private final ServerPlayer jogador;

    public Ingredientes(ServerPlayer jogador, List<ItemStack> casinhas) {
        this.jogador = jogador;
        for (ItemStack pilha : casinhas) {
            if (!pilha.isEmpty()) {
                pilhas.add(pilha);
            }
        }
    }

    public List<ItemStack> pilhas() {
        return pilhas;
    }

    public int quantos() {
        return pilhas.size();
    }

    public ItemStack get(int i) {
        return pilhas.get(i);
    }

    public ServerPlayer jogador() {
        return jogador;
    }

    public long semente() {
        return Identificacao.sementeDaRun(jogador);
    }

    public DadosDaRun run() {
        return DadosDaRun.pegar(jogador.getServer());
    }
}
