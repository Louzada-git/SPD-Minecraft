package com.louzada.pixeldungeon.item;

import java.util.List;

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
public class ArmaDoShattered extends Item {
    private int tier;
    private int danoMinimo;
    private int danoMaximo;
    // Multiplicador de precisão (1.0 = normal, 1.4 = 40% a mais)
    private float precisao;
    // Tempo entre ataques, em turnos (1 turno = 1 segundo). 0.5 = ataca 2x mais rápido.
    private float atraso;
    // Chave da descrição no arquivo de idioma
    private String chaveDaDescricao;

    public ArmaDoShattered(Properties propriedades, String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso) {
        super(propriedades.stacksTo(1).attributes(atributos(atraso)));
        this.tier = tier;
        this.danoMinimo = danoMinimo;
        this.danoMaximo = danoMaximo;
        this.precisao = precisao;
        this.atraso = atraso;
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

    // Igual ao Shattered: 8 + 2 por tier (tier 1 = 10 de força)
    public int forcaNecessaria() {
        return 8 + tier * 2;
    }

    // Dano de um golpe. Cada ponto de força acima do necessário pode somar até +1 de dano.
    public int sortearDano(RandomSource sorteio, int forcaDoHeroi) {
        int dano = danoMinimo + sorteio.nextInt(danoMaximo - danoMinimo + 1);
        int forcaSobrando = forcaDoHeroi - forcaNecessaria();
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    // Multiplicador de precisão. Cada ponto de força que falta divide a precisão por 1,5.
    public float precisaoCom(int forcaDoHeroi) {
        int forcaFaltando = forcaNecessaria() - forcaDoHeroi;
        if (forcaFaltando > 0) {
            return (float) (precisao / Math.pow(1.5, forcaFaltando));
        }
        return precisao;
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
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
