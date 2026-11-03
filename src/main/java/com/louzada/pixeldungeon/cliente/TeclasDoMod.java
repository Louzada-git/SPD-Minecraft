package com.louzada.pixeldungeon.cliente;

import org.lwjgl.glfw.GLFW;

import com.louzada.pixeldungeon.rede.PacoteUsarHabilidade;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Teclas do mod (dá pra trocar nas opções de controles do Minecraft, na categoria Pixel Dungeon)
//   K: tela de talentos
//   V: habilidade da arma (Duelista)
public class TeclasDoMod {
    private static final String CATEGORIA = "key.categories.pixeldungeon";
    public static final KeyMapping TALENTOS = new KeyMapping("key.pixeldungeon.talentos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORIA);
    public static final KeyMapping HABILIDADE = new KeyMapping("key.pixeldungeon.habilidade", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORIA);

    public static void aoRegistrar(RegisterKeyMappingsEvent evento) {
        evento.register(TALENTOS);
        evento.register(HABILIDADE);
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        Minecraft jogo = Minecraft.getInstance();
        if (jogo.player == null) {
            return;
        }
        while (TALENTOS.consumeClick()) {
            if (jogo.screen == null) {
                jogo.setScreen(new TelaDeTalentos());
            }
        }
        while (HABILIDADE.consumeClick()) {
            PacketDistributor.sendToServer(new PacoteUsarHabilidade());
        }
    }
}
