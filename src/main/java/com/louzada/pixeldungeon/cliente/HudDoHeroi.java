package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

// Avisos do lado direito da barra de itens:
//   - o ouro do herói (com o desenho da moeda)
//   - pontos de talento pra gastar (tecla K)
//   - cargas da habilidade da arma da Duelista (tecla V) e os golpes seguidos das luvas
public class HudDoHeroi {
    private static final int AMARELO = 0xFFFF44;
    private static final int CINZA = 0xAAAAAA;
    private static final int DOURADO = 0xFFD700;
    private static ItemStack moeda = ItemStack.EMPTY;

    public static void aoRegistrar(RegisterGuiLayersEvent evento) {
        evento.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "hud_do_heroi"), HudDoHeroi::desenhar);
    }

    private static void desenhar(GuiGraphics desenho, DeltaTracker tempo) {
        Minecraft jogo = Minecraft.getInstance();
        if (jogo.player == null || jogo.options.hideGui || jogo.player.isSpectator() || EstadoNoCliente.classe() == null) {
            return;
        }
        int x = desenho.guiWidth() / 2 + 96;
        int y = desenho.guiHeight() - 12;

        // Ouro: o desenho da pilha de moedas, pela metade, e o total do lado
        if (moeda.isEmpty()) {
            moeda = new ItemStack(ModItens.OURO.get());
        }
        desenho.pose().pushPose();
        desenho.pose().translate(x, y - 1, 0);
        desenho.pose().scale(0.625F, 0.625F, 1.0F);
        desenho.renderItem(moeda, 0, 0);
        desenho.pose().popPose();
        desenho.drawString(jogo.font, String.valueOf(EstadoNoCliente.ouro()), x + 12, y, DOURADO);
        y -= 11;

        if (EstadoNoCliente.classe() == ClasseDoHeroi.DUELISTA) {
            int cargas = EstadoNoCliente.cargasDaHabilidade();
            int maximo = EstadoNoCliente.cargasMaximasDaHabilidade();
            StringBuilder pontos = new StringBuilder();
            for (int i = 0; i < maximo; i++) {
                pontos.append(i < cargas ? '◆' : '◇');
            }
            Component texto = Component.translatable("hud.pixeldungeon.habilidade", TeclasDoMod.HABILIDADE.getTranslatedKeyMessage(), pontos.toString());
            desenho.drawString(jogo.font, texto, x, y, cargas > 0 ? AMARELO : CINZA);
            if (EstadoNoCliente.golpesSeguidos() > 0) {
                desenho.drawString(jogo.font, "x" + EstadoNoCliente.golpesSeguidos(), x + jogo.font.width(texto) + 4, y, 0xFF8800);
            }
            y -= 11;
        }
        if (EstadoNoCliente.pontosLivres() > 0) {
            desenho.drawString(jogo.font, Component.translatable("hud.pixeldungeon.pontos_de_talento", EstadoNoCliente.pontosLivres(),
                    TeclasDoMod.TALENTOS.getTranslatedKeyMessage()), x, y, AMARELO);
        }
    }
}
