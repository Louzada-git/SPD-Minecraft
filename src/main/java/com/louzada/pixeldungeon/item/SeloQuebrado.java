package com.louzada.pixeldungeon.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// O selo quebrado do Guerreiro, igual ao Shattered:
//   - afixado na armadura que o Guerreiro veste, dá blindagem quando a vida cai pela metade (ver Escudos)
//   - guarda um aprimoramento: melhorar a armadura que tem o selo faz o selo guardar +1, que vai junto pra próxima armadura
//   - transferência rúnica (talento): o selo leva o glifo junto (+1: comuns e incomuns; +2: todos)
// Como mexer:
//   - soltar: com a armadura na mão, clique direito agachado (o selo vai pra mochila)
//   - afixar: clique direito no selo e escolher a armadura (ela não pode ser amaldiçoada, nem estar com a maldição desconhecida)
//   - vestir outra armadura: o chat pergunta se é pra passar o selo pra ela
//   - se o selo e a armadura tiverem glifos diferentes, o chat pergunta qual fica
// Diferente do Shattered (o mod não tem pedras de encantamento pra inscrever um glifo com o selo afixado):
// o selo aprende o glifo da armadura em que é afixado, se ele mesmo não estiver carregando um.
public class SeloQuebrado extends Item {
    // Onde fica a armadura vestida no inventário (36 = pés, 37 = pernas, 38 = peito, 39 = cabeça)
    public static final int ESPACO_DO_PEITO = 38;
    private static final String PERGUNTOU = "selo_perguntou";

    // A escolha de glifo esperando a resposta no chat: de onde vem o selo e pra que armadura vai
    private record Pendente(int origem, int alvo) {
    }

    private static Map<UUID, Pendente> pendentes = new HashMap<>();

    public SeloQuebrado(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (jogador instanceof ServerPlayer heroi && !EscolhaDeItem.pedirArmaduraProSelo(heroi)) {
            heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selo_sem_armadura").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack pilha) {
        return pilha.has(ModComponentes.SELO_GLIFO.get());
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        dicaDoSelo(pilha, linhas);
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.selo_quebrado.descricao", ChatFormatting.DARK_GRAY);
    }

    // O que o selo guarda (no selo solto ou na armadura que tem o selo)
    public static void dicaDoSelo(ItemStack pilha, List<Component> linhas) {
        if (pilha.getOrDefault(ModComponentes.SELO_MELHORIA.get(), false)) {
            linhas.add(Component.translatable("dica.pixeldungeon.selo_melhoria").withStyle(ChatFormatting.GOLD));
        }
        Glifo glifo = Glifo.peloId(pilha.get(ModComponentes.SELO_GLIFO.get()));
        if (glifo != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.selo_glifo", nomeDoGlifo(glifo)).withStyle(ChatFormatting.AQUA));
        }
    }

    private static Component nomeDoGlifo(Glifo glifo) {
        return Component.translatable(glifo.chaveDoNome(), Component.translatable("dica.pixeldungeon.glifo_palavra"));
    }

    // ---- Regras ----

    // Transferência rúnica: +1 leva os glifos comuns e incomuns; +2, todos
    public static boolean podeLevarGlifo(ServerPlayer jogador, @Nullable Glifo glifo) {
        if (glifo == null) {
            return false;
        }
        int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.TRANSFERENCIA_RUNICA);
        return pontos >= 2 || (pontos == 1 && glifo.raridade() != Glifo.Raridade.RARO);
    }

    public static boolean temSelo(ItemStack pilha) {
        return pilha.getItem() instanceof ArmaduraDoShattered && pilha.getOrDefault(ModComponentes.SELO.get(), false);
    }

