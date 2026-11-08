package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

// Nível, identificação e maldição das armas e armaduras (os Equipamentos), igual ao Shattered:
//   - o pergaminho de melhoria dá +1 de nível (mais dano ou mais defesa, e precisa de menos força)
//   - o que é achado na dungeon vem sem identificar: não dá pra saber o nível nem se tem maldição
//   - se identifica sozinho com o uso: a arma depois de 20 golpes, a armadura depois de 20 golpes recebidos,
//     as armas de arremesso depois de 10 acertos e as varinhas depois de 10 disparos
//     (as intuições do Guerreiro, da Duelista, da Caçadora e do Mago aceleram)
//   - segurar a arma (ou vestir a armadura) revela se ela é amaldiçoada; a armadura amaldiçoada gruda no corpo
//   - 30% do que é achado vem com uma maldição (de arma ou de armadura); 15% das armaduras vêm com um glifo (ver Glifo)
public class NivelDoItem {
    public static final float USOS_PARA_IDENTIFICAR = 20;

    // ---- O que o item é ----

    public static boolean podeIdentificar(ItemStack pilha) {
        return pilha.getItem() instanceof Equipamento;
    }

    // O pergaminho de melhoria funciona em armas (o cajado também) e armaduras
    public static boolean podeMelhorar(ItemStack pilha) {
        return pilha.getItem() instanceof Equipamento;
    }

    // Quantos usos o item leva pra se identificar sozinho (as armas de arremesso e as varinhas, metade)
    public static float usosParaIdentificar(ItemStack pilha) {
        if (pilha.getItem() instanceof ArmaDeArremesso) {
            return ArmaDeArremesso.USOS_PARA_IDENTIFICAR;
        }
        if (pilha.getItem() instanceof ItemVarinha) {
            return ItemVarinha.USOS_PARA_IDENTIFICAR;
        }
        return USOS_PARA_IDENTIFICAR;
    }

    public static boolean ehArmadura(ItemStack pilha) {
        return pilha.getItem() instanceof Equipamento equipamento && equipamento.alvoDasMaldicoes() == Maldicao.Alvo.ARMADURA;
    }

