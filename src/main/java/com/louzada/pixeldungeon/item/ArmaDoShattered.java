package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista.Habilidade;

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
//   - dano sorteado entre o mínimo e o máximo a cada golpe (mais provável no meio)
//   - força necessária: quem tem menos força erra mais e bate mais devagar; quem tem força sobrando bate mais forte
//   - cada nível (pergaminho de melhoria) dá +1 no dano mínimo e mais um tanto no máximo, e pede um pouco menos de força
//   - armas achadas na dungeon vêm sem identificar e podem ser amaldiçoadas (ver NivelDoItem e Maldicao)
// Os números de cada arma ficam no ModItens (ver Numeros). Quem usa a arma no golpe é o combate (HeroiEmCombate).
public class ArmaDoShattered extends Item implements Arma, Equipamento {

    // Os números de uma arma. Começam com os padrões do tier no Shattered (dano de tier a 5 x (tier + 1),
    // precisão normal, um ataque por turno) e cada arma muda só o que tem de diferente:
    //   Numeros.doTier(1).dano(1, 8).precisao(1.4f).habilidade(Habilidade.GOLPE_PESADO, 3)
    public static class Numeros {
        private int tier;
        private int danoMinimo;
        private int danoMaximo;
        // Quanto o dano máximo cresce por nível (o padrão é tier + 1)
        private int crescimento;
        // Multiplicador de precisão (1 = normal, 1,4 = 40% a mais)
        private float precisao = 1.0F;
        // Tempo entre ataques, em turnos (1 turno = 2 segundos). 0,5 = ataca 2x mais rápido.
        private float atraso = 1.0F;
        // Defesa extra que a arma dá a quem segura (o florete dá 1, a katana 3)
        private int defesa = 0;
        // Ataque surpresa forte: o dano começa mais perto do máximo (adaga 75%, punhal 67%, lâmina assassina 50%)
        private float surpresa = 0;
        // Habilidade que a Duelista usa com esta arma e o número dela (dano a mais; no furtivo, o alcance)
        @Nullable
        private Habilidade habilidade;
        private int valorDaHabilidade;

        public static Numeros doTier(int tier) {
            Numeros numeros = new Numeros();
            numeros.tier = tier;
            numeros.danoMinimo = tier;
            numeros.danoMaximo = 5 * (tier + 1);
            numeros.crescimento = tier + 1;
            return numeros;
        }

        public Numeros dano(int minimo, int maximo) {
            danoMinimo = minimo;
            danoMaximo = maximo;
            return this;
        }

        public Numeros precisao(float multiplicador) {
            precisao = multiplicador;
            return this;
        }

        // Ataca 2x por turno, e o dano cresce metade por nível (luvas, sai)
        public Numeros rapida() {
            atraso = 0.5F;
            crescimento = Math.round(0.5F * (tier + 1));
            return this;
        }

        public Numeros defesa(int quanto) {
            defesa = quanto;
            return this;
        }

        public Numeros surpresa(float parte) {
            surpresa = parte;
            return this;
        }

        public Numeros habilidade(Habilidade qual, int valor) {
            habilidade = qual;
            valorDaHabilidade = valor;
            return this;
        }
    }

    private Numeros numeros;
    private String chaveDaDescricao;

