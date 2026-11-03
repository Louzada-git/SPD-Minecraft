package com.louzada.pixeldungeon.rede;

import com.louzada.pixeldungeon.cliente.ClienteDoMod;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
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
        registrador.playToClient(PacoteEstadoDoHeroi.TIPO, PacoteEstadoDoHeroi.CODEC, ModRede::aoReceberEstado);
        registrador.playToServer(PacoteEscolherClasse.TIPO, PacoteEscolherClasse.CODEC, ModRede::aoReceberEscolherClasse);
        registrador.playToServer(PacoteAprimorarTalento.TIPO, PacoteAprimorarTalento.CODEC, ModRede::aoReceberAprimorarTalento);
        registrador.playToServer(PacoteUsarHabilidade.TIPO, PacoteUsarHabilidade.CODEC, ModRede::aoReceberUsarHabilidade);
        registrador.playToServer(PacoteLancarMagia.TIPO, PacoteLancarMagia.CODEC, ModRede::aoReceberLancarMagia);
    }

    // ---- No jogo do jogador (a classe ClienteDoMod só existe lá, nunca no servidor) ----

    private static void aoReceberAbrirMenu(PacoteAbrirMenu pacote, IPayloadContext contexto) {
        ClienteDoMod.abrirMenuDeHeroi(pacote.runEmAndamento());
    }

    private static void aoReceberEstado(PacoteEstadoDoHeroi pacote, IPayloadContext contexto) {
        ClienteDoMod.receberEstado(pacote);
    }

    // ---- No servidor ----

    private static void aoReceberEscolherClasse(PacoteEscolherClasse pacote, IPayloadContext contexto) {
        ClasseDoHeroi classe = ClasseDoHeroi.peloId(pacote.classe());
        if (classe != null && contexto.player() instanceof ServerPlayer jogador) {
            GerenciadorDaRun.escolherClasse(jogador, classe);
        }
    }

    private static void aoReceberAprimorarTalento(PacoteAprimorarTalento pacote, IPayloadContext contexto) {
        Talento talento = Talento.peloId(pacote.talento());
        if (talento != null && contexto.player() instanceof ServerPlayer jogador && GerenciadorDaRun.estaVivoNaRun(jogador)) {
            Talentos.aprimorar(jogador, talento);
            RedeDoHeroi.enviarEstado(jogador, true);
        }
    }

    private static void aoReceberUsarHabilidade(PacoteUsarHabilidade pacote, IPayloadContext contexto) {
        if (contexto.player() instanceof ServerPlayer jogador) {
            HabilidadesDaDuelista.usar(jogador);
            RedeDoHeroi.enviarEstado(jogador, false);
        }
    }

    private static void aoReceberLancarMagia(PacoteLancarMagia pacote, IPayloadContext contexto) {
        MagiasDoClerigo.Magia magia = MagiasDoClerigo.Magia.peloId(pacote.magia());
        if (magia != null && contexto.player() instanceof ServerPlayer jogador) {
            MagiasDoClerigo.lancar(jogador, magia);
        }
    }
}
