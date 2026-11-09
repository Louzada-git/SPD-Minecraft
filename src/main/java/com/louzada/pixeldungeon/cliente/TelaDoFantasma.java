package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.rede.PacoteRecompensaDoFantasma;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// A conversa com o fantasma triste (o "WndQuest" e o "WndSadGhost" do Shattered): o que ele diz e, cumprida a missão,
// as duas recompensas. Clicar numa recompensa escolhe; o botão confirma (passar o mouse mostra o que ela é).
public class TelaDoFantasma extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int LARGURA = 240;
    private static final int CASINHA = 22;

    private Component texto;
    private ItemStack arma;
    private ItemStack armadura;
    private List<FormattedCharSequence> linhas;
    private int escolhida = -1;
    private int esquerda;
    private int topo;
    private int altura;
    private Button confirmar;

    public TelaDoFantasma(Component texto, ItemStack arma, ItemStack armadura) {
        super(Component.translatable("tela.pixeldungeon.fantasma"));
        this.texto = texto;
        this.arma = arma;
        this.armadura = armadura;
    }

    private boolean temRecompensa() {
        return !arma.isEmpty() || !armadura.isEmpty();
    }

    @Override
    protected void init() {
        linhas = font.split(texto, LARGURA - 16);
        altura = 30 + linhas.size() * 10 + (temRecompensa() ? CASINHA + 40 : 26);
        esquerda = (width - LARGURA) / 2;
        topo = (height - altura) / 2;
        int yDoBotao = topo + altura - 24;
        if (temRecompensa()) {
            confirmar = Button.builder(Component.translatable("tela.pixeldungeon.fantasma.escolher"), botao -> confirmar())
                    .bounds(esquerda + LARGURA / 2 - 60, yDoBotao, 120, 18).build();
            confirmar.active = false;
            addRenderableWidget(confirmar);
        } else {
            addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.fantasma.ok"), botao -> onClose())
                    .bounds(esquerda + LARGURA / 2 - 40, yDoBotao, 80, 18).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Onde fica cada recompensa (0 = arma, 1 = armadura)
    private int xDaCasinha(int qual) {
        return esquerda + LARGURA / 2 + (qual == 0 ? -CASINHA - 8 : 8);
    }

    private int yDasCasinhas() {
        return topo + 30 + linhas.size() * 10 + 6;
    }

    private int casinhaEmbaixo(double mouseX, double mouseY) {
        if (!temRecompensa()) {
            return -1;
        }
        for (int qual = 0; qual < 2; qual++) {
            int x = xDaCasinha(qual);
            int y = yDasCasinhas();
            if (mouseX >= x && mouseX < x + CASINHA && mouseY >= y && mouseY < y + CASINHA && !(qual == 0 ? arma : armadura).isEmpty()) {
                return qual;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        int qual = casinhaEmbaixo(mouseX, mouseY);
        if (qual >= 0) {
            escolhida = qual;
            confirmar.active = true;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    private void confirmar() {
        if (escolhida >= 0) {
            PacketDistributor.sendToServer(new PacoteRecompensaDoFantasma(escolhida));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        desenho.drawCenteredString(font, title, esquerda + LARGURA / 2, topo + 10, COR_DO_TITULO);
        for (int i = 0; i < linhas.size(); i++) {
            desenho.drawString(font, linhas.get(i), esquerda + 8, topo + 26 + i * 10, COR_DO_TEXTO);
        }
        if (!temRecompensa()) {
            return;
        }
        int embaixo = casinhaEmbaixo(mouseX, mouseY);
        for (int qual = 0; qual < 2; qual++) {
            ItemStack pilha = qual == 0 ? arma : armadura;
            if (pilha.isEmpty()) {
                continue;
            }
            int x = xDaCasinha(qual);
            int y = yDasCasinhas();
            int borda = qual == escolhida ? 0xFFFFFF44 : qual == embaixo ? 0xFFAAAAAA : 0xFF5A5E62;
            desenho.fill(x - 1, y - 1, x + CASINHA + 1, y + CASINHA + 1, borda);
            desenho.fill(x, y, x + CASINHA, y + CASINHA, 0xFF3A3D40);
            desenho.renderItem(pilha, x + 3, y + 3);
            desenho.renderItemDecorations(font, pilha, x + 3, y + 3);
        }
        if (embaixo >= 0) {
            desenho.renderTooltip(font, embaixo == 0 ? arma : armadura, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + altura + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + altura, COR_DO_FUNDO);
    }
}
