package com.louzada.pixeldungeon.rede;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.louzada.pixeldungeon.cliente.ClienteDoMod;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.ConhecimentoDoCliente;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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
        registrador.playToClient(PacoteAtaquePronto.TIPO, PacoteAtaquePronto.CODEC, ModRede::aoReceberAtaquePronto);
        registrador.playToServer(PacoteEscolherClasse.TIPO, PacoteEscolherClasse.CODEC, ModRede::aoReceberEscolherClasse);
        registrador.playToServer(PacoteAprimorarTalento.TIPO, PacoteAprimorarTalento.CODEC, ModRede::aoReceberAprimorarTalento);
        registrador.playToServer(PacoteUsarHabilidade.TIPO, PacoteUsarHabilidade.CODEC, ModRede::aoReceberUsarHabilidade);
        registrador.playToServer(PacoteLancarMagia.TIPO, PacoteLancarMagia.CODEC, ModRede::aoReceberLancarMagia);
        registrador.playToServer(PacoteProcurar.TIPO, PacoteProcurar.CODEC, ModRede::aoReceberProcurar);
        // Identificação: o que o jogador já sabe, a tela de escolher item e a de compartilhar
        registrador.playToClient(PacoteConhecimento.TIPO, PacoteConhecimento.CODEC, ModRede::aoReceberConhecimento);
        registrador.playToClient(PacoteEscolherItem.TIPO, PacoteEscolherItem.CODEC, ModRede::aoReceberEscolherItem);
        registrador.playToServer(PacoteItemEscolhido.TIPO, PacoteItemEscolhido.CODEC, ModRede::aoReceberItemEscolhido);
        registrador.playToServer(PacotePedirCompartilhar.TIPO, PacotePedirCompartilhar.CODEC, ModRede::aoReceberPedirCompartilhar);
        registrador.playToClient(PacoteTelaCompartilhar.TIPO, PacoteTelaCompartilhar.CODEC, ModRede::aoReceberTelaCompartilhar);
        registrador.playToServer(PacoteDefinirCompartilhar.TIPO, PacoteDefinirCompartilhar.CODEC, ModRede::aoReceberDefinirCompartilhar);
    }

    // ---- No jogo do jogador (a classe ClienteDoMod só existe lá, nunca no servidor) ----

    private static void aoReceberAbrirMenu(PacoteAbrirMenu pacote, IPayloadContext contexto) {
        ClienteDoMod.abrirMenuDeHeroi(pacote.runEmAndamento());
    }

    private static void aoReceberEstado(PacoteEstadoDoHeroi pacote, IPayloadContext contexto) {
        ClienteDoMod.receberEstado(pacote);
    }

    private static void aoReceberAtaquePronto(PacoteAtaquePronto pacote, IPayloadContext contexto) {
        ClienteDoMod.ataquePronto();
    }

    private static void aoReceberConhecimento(PacoteConhecimento pacote, IPayloadContext contexto) {
        ConhecimentoDoCliente.atualizar(pacote.pocoes(), pacote.pergaminhos(), pacote.aneis());
    }

    private static void aoReceberEscolherItem(PacoteEscolherItem pacote, IPayloadContext contexto) {
        ClienteDoMod.abrirEscolhaDeItem(pacote.pergaminho(), pacote.espacos());
    }

    private static void aoReceberTelaCompartilhar(PacoteTelaCompartilhar pacote, IPayloadContext contexto) {
        ClienteDoMod.abrirCompartilhar(pacote.modo(), pacote.jogadores(), pacote.escolhidos());
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

    private static void aoReceberProcurar(PacoteProcurar pacote, IPayloadContext contexto) {
        if (contexto.player() instanceof ServerPlayer jogador && GerenciadorDoHeroi.emJogo(jogador)) {
            Busca.procurar(jogador);
        }
    }

    private static void aoReceberItemEscolhido(PacoteItemEscolhido pacote, IPayloadContext contexto) {
        if (contexto.player() instanceof ServerPlayer jogador) {
            EscolhaDeItem.escolheu(jogador, pacote.espaco());
        }
    }

    // A tela de compartilhar mostra os outros jogadores que estão no servidor
    private static void aoReceberPedirCompartilhar(PacotePedirCompartilhar pacote, IPayloadContext contexto) {
        if (!(contexto.player() instanceof ServerPlayer jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        List<String> outros = new ArrayList<>();
        for (ServerPlayer outro : jogador.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (outro != jogador) {
                outros.add(outro.getGameProfile().getName());
            }
        }
        PacketDistributor.sendToPlayer(jogador, new PacoteTelaCompartilhar(dados.modoDeCompartilhar(), outros, new ArrayList<>(dados.compartilharCom())));
    }

    private static void aoReceberDefinirCompartilhar(PacoteDefinirCompartilhar pacote, IPayloadContext contexto) {
        if (contexto.player() instanceof ServerPlayer jogador) {
            String modo = pacote.modo();
            if (!modo.equals(DadosDoHeroi.COMPARTILHAR_COM_TODOS) && !modo.equals(DadosDoHeroi.COMPARTILHAR_COM_NINGUEM)
                    && !modo.equals(DadosDoHeroi.COMPARTILHAR_COM_ESCOLHIDOS)) {
                return;
            }
            RegrasDoHeroi.dados(jogador).definirCompartilhamento(modo, new HashSet<>(pacote.escolhidos()));
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.compartilhar_salvo"), true);
        }
    }

    private static void aoReceberLancarMagia(PacoteLancarMagia pacote, IPayloadContext contexto) {
        MagiasDoClerigo.Magia magia = MagiasDoClerigo.Magia.peloId(pacote.magia());
        if (magia != null && contexto.player() instanceof ServerPlayer jogador) {
            MagiasDoClerigo.lancar(jogador, magia);
        }
    }
}
