package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ViewportEvent;

// Olho de salamandra (acessório): o herói enxerga menos longe. No Shattered a visão é de 8 quadradinhos;
// com o olho, 12,5% a 50% menos. Aqui vira uma névoa que começa um pouco antes desse limite.
// (A visão mental dos inimigos perto, a parte boa, é do servidor: ver Acessorios.visaoMental)
public class VisaoDoOlho {
    private static final float VISAO_NORMAL = 8 * Andares.TAMANHO_DO_QUADRADINHO;

    public static void aoDesenharNevoa(ViewportEvent.RenderFog evento) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int nivel = Acessorios.nivel(minecraft.player, TipoDeAcessorio.OLHO_DE_SALAMANDRA);
        if (nivel < 0) {
            return;
        }
        float longe = Math.min(evento.getFarPlaneDistance(), VISAO_NORMAL * Acessorios.multiplicadorDaVisao(nivel));
        evento.setFarPlaneDistance(longe);
        evento.setNearPlaneDistance(Math.min(evento.getNearPlaneDistance(), longe * 0.5F));
        evento.setCanceled(true);
    }
}
