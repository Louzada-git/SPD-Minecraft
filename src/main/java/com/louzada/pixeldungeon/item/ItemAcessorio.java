package com.louzada.pixeldungeon.item;

import java.text.DecimalFormat;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Um acessório (ver TipoDeAcessorio e Acessorios): vale enquanto estiver no inventário. O nível (0 a 3) sobe no pote
// de alquimia. O fragmento do esquecimento ainda identifica, no clique direito, os equipamentos que estão prontos.
public class ItemAcessorio extends Item {
    private final TipoDeAcessorio tipo;

    public ItemAcessorio(Properties propriedades, TipoDeAcessorio tipo) {
        super(propriedades.stacksTo(1));
        this.tipo = tipo;
    }

    public TipoDeAcessorio tipo() {
        return tipo;
    }

    @Override
    public Component getName(ItemStack pilha) {
        int nivel = NivelDoItem.nivel(pilha);
        Component nome = super.getName(pilha);
        return nivel > 0 ? Component.empty().append(nome).append(" +" + nivel) : nome;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (tipo != TipoDeAcessorio.FRAGMENTO_DO_ESQUECIMENTO) {
            return InteractionResultHolder.pass(pilha);
        }
        if (jogador instanceof ServerPlayer heroi) {
            boolean abriu = EscolhaDeItem.pedirProArtefato(heroi, tipo.nomeDoItem(), NivelDoItem::prontoPraIdentificar,
                    (quem, espaco) -> identificar(quem, quem.getInventory().getItem(espaco)));
            if (!abriu) {
                heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.fragmento.nada").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // O fragmento do esquecimento identifica um equipamento que já está pronto
    public static boolean identificar(ServerPlayer jogador, ItemStack pilha) {
        if (!NivelDoItem.prontoPraIdentificar(pilha)) {
            return false;
        }
        Identificacao.identificarItem(jogador, pilha);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.fragmento.identificou").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag bandeira) {
        String chave = "item.pixeldungeon." + tipo.nomeDoItem();
        Textos.adicionarQuebrado(linhas, chave + ".descricao", ChatFormatting.GRAY);
        int nivel = Math.min(TipoDeAcessorio.NIVEL_MAXIMO, NivelDoItem.nivel(pilha));
        String sufixo = tipo == TipoDeAcessorio.LUNETA_TRINCADA && nivel >= 2 ? ".estatisticas_aprimorada" : ".estatisticas";
        Textos.adicionarQuebrado(linhas, chave + sufixo, ChatFormatting.YELLOW, numeros(tipo, nivel));
        if (nivel < TipoDeAcessorio.NIVEL_MAXIMO) {
            linhas.add(Component.translatable("dica.pixeldungeon.acessorio", tipo.custoParaAprimorar(nivel)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (tipo == TipoDeAcessorio.FRAGMENTO_DO_ESQUECIMENTO) {
            linhas.add(Component.translatable("dica.pixeldungeon.fragmento").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // Os números do texto de cada acessório nesse nível (iguais aos do Shattered)
    public static Object[] numeros(TipoDeAcessorio tipo, int nivel) {
        return switch (tipo) {
            case CRANIO_DE_RATO -> new Object[] { inteiro(Acessorios.multiplicadorDeRaros(nivel)) };
            case PEDACO_DE_PAPIRO -> new Object[] { inteiro(Acessorios.multiplicadorDeEncantos(nivel)), decimal(Acessorios.multiplicadorDeMaldicoes(nivel)) };
            case SEMENTE_PETRIFICADA -> new Object[] { decimal(100 * Acessorios.chanceDePedraNaGrama(nivel)),
                    decimal(100 * (Acessorios.multiplicadorDaGrama(nivel) - 1)) };
            case CRISTAIS_EXOTICOS -> new Object[] { decimal(100 * Acessorios.chanceDeExotico(nivel)) };
            case MOITA_DE_MUSGO -> new Object[] { inteiro(100 * Acessorios.chanceDeTema(nivel)) };
            case RELOGIO_DE_SOL -> new Object[] { inteiro(100 * (1 - Acessorios.renascerDeDia(nivel))), inteiro(100 * (Acessorios.renascerDeNoite(nivel) - 1)) };
            case TREVO_DE_TREZE_FOLHAS -> new Object[] { inteiro(100 * Acessorios.chanceDoTrevo(nivel) * 0.6F),
                    inteiro(100 * Acessorios.chanceDoTrevo(nivel) * 0.4F) };
            case MECANISMO_DE_ARMADILHA -> new Object[] { inteiro(100 * Acessorios.chanceDeTema(nivel)), inteiro(100 * Acessorios.chanceDeArmadilhaAVista(nivel)) };
            case DENTE_DE_MIMICO -> new Object[] { decimal(Acessorios.multiplicadorDeMimicos(nivel)) };
            case RESINA_MARAVILHOSA -> new Object[] { decimal(100 * Acessorios.chanceDeDisparoBom(nivel)), decimal(100 * Acessorios.chanceDeDisparoExtra(nivel)) };
            case OLHO_DE_SALAMANDRA -> new Object[] { decimal(100 * (1 - Acessorios.multiplicadorDaVisao(nivel))),
                    String.valueOf(Acessorios.alcanceDaVisaoMental(nivel)) };
            case CUBO_DE_SAL -> new Object[] { decimal(100 * (1 / Acessorios.multiplicadorDaFome(nivel) - 1)),
                    decimal(100 * (1 - Acessorios.multiplicadorDaRegeneracao(nivel))) };
            case FRASCO_DE_SANGUE -> new Object[] { decimal(100 * (Acessorios.multiplicadorDaCura(nivel) - 1)),
                    (4 - nivel) + " + " + new int[] { 15, 10, 7, 5 }[nivel] + "%" };
            case FRAGMENTO_DO_ESQUECIMENTO -> new Object[] { String.valueOf(Acessorios.maximoDoFragmento(nivel)) };
            case INCENSARIO_CAOTICO -> new Object[] { String.valueOf(Acessorios.turnosDoIncensario(nivel)) };
            case TUFO_DE_FURAO -> new Object[] { decimal(100 * (Acessorios.multiplicadorDeEsquiva(nivel) - 1)) };
            case LUNETA_TRINCADA -> new Object[] { decimal(100 * (nivel >= 2 ? Acessorios.chanceDaLuneta(nivel) - 1 : Acessorios.chanceDaLuneta(nivel))) };
        };
    }

    private static String inteiro(float numero) {
        return String.valueOf(Math.round(numero));
    }

    private static String decimal(float numero) {
        return new DecimalFormat("#.##").format(numero);
    }
}
