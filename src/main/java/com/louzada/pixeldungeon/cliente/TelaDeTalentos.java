package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.rede.PacoteAprimorarTalento;
import com.louzada.pixeldungeon.rede.PacotePedirCompartilhar;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

// Tela de talentos (tecla K), no estilo do Shattered:
//   os 4 talentos do tier 1 com os pontos de cada um embaixo;
//   clicando num talento aparece o que ele faz e o botão "Aprimorar"
public class TelaDeTalentos extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int COR_APAGADA = 0xFF9A9A9A;
    private static final int LARGURA = 240;
    private static final int ALTURA = 220;
    private static final int TAMANHO_DO_ICONE = 32;
    private static final int ESPACO_DO_ICONE = 52;

    private ClasseDoHeroi classe;
    private List<Talento> talentos = List.of();
    private Talento escolhido;
    private Button botaoAprimorar;
    private int esquerda;
    private int topo;

    public TelaDeTalentos() {
        super(Component.translatable("tela.pixeldungeon.talentos"));
    }

    @Override
    protected void init() {
        esquerda = (width - LARGURA) / 2;
        topo = (height - ALTURA) / 2;
        classe = EstadoNoCliente.classe();
        if (classe != null) {
            talentos = Talento.daClasse(classe);
            if (escolhido == null && !talentos.isEmpty()) {
                escolhido = talentos.get(0);
            }
        }
        botaoAprimorar = addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.aprimorar"), botao -> aprimorar())
                .bounds(esquerda + LARGURA / 2 - 50, topo + ALTURA - 28, 100, 20)
                .build());
        // Com quem compartilhar o que identificar (poções e pergaminhos)
        addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.compartilhar.botao"),
                botao -> PacketDistributor.sendToServer(new PacotePedirCompartilhar()))
                .bounds(esquerda + LARGURA + 6, topo, 90, 20)
                .build());
    }

    private void aprimorar() {
        if (escolhido != null) {
            PacketDistributor.sendToServer(new PacoteAprimorarTalento(escolhido.id()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Clique nos talentos ----

    private int xDoIcone(int posicao) {
        int inicio = esquerda + (LARGURA - talentos.size() * ESPACO_DO_ICONE) / 2;
        return inicio + posicao * ESPACO_DO_ICONE + (ESPACO_DO_ICONE - TAMANHO_DO_ICONE) / 2;
    }

    private int yDosIcones() {
        return topo + 42;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        for (int i = 0; i < talentos.size(); i++) {
            int x = xDoIcone(i);
            if (mouseX >= x && mouseX < x + TAMANHO_DO_ICONE && mouseY >= yDosIcones() && mouseY < yDosIcones() + TAMANHO_DO_ICONE) {
                escolhido = talentos.get(i);
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    // ---- Desenho ----

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        int pontosLivres = EstadoNoCliente.pontosLivres();
        botaoAprimorar.active = escolhido != null && pontosLivres > 0 && EstadoNoCliente.pontos(escolhido) < Talento.PONTOS_MAXIMOS;
        botaoAprimorar.visible = classe != null;

        super.render(desenho, mouseX, mouseY, parcial);

        int centro = esquerda + LARGURA / 2;
        desenho.drawCenteredString(font, title, centro, topo + 8, COR_DO_TITULO);
        if (classe == null) {
            desenho.drawCenteredString(font, Component.translatable("tela.pixeldungeon.sem_heroi"), centro, topo + ALTURA / 2, COR_APAGADA);
            return;
        }
        desenho.drawCenteredString(font, Component.translatable("tela.pixeldungeon.tier_e_pontos", 1, pontosLivres), centro, topo + 24,
                pontosLivres > 0 ? COR_DO_TITULO : COR_APAGADA);

        // Os talentos e os pontos de cada um (quadradinhos embaixo)
        for (int i = 0; i < talentos.size(); i++) {
            Talento talento = talentos.get(i);
            int x = xDoIcone(i);
            int y = yDosIcones();
            if (talento == escolhido) {
                desenho.renderOutline(x - 2, y - 2, TAMANHO_DO_ICONE + 4, TAMANHO_DO_ICONE + 4, COR_DO_TITULO);
            }
            desenho.blit(iconeDe(talento), x, y, TAMANHO_DO_ICONE, TAMANHO_DO_ICONE, 0, 0, 16, 16, 16, 16);
            int pontos = EstadoNoCliente.pontos(talento);
            if (pontos == 0) {
                // Talento sem pontos fica mais escuro, igual ao Shattered
                desenho.fill(x, y, x + TAMANHO_DO_ICONE, y + TAMANHO_DO_ICONE, 0x88000000);
            }
            for (int ponto = 0; ponto < Talento.PONTOS_MAXIMOS; ponto++) {
                int px = x + 7 + ponto * 11;
                int py = y + TAMANHO_DO_ICONE + 4;
                desenho.fill(px, py, px + 8, py + 4, ponto < pontos ? COR_DO_TITULO : 0xFF45484B);
            }
        }

        // O talento escolhido: nome, o que faz e (se for o caso) o aviso de que ainda não faz nada
        if (escolhido != null) {
            int y = yDosIcones() + TAMANHO_DO_ICONE + 16;
            desenho.drawCenteredString(font, Component.translatable(escolhido.chaveDoNome()), centro, y, COR_DO_TITULO);
            y += 13;
            y = escrever(desenho, TextoDoShattered.destacado(escolhido.chaveDaDescricao()), y, COR_DO_TEXTO);
            if (escolhido.chaveDoPendente() != null) {
                y += 4;
                escrever(desenho, Component.translatable(escolhido.chaveDoPendente()).withStyle(ChatFormatting.ITALIC), y, COR_APAGADA);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + ALTURA + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + ALTURA, COR_DO_FUNDO);
    }

    private int escrever(GuiGraphics desenho, Component texto, int y, int cor) {
        for (FormattedCharSequence linha : font.split(texto, LARGURA - 16)) {
            desenho.drawString(font, linha, esquerda + 8, y, cor);
            y += font.lineHeight + 1;
        }
        return y;
    }

    private static ResourceLocation iconeDe(Talento talento) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/talentos/" + talento.id() + ".png");
    }
}