    public ArmaDoShattered(Properties propriedades, String nome, Numeros numeros) {
        super(propriedades.stacksTo(1).attributes(atributos(numeros.atraso)));
        this.numeros = numeros;
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

    @Override
    public int tier() {
        return numeros.tier;
    }

    @Override
    public Maldicao.Alvo alvoDasMaldicoes() {
        return Maldicao.Alvo.ARMA;
    }

    @Nullable
    public Habilidade habilidade() {
        return numeros.habilidade;
    }

    // ---- Números no nível de cada arma ----

    public int danoMinimo(int nivel) {
        return numeros.danoMinimo + nivel;
    }

    public int danoMaximo(int nivel) {
        return numeros.danoMaximo + nivel * numeros.crescimento;
    }

    // O número da habilidade cresce com o nível, igual ao Shattered
    // (cortar e combinação: +1 por nível; golpe pesado e estocada: +1,5 por nível; o furtivo não muda o alcance)
    public int valorDaHabilidade(int nivel) {
        if (numeros.habilidade == null) {
            return 0;
        }
        return switch (numeros.habilidade) {
            case CORTAR, COMBINACAO -> numeros.valorDaHabilidade + nivel;
            case GOLPE_PESADO, ESTOCADA -> numeros.valorDaHabilidade + Math.round(1.5f * nivel);
            case FURTIVO -> numeros.valorDaHabilidade;
        };
    }

    public int valorDaHabilidade(ItemStack pilha) {
        return valorDaHabilidade(NivelDoItem.nivel(pilha));
    }

    // ---- Combate ----

    // Dano de um golpe. Cada ponto de força acima do necessário pode somar até +1 de dano.
    // No ataque surpresa, a adaga bate entre 75% e 100% do dano máximo (o punhal 67%, a lâmina assassina 50%).
    @Override
    public int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean deSurpresa) {
        int nivel = NivelDoItem.nivel(pilha);
        int minimo = danoMinimo(nivel);
        int maximo = danoMaximo(nivel);
        if (deSurpresa && numeros.surpresa > 0) {
            minimo = minimo + Math.round((maximo - minimo) * numeros.surpresa);
        }
        int dano = Sorteio.normal(sorteio, minimo, maximo);
        int forcaSobrando = heroi.forca() - forcaNecessaria(nivel);
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    @Override
    public float precisao(ItemStack pilha, DadosDoHeroi heroi) {
        return numeros.precisao * Equipamento.divisaoPelaForca(forcaFaltando(pilha, heroi.forca()));
    }

    // Defesa que a arma dá a quem segura (sorteada de 0 até a defesa dela; cada ponto de força que falta tira 2)
    @Override
    public int sortearDefesa(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi) {
        int valor = Sorteio.normal(sorteio, 0, numeros.defesa) - 2 * forcaFaltando(pilha, heroi.forca());
        return Math.max(0, valor);
    }

    // ---- Aparência ----

    // Brilha com encantamento
    @Override
    public boolean isFoil(ItemStack pilha) {
        return NivelDoItem.encantamento(pilha) != null || super.isFoil(pilha);
    }

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
        linhas.add(Component.translatable("dica.pixeldungeon.tier", numeros.tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.dano" : "dica.pixeldungeon.dano_tipico", danoMinimo(nivel), danoMaximo(nivel))
                .withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.forca" : "dica.pixeldungeon.forca_tipica", forcaNecessaria(nivel))
                .withStyle(ChatFormatting.GRAY));
        if (numeros.precisao > 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.precisao", Math.round((numeros.precisao - 1.0f) * 100)).withStyle(ChatFormatting.GRAY));
        }
        if (numeros.atraso < 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.rapida", Math.round(1.0f / numeros.atraso)).withStyle(ChatFormatting.GRAY));
        }
        if (numeros.defesa > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.defesa_extra", numeros.defesa).withStyle(ChatFormatting.GRAY));
        }
        if (numeros.surpresa > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.surpresa_forte", Math.round(numeros.surpresa * 100)).withStyle(ChatFormatting.GRAY));
        }
        NivelDoItem.dicaDeIdentificacao(pilha, linhas);
        // Habilidade que a Duelista pode usar com esta arma (com o número desta arma)
        if (numeros.habilidade != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.habilidade", Component.translatable(numeros.habilidade.chaveDoNome()))
                    .withStyle(ChatFormatting.AQUA));
            Textos.adicionarQuebrado(linhas, numeros.habilidade.chaveDaDescricao(), ChatFormatting.DARK_AQUA, valorDaHabilidade(nivel));
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
