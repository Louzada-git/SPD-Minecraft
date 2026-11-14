package com.louzada.pixeldungeon.cliente;

import org.lwjgl.glfw.GLFW;

import com.louzada.pixeldungeon.rede.PacoteEsperar;
import com.louzada.pixeldungeon.rede.PacoteProcurar;
import com.louzada.pixeldungeon.rede.PacoteUsarHabilidade;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Teclas do mod (dá pra trocar nas opções de controles do Minecraft, na categoria Pixel Dungeon)
//   K: tela de talentos
//   V: habilidade da arma (Duelista)
//   G: procurar (as portas escondidas das salas secretas)
//   Z: esperar 1 turno (segurando, continua esperando), igual à tecla de descansar do Shattered
public class TeclasDoMod {
    private static final String CATEGORIA = "key.categories.pixeldungeon";
    public static final KeyMapping TALENTOS = new KeyMapping("key.pixeldungeon.talentos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORIA);
    public static final KeyMapping HABILIDADE = new KeyMapping("key.pixeldungeon.habilidade", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORIA);
    public static final KeyMapping PROCURAR = new KeyMapping("key.pixeldungeon.procurar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORIA);
    public static final KeyMapping ESPERAR = new KeyMapping("key.pixeldungeon.esperar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORIA);
    // Segurando a tecla de esperar: quando o turno de espera acaba, pede outro (sem mandar pedido de novo antes da resposta chegar)
    private static long proximoPedidoDeEspera = 0;

    public static void aoRegistrar(RegisterKeyMappingsEvent evento) {
        evento.register(TALENTOS);
        evento.register(HABILIDADE);
        evento.register(PROCURAR);
        evento.register(ESPERAR);
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
        while (PROCURAR.consumeClick()) {
            PacketDistributor.sendToServer(new PacoteProcurar());
        }
        boolean apertou = false;
        while (ESPERAR.consumeClick()) {
            apertou = true;
        }
        long agora = jogo.player.level().getGameTime();
        if ((apertou || ESPERAR.isDown()) && !jogo.player.hasEffect(ModEfeitos.ESPERANDO) && agora >= proximoPedidoDeEspera) {
            pedirParaEsperar(agora);
        }
    }

    // Manda o pedido de esperar 1 turno (a tecla ou o botão da barra do inventário)
    public static void pedirParaEsperar(long agora) {
        proximoPedidoDeEspera = agora + 10;
        PacketDistributor.sendToServer(new PacoteEsperar());
    }
}
