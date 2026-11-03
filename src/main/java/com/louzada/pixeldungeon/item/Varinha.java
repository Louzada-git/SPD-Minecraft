package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Uma varinha do Shattered. Igual ao Shattered:
//   - começa com algumas cargas e ganha +1 por nível, até 10 (o cajado do Mago dá +1)
//   - as cargas voltam sozinhas: 10 + 40 x 0,875^(cargas que faltam) turnos por carga (mais rápido quanto mais vazia)
//   - lançar gasta 1 carga e 1 turno
// Cada varinha diz quantas cargas tem e o que acontece quando é lançada. Quem guarda as cargas no item é o Cargas;
// quem segura a varinha é o cajado do Mago (no Shattered dá pra trocar a varinha do cajado; aqui é sempre a de míssil mágico).
public abstract class Varinha {
    public static final int CARGAS_NO_MAXIMO = 10;

    // Quantas cargas ela tem no +0
    public abstract int cargasIniciais();

    // O que acontece quando é lançada (quem chamou já gastou a carga)
    public abstract void lancar(ServerPlayer jogador, int nivel);

    // As linhas da dica que dizem o que ela faz neste nível
    public abstract void dica(List<Component> linhas, int nivel);

    // extra: cargas a mais de quem segura (o cajado dá 1)
    public int cargasMaximas(int nivel, int extra) {
        return Math.min(CARGAS_NO_MAXIMO, cargasIniciais() + Math.max(0, nivel) + extra);
    }

    // Quantos ticks leva pra encher uma carga, sabendo quantas estão faltando
    public static long ticksParaCarregar(int faltando) {
        return Math.round((10 + 40 * Math.pow(0.875, faltando)) * Turno.TICKS);
    }
}
