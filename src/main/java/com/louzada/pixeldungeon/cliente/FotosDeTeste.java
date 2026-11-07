package com.louzada.pixeldungeon.cliente;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.run.HeroiDasFotos;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Só no teste do GitHub (propriedade pixeldungeon.fotografar): o jogo abre o mundo de teste sozinho,
// o servidor transforma o jogador num Guerreiro (HeroiDasFotos), e aqui saem as fotos da tela do jogo
// e do inventário, em dois tamanhos de interface. Depois o jogo fecha.
public class FotosDeTeste {
    // Se nada der certo em 5 minutos, fecha o jogo do mesmo jeito
    private static final int LIMITE = 20 * 60 * 5;
    private static int ticks = 0;
    private static int ticksNaRun = 0;
    // Botões que deixam o mundo abrir (carregar sem cópia de segurança, continuar...)
    private static final Set<String> CONTINUAR = Set.of("selectWorld.backupJoinSkipButton", "gui.proceed", "gui.continue", "gui.yes");

    public static boolean ligado() {
        return HeroiDasFotos.ligado();
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        Minecraft jogo = Minecraft.getInstance();
        ticks++;
        if (ticks > LIMITE) {
            PixelDungeon.LOGGER.info("Fotos de teste: o herói não entrou na run, fechando o jogo");
            jogo.stop();
            return;
        }
        if (jogo.player == null || !HudDoHeroi.ativo()) {
            // O Minecraft pergunta antes de abrir um mundo com configurações experimentais (as do mod):
            // aperta o botão de continuar
            if (jogo.player == null && jogo.screen != null && ticks % 40 == 0) {
                apertarContinuar(jogo.screen);
            }
            return;
        }
        ticksNaRun++;
        switch (ticksNaRun) {
            // Espera o mundo carregar em volta e fecha qualquer tela que tenha aberto
            case 100 -> jogo.setScreen(null);
            case 120 -> foto(jogo, "hud.png");
            case 130 -> jogo.setScreen(new TelaDoInventario(jogo.player));
            case 160 -> foto(jogo, "inventario.png");
            // A dica do primeiro ícone da fileira de efeitos e a de uma arma não identificada
            case 162 -> TelaDoInventario.mouseDeTeste = new int[] {39, jogo.getWindow().getGuiScaledHeight() - PainelDeStatus.ALTURA + 8};
            case 175 -> foto(jogo, "dica_efeito.png");
            case 177 -> {
                if (jogo.screen instanceof TelaDoInventario tela) {
                    Slot casinha = tela.getMenu().slots.get(10);
                    TelaDoInventario.mouseDeTeste = new int[] {casinha.x + 8, casinha.y + 8};
                }
            }
            case 190 -> foto(jogo, "dica_item.png");
            // A mesma coisa com a interface menor (sobra mais espaço na tela)
            case 195 -> {
                TelaDoInventario.mouseDeTeste = null;
                jogo.setScreen(null);
                jogo.options.guiScale().set(2);
                jogo.resizeDisplay();
            }
            case 215 -> foto(jogo, "hud_escala_2.png");
            case 225 -> jogo.setScreen(new TelaDoInventario(jogo.player));
            case 255 -> foto(jogo, "inventario_escala_2.png");
            // As portas gigantes do andar 1: uma comum e uma trancada
            case 260 -> {
                jogo.setScreen(null);
                noServidor(jogo, jogador -> HeroiDasFotos.olharPraPorta(jogador, false));
            }
            case 330 -> foto(jogo, "porta.png");
            case 335 -> noServidor(jogo, jogador -> HeroiDasFotos.olharPraPorta(jogador, true));
            case 405 -> foto(jogo, "porta_trancada.png");
            // Os inimigos com os desenhos do Shattered
            case 410 -> noServidor(jogo, HeroiDasFotos::mostrarInimigos);
            case 480 -> foto(jogo, "inimigos.png");
            // A tela de talentos (com o tier 2) e a do tomo com as 9 magias, na interface automática
            // (a maior que cabe: é onde falta mais espaço)
            case 485 -> {
                jogo.options.guiScale().set(0);
                jogo.resizeDisplay();
                noServidor(jogo, HeroiDasFotos::mostrarTalentos);
            }
            case 510 -> jogo.setScreen(new TelaDeTalentos(Talento.RAIO_SOLAR));
            case 530 -> foto(jogo, "talentos.png");
            case 535 -> jogo.setScreen(new TelaDoTomo());
            case 555 -> foto(jogo, "tomo.png");
            case 575 -> {
                PixelDungeon.LOGGER.info("Fotos de teste: pronto");
                jogo.stop();
            }
            default -> {
            }
        }
    }

    private static void apertarContinuar(Screen tela) {
        PixelDungeon.LOGGER.info("Fotos de teste: na tela {} ({})", tela.getClass().getSimpleName(), tela.getTitle().getString());
        for (GuiEventListener parte : tela.children()) {
            if (parte instanceof Button botao && botao.getMessage().getContents() instanceof TranslatableContents texto
                    && CONTINUAR.contains(texto.getKey())) {
                PixelDungeon.LOGGER.info("Fotos de teste: apertando {}", botao.getMessage().getString());
                botao.onPress();
                return;
            }
        }
    }

    // Faz algo com o jogador lá no servidor (no single player, o servidor roda junto com o jogo)
    private static void noServidor(Minecraft jogo, Consumer<ServerPlayer> acao) {
        MinecraftServer servidor = jogo.getSingleplayerServer();
        if (servidor == null || jogo.player == null) {
            return;
        }
        UUID id = jogo.player.getUUID();
        servidor.execute(() -> {
            ServerPlayer jogador = servidor.getPlayerList().getPlayer(id);
            if (jogador != null) {
                acao.accept(jogador);
            }
        });
    }

    private static void foto(Minecraft jogo, String nome) {
        Screenshot.grab(jogo.gameDirectory, nome, jogo.getMainRenderTarget(),
                mensagem -> PixelDungeon.LOGGER.info("Fotos de teste: {} ({})", nome, mensagem.getString()));
    }
}
