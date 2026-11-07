package com.louzada.pixeldungeon.cliente;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo.Magia;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.rede.PacoteLancarMagia;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// Tela do tomo sagrado (clique direito no tomo), no estilo do Shattered:
// as magias que o Clérigo conhece (em linhas de 5); clicando numa aparece o que ela faz e o botão "Lançar".
// Magias com alvo (luz guia, escudo de luz, raio solar, bênção) vão pra onde o Clérigo está mirando.
public class TelaDoTomo extends Screen {
    private static final int COR_DO_FUNDO = 0xF02D2F31;
    private static final int COR_DA_BORDA = 0xFF5A5E62;
    private static final int COR_DO_TITULO = 0xFFFFFF44;
    private static final int COR_DO_TEXTO = 0xFFFFFFFF;
    private static final int COR_APAGADA = 0xFF9A9A9A;
    private static final int LARGURA = 250;
    // A altura com uma linha de magias; cada linha a mais soma ALTURA_DA_LINHA
    private static final int ALTURA_BASE = 200;
    private static final int TAMANHO_DO_ICONE = 32;
    private static final int ESPACO_DO_ICONE = 44;
    private static final int ALTURA_DA_LINHA = 38;
    private static final int MAGIAS_POR_LINHA = 5;

    private List<Magia> magias = List.of();
    private Magia escolhida;
    private Button botaoLancar;
    private int esquerda;
    private int topo;
    private int altura = ALTURA_BASE;

    public TelaDoTomo() {
        super(Component.translatable("item.pixeldungeon.tomo_sagrado"));
    }

    @Override
    protected void init() {
        magias = MagiasDoClerigo.conhecidas(EstadoNoCliente::pontos);
        altura = ALTURA_BASE + (linhas() - 1) * ALTURA_DA_LINHA;
        esquerda = (width - LARGURA) / 2;
        topo = Math.max(2, (height - altura) / 2);
        if (escolhida == null && !magias.isEmpty()) {
            escolhida = magias.get(0);
        }
        botaoLancar = addRenderableWidget(Button.builder(Component.translatable("tela.pixeldungeon.lancar"), botao -> lancar())
                .bounds(esquerda + LARGURA / 2 - 50, topo + altura - 28, 100, 20)
                .build());
    }

    private void lancar() {
        if (escolhida != null) {
            PacketDistributor.sendToServer(new PacoteLancarMagia(escolhida.id()));
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int custo(Magia magia) {
        return magia.custo(magia == Magia.INTUICAO_DIVINA ? EstadoNoCliente.pontos(Talento.INTUICAO_DIVINA) : 0);
    }

    @Nullable
    private ItemStack tomo() {
        return minecraft == null || minecraft.player == null ? null : TomoSagrado.acharTomo(minecraft.player);
    }

    // ---- Clique nas magias ----

    private int linhas() {
        return Math.max(1, (magias.size() + MAGIAS_POR_LINHA - 1) / MAGIAS_POR_LINHA);
    }

    // Cada linha fica centralizada (a última pode ter menos magias)
    private int xDoIcone(int posicao) {
        int linha = posicao / MAGIAS_POR_LINHA;
        int naLinha = Math.min(MAGIAS_POR_LINHA, magias.size() - linha * MAGIAS_POR_LINHA);
        int inicio = esquerda + (LARGURA - naLinha * ESPACO_DO_ICONE) / 2;
        return inicio + (posicao % MAGIAS_POR_LINHA) * ESPACO_DO_ICONE + (ESPACO_DO_ICONE - TAMANHO_DO_ICONE) / 2;
    }

    private int yDoIcone(int posicao) {
        return topo + 38 + (posicao / MAGIAS_POR_LINHA) * ALTURA_DA_LINHA;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        for (int i = 0; i < magias.size(); i++) {
            int x = xDoIcone(i);
            int y = yDoIcone(i);
            if (mouseX >= x && mouseX < x + TAMANHO_DO_ICONE && mouseY >= y && mouseY < y + TAMANHO_DO_ICONE) {
                escolhida = magias.get(i);
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
        ItemStack tomo = tomo();
        int maximo = tomo == null ? 0 : Artefato.cargasMaximas(tomo);
        int cargas = tomo == null ? 0 : Cargas.cargas(tomo, maximo);
        botaoLancar.active = escolhida != null && cargas >= custo(escolhida);

        super.render(desenho, mouseX, mouseY, parcial);

        int centro = esquerda + LARGURA / 2;
        desenho.drawCenteredString(font, title, centro, topo + 8, COR_DO_TITULO);
        desenho.drawCenteredString(font, Component.translatable("dica.pixeldungeon.cargas", cargas, maximo), centro, topo + 22, COR_APAGADA);

        for (int i = 0; i < magias.size(); i++) {
            Magia magia = magias.get(i);
            int x = xDoIcone(i);
            int y = yDoIcone(i);
            if (magia == escolhida) {
                desenho.renderOutline(x - 2, y - 2, TAMANHO_DO_ICONE + 4, TAMANHO_DO_ICONE + 4, COR_DO_TITULO);
            }
            desenho.blit(iconeDe(magia), x, y, TAMANHO_DO_ICONE, TAMANHO_DO_ICONE, 0, 0, 16, 16, 16, 16);
            if (cargas < custo(magia)) {
                desenho.fill(x, y, x + TAMANHO_DO_ICONE, y + TAMANHO_DO_ICONE, 0x99000000);
            }
        }

        if (escolhida != null) {
            int y = topo + 38 + (linhas() - 1) * ALTURA_DA_LINHA + TAMANHO_DO_ICONE + 10;
            desenho.drawCenteredString(font, Component.translatable(escolhida.chaveDoNome()), centro, y, COR_DO_TITULO);
            y += 12;
            int custo = custo(escolhida);
            desenho.drawCenteredString(font, Component.translatable(custo == 1 ? "tela.pixeldungeon.custo_uma" : "tela.pixeldungeon.custo", custo),
                    centro, y, COR_APAGADA);
            y += 14;
            for (FormattedCharSequence linha : font.split(TextoDoShattered.destacado(escolhida.chaveDaDescricao()), LARGURA - 16)) {
                desenho.drawString(font, linha, esquerda + 8, y, COR_DO_TEXTO);
                y += font.lineHeight + 1;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        desenho.fill(esquerda - 1, topo - 1, esquerda + LARGURA + 1, topo + altura + 1, COR_DA_BORDA);
        desenho.fill(esquerda, topo, esquerda + LARGURA, topo + altura, COR_DO_FUNDO);
    }

    private static ResourceLocation iconeDe(Magia magia) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/gui/feiticos/" + magia.id() + ".png");
    }
}