    public static int nivel(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.NIVEL.get(), 0);
    }

    public static void definirNivel(ItemStack pilha, int nivel) {
        if (nivel <= 0) {
            pilha.remove(ModComponentes.NIVEL.get());
        } else {
            pilha.set(ModComponentes.NIVEL.get(), nivel);
        }
    }

    // Sem o dado, o item é conhecido (o que as classes trazem de casa)
    public static boolean identificado(ItemStack pilha) {
        return !Boolean.FALSE.equals(pilha.get(ModComponentes.IDENTIFICADO.get()));
    }

    public static boolean maldicaoConhecida(ItemStack pilha) {
        return identificado(pilha) || !Boolean.FALSE.equals(pilha.get(ModComponentes.MALDICAO_CONHECIDA.get()));
    }

    @Nullable
    public static Maldicao maldicao(ItemStack pilha) {
        return Maldicao.peloId(pilha.get(ModComponentes.MALDICAO.get()));
    }

    public static boolean amaldicoado(ItemStack pilha) {
        return maldicao(pilha) != null;
    }

    // O glifo da armadura, se tiver
    @Nullable
    public static Glifo glifo(ItemStack pilha) {
        return ehArmadura(pilha) ? Glifo.peloId(pilha.get(ModComponentes.ENCANTO.get())) : null;
    }

    // Inscreve (ou tira, com null) o glifo. Igual ao Shattered, o selo do Guerreiro afixado na armadura guarda o glifo também.
    public static void definirGlifo(ItemStack pilha, @Nullable Glifo glifo) {
        if (glifo == null) {
            pilha.remove(ModComponentes.ENCANTO.get());
        } else {
            pilha.set(ModComponentes.ENCANTO.get(), glifo.id());
        }
        if (pilha.getOrDefault(ModComponentes.SELO.get(), false)) {
            if (glifo == null) {
                pilha.remove(ModComponentes.SELO_GLIFO.get());
            } else {
                pilha.set(ModComponentes.SELO_GLIFO.get(), glifo.id());
            }
        }
    }

    // Força que a arma ou armadura pede no nível dela: cada melhoria vai tirando um pouco, igual ao Shattered
    public static int forcaNecessaria(int tier, int nivel) {
        return 8 + 2 * tier - (int) (Math.sqrt(8 * Math.max(0, nivel) + 1) - 1) / 2;
    }

    // ---- Achados na dungeon ----

    // Igual ao Shattered: +0 (75%), +1 (20%), +2 (5%); 30% amaldiçoados; 15% das armaduras com um glifo. E vem sem identificar.
    public static ItemStack sortearDoChao(ItemStack pilha, Random sorteio, boolean sortearNivel) {
        if (!(pilha.getItem() instanceof Equipamento equipamento)) {
            return pilha;
        }
        int nivel = 0;
        if (sortearNivel && sorteio.nextInt(4) == 0) {
            nivel++;
            if (sorteio.nextInt(5) == 0) {
                nivel++;
            }
        }
        definirNivel(pilha, nivel);
        float efeito = sorteio.nextFloat();
        if (efeito < 0.3F) {
            List<Maldicao> possiveis = Maldicao.doAlvo(equipamento.alvoDasMaldicoes());
            pilha.set(ModComponentes.MALDICAO.get(), possiveis.get(sorteio.nextInt(possiveis.size())).id());
        } else if (efeito >= 0.85F && ehArmadura(pilha)) {
            definirGlifo(pilha, Glifo.sortear(sorteio));
        }
        pilha.set(ModComponentes.IDENTIFICADO.get(), false);
        pilha.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), usosParaIdentificar(pilha));
        return pilha;
    }

    // ---- Descobrir ----

    public static void identificar(@Nullable ServerPlayer jogador, ItemStack pilha) {
        if (identificado(pilha)) {
            return;
        }
        pilha.remove(ModComponentes.IDENTIFICADO.get());
        pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        pilha.remove(ModComponentes.USOS_PARA_IDENTIFICAR.get());
        if (jogador != null) {
            String chave = ehArmadura(pilha) ? "mensagem.pixeldungeon.armadura_identificada"
                    : pilha.getItem() instanceof ItemVarinha ? "mensagem.pixeldungeon.varinha_identificada"
                    : pilha.getItem() instanceof ItemAnel ? "mensagem.pixeldungeon.anel_identificado" : "mensagem.pixeldungeon.arma_identificada";
            jogador.displayClientMessage(Component.translatable(chave, pilha.getHoverName()).withStyle(ChatFormatting.AQUA), false);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
            if (gruda(pilha)) {
                prenderSeAmaldicoada(jogador, pilha);
            }
        }
    }

    // A armadura e os anéis amaldiçoados grudam no corpo
    private static boolean gruda(ItemStack pilha) {
        return ehArmadura(pilha) || pilha.getItem() instanceof ItemAnel;
    }

    // Segurou a arma ou vestiu a armadura: descobre se é amaldiçoada.
    // As intuições +2 identificam na hora: a de aventureiro (Duelista) as armas, a de veterano (Guerreiro) as armaduras.
    // (As armas de arremesso e as varinhas não são equipadas: a maldição delas aparece quando acertam alguém ou são lançadas.)
    // Os anéis também: pôr revela a maldição (e o anel amaldiçoado gruda no dedo).
    public static void aoEquipar(ServerPlayer jogador, ItemStack pilha) {
        if (!(pilha.getItem() instanceof ArmaDoShattered) && !gruda(pilha)) {
            return;
        }
        if (!identificado(pilha) && !(pilha.getItem() instanceof ItemAnel)) {
            Talento intuicao = ehArmadura(pilha) ? Talento.INTUICAO_DE_VETERANO : Talento.INTUICAO_DE_AVENTUREIRO;
            if (RegrasDoHeroi.dados(jogador).pontos(intuicao) >= 2) {
                identificar(jogador, pilha);
                return;
            }
        }
        if (!maldicaoConhecida(pilha)) {
            pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
            if (amaldicoado(pilha)) {
                String chave = ehArmadura(pilha) ? "mensagem.pixeldungeon.armadura_amaldicoada"
                        : pilha.getItem() instanceof ItemAnel ? "mensagem.pixeldungeon.anel_amaldicoado" : "mensagem.pixeldungeon.arma_amaldicoada";
                jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.DARK_PURPLE), false);
                jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
        }
        if (gruda(pilha)) {
            prenderSeAmaldicoada(jogador, pilha);
        }
    }

    // Cada golpe que a arma acerta (ou que acerta quem veste a armadura) deixa o herói mais acostumado com ela
    public static void aoUsar(ServerPlayer jogador, ItemStack pilha) {
        if (!podeIdentificar(pilha) || identificado(pilha)) {
            return;
        }
        float faltando = pilha.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), usosParaIdentificar(pilha));
        faltando -= fatorDosTalentos(RegrasDoHeroi.dados(jogador), pilha);
        if (faltando <= 0) {
            identificar(jogador, pilha);
        } else {
            pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), faltando);
        }
    }

    // Intuição de veterano (Guerreiro): armas 1,75x / 2,5x, armaduras 2,5x / na hora.
    // Intuição de aventureiro (Duelista): armas 2,5x / na hora, armaduras 1,75x / 2,5x.
    // Intuição de sobrevivente (Caçadora): armas de arremesso 3x / ao acertar (ver ArmaArremessada).
    // Intuição do estudioso (Mago): varinhas 3x / ao lançar (ver ItemVarinha).
    public static float fatorDosTalentos(DadosDoHeroi dados, ItemStack pilha) {
        if (pilha.getItem() instanceof ArmaDeArremesso) {
            return 1 + 2.0F * dados.pontos(Talento.INTUICAO_DE_SOBREVIVENTE);
        }
        if (pilha.getItem() instanceof ItemVarinha) {
            return 1 + 2.0F * dados.pontos(Talento.INTUICAO_DO_ESTUDIOSO);
        }
        int aventureiro = dados.pontos(Talento.INTUICAO_DE_AVENTUREIRO);
        int veterano = dados.pontos(Talento.INTUICAO_DE_VETERANO);
        if (ehArmadura(pilha)) {
            return (1 + 0.75F * aventureiro) * (1 + 1.5F * veterano);
        }
        return (1 + 1.5F * aventureiro) * (1 + 0.75F * veterano);
    }

    // ---- Armadura amaldiçoada: não dá pra tirar ----

    // Igual ao Shattered, a armadura e o anel amaldiçoados (que já se sabe que são) grudam no corpo.
    // Usa a "maldição do ligamento" do Minecraft, escondida (sem brilho nem nome na dica).
    private static void prenderSeAmaldicoada(ServerPlayer jogador, ItemStack pilha) {
        // (todo item já vem com a lista de encantamentos vazia: o que importa é se já está grudada)
        if (!amaldicoado(pilha) || !maldicaoConhecida(pilha) || grudada(pilha)) {
            return;
        }
        Holder<Enchantment> ligamento = jogador.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE);
        ItemEnchantments.Mutable encantamentos = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        encantamentos.set(ligamento, 1);
        pilha.set(DataComponents.ENCHANTMENTS, encantamentos.toImmutable().withTooltip(false));
        pilha.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        String chave = pilha.getItem() instanceof ItemAnel ? "mensagem.pixeldungeon.anel_grudou" : "mensagem.pixeldungeon.armadura_grudou";
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    public static boolean grudada(ItemStack pilha) {
        return EnchantmentHelper.has(pilha, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    // Volta a lista de encantamentos pra vazia, igual à de qualquer item
    private static void soltar(ItemStack pilha) {
        pilha.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        pilha.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    // ---- Melhorar e tirar maldição ----

    // +1 de nível. Igual ao Shattered, melhorar um item amaldiçoado tem 1 chance em 3 de tirar a maldição.
    // Nas armas de arremesso, o conjunto também volta a ficar inteiro; a varinha ganha uma carga.
    // O glifo pode se perder a partir do +4: 10% no +4, 20% no +5, 40% no +6, 80% no +7, sempre no +8.
    // O selo do Guerreiro afixado na armadura guarda um aprimoramento (pra levar pra próxima armadura).
    public static void melhorar(@Nullable ServerPlayer jogador, ItemStack pilha, RandomSource sorteio) {
        int antes = nivel(pilha);
        if (glifo(pilha) != null && antes >= 4 && sorteio.nextFloat() * 10 < Math.pow(2, antes - 4)) {
            definirGlifo(pilha, null);
            if (jogador != null) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.glifo_sumiu").withStyle(ChatFormatting.GRAY), false);
            }
        }
        if (pilha.getOrDefault(ModComponentes.SELO.get(), false)) {
            pilha.set(ModComponentes.SELO_MELHORIA.get(), true);
        }
        definirNivel(pilha, antes + 1);
        ArmaDeArremesso.definirDurabilidade(pilha, ArmaDeArremesso.DURABILIDADE_MAXIMA);
        // Igual ao Shattered, a varinha melhorada ganha uma carga
        if (pilha.getItem() instanceof ItemVarinha) {
            Cargas.somar(pilha, ItemVarinha.cargasMaximas(pilha), 1);
        }
        if (amaldicoado(pilha) && sorteio.nextInt(3) == 0) {
            pilha.remove(ModComponentes.MALDICAO.get());
            soltar(pilha);
            if (jogador != null && maldicaoConhecida(pilha)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.maldicao_sumiu").withStyle(ChatFormatting.AQUA), false);
            }
        }
    }

    public static boolean removerMaldicao(ItemStack pilha) {
        boolean tinha = amaldicoado(pilha);
        pilha.remove(ModComponentes.MALDICAO.get());
        pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        soltar(pilha);
        return tinha;
    }

    // ---- Nome e dica ----

    // "Espada +1", "Espada irritante +2", "Armadura de malha de fedor", "Armadura de couro de pedra"...
    // (o que ainda não se sabe não aparece; igual ao Shattered, o glifo aparece mesmo sem identificar)
    public static Component nome(ItemStack pilha, Component nomeDoItem) {
        MutableComponent nome = nomeDoItem.copy();
        Maldicao maldicao = maldicao(pilha);
        Glifo glifo = glifo(pilha);
        if (maldicao != null && maldicaoConhecida(pilha)) {
            nome = Component.translatable(maldicao.chaveDoNome(), nomeDoItem);
        } else if (glifo != null) {
            nome = Component.translatable(glifo.chaveDoNome(), nomeDoItem);
        }
        int nivel = nivel(pilha);
        if (identificado(pilha) && nivel > 0) {
            nome = nome.append(" +" + nivel);
        }
        return nome;
    }

    // As linhas da dica sobre identificação, glifo e maldição (iguais pra armas e armaduras)
    public static void dicaDeIdentificacao(ItemStack pilha, List<Component> linhas) {
        boolean conhecido = identificado(pilha);
        boolean armadura = ehArmadura(pilha);
        Glifo glifo = glifo(pilha);
        if (glifo != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.glifo", Component.translatable(glifo.chaveDoNome(), Component.translatable("dica.pixeldungeon.glifo_palavra")))
                    .withStyle(ChatFormatting.AQUA));
            Textos.adicionarQuebrado(linhas, glifo.chaveDaDescricao(), ChatFormatting.DARK_AQUA);
        }
        if (!conhecido) {
            linhas.add(Component.translatable(armadura ? "dica.pixeldungeon.armadura_nao_identificada" : "dica.pixeldungeon.nao_identificada")
                    .withStyle(ChatFormatting.YELLOW));
        }
        Maldicao maldicao = maldicao(pilha);
        if (maldicaoConhecida(pilha)) {
            if (maldicao != null) {
                linhas.add(Component.translatable(armadura ? "dica.pixeldungeon.armadura_amaldicoada" : "dica.pixeldungeon.amaldicoada")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                Textos.adicionarQuebrado(linhas, maldicao.chaveDaDescricao(), ChatFormatting.DARK_PURPLE);
            } else if (!conhecido) {
                linhas.add(Component.translatable(armadura ? "dica.pixeldungeon.armadura_sem_maldicao" : "dica.pixeldungeon.sem_maldicao")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
