package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// A tela do pote de alquimia (a "AlchemyScene" do Shattered): as 3 casinhas de ingredientes à esquerda; no meio,
// um botão "Criar" pra cada receita que serve (até 3), com o que vai sair do lado (apagadinho); o resultado à direita;
// "Energizar" embaixo (e o "Guia", que abre o guia de alquimia); a energia do herói em cima e o inventário embaixo de tudo.
public class TelaDaAlquimia extends AbstractContainerScreen<MenuDaAlquimia> {
    private static final int AZUL = 0xFF44CCFF;
    private static final int CINZA = 0xFFAAAAAA;
    private static final int X_DOS_BOTOES = 36;
    private static final int LARGURA_DOS_BOTOES = 70;
    private static final int Y_DO_ENERGIZAR = 92;
    // O "Energizar (+N)" é um pouco mais largo que os botões de criar (senão o texto não cabe)
    private static final int LARGURA_DO_ENERGIZAR = 76;
    private static final int X_DO_GUIA = 116;
    private static final int LARGURA_DO_GUIA = 50;

    private Button[] misturar = new Button[MenuDaAlquimia.RECEITAS];
    private Button energizar;

    public TelaDaAlquimia(MenuDaAlquimia menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        imageWidth = 176;
        imageHeight = MenuDaAlquimia.Y_DO_INVENTARIO + 82;
        inventoryLabelY = MenuDaAlquimia.Y_DO_INVENTARIO - 11;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < MenuDaAlquimia.RECEITAS; i++) {
            int qual = i;
            misturar[i] = addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.alquimia.misturar"),
                    botao -> apertar(MenuDaAlquimia.BOTAO_MISTURAR + qual))
                    .bounds(leftPos + X_DOS_BOTOES, topPos + MenuDaAlquimia.Y_DOS_INGREDIENTES - 1 + i * 22, LARGURA_DOS_BOTOES, 18).build());
        }
        energizar = addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.alquimia.energizar"),
                botao -> apertar(MenuDaAlquimia.BOTAO_ENERGIZAR)).bounds(leftPos + X_DOS_BOTOES, topPos + Y_DO_ENERGIZAR, LARGURA_DO_ENERGIZAR, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.alquimia.guia"),
                botao -> minecraft.setScreen(new TelaDoGuiaDeAlquimia(-1, this)))
                .bounds(leftPos + X_DO_GUIA, topPos + Y_DO_ENERGIZAR, LARGURA_DO_GUIA, 18).build());
        atualizarBotoes();
    }

    private void apertar(int botao) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, botao);
        }
    }

    // Um botão por receita (só aparecem as que servem; apagado sem energia); energizar só se algum ingrediente der energia
    private void atualizarBotoes() {
        for (int i = 0; i < MenuDaAlquimia.RECEITAS; i++) {
            int custo = menu.custo(i);
            misturar[i].visible = custo >= 0;
            misturar[i].active = custo >= 0 && menu.energiaTotal() >= custo;
            misturar[i].setMessage(custo > 0 ? Component.translatable("tela.pixeldungeon.alquimia.misturar_custo", custo)
                    : Component.translatable("tela.pixeldungeon.alquimia.misturar"));
        }
        int energia = menu.energiaAoEnergizar();
        energizar.active = energia > 0;
        energizar.setMessage(energia > 0 ? Component.translatable("tela.pixeldungeon.alquimia.energizar_quanto", energia)
                : Component.translatable("tela.pixeldungeon.alquimia.energizar"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        atualizarBotoes();
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        // O que cada receita vai fazer, apagadinho
        for (int i = 0; i < MenuDaAlquimia.RECEITAS; i++) {
            ItemStack exemplo = menu.exemplo(i);
            if (exemplo.isEmpty()) {
                continue;
            }
            int x = leftPos + MenuDaAlquimia.X_DOS_EXEMPLOS;
            int y = topPos + MenuDaAlquimia.Y_DOS_INGREDIENTES + i * 22;
            desenho.renderFakeItem(exemplo, x, y);
            desenho.renderItemDecorations(font, exemplo, x, y);
            if (menu.energiaTotal() < menu.custo(i)) {
                desenho.pose().pushPose();
                desenho.pose().translate(0, 0, 300);
                desenho.fill(x, y, x + 16, y + 16, 0x99303236);
                desenho.pose().popPose();
            }
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                desenho.renderTooltip(font, exemplo, mouseX, mouseY);
            }
        }
        renderTooltip(desenho, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics desenho, float parcial, int mouseX, int mouseY) {
        DesenhoDoShattered.janela(desenho, leftPos, topPos, imageWidth, imageHeight);
        for (Slot casinha : menu.slots) {
            if (casinha.isActive()) {
                int x = leftPos + casinha.x;
                int y = topPos + casinha.y;
                desenho.fill(x - 1, y - 1, x + 17, y + 17, DesenhoDoShattered.FUNDO_DA_CASINHA);
            }
        }
        // A setinha de cada receita pro resultado
        int xFim = leftPos + MenuDaAlquimia.X_DO_RESULTADO - 4;
        int ySeta = topPos + MenuDaAlquimia.Y_DO_RESULTADO + 7;
        int xComeco = leftPos + MenuDaAlquimia.X_DOS_EXEMPLOS + 19;
        for (int i = 0; i < MenuDaAlquimia.RECEITAS; i++) {
            if (menu.custo(i) >= 0) {
                int y = topPos + MenuDaAlquimia.Y_DOS_INGREDIENTES + 7 + i * 22;
                desenho.fill(xComeco, y, xComeco + 6, y + 2, CINZA);
                desenho.fill(xComeco + 6, Math.min(y, ySeta), xComeco + 8, Math.max(y, ySeta) + 2, CINZA);
            }
        }
        desenho.fill(xComeco + 6, ySeta, xFim, ySeta + 2, CINZA);
        desenho.fill(xFim - 2, ySeta - 2, xFim, ySeta + 4, CINZA);
    }

    @Override
    protected void renderLabels(GuiGraphics desenho, int mouseX, int mouseY) {
        desenho.drawString(font, title, titleLabelX, titleLabelY, DesenhoDoShattered.AMARELO, false);
        desenho.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DesenhoDoShattered.BRANCO, false);
        // Aberta pelo kit do alquimista: a energia do herói + a guardada no kit
        Component energia = menu.energiaNoKit() >= 0
                ? Component.translatable("tela.pixeldungeon.alquimia.energia_com_kit", menu.energia(), menu.energiaNoKit())
                : Component.translatable("tela.pixeldungeon.alquimia.energia", menu.energia());
        desenho.drawString(font, energia, imageWidth - 8 - font.width(energia), titleLabelY, AZUL, false);
        // Sem receita: a dica no lugar dos botões
        if (menu.custo(0) < 0) {
            Component dica = Component.translatable("tela.pixeldungeon.alquimia.sem_receita").withStyle(ChatFormatting.GRAY);
            desenho.drawCenteredString(font, dica, X_DOS_BOTOES + 45, MenuDaAlquimia.Y_DOS_INGREDIENTES + 27, CINZA);
        }
    }
}
