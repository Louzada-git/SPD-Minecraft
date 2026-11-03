package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

// Arma corpo a corpo com as regras do Shattered:
//   - dano sorteado entre o mínimo e o máximo a cada golpe
//   - força necessária: quem tem menos força erra mais; quem tem força sobrando bate mais forte
//   - precisão e velocidade próprias de cada arma
//   - cada nível (pergaminho de melhoria) dá +1 no dano mínimo e +(tier + 1) no máximo
//     (as armas rápidas, luvas e sai, ganham metade), e pede um pouco menos de força
//   - armas achadas na dungeon vêm sem identificar (ver NivelDoItem)
// O dano de verdade é aplicado no EventosDeCombate.
public class ArmaDoShattered extends Item implements ArmaComDano {
    private int tier;
    private int danoMinimo;
    private int danoMaximo;
    // Multiplicador de precisão (1.0 = normal, 1.4 = 40% a mais)
    private float precisao;
    // Tempo entre ataques, em turnos (1 turno = 2 segundos). 0.5 = ataca 2x mais rápido.
    private float atraso;
    // Defesa extra que a arma dá (o florete dá 1, a katana 3)
    private int defesa;
    // Ataque surpresa forte: o dano começa mais perto do máximo (adaga 75%, punhal 67%, lâmina assassina 50%; 0 = normal)
    private float surpresa;
    // Habilidade que a Duelista usa com esta arma (ou null) e o número dela
    // (dano a mais, ou alcance em quadradinhos no caso do furtivo)
    @Nullable
    private HabilidadesDaDuelista.Habilidade habilidade;
    private int valorDaHabilidade;
    // Chave da descrição no arquivo de idioma
    private String chaveDaDescricao;

    public ArmaDoShattered(Properties propriedades, String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso,
            int defesa, float surpresa, @Nullable HabilidadesDaDuelista.Habilidade habilidade, int valorDaHabilidade) {
        super(propriedades.stacksTo(1).attributes(atributos(atraso)));
        this.tier = tier;
        this.danoMinimo = danoMinimo;
        this.danoMaximo = danoMaximo;
        this.precisao = precisao;
        this.atraso = atraso;
        this.defesa = defesa;
        this.surpresa = surpresa;
        this.habilidade = habilidade;
        this.valorDaHabilidade = valorDaHabilidade;
        this.chaveDaDescricao = "item.pixeldungeon." + nome + ".descricao";
    }

