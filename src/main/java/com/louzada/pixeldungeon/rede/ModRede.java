package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.cliente.ClienteDoMod;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// Registra os pacotes que vão e voltam entre o servidor e os jogadores.
// (Os tratamentos rodam na linha principal do jogo, então é seguro mexer no mundo aqui.)
public class ModRede {

    public static void aoRegistrar(RegisterPayloadHandlersEvent evento) {
        PayloadRegistrar registrador = evento.registrar("1");
        registrador.playToClient(PacoteAbrirMenu.TIPO, PacoteAbrirMenu.CODEC, ModRede::aoReceberAbrirMenu);
        registrador.playToServer(PacoteEscolherClasse.TIPO, PacoteEscolherClasse.CODEC, ModRede::aoReceberEscolherClasse);
    }

    // No jogador: abre a tela. (A classe ClienteDoMod só existe no jogo do jogador, nunca no servidor.)
    private static void aoReceberAbrirMenu(PacoteAbrirMenu pacote, IPayloadContext contexto) {
        ClienteDoMod.abrirMenuDeHeroi(pacote.runEmAndamento());
    }

    // No servidor: o jogador escolheu a classe
    private static void aoReceberEscolherClasse(PacoteEscolherClasse pacote, IPayloadContext contexto) {
        ClasseDoHeroi classe = ClasseDoHeroi.peloId(pacote.classe());
        if (classe != null && contexto.player() instanceof ServerPlayer jogador) {
            GerenciadorDaRun.escolherClasse(jogador, classe);
        }
    }
}
