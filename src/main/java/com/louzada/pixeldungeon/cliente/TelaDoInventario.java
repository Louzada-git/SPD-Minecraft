package com.louzada.pixeldungeon.cliente;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.item.ConhecimentoDoCliente;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.NivelDoItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

// O inventário igual ao da interface completa do Shattered. Quando abre, a parte de baixo da tela "cresce":
//   - à direita, o painel do inventário: o equipamento (a armadura, a mão esquerda e os 2 anéis) e o ouro em cima,
//     e as 27 casinhas da mochila embaixo; a barra de ferramentas sobe e fica em cima dele
//   - à esquerda, o painel do herói (a skin do jogador em 3D, a classe, o nível, a força e o andar)
//     e a barra de status (rosto, vida, experiência e efeitos)
// O jogo continua aparecendo atrás. O Shattered não tem mesa de trabalho, então as casinhas de craft ficam escondidas.
public class TelaDoInventario extends AbstractContainerScreen<InventoryMenu> {
    // Casinhas de 24x24 com 1 de espaço entre elas, 9 colunas e 4 fileiras (equipamento + 3 da mochila)
    private static final int CASINHA = 24;
    private static final int PASSO = CASINHA + 1;
    private static final int MARGEM = 4;
    private static final int COLUNAS = 9;
    public static final int LARGURA_DO_PAINEL = MARGEM * 2 + PASSO * COLUNAS - 1;
    public static final int ALTURA_DO_PAINEL = MARGEM * 2 + PASSO * 4 - 1;
    // Largura máxima das dicas (o texto quebra em várias linhas)
    private static final int LARGURA_DA_DICA = 200;
    // Lugar escondido (fora da tela) pras casinhas que o Shattered não tem
    private static final int ESCONDIDA = -10000;

    // Os números das casinhas no inventário do Minecraft
    // Igual ao Shattered, só uma armadura (a do peito); o capacete fica escondido,
    // e a calça e a bota viraram as 2 casinhas de anel
    private static final int ARMADURA = 6;
    private static final int PRIMEIRO_ANEL = 7;
    private static final int SEGUNDO_ANEL = 8;
    private static final int PRIMEIRA_DA_MOCHILA = 9;
    private static final int PRIMEIRA_DO_ATALHO = 36;
    private static final int OUTRA_MAO = 45;

    // Só no teste do GitHub (FotosDeTeste): onde fingir que o mouse está, pra foto mostrar as dicas
    @Nullable
    static int[] mouseDeTeste = null;

    private final LocalPlayer jogador;
    // Onde as casinhas ficavam no inventário do Minecraft (volta tudo quando a tela fecha)
    private final int[] xOriginal;
    private final int[] yOriginal;

    private int xDoPainel;
    private int yDoPainel;
    private int topoDaBarra;
    private int yDoStatus;
    private boolean comPainelDoHeroi;

    public TelaDoInventario(LocalPlayer jogador) {
        super(jogador.inventoryMenu, jogador.getInventory(), Component.translatable("container.inventory"));
        this.jogador = jogador;
        xOriginal = new int[menu.slots.size()];
        yOriginal = new int[menu.slots.size()];
        for (int i = 0; i < menu.slots.size(); i++) {
            xOriginal[i] = menu.slots.get(i).x;
            yOriginal[i] = menu.slots.get(i).y;
        }
    }

    // No lugar do inventário do Minecraft, abre este (só pra quem está na run e não está no criativo)
    public static void aoAbrirTela(ScreenEvent.Opening evento) {
        Minecraft jogo = Minecraft.getInstance();
        if (evento.getNewScreen() instanceof InventoryScreen && jogo.player != null && HudDoHeroi.ativo() && !jogo.player.hasInfiniteMaterials()) {
            evento.setNewScreen(new TelaDoInventario(jogo.player));
        }
    }

