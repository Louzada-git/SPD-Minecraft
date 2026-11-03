package com.louzada.pixeldungeon.cliente;

import java.util.Set;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

// A tela do jogo igual à interface completa do Shattered:
//   - embaixo à esquerda, a barra de status (rosto do herói, vida, experiência, nível e efeitos)
//   - embaixo à direita, a barra de ferramentas com as 9 casinhas de atalho
// A barra de itens, os corações, a fome, a armadura, a experiência e os efeitos do Minecraft somem durante a run.
public class HudDoHeroi {
    // As partes da tela do Minecraft que o Shattered mostra do jeito dele
    private static final Set<ResourceLocation> ESCONDIDAS = Set.of(VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.EFFECTS);
    private static final Set<ResourceLocation> ESCONDIDAS_NO_INVENTARIO = Set.of(VanillaGuiLayers.CHAT, VanillaGuiLayers.CROSSHAIR);
    // Espaço entre a barra de status e a barra de ferramentas
    private static final int ESPACO = 4;

    public static void aoRegistrar(RegisterGuiLayersEvent evento) {
        evento.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "hud_do_heroi"), HudDoHeroi::desenhar);
    }

    // O herói está na run (tem classe e não está só assistindo)
    public static boolean ativo() {
        Minecraft jogo = Minecraft.getInstance();
        return jogo.player != null && EstadoNoCliente.classe() != null && !jogo.player.isSpectator();
    }

    // Cabe a barra de status do lado da barra de ferramentas? Se não couber, ela vai pro canto de cima.
    public static boolean cabeEmbaixo(int largura, int larguraDaDireita) {
        return largura >= PainelDeStatus.LARGURA + ESPACO + larguraDaDireita;
    }

    public static void aoDesenharCamada(RenderGuiLayerEvent.Pre evento) {
        if (ESCONDIDAS.contains(evento.getName()) && ativo()) {
            evento.setCanceled(true);
        }
        // Com o inventário aberto, o chat e a mira ficariam aparecendo entre os painéis
        if (ESCONDIDAS_NO_INVENTARIO.contains(evento.getName()) && Minecraft.getInstance().screen instanceof TelaDoInventario) {
            evento.setCanceled(true);
        }
    }

    private static void desenhar(GuiGraphics desenho, DeltaTracker tempo) {
        Minecraft jogo = Minecraft.getInstance();
        LocalPlayer jogador = jogo.player;
        // Com o inventário aberto, quem desenha tudo é a tela do inventário
        if (!ativo() || jogo.options.hideGui || jogo.screen instanceof TelaDoInventario) {
            return;
        }
        int largura = desenho.guiWidth();
        int altura = desenho.guiHeight();
        int yDoStatus = cabeEmbaixo(largura, BarraDeFerramentas.LARGURA) ? altura - PainelDeStatus.ALTURA : 0;
        PainelDeStatus.desenhar(desenho, jogador, 0, yDoStatus);
        BarraDeFerramentas.desenhar(desenho, jogador, largura, altura - BarraDeFerramentas.ALTURA, true, -1, -1);
    }
}
