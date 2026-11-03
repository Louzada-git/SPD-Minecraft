package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

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
// O dano de verdade é aplicado no EventosDeCombate.
public class ArmaDoShattered extends Item implements ArmaComDano {
    private int tier;
    private int danoMinimo;
    private int danoMaximo;
    // Multiplicador de precisão (1.0 = normal, 1.4 = 40% a mais)
    private float precisao;
    // Tempo entre ataques, em turnos (1 turno = 1 segundo). 0.5 = ataca 2x mais rápido.
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
        double ataquesPorSegundo = 1.0 / atraso;
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

    public int valorDaHabilidade() {
        return valorDaHabilidade;
    }

    // Igual ao Shattered: 8 + 2 por tier (tier 1 = 10 de força)
    public int forcaNecessaria() {
        return 8 + tier * 2;
    }

    // Dano de um golpe (mais provável no meio do intervalo, igual ao Shattered).
    // Cada ponto de força acima do necessário pode somar até +1 de dano.
    // No ataque surpresa, a adaga bate entre 75% e 100% do dano máximo (o punhal 67%, a lâmina assassina 50%).
    public int sortearDano(RandomSource sorteio, int forcaDoHeroi, boolean deSurpresa) {
        int minimo = danoMinimo;
        if (deSurpresa && surpresa > 0) {
            minimo = danoMinimo + Math.round((danoMaximo - danoMinimo) * surpresa);
        }
        int dano = Sorteio.normal(sorteio, minimo, danoMaximo);
        int forcaSobrando = forcaDoHeroi - forcaNecessaria();
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    @Override
    public int sortearDano(RandomSource sorteio, DadosDoHeroi heroi, boolean surpresa) {
        return sortearDano(sorteio, heroi.forca(), surpresa);
    }

    @Override
    public float precisaoCom(DadosDoHeroi heroi) {
        return precisaoCom(heroi.forca());
    }

    // Multiplicador de precisão. Cada ponto de força que falta divide a precisão por 1,5.
    public float precisaoCom(int forcaDoHeroi) {
        int forcaFaltando = forcaNecessaria() - forcaDoHeroi;
        if (forcaFaltando > 0) {
            return (float) (precisao / Math.pow(1.5, forcaFaltando));
        }
        return precisao;
    }

    // Defesa que a arma dá a quem segura (sorteada de 0 até a defesa dela)
    public int sortearDefesa(RandomSource sorteio, int forcaDoHeroi) {
        int valor = Sorteio.normal(sorteio, 0, defesa);
        int forcaFaltando = forcaNecessaria() - forcaDoHeroi;
        if (forcaFaltando > 0) {
            valor -= 2 * forcaFaltando;
        }
        return Math.max(0, valor);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("dica.pixeldungeon.tier", tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dica.pixeldungeon.dano", danoMinimo, danoMaximo).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dica.pixeldungeon.forca", forcaNecessaria()).withStyle(ChatFormatting.GRAY));
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
        // Habilidade que a Duelista pode usar com esta arma (com o número desta arma)
        if (habilidade != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.habilidade", Component.translatable(habilidade.chaveDoNome()))
                    .withStyle(ChatFormatting.AQUA));
            Textos.adicionarQuebrado(linhas, habilidade.chaveDaDescricao(), ChatFormatting.DARK_AQUA, valorDaHabilidade);
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
