package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.alquimia.GuiaDeAlquimia;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

// O guia de alquimia (a aba de alquimia do diário do Shattered): em cima, uma página pra cada grupo de receitas
// (as que o herói ainda não achou ficam apagadas); embaixo, o texto da página escolhida e o desenho das receitas dela.
// Abre pelo botão "Guia" do pote (e volta pro pote quando fecha) ou quando o herói lê uma página.
public class TelaDoGuiaDeAlquimia extends Screen {
    private static final int LARGURA = 230;
    private static final int MARGEM = 10;
    private static final int ESPACO_DA_PAGINA = 24;
    private static final int Y_DAS_PAGINAS = 22;
    private static final int Y_DO_TITULO = 48;
    private static final int Y_DO_TEXTO = 62;
    private static final int ALTURA_DA_LINHA = 10;
    private static final int ALTURA_DA_RECEITA = 22;
    private static final int APAGADO = 0xAA303236;
    private static final int AZUL = 0xFF44CCFF;
    private static final int CINZA = 0xFFAAAAAA;

    // Uma receita desenhada: os ingredientes, a energia que ela gasta e o que sai
    private record Receita(List<ItemStack> ingredientes, int energia, ItemStack resultado) {
    }

    @Nullable
    private Screen anterior;
    private int pagina;
    private int esquerda;
    private int topo;
    private int altura;
    private List<FormattedCharSequence> linhas = List.of();
    private List<Receita> receitas = List.of();

    // pagina: a que abre (se o herói não tiver essa, abre na primeira que ele tem); anterior: a tela pra onde volta
    public TelaDoGuiaDeAlquimia(int pagina, @Nullable Screen anterior) {
        super(Component.translatable("tela.pixeldungeon.guia_de_alquimia"));
        this.anterior = anterior;
        this.pagina = pagina;
    }

    @Override
    protected void init() {
        int paginas = EstadoNoCliente.paginasDoGuia();
        if (pagina < 0 || pagina >= GuiaDeAlquimia.PAGINAS.length || !GuiaDeAlquimia.conhece(paginas, pagina)) {
            pagina = -1;
            for (int i = GuiaDeAlquimia.PAGINAS.length - 1; i >= 0; i--) {
                if (GuiaDeAlquimia.conhece(paginas, i)) {
                    pagina = i;
                }
            }
        }
        Component texto = pagina >= 0 ? Component.translatable(GuiaDeAlquimia.chaveDoTexto(pagina))
                : Component.translatable("tela.pixeldungeon.guia_de_alquimia.nenhuma");
        linhas = font.split(texto, LARGURA - 2 * MARGEM);
        receitas = receitas(pagina);
        altura = Y_DO_TEXTO + linhas.size() * ALTURA_DA_LINHA + 6 + receitas.size() * ALTURA_DA_RECEITA + 6;
        esquerda = (width - LARGURA) / 2;
        topo = Math.max(2, (height - altura) / 2);
    }

    // As receitas de cada página (iguais às do pote; os ingredientes são só exemplos)
    private static List<Receita> receitas(int pagina) {
        List<Receita> lista = new ArrayList<>();
        ItemStack carne = new ItemStack(ModItens.CARNE_MISTERIOSA.get());
        switch (pagina) {
            case 0 -> lista.add(new Receita(List.of(new ItemStack(ModItens.SEMENTES.get(0).get()), new ItemStack(ModItens.SEMENTES.get(1).get()),
                    new ItemStack(ModItens.SEMENTES.get(2).get())), 0, new ItemStack(ModItens.POCAO_ALEATORIA.get())));
            case 1 -> lista.add(new Receita(List.of(new ItemStack(ModItens.PERGAMINHOS.get(0).get())), 0,
                    new ItemStack(ModItens.PEDRA_ALEATORIA.get(), 2)));
            case 2 -> {
                lista.add(new Receita(List.of(carne), 1, new ItemStack(ModItens.ENSOPADO.get())));
                lista.add(new Receita(List.of(new ItemStack(ModItens.PASTEL.get()), new ItemStack(ModItens.RACAO.get()), carne), 6,
                        new ItemStack(ModItens.TORTA_DE_CARNE.get())));
                lista.add(new Receita(List.of(new ItemStack(ModItens.FRUTA_SEM_GRACA.get()), new ItemStack(ItemSemente.daPlanta(TipoDePlanta.GRAMA_SOLAR))),
                        2, FrutaSemGraca.cozida(TipoDePocao.CURA, 1)));
            }
            case 3 -> lista.add(new Receita(List.of(new ItemStack(ModItens.POCOES.get(0).get())), 4,
                    new ItemStack(ModItens.POCOES_EXOTICAS.get(0).get())));
            case 4 -> lista.add(new Receita(List.of(new ItemStack(ModItens.PERGAMINHOS.get(0).get())), 6,
                    new ItemStack(ModItens.PERGAMINHOS_EXOTICOS.get(0).get())));
            default -> {
            }
        }
        return lista;
    }

