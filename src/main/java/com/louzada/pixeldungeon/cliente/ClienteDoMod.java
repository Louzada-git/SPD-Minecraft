package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.rede.PacoteEstadoDoHeroi;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Coisas que só existem no jogo do jogador (telas, desenho...).
// O servidor nunca carrega esta classe.
public class ClienteDoMod {

    public static void abrirMenuDeHeroi(boolean runEmAndamento) {
        Minecraft.getInstance().setScreen(new TelaEscolhaDeHeroi(runEmAndamento));
    }

    public static void abrirTomo() {
        Minecraft.getInstance().setScreen(new TelaDoTomo());
    }

    // O próximo golpe já está carregado (a barrinha de recarga do ataque enche na hora)
    public static void ataquePronto() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.attackStrengthTicker = TalentosDoTier2.ATAQUE_CARREGADO;
        }
    }

    public static void receberEstado(PacoteEstadoDoHeroi pacote) {
        EstadoNoCliente.atualizar(pacote);
    }

    public static void abrirEscolhaDeItem(String pergaminho, List<Integer> espacos) {
        Minecraft.getInstance().setScreen(new TelaEscolherItem(pergaminho, espacos));
    }

    public static void abrirFantasma(Component texto, ItemStack arma, ItemStack armadura) {
        Minecraft.getInstance().setScreen(new TelaDoFantasma(texto, arma, armadura));
    }

    public static void abrirCompartilhar(String modo, List<String> jogadores, List<String> escolhidos) {
        Minecraft.getInstance().setScreen(new TelaCompartilhar(modo, jogadores, escolhidos));
    }
}
