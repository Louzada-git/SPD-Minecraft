package com.louzada.pixeldungeon.cliente;

import java.util.List;

import com.louzada.pixeldungeon.rede.PacoteItemEscolhido;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// Tela de escolher um item pro pergaminho (melhoria, identificação, remover maldição, transmutação).
// Mostra só os itens que servem; clicar escolhe. Fechar sem escolher é desistir.
public class TelaEscolherItem extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int LARGURA = 220;
    private static final int ALTURA_DA_LINHA = 20;

    private List<Integer> espacos;
    private boolean escolheu = false;
    private int esquerda;
    private int topo;
    private int altura;

    public TelaEscolherItem(String pergaminho, List<Integer> espacos) {
        super(Component.translatable("tela.pixeldungeon.escolher_item." + pergaminho));
        this.espacos = espacos;
    }

    @Override
    protected void init() {
        altura = 36 + Math.min(espacos.size(), 10) * ALTURA_DA_LINHA;
        esquerda = (width - LARGURA) / 2;
        topo = (height - altura) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ItemStack itemNoEspaco(int espaco) {
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(espaco);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        for (int i = 0; i < Math.min(espacos.size(), 10); i++) {
            int y = topo + 28 + i * ALTURA_DA_LINHA;
            if (mouseX >= esquerda + 6 && mouseX < esquerda + LARGURA - 6 && mouseY >= y && mouseY < y + ALTURA_DA_LINHA - 2) {
                escolheu = true;
                PacketDistributor.sendToServer(new PacoteItemEscolhido(espacos.get(i)));
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    // Fechou sem escolher: desistiu
    @Override
    public void removed() {
        if (!escolheu) {
            PacketDistributor.sendToServer(new PacoteItemEscolhido(-1));
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        desenho.drawCenteredString(font, title, esquerda + LARGURA / 2, topo + 10, COR_DO_TITULO);
        for (int i = 0; i < Math.min(espacos.size(), 10); i++) {
            int y = topo + 28 + i * ALTURA_DA_LINHA;
            boolean emCima = mouseX >= esquerda + 6 && mouseX < esquerda + LARGURA - 6 && mouseY >= y && mouseY < y + ALTURA_DA_LINHA - 2;
            desenho.fill(esquerda + 6, y, esquerda + LARGURA - 6, y + ALTURA_DA_LINHA - 2, emCima ? 0xFF4A4E52 : 0xFF3A3D40);
            ItemStack pilha = itemNoEspaco(espacos.get(i));
            desenho.renderItem(pilha, esquerda + 9, y + 1);
            desenho.renderItemDecorations(font, pilha, esquerda + 9, y + 1);
            desenho.drawString(font, pilha.getHoverName(), esquerda + 30, y + 5, COR_DO_TEXTO);
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + altura + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + altura, COR_DO_FUNDO);
    }
}
