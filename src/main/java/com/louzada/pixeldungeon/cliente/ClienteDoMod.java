package com.louzada.pixeldungeon.cliente;

import net.minecraft.client.Minecraft;

// Coisas que só existem no jogo do jogador (telas, desenho...).
// O servidor nunca carrega esta classe.
public class ClienteDoMod {

    public static void abrirMenuDeHeroi(boolean runEmAndamento) {
        Minecraft.getInstance().setScreen(new TelaEscolhaDeHeroi(runEmAndamento));
    }
}
