package com.louzada.pixeldungeon.item;

import java.util.List;

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
    // Defesa extra que a arma dá (o florete dá 1)
    private int defesa;
    // Ataque surpresa forte (a adaga)
    private boolean surpresaForte;
    // Chave da descrição no arquivo de idioma
    private String chaveDaDescricao;

    public ArmaDoShattered(Properties propriedades, String nome, int tier, int danoMinimo, int danoMaximo, float precisao, float atraso,
            int defesa, boolean surpresaForte) {
        super(propriedades.stacksTo(1).attributes(atributos(atraso)));
        this.tier = tier;
        this.danoMinimo = danoMinimo;
        this.danoMaximo = danoMaximo;
        this.precisao = precisao;
        this.atraso = atraso;
        this.defesa = defesa;
        this.surpresaForte = surpresaForte;
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

    // Dano de um golpe (mais provável no meio do intervalo, igual ao Shattered).
    // Cada ponto de força acima do necessário pode somar até +1 de dano.
    // No ataque surpresa, a adaga bate entre 75% e 100% do dano máximo.
    public int sortearDano(RandomSource sorteio, int forcaDoHeroi, boolean surpresa) {
        int minimo = danoMinimo;
        if (surpresa && surpresaForte) {
            minimo = danoMinimo + Math.round((danoMaximo - danoMinimo) * 0.75f);
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
        if (surpresaForte) {
            linhas.add(Component.translatable("dica.pixeldungeon.surpresa_forte").withStyle(ChatFormatting.GRAY));
        }
        // Habilidade que a Duelista pode usar com esta arma
        HabilidadesDaDuelista.Habilidade habilidade = HabilidadesDaDuelista.habilidadeDe(this);
        if (habilidade != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.habilidade", Component.translatable(habilidade.chaveDoNome()))
                    .withStyle(ChatFormatting.AQUA));
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