    private int xDaPagina(int i) {
        return esquerda + (LARGURA - GuiaDeAlquimia.PAGINAS.length * ESPACO_DA_PAGINA) / 2 + i * ESPACO_DA_PAGINA + 3;
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.render(desenho, mouseX, mouseY, parcial);
        desenho.drawCenteredString(font, title, esquerda + LARGURA / 2, topo + 8, DesenhoDoShattered.AMARELO);

        // As páginas
        int paginas = EstadoNoCliente.paginasDoGuia();
        ItemStack folha = new ItemStack(ModItens.PAGINA_DO_GUIA.get());
        Component dica = null;
        for (int i = 0; i < GuiaDeAlquimia.PAGINAS.length; i++) {
            int x = xDaPagina(i);
            int y = topo + Y_DAS_PAGINAS;
            desenho.fill(x - 1, y - 1, x + 17, y + 17, i == pagina ? DesenhoDoShattered.FUNDO_EQUIPADO : DesenhoDoShattered.FUNDO_DA_CASINHA);
            desenho.renderFakeItem(folha, x, y);
            boolean achou = GuiaDeAlquimia.conhece(paginas, i);
            if (!achou) {
                desenho.pose().pushPose();
                desenho.pose().translate(0, 0, 300);
                desenho.fill(x - 1, y - 1, x + 17, y + 17, APAGADO);
                desenho.pose().popPose();
            }
            if (DesenhoDoShattered.dentro(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                dica = achou ? Component.translatable(GuiaDeAlquimia.chaveDoTitulo(i)) : Component.translatable("tela.pixeldungeon.guia_de_alquimia.nao_achou");
            }
        }

        // O título e o texto da página
        if (pagina >= 0) {
            desenho.drawCenteredString(font, Component.translatable(GuiaDeAlquimia.chaveDoTitulo(pagina)), esquerda + LARGURA / 2, topo + Y_DO_TITULO,
                    DesenhoDoShattered.AMARELO_CLARO);
        }
        int y = topo + Y_DO_TEXTO;
        for (FormattedCharSequence linha : linhas) {
            desenho.drawString(font, linha, esquerda + MARGEM, y, DesenhoDoShattered.BRANCO, false);
            y += ALTURA_DA_LINHA;
        }

        // As receitas: ingredientes, a seta (com a energia em cima) e o resultado
        y += 6;
        ItemStack dicaDeItem = ItemStack.EMPTY;
        for (Receita receita : receitas) {
            int x = esquerda + MARGEM + 4;
            for (ItemStack ingrediente : receita.ingredientes()) {
                dicaDeItem = desenharItem(desenho, ingrediente, x, y, mouseX, mouseY, dicaDeItem);
                x += 20;
            }
            int xSeta = x + 4;
            desenho.fill(xSeta, y + 7, xSeta + 22, y + 9, CINZA);
            desenho.fill(xSeta + 18, y + 5, xSeta + 20, y + 11, CINZA);
            if (receita.energia() > 0) {
                String custo = String.valueOf(receita.energia());
                desenho.drawString(font, custo, xSeta + 2, y - 2, AZUL, false);
                DesenhoDoShattered.ENERGIA.desenhar(desenho, xSeta + 3 + font.width(custo), y - 2);
            }
            dicaDeItem = desenharItem(desenho, receita.resultado(), xSeta + 28, y, mouseX, mouseY, dicaDeItem);
            y += ALTURA_DA_RECEITA;
        }

        if (dica != null) {
            desenho.renderTooltip(font, dica, mouseX, mouseY);
        } else if (!dicaDeItem.isEmpty()) {
            desenho.renderTooltip(font, dicaDeItem, mouseX, mouseY);
        }
    }

    private ItemStack desenharItem(GuiGraphics desenho, ItemStack item, int x, int y, int mouseX, int mouseY, ItemStack dica) {
        desenho.fill(x - 1, y - 1, x + 17, y + 17, DesenhoDoShattered.FUNDO_DA_CASINHA);
        desenho.renderFakeItem(item, x, y);
        desenho.renderItemDecorations(font, item, x, y);
        return DesenhoDoShattered.dentro(mouseX, mouseY, x - 1, y - 1, 18, 18) ? item : dica;
    }

    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        super.renderBackground(desenho, mouseX, mouseY, parcial);
        DesenhoDoShattered.janela(desenho, esquerda, topo, LARGURA, altura);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        int paginas = EstadoNoCliente.paginasDoGuia();
        for (int i = 0; i < GuiaDeAlquimia.PAGINAS.length; i++) {
            if (i != pagina && GuiaDeAlquimia.conhece(paginas, i)
                    && DesenhoDoShattered.dentro(mouseX, mouseY, xDaPagina(i) - 1, topo + Y_DAS_PAGINAS - 1, 18, 18)) {
                pagina = i;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    // Volta pro pote (se ele ainda estiver aberto)
    @Override
    public void onClose() {
        Screen volta = anterior;
        if (volta instanceof AbstractContainerScreen<?> pote && (minecraft.player == null || minecraft.player.containerMenu != pote.getMenu())) {
            volta = null;
        }
        minecraft.setScreen(volta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
