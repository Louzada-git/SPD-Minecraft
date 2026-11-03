package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.rede.PacoteEstadoDoHeroi;

import net.minecraft.client.Minecraft;

// Coisas que só existem no jogo do jogador (telas, desenho...).
// O servidor nunca carrega esta classe.
public class ClienteDoMod {

    public static void abrirMenuDeHeroi(boolean runEmAndamento) {
        Minecraft.getInstance().setScreen(new TelaEscolhaDeHeroi(runEmAndamento));
    }

    public static void abrirTomo() {
        Minecraft.getInstance().setScreen(new TelaDoTomo());
    }

    public static void receberEstado(PacoteEstadoDoHeroi pacote) {
        EstadoNoCliente.atualizar(pacote);
    }
}