    @Override
    protected void init() {
        // A tela inteira é a "janela": as casinhas ficam onde a gente quiser
        imageWidth = width;
        imageHeight = height;
        super.init();
        xDoPainel = width - LARGURA_DO_PAINEL;
        yDoPainel = height - ALTURA_DO_PAINEL;
        topoDaBarra = yDoPainel - BarraDeFerramentas.ALTURA;
        comPainelDoHeroi = HudDoHeroi.cabeEmbaixo(width, Math.max(LARGURA_DO_PAINEL, BarraDeFerramentas.LARGURA));
        yDoStatus = comPainelDoHeroi ? height - PainelDeStatus.ALTURA : 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot casinha = menu.slots.get(i);
            casinha.x = xDaCasinha(i);
            casinha.y = yDaCasinha(i);
        }
    }

    // Igual ao Shattered: com o inventário aberto, o tempo para (no mundo de um jogador só)
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        for (int i = 0; i < menu.slots.size() && i < xOriginal.length; i++) {
            menu.slots.get(i).x = xOriginal[i];
            menu.slots.get(i).y = yOriginal[i];
        }
        super.removed();
    }

    // ---- Onde fica cada casinha ----

    private int xDaCelula(int coluna) {
        return xDoPainel + MARGEM + coluna * PASSO;
    }

    private int yDaCelula(int fileira) {
        return yDoPainel + MARGEM + fileira * PASSO;
    }

    // O lugar do desenho do item (16x16, no meio da casinha)
    private int xDaCasinha(int indice) {
        if (indice == ARMADURA) {
            return xDaCelula(0) + 4;
        }
        if (indice == OUTRA_MAO) {
            return xDaCelula(1) + 4;
        }
        if (indice == PRIMEIRO_ANEL || indice == SEGUNDO_ANEL) {
            return xDaCelula(2 + indice - PRIMEIRO_ANEL) + 4;
        }
        if (indice >= PRIMEIRA_DA_MOCHILA && indice < PRIMEIRA_DO_ATALHO) {
            return xDaCelula((indice - PRIMEIRA_DA_MOCHILA) % COLUNAS) + 4;
        }
        if (indice >= PRIMEIRA_DO_ATALHO && indice < PRIMEIRA_DO_ATALHO + BarraDeFerramentas.CASINHAS) {
            return BarraDeFerramentas.xDoItem(width, indice - PRIMEIRA_DO_ATALHO);
        }
        return ESCONDIDA;
    }

    private int yDaCasinha(int indice) {
        if (indice == ARMADURA || indice == OUTRA_MAO || indice == PRIMEIRO_ANEL || indice == SEGUNDO_ANEL) {
            return yDaCelula(0) + 4;
        }
        if (indice >= PRIMEIRA_DA_MOCHILA && indice < PRIMEIRA_DO_ATALHO) {
            return yDaCelula(1 + (indice - PRIMEIRA_DA_MOCHILA) / COLUNAS) + 4;
        }
        if (indice >= PRIMEIRA_DO_ATALHO && indice < PRIMEIRA_DO_ATALHO + BarraDeFerramentas.CASINHAS) {
            return BarraDeFerramentas.yDoItem(topoDaBarra);
        }
        return ESCONDIDA;
    }

    // ---- Desenho ----

    // Igual ao Shattered: o jogo continua aparecendo atrás do inventário (sem escurecer)
    @Override
    public void renderBackground(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        renderBg(desenho, parcial, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics desenho, float parcial, int mouseX, int mouseY) {
        if (comPainelDoHeroi) {
            desenharPainelDoHeroi(desenho, mouseX, mouseY);
        }
        PainelDeStatus.desenhar(desenho, jogador, 0, yDoStatus);
        BarraDeFerramentas.desenhar(desenho, jogador, width, topoDaBarra, false, mouseX, mouseY);
        desenharPainel(desenho, mouseX, mouseY);
    }

    // As casinhas vazias de equipamento mostram o desenho do Shattered (já desenhado no fundo), não o do Minecraft
    @Override
    protected void renderSlot(GuiGraphics desenho, Slot casinha) {
        if ((casinha.index == ARMADURA || casinha.index == OUTRA_MAO || casinha.index == PRIMEIRO_ANEL || casinha.index == SEGUNDO_ANEL)
                && !casinha.hasItem()) {
            return;
        }
        super.renderSlot(desenho, casinha);
    }

    // Os nomes "Inventário" e "Criar" do Minecraft não aparecem
    @Override
    protected void renderLabels(GuiGraphics desenho, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics desenho, int mouseX, int mouseY, float parcial) {
        if (mouseDeTeste != null) {
            mouseX = mouseDeTeste[0];
            mouseY = mouseDeTeste[1];
        }
        super.render(desenho, mouseX, mouseY, parcial);
        if (menu.getCarried().isEmpty() && hoveredSlot == null) {
            List<Component> dica = dicaEm(mouseX, mouseY);
            if (dica != null) {
                // As descrições compridas quebram em várias linhas
                List<FormattedCharSequence> linhas = new ArrayList<>();
                for (Component linha : dica) {
                    linhas.addAll(font.split(linha, LARGURA_DA_DICA));
                }
                desenho.renderTooltip(font, linhas, mouseX, mouseY);
            }
        }
        renderTooltip(desenho, mouseX, mouseY);
    }

    // O painel do inventário: o fundo escuro, as casinhas e o ouro
    private void desenharPainel(GuiGraphics desenho, int mouseX, int mouseY) {
        DesenhoDoShattered.painel(desenho, xDoPainel, yDoPainel, LARGURA_DO_PAINEL, ALTURA_DO_PAINEL);
        // Equipamento: a armadura, a mão esquerda e os anéis (vazias, com o desenho do Shattered)
        ItemStack armadura = menu.slots.get(ARMADURA).getItem();
        desenharCelula(desenho, xDaCelula(0), yDaCelula(0), armadura, !armadura.isEmpty(), mouseX, mouseY);
        if (armadura.isEmpty()) {
            DesenhoDoShattered.ESPACO_ARMADURA.desenhar(desenho, xDaCelula(0) + 4, yDaCelula(0) + 4);
        }
        ItemStack outraMao = menu.slots.get(OUTRA_MAO).getItem();
        desenharCelula(desenho, xDaCelula(1), yDaCelula(0), outraMao, !outraMao.isEmpty(), mouseX, mouseY);
        if (outraMao.isEmpty()) {
            DesenhoDoShattered.ESPACO_ARTEFATO.desenhar(desenho, xDaCelula(1) + 4, yDaCelula(0) + 4);
        }
        for (int anel = 0; anel < 2; anel++) {
            ItemStack pilha = menu.slots.get(PRIMEIRO_ANEL + anel).getItem();
            desenharCelula(desenho, xDaCelula(2 + anel), yDaCelula(0), pilha, !pilha.isEmpty(), mouseX, mouseY);
            if (pilha.isEmpty()) {
                DesenhoDoShattered.ESPACO_ANEL.desenhar(desenho, xDaCelula(2 + anel) + 4, yDaCelula(0) + 4);
            }
        }
        for (int i = 0; i < PRIMEIRA_DO_ATALHO - PRIMEIRA_DA_MOCHILA; i++) {
            ItemStack pilha = menu.slots.get(PRIMEIRA_DA_MOCHILA + i).getItem();
            desenharCelula(desenho, xDaCelula(i % COLUNAS), yDaCelula(1 + i / COLUNAS), pilha, false, mouseX, mouseY);
        }

        // Ouro, do lado do equipamento (igual ao Shattered: o número em amarelo e a moedinha)
        String ouro = String.valueOf(EstadoNoCliente.ouro());
        int x = xDaCelula(4) + 2;
        int y = yDaCelula(0) + 8;
        desenho.drawString(font, ouro, x, y, DesenhoDoShattered.AMARELO, false);
        DesenhoDoShattered.MOEDA.desenhar(desenho, x + font.width(ouro) + 2, y);
    }

    private void desenharCelula(GuiGraphics desenho, int x, int y, ItemStack pilha, boolean equipado, int mouseX, int mouseY) {
        desenho.fill(x, y, x + CASINHA, y + CASINHA, corDoFundo(pilha, equipado));
        if (DesenhoDoShattered.dentro(mouseX, mouseY, x, y, CASINHA, CASINHA)) {
            desenho.fill(x, y, x + CASINHA, y + CASINHA, 0x30FFFFFF);
        }
    }

    // A cor do fundo da casinha, igual ao Shattered: cinza claro pro que está equipado,
    // avermelhado pro que se sabe que é amaldiçoado e arroxeado pro que ainda não foi identificado
    private static int corDoFundo(ItemStack pilha, boolean equipado) {
        int base = equipado ? DesenhoDoShattered.FUNDO_EQUIPADO : DesenhoDoShattered.FUNDO_DA_CASINHA;
        float vermelho = 0;
        float verde = 0;
        float azul = 0;
        if (NivelDoItem.amaldicoado(pilha) && NivelDoItem.maldicaoConhecida(pilha)) {
            vermelho = 0.3F;
            verde = -0.15F;
            azul = -0.15F;
        } else if (NivelDoItem.podeIdentificar(pilha) && !NivelDoItem.identificado(pilha)) {
            if (NivelDoItem.maldicaoConhecida(pilha)) {
                azul = 0.3F;
                vermelho = -0.1F;
            } else {
                vermelho = 0.35F;
                azul = 0.35F;
            }
        } else if (pocaoOuPergaminhoDesconhecido(pilha)) {
            vermelho = 0.35F;
            azul = 0.35F;
        }
        return (base & 0xFF000000) | (canal(base >> 16, vermelho) << 16) | (canal(base >> 8, verde) << 8) | canal(base, azul);
    }

    private static int canal(int cor, float mais) {
        return Math.max(0, Math.min(255, (cor & 0xFF) + Math.round(mais * 255)));
    }

    private static boolean pocaoOuPergaminhoDesconhecido(ItemStack pilha) {
        if (pilha.getItem() instanceof ItemPocao pocao) {
            return ConhecimentoDoCliente.pocao(pocao.cor()) == null;
        }
        if (pilha.getItem() instanceof ItemPergaminho pergaminho) {
            return ConhecimentoDoCliente.pergaminho(pergaminho.runa()) == null;
        }
        return false;
    }

    // Painel do herói: a skin em 3D (olhando pro mouse) e as informações do lado
    private void desenharPainelDoHeroi(GuiGraphics desenho, int mouseX, int mouseY) {
        int altura = yDoStatus - topoDaBarra;
        DesenhoDoShattered.janela(desenho, 0, topoDaBarra, PainelDeStatus.LARGURA, altura);
        InventoryScreen.renderEntityInInventoryFollowsMouse(desenho, 7, topoDaBarra + 7, 59, topoDaBarra + altura - 5, 33, 0.0625F, mouseX, mouseY,
                jogador);

        int x = 64;
        int y = topoDaBarra + 9;
        ClasseDoHeroi classe = EstadoNoCliente.classe();
        if (classe != null) {
            desenho.drawString(font, Component.translatable(classe.chaveDoNome()), x, y, DesenhoDoShattered.AMARELO, true);
        }
        y += 14;
        desenho.drawString(font, Component.translatable("hud.pixeldungeon.nivel", EstadoNoCliente.nivel()), x, y, DesenhoDoShattered.BRANCO, true);
        y += 11;
        desenho.drawString(font, Component.translatable("hud.pixeldungeon.forca", EstadoNoCliente.forca()), x, y, DesenhoDoShattered.BRANCO, true);
        y += 11;
        int andar = Math.max(1, Andares.andarPelaAltura(jogador.getBlockY()));
        desenho.drawString(font, Component.translatable("hud.pixeldungeon.andar", andar), x, y, DesenhoDoShattered.BRANCO, true);
        desenho.drawString(font, Component.translatable("hud.pixeldungeon.talentos", TeclasDoMod.TALENTOS.getTranslatedKeyMessage()), x,
                topoDaBarra + altura - 17, EstadoNoCliente.pontosLivres() > 0 ? DesenhoDoShattered.AMARELO : 0xFFAAAAAA, true);
    }

    private boolean noPainelDoHeroi(double mouseX, double mouseY) {
        return comPainelDoHeroi && DesenhoDoShattered.dentro(mouseX, mouseY, 0, topoDaBarra, PainelDeStatus.LARGURA, yDoStatus - topoDaBarra);
    }

    // ---- Dicas e cliques ----

    @Nullable
    private List<Component> dicaEm(double mouseX, double mouseY) {
        BarraDeFerramentas.Botao botao = BarraDeFerramentas.botaoEm(width, topoDaBarra, mouseX, mouseY);
        if (botao != null) {
            String chave = "hud.pixeldungeon." + botao.name().toLowerCase();
            Component tecla = minecraft.options.keyInventory.getTranslatedKeyMessage();
            return List.of(Component.translatable(chave, tecla).withStyle(ChatFormatting.YELLOW),
                    Component.translatable(chave + ".descricao").withStyle(ChatFormatting.GRAY));
        }
        if (noPainelDoHeroi(mouseX, mouseY)) {
            return PainelDeStatus.dicaDoHeroi(jogador);
        }
        if (DesenhoDoShattered.dentro(mouseX, mouseY, xDaCelula(4), yDaCelula(0), CASINHA * 4, CASINHA)) {
            return List.of(Component.translatable("hud.pixeldungeon.ouro", EstadoNoCliente.ouro()).withStyle(ChatFormatting.YELLOW));
        }
        return PainelDeStatus.dica(jogador, 0, yDoStatus, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int botao) {
        if (menu.getCarried().isEmpty() && botao == 0) {
            BarraDeFerramentas.Botao clicado = BarraDeFerramentas.botaoEm(width, topoDaBarra, mouseX, mouseY);
            if (clicado != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                // A mochila fecha o inventário; esperar e procurar só mostram a dica
                if (clicado == BarraDeFerramentas.Botao.MOCHILA) {
                    onClose();
                }
                return true;
            }
            // Igual ao Shattered: clicar no herói abre a tela dele (aqui, os talentos)
            if (noPainelDoHeroi(mouseX, mouseY) || PainelDeStatus.noRosto(0, yDoStatus, mouseX, mouseY)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.setScreen(new TelaDeTalentos());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, botao);
    }

    @Override
    public boolean keyPressed(int tecla, int codigo, int modificadores) {
        if (TeclasDoMod.TALENTOS.matches(tecla, codigo)) {
            minecraft.setScreen(new TelaDeTalentos());
            return true;
        }
        return super.keyPressed(tecla, codigo, modificadores);
    }

    // Clicar fora dos painéis com um item no mouse joga o item fora (igual ao Minecraft)
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int esquerda, int topo, int botao) {
        boolean dentro = DesenhoDoShattered.dentro(mouseX, mouseY, xDoPainel, yDoPainel, LARGURA_DO_PAINEL, ALTURA_DO_PAINEL)
                || BarraDeFerramentas.dentro(width, topoDaBarra, mouseX, mouseY)
                || DesenhoDoShattered.dentro(mouseX, mouseY, 0, yDoStatus, PainelDeStatus.LARGURA, PainelDeStatus.ALTURA)
                || noPainelDoHeroi(mouseX, mouseY);
        return !dentro;
    }
}