    // A armadura pode receber o selo? (avisa o porquê, se não)
    public static boolean podeAfixar(ServerPlayer jogador, ItemStack armadura) {
        if (!(armadura.getItem() instanceof ArmaduraDoShattered) || temSelo(armadura)) {
            return false;
        }
        if (!NivelDoItem.maldicaoConhecida(armadura)) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selo_armadura_desconhecida").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        if (NivelDoItem.amaldicoado(armadura)) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selo_armadura_amaldicoada").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        return true;
    }

    // ---- Soltar ----

    // Tira o selo da armadura: ela perde o aprimoramento que o selo guardava, e o glifo vai junto se o talento deixar
    public static ItemStack soltar(ServerPlayer jogador, ItemStack armadura) {
        ItemStack selo = new ItemStack(ModItens.SELO_QUEBRADO.get());
        if (armadura.getOrDefault(ModComponentes.SELO_MELHORIA.get(), false)) {
            NivelDoItem.definirNivel(armadura, NivelDoItem.nivel(armadura) - 1);
            selo.set(ModComponentes.SELO_MELHORIA.get(), true);
        }
        Glifo glifo = Glifo.peloId(armadura.get(ModComponentes.SELO_GLIFO.get()));
        armadura.remove(ModComponentes.SELO.get());
        armadura.remove(ModComponentes.SELO_MELHORIA.get());
        armadura.remove(ModComponentes.SELO_GLIFO.get());
        if (podeLevarGlifo(jogador, glifo) && NivelDoItem.glifo(armadura) == glifo) {
            NivelDoItem.definirGlifo(armadura, null);
            selo.set(ModComponentes.SELO_GLIFO.get(), glifo.id());
        }
        return selo;
    }

    // Agachado, clique direito com a armadura na mão: o selo vai pra mochila
    public static void soltarDaMao(ServerPlayer jogador, ItemStack armadura) {
        ItemStack selo = soltar(jogador, armadura);
        if (!jogador.getInventory().add(selo)) {
            jogador.drop(selo, false);
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selo_solto").withStyle(ChatFormatting.GOLD), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // ---- Afixar ----

    private static void afixar(ServerPlayer jogador, ItemStack armadura, boolean melhoria, @Nullable Glifo glifoDoSelo, boolean ficaODoSelo) {
        armadura.set(ModComponentes.SELO.get(), true);
        if (melhoria) {
            NivelDoItem.definirNivel(armadura, NivelDoItem.nivel(armadura) + 1);
            armadura.set(ModComponentes.SELO_MELHORIA.get(), true);
        }
        if (glifoDoSelo != null && ficaODoSelo) {
            NivelDoItem.definirGlifo(armadura, glifoDoSelo);
        } else {
            // O selo aprende o glifo que a armadura já tinha (ver o começo do arquivo)
            NivelDoItem.definirGlifo(armadura, NivelDoItem.glifo(armadura));
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.selo_afixado").withStyle(ChatFormatting.GOLD), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // Leva o selo de "origem" (um selo solto ou uma armadura com o selo) pra armadura em "alvo" (espaços do inventário).
    // Se os dois tiverem glifos diferentes, pergunta no chat qual fica (e espera a resposta).
    public static void levar(ServerPlayer jogador, int origem, int alvo) {
        Inventory inventario = jogador.getInventory();
        ItemStack fonte = inventario.getItem(origem);
        ItemStack armadura = inventario.getItem(alvo);
        if (!podeAfixar(jogador, armadura)) {
            return;
        }
        Glifo doSelo = glifoQueVem(jogador, fonte);
        Glifo daArmadura = NivelDoItem.glifo(armadura);
        if (doSelo != null && daArmadura != null && doSelo != daArmadura) {
            pendentes.put(jogador.getUUID(), new Pendente(origem, alvo));
            perguntarGlifo(jogador, daArmadura, doSelo);
            return;
        }
        concluir(jogador, origem, alvo, true);
    }

    // O glifo que vem junto com o selo (o do selo solto, ou o da armadura de origem se o talento deixar levar)
    @Nullable
    private static Glifo glifoQueVem(ServerPlayer jogador, ItemStack fonte) {
        Glifo glifo = Glifo.peloId(fonte.get(ModComponentes.SELO_GLIFO.get()));
        if (fonte.getItem() instanceof SeloQuebrado) {
            return glifo;
        }
        return podeLevarGlifo(jogador, glifo) && NivelDoItem.glifo(fonte) == glifo ? glifo : null;
    }

    private static void concluir(ServerPlayer jogador, int origem, int alvo, boolean ficaODoSelo) {
        Inventory inventario = jogador.getInventory();
        ItemStack fonte = inventario.getItem(origem);
        ItemStack armadura = inventario.getItem(alvo);
        ItemStack selo;
        if (fonte.getItem() instanceof SeloQuebrado) {
            selo = fonte.copy();
            fonte.shrink(1);
        } else if (temSelo(fonte)) {
            selo = soltar(jogador, fonte);
        } else {
            return;
        }
        afixar(jogador, armadura, selo.getOrDefault(ModComponentes.SELO_MELHORIA.get(), false),
                Glifo.peloId(selo.get(ModComponentes.SELO_GLIFO.get())), ficaODoSelo);
    }

    // Resposta do chat (/selo glifo armadura|selo)
    public static void escolheuGlifo(ServerPlayer jogador, boolean ficaODoSelo) {
        Pendente pendente = pendentes.remove(jogador.getUUID());
        if (pendente == null) {
            return;
        }
        Inventory inventario = jogador.getInventory();
        ItemStack fonte = inventario.getItem(pendente.origem());
        if (!podeAfixar(jogador, inventario.getItem(pendente.alvo())) || !(fonte.getItem() instanceof SeloQuebrado || temSelo(fonte))) {
            return;
        }
        concluir(jogador, pendente.origem(), pendente.alvo(), ficaODoSelo);
    }

    private static void perguntarGlifo(ServerPlayer jogador, Glifo daArmadura, Glifo doSelo) {
        MutableComponent pergunta = Component.translatable("mensagem.pixeldungeon.selo_qual_glifo").withStyle(ChatFormatting.GOLD);
        pergunta.append(" ").append(botao(nomeDoGlifo(daArmadura), "/selo glifo armadura"));
        pergunta.append(" ").append(botao(nomeDoGlifo(doSelo), "/selo glifo selo"));
        jogador.displayClientMessage(pergunta, false);
    }

    private static Component botao(Component texto, String comando) {
        return Component.literal("[").append(texto).append("]").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, comando))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("mensagem.pixeldungeon.selo_clique"))));
    }

    // ---- Trocar de armadura ----

    // O Guerreiro vestiu outra armadura e o selo ficou na velha: pergunta no chat (uma vez) se é pra passar o selo
    public static void talvezPerguntar(ServerPlayer jogador, DadosDoHeroi dados) {
        ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
        int origem = armaduraComSeloNaMochila(jogador);
        boolean podePerguntar = peito.getItem() instanceof ArmaduraDoShattered && !temSelo(peito) && origem >= 0
                && NivelDoItem.maldicaoConhecida(peito) && !NivelDoItem.amaldicoado(peito);
        if (!podePerguntar) {
            dados.definir(PERGUNTOU, false);
            return;
        }
        if (dados.sim(PERGUNTOU)) {
            return;
        }
        dados.definir(PERGUNTOU, true);
        MutableComponent pergunta = Component.translatable("mensagem.pixeldungeon.selo_passar").withStyle(ChatFormatting.GOLD);
        pergunta.append(" ").append(botao(Component.translatable("mensagem.pixeldungeon.selo_sim"), "/selo transferir"));
        jogador.displayClientMessage(pergunta, false);
    }

    // Resposta do chat (/selo transferir): passa o selo da armadura da mochila pra armadura vestida
    public static void transferir(ServerPlayer jogador) {
        int origem = armaduraComSeloNaMochila(jogador);
        if (origem >= 0) {
            levar(jogador, origem, ESPACO_DO_PEITO);
        }
    }

    private static int armaduraComSeloNaMochila(ServerPlayer jogador) {
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            if (i != ESPACO_DO_PEITO && temSelo(inventario.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
