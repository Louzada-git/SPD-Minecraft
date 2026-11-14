package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Esperar (o botão de esperar do Shattered, tecla Z): o herói passa 1 turno parado, sem fazer nada. Nesse turno ele não anda,
// não pula, não ataca, não usa itens e não mexe em nada (nem na habilidade da arma, nas magias ou em procurar).
// Segurando a tecla, ele continua esperando, turno depois de turno.
// Igual ao Shattered, esperar é o que prepara o golpe paciente da Duelista (ver Talentos).
public final class Espera {

    private Espera() {
    }

    public static boolean esperando(Player jogador) {
        return jogador.hasEffect(ModEfeitos.ESPERANDO);
    }

    // Começa a esperar 1 turno (se já estiver esperando, ou não puder, não faz nada); devolve se começou
    public static boolean esperar(ServerPlayer jogador) {
        if (!jogador.isAlive() || jogador.isSpectator() || esperando(jogador) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return false;
        }
        jogador.stopUsingItem();
        // ("ambiente": o ícone não fica piscando, já que o efeito inteiro dura menos que o aviso de que está acabando)
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ESPERANDO, Turno.TICKS, 0, true, false, true));
        NumerosFlutuantes.mostrarTraduzido(jogador, "efeito.pixeldungeon.esperar", NumerosFlutuantes.BRANCO);
        Talentos.aoEsperar(jogador);
        return true;
    }

    // ---- Esperando, não dá pra fazer nada ----

    public static void aoAtacar(AttackEntityEvent evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    // Clique direito com item, em bloco ou em alguém, e clique esquerdo em bloco
    // (o barramento de eventos não deixa ouvir o PlayerInteractEvent inteiro, então é um de cada vez)
    public static void aoUsarItem(PlayerInteractEvent.RightClickItem evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void aoClicarNoBloco(PlayerInteractEvent.RightClickBlock evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void aoBaterNoBloco(PlayerInteractEvent.LeftClickBlock evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void aoMexerEmAlguem(PlayerInteractEvent.EntityInteract evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void aoMexerEmAlguemNoPonto(PlayerInteractEvent.EntityInteractSpecific evento) {
        if (esperando(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