    // Velocidade de ataque do Minecraft: o jogador tem 4 de base, então somamos (ataques por segundo - 4)
    private static ItemAttributeModifiers atributos(float atraso) {
        double ataquesPorSegundo = 20.0 / (atraso * Turno.TICKS);
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ataquesPorSegundo - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public int tier() {
        return tier;
    }

    @Nullable
    public HabilidadesDaDuelista.Habilidade habilidade() {
        return habilidade;
    }

    // ---- Números no nível de cada arma ----

    public int danoMinimo(int nivel) {
        return danoMinimo + nivel;
    }

    // As armas rápidas (luvas, sai) crescem metade por nível, igual ao Shattered
    public int danoMaximo(int nivel) {
        int porNivel = atraso < 1.0f ? Math.round(0.5f * (tier + 1)) : tier + 1;
        return danoMaximo + nivel * porNivel;
    }

    public int forcaNecessaria(int nivel) {
        return NivelDoItem.forcaNecessaria(tier, nivel);
    }

    public int forcaNecessaria(ItemStack pilha) {
        return forcaNecessaria(NivelDoItem.nivel(pilha));
    }

    // O número da habilidade cresce com o nível, igual ao Shattered
    // (cortar e combinação: +1 por nível; golpe pesado e estocada: +1,5 por nível; o furtivo não muda o alcance)
    public int valorDaHabilidade(int nivel) {
        if (habilidade == null) {
            return 0;
        }
        return switch (habilidade) {
            case CORTAR, COMBINACAO -> valorDaHabilidade + nivel;
            case GOLPE_PESADO, ESTOCADA -> valorDaHabilidade + Math.round(1.5f * nivel);
            case FURTIVO -> valorDaHabilidade;
        };
    }

    public int valorDaHabilidade(ItemStack pilha) {
        return valorDaHabilidade(NivelDoItem.nivel(pilha));
    }

    // ---- Combate ----

    // Dano de um golpe (mais provável no meio do intervalo, igual ao Shattered).
    // Cada ponto de força acima do necessário pode somar até +1 de dano.
    // No ataque surpresa, a adaga bate entre 75% e 100% do dano máximo (o punhal 67%, a lâmina assassina 50%).
    public int sortearDano(ItemStack pilha, RandomSource sorteio, int forcaDoHeroi, boolean deSurpresa) {
        int nivel = NivelDoItem.nivel(pilha);
        int minimo = danoMinimo(nivel);
        int maximo = danoMaximo(nivel);
        if (deSurpresa && surpresa > 0) {
            minimo = minimo + Math.round((maximo - minimo) * surpresa);
        }
        int dano = Sorteio.normal(sorteio, minimo, maximo);
        int forcaSobrando = forcaDoHeroi - forcaNecessaria(nivel);
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    @Override
    public int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean deSurpresa) {
        return sortearDano(pilha, sorteio, heroi.forca(), deSurpresa);
    }

    @Override
    public float precisaoCom(ItemStack pilha, DadosDoHeroi heroi) {
        return precisaoCom(pilha, heroi.forca());
    }

    // Multiplicador de precisão. Cada ponto de força que falta divide a precisão por 1,5.
    public float precisaoCom(ItemStack pilha, int forcaDoHeroi) {
        int forcaFaltando = forcaNecessaria(pilha) - forcaDoHeroi;
        if (forcaFaltando > 0) {
            return (float) (precisao / Math.pow(1.5, forcaFaltando));
        }
        return precisao;
    }

    // Defesa que a arma dá a quem segura (sorteada de 0 até a defesa dela)
    public int sortearDefesa(ItemStack pilha, RandomSource sorteio, int forcaDoHeroi) {
        int valor = Sorteio.normal(sorteio, 0, defesa);
        int forcaFaltando = forcaNecessaria(pilha) - forcaDoHeroi;
        if (forcaFaltando > 0) {
            valor -= 2 * forcaFaltando;
        }
        return Math.max(0, valor);
    }

    // ---- Aparência ----

    // "Espada +1", "Espada irritante"... só o que já foi descoberto
    @Override
    public Component getName(ItemStack pilha) {
        return NivelDoItem.nome(pilha, super.getName(pilha));
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        boolean conhecida = NivelDoItem.identificado(pilha);
        // Sem identificar, mostra os números de uma arma +0 (os "típicos", igual ao Shattered)
        int nivel = conhecida ? NivelDoItem.nivel(pilha) : 0;
        linhas.add(Component.translatable("dica.pixeldungeon.tier", tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.dano" : "dica.pixeldungeon.dano_tipico", danoMinimo(nivel), danoMaximo(nivel))
                .withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.forca" : "dica.pixeldungeon.forca_tipica", forcaNecessaria(nivel))
                .withStyle(ChatFormatting.GRAY));
        if (precisao > 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.precisao", Math.round((precisao - 1.0f) * 100)).withStyle(ChatFormatting.GRAY));
        }
        if (atraso < 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.rapida", Math.round(1.0f / atraso)).withStyle(ChatFormatting.GRAY));
        }
        if (defesa > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.defesa_extra", defesa).withStyle(ChatFormatting.GRAY));
        }
        if (surpresa > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.surpresa_forte", Math.round(surpresa * 100)).withStyle(ChatFormatting.GRAY));
        }
        // Identificação e maldição
        if (!conhecida) {
            linhas.add(Component.translatable("dica.pixeldungeon.nao_identificada").withStyle(ChatFormatting.YELLOW));
        }
        Maldicao maldicao = NivelDoItem.maldicao(pilha);
        if (NivelDoItem.maldicaoConhecida(pilha)) {
            if (maldicao != null) {
                linhas.add(Component.translatable("dica.pixeldungeon.amaldicoada").withStyle(ChatFormatting.DARK_PURPLE));
                Textos.adicionarQuebrado(linhas, maldicao.chaveDaDescricao(), ChatFormatting.DARK_PURPLE);
            } else if (!conhecida) {
                linhas.add(Component.translatable("dica.pixeldungeon.sem_maldicao").withStyle(ChatFormatting.GRAY));
            }
        }
        // Habilidade que a Duelista pode usar com esta arma (com o número desta arma)
        if (habilidade != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.habilidade", Component.translatable(habilidade.chaveDoNome()))
                    .withStyle(ChatFormatting.AQUA));
            Textos.adicionarQuebrado(linhas, habilidade.chaveDaDescricao(), ChatFormatting.DARK_AQUA, valorDaHabilidade(nivel));
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
