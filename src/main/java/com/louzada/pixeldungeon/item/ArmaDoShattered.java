package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista.Habilidade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
//   - algumas são lentas ou rápidas, têm alcance maior (1 quadradinho = 4 blocos), defendem ou não atacam de surpresa
//   - armas achadas na dungeon vêm sem identificar e podem ser amaldiçoadas (ver NivelDoItem e Maldicao)
// Os números de cada arma ficam no ModItens (ver Numeros). Quem usa a arma no golpe é o combate (HeroiEmCombate).
public class ArmaDoShattered extends Item implements Arma, Equipamento {
    private static final ResourceLocation ALCANCE_DA_ARMA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "alcance_da_arma");

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
        // Quanto a defesa cresce por nível (escudo redondo +1, escudo grande +2)
        private int defesaPorNivel = 0;
        // Ataque surpresa forte: o dano começa mais perto do máximo (adaga 75%, punhal 67%, lâmina assassina 50%)
        private float surpresa = 0;
        // O mangual não ataca de surpresa
        private boolean semSurpresa = false;
        // Alcance a mais, em quadradinhos (lança e gládio 1, chicote 2)
        private int alcance = 0;
        // A força pedida é a de outro tier (o machado grande pede a do tier 6: 20)
        private int tierDaForca;
        // Habilidade que a Duelista usa com esta arma e o número dela (dano a mais; no furtivo, o alcance; nas posturas, os turnos)
        @Nullable
        private Habilidade habilidade;
        private int valorDaHabilidade;
        // Quanto o número da habilidade cresce por nível
        private float habilidadePorNivel;

        public static Numeros doTier(int tier) {
            Numeros numeros = new Numeros();
            numeros.tier = tier;
            numeros.danoMinimo = tier;
            numeros.danoMaximo = 5 * (tier + 1);
            numeros.crescimento = tier + 1;
            numeros.tierDaForca = tier;
            return numeros;
        }

        public Numeros dano(int minimo, int maximo) {
            danoMinimo = minimo;
            danoMaximo = maximo;
            return this;
        }

        // Dano mínimo, máximo e quanto o máximo cresce por nível
        public Numeros dano(int minimo, int maximo, int porNivel) {
            danoMinimo = minimo;
            danoMaximo = maximo;
            crescimento = porNivel;
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

        // Tempo entre ataques, em turnos (1,5 = mais lenta, 0,8 = mais rápida)
        public Numeros atraso(float turnos) {
            atraso = turnos;
            return this;
        }

        public Numeros defesa(int quanto) {
            defesa = quanto;
            return this;
        }

        public Numeros defesa(int quanto, int porNivel) {
            defesa = quanto;
            defesaPorNivel = porNivel;
            return this;
        }

        public Numeros alcance(int quadradinhos) {
            alcance = quadradinhos;
            return this;
        }

        public Numeros semSurpresa() {
            semSurpresa = true;
            return this;
        }

        public Numeros forcaDoTier(int tier) {
            tierDaForca = tier;
            return this;
        }

        public Numeros surpresa(float parte) {
            surpresa = parte;
            return this;
        }

        // O número cresce como no Shattered: cortar e combinação +1 por nível, golpe pesado e estocada +1,5, furtivo nada
        public Numeros habilidade(Habilidade qual, int valor) {
            float porNivel = switch (qual) {
                case CORTAR, COMBINACAO -> 1;
                case GOLPE_PESADO, ESTOCADA -> 1.5F;
                default -> 0;
            };
            return habilidade(qual, valor, porNivel);
        }

        public Numeros habilidade(Habilidade qual, int valor, float porNivel) {
            habilidade = qual;
            valorDaHabilidade = valor;
            habilidadePorNivel = porNivel;
            return this;
        }
    }

    private Numeros numeros;
    private String chaveDaDescricao;

    public ArmaDoShattered(Properties propriedades, String nome, Numeros numeros) {
        super(propriedades.stacksTo(1).attributes(atributos(numeros.atraso, numeros.alcance)));
        this.numeros = numeros;
        this.chaveDaDescricao = "item.pixeldungeon." + nome + ".descricao";
    }

    // Velocidade de ataque do Minecraft: o jogador tem 4 de base, então somamos (ataques por segundo - 4).
    // O alcance a mais vira blocos a mais pra bater (4 por quadradinho).
    private static ItemAttributeModifiers atributos(float atraso, int alcance) {
        double ataquesPorSegundo = 20.0 / (atraso * Turno.TICKS);
        ItemAttributeModifiers.Builder construtor = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ataquesPorSegundo - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (alcance > 0) {
            construtor.add(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(ALCANCE_DA_ARMA, 4.0 * alcance, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return construtor.build();
    }

    // A pedra de alteração muda a velocidade de ataque desta arma (só desta pilha)
    public void atualizarVelocidade(ItemStack pilha) {
        pilha.set(DataComponents.ATTRIBUTE_MODIFIERS, atributos(atraso(pilha), numeros.alcance));
    }

    // Quantos turnos leva cada ataque (com a alteração, se tiver)
    public float atraso(ItemStack pilha) {
        return numeros.atraso * Alteracao.fatorDeAtraso(pilha);
    }

    // Alcance a mais, em blocos
    public double alcanceExtra() {
        return 4.0 * numeros.alcance;
    }

    // O mangual não ataca de surpresa
    public boolean atacaDeSurpresa() {
        return !numeros.semSurpresa;
    }

    @Override
    public int forcaNecessaria(int nivel) {
        return NivelDoItem.forcaNecessaria(numeros.tierDaForca, nivel);
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

    // O número da habilidade cresce com o nível, igual ao Shattered (ver Numeros.habilidade)
    public int valorDaHabilidade(int nivel) {
        if (numeros.habilidade == null) {
            return 0;
        }
        return numeros.valorDaHabilidade + Math.round(numeros.habilidadePorNivel * nivel);
    }

    public int valorDaHabilidade(ItemStack pilha) {
        return valorDaHabilidade(NivelDoItem.nivel(pilha));
    }

    // ---- Combate ----

    // Dano de um golpe. Cada ponto de força acima do necessário pode somar até +1 de dano.
    // A alteração (pedra de alteração) muda o dano sorteado, antes do bônus da força, igual ao Shattered.
    // No ataque surpresa, a adaga bate entre 75% e 100% do dano máximo (o punhal 67%, a lâmina assassina 50%).
    @Override
    public int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean deSurpresa) {
        int nivel = NivelDoItem.nivel(pilha);
        int minimo = danoMinimo(nivel);
        int maximo = danoMaximo(nivel);
        if (deSurpresa && numeros.surpresa > 0) {
            minimo = minimo + Math.round((maximo - minimo) * numeros.surpresa);
        }
        int dano = Alteracao.dano(pilha, Sorteio.normal(sorteio, minimo, maximo));
        int forcaSobrando = heroi.forca() - forcaNecessaria(pilha);
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    @Override
    public float precisao(ItemStack pilha, DadosDoHeroi heroi) {
        return precisaoComForca(pilha, heroi.forca());
    }

    // ---- Pra quem não é o herói (o fantasma da rosa seca): os mesmos números, com a força dele ----

    public float precisaoComForca(ItemStack pilha, int forca) {
        return numeros.precisao * Equipamento.divisaoPelaForca(forcaFaltando(pilha, forca));
    }

    public int sortearDanoComForca(ItemStack pilha, RandomSource sorteio, int forca) {
        int nivel = NivelDoItem.nivel(pilha);
        int dano = Alteracao.dano(pilha, Sorteio.normal(sorteio, danoMinimo(nivel), danoMaximo(nivel)));
        int forcaSobrando = forca - forcaNecessaria(pilha);
        if (forcaSobrando > 0) {
            dano += sorteio.nextInt(forcaSobrando + 1);
        }
        return dano;
    }

    public int sortearDefesaComForca(ItemStack pilha, RandomSource sorteio, int forca) {
        int valor = Sorteio.normal(sorteio, 0, defesa(NivelDoItem.nivel(pilha))) - 2 * forcaFaltando(pilha, forca);
        return Math.max(0, valor);
    }

    // Defesa que a arma dá a quem segura (sorteada de 0 até a defesa dela; cada ponto de força que falta tira 2)
    @Override
    public int sortearDefesa(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi) {
        int valor = Sorteio.normal(sorteio, 0, defesa(NivelDoItem.nivel(pilha))) - 2 * forcaFaltando(pilha, heroi.forca());
        return Math.max(0, valor);
    }

    public int defesa(int nivel) {
        return numeros.defesa + numeros.defesaPorNivel * Math.max(0, nivel);
    }

    // ---- Aparência ----

    // Brilha com encantamento
    @Override
    public boolean isFoil(ItemStack pilha) {
        return NivelDoItem.encantamento(pilha) != null || super.isFoil(pilha);
    }

    // "2", "1.25", "1.5"...
    private static String vezes(float numero) {
        return String.format(Locale.ROOT, "%.2f", numero).replaceAll("\\.?0+$", "");
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
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.dano" : "dica.pixeldungeon.dano_tipico", Alteracao.dano(pilha, danoMinimo(nivel)),
                Alteracao.dano(pilha, danoMaximo(nivel))).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.forca" : "dica.pixeldungeon.forca_tipica", forcaNecessaria(nivel) - maestria(pilha))
                .withStyle(ChatFormatting.GRAY));
        if (maestria(pilha) > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.maestria").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (numeros.precisao > 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.precisao", Math.round((numeros.precisao - 1.0f) * 100)).withStyle(ChatFormatting.GRAY));
        } else if (numeros.precisao < 1.0f) {
            linhas.add(Component.translatable("dica.pixeldungeon.imprecisa", Math.round((1.0f - numeros.precisao) * 100)).withStyle(ChatFormatting.GRAY));
        }
        float atraso = atraso(pilha);
        if (atraso < 0.999f) {
            linhas.add(Component.translatable("dica.pixeldungeon.rapida", vezes(1.0f / atraso)).withStyle(ChatFormatting.GRAY));
        } else if (atraso > 1.001f) {
            linhas.add(Component.translatable("dica.pixeldungeon.lenta", vezes(atraso)).withStyle(ChatFormatting.GRAY));
        }
        Alteracao alteracao = Alteracao.de(pilha);
        if (alteracao != null) {
            linhas.add(Component.translatable(alteracao.chaveDaDica()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (numeros.alcance > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.alcance", numeros.alcance).withStyle(ChatFormatting.GRAY));
        }
        if (defesa(nivel) > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.defesa_extra", defesa(nivel)).withStyle(ChatFormatting.GRAY));
        }
        if (numeros.semSurpresa) {
            linhas.add(Component.translatable("dica.pixeldungeon.sem_surpresa").withStyle(ChatFormatting.GRAY));
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
