package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Objects;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.cliente.EstadoNoCliente;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Armas de arremesso do Shattered (pedras, facas, shurikens...), com os números de lá:
//   - clique direito arremessa uma (gasta 1 turno; a shuriken sai na hora uma vez a cada 20 turnos)
//   - longe do inimigo acertam 1,5x melhor; coladas nele, pela metade (ver Combate)
//   - dano de 2 x tier a 5 x tier (cada arma muda um pouco); pedem 1 de força a menos que as armas do mesmo tier
//   - o conjunto vai se desgastando: cada acerto gasta um pouco, e quando a durabilidade acaba uma delas quebra
//   - a maioria fica cravada no inimigo e cai quando ele morre; as que não grudam caem no chão
//   - achadas na dungeon, vêm em 3, sem identificar, e podem ser amaldiçoadas (se identificam com 10 acertos)
// O voo, o acerto e o desgaste ficam na ArmaArremessada.
public class ArmaDeArremesso extends Item implements Arma, Equipamento {
    public static final float DURABILIDADE_MAXIMA = 100;
    public static final int QUANTIDADE_PADRAO = 3;
    public static final float USOS_PARA_IDENTIFICAR = 10;
    // Shuriken: quando foi a última vez que saiu na hora
    public static final String SHURIKEN_RECARGA = "shuriken_instantanea";

    // O que algumas armas fazem de diferente
    public enum Especial {
        NENHUM,
        // Boleadeiras: aleijam o alvo
        ALEIJA,
        // Tomahawk: faz o alvo sangrar
        SANGRA,
        // Shuriken: sai na hora, uma vez a cada 20 turnos
        INSTANTANEA,
        // Bumerangue pesado: volta pra onde foi arremessado depois de 5 turnos
        VOLTA,
        // Cubo de força: acerta todo mundo em volta de onde cai
        EM_AREA
    }

    // Os números de uma arma de arremesso. Começam com os padrões do tier no Shattered
    // (dano de 2 x tier a 5 x tier, +1 no mínimo e +tier no máximo por nível, 8 usos, gruda no inimigo)
    // e cada arma muda só o que tem de diferente:
    //   Numeros.doTier(1).dano(2, 6).crescimento(1, 2).usos(5).surpresa(0.75f)
    public static class Numeros {
        private int tier;
        private int danoMinimo;
        private int danoMaximo;
        private int crescimentoMinimo = 1;
        private int crescimentoMaximo;
        // Quantos acertos o conjunto aguenta antes de uma quebrar (cada nível multiplica por 1,5)
        private float usos = 8;
        private boolean gruda = true;
        // Ataque surpresa forte: o dano começa mais perto do máximo (faca 75%, kunai 60%)
        private float surpresa = 0;
        private Especial especial = Especial.NENHUM;

        public static Numeros doTier(int tier) {
            Numeros numeros = new Numeros();
            numeros.tier = tier;
            numeros.danoMinimo = 2 * tier;
            numeros.danoMaximo = 5 * tier;
            numeros.crescimentoMaximo = tier;
            return numeros;
        }

        public Numeros dano(int minimo, int maximo) {
            danoMinimo = minimo;
            danoMaximo = maximo;
            return this;
        }

        public Numeros crescimento(int minimo, int maximo) {
            crescimentoMinimo = minimo;
            crescimentoMaximo = maximo;
            return this;
        }

        public Numeros usos(float quantos) {
            usos = quantos;
            return this;
        }

        public Numeros naoGruda() {
            gruda = false;
            return this;
        }

        public Numeros surpresa(float parte) {
            surpresa = parte;
            return this;
        }

        public Numeros especial(Especial qual) {
            especial = qual;
            return this;
        }
    }

    private Numeros numeros;
    private String chaveDaDescricao;

    public ArmaDeArremesso(Properties propriedades, String nome, Numeros numeros) {
        super(propriedades);
        this.numeros = numeros;
        this.chaveDaDescricao = "item.pixeldungeon." + nome + ".descricao";
    }

    @Override
    public int tier() {
        return numeros.tier;
    }

    @Override
    public Maldicao.Alvo alvoDasMaldicoes() {
        return Maldicao.Alvo.ARMA;
    }

    // Igual ao Shattered: 1 de força a menos que uma arma do mesmo tier
    @Override
    public int forcaNecessaria(int nivel) {
        return NivelDoItem.forcaNecessaria(numeros.tier, nivel) - 1;
    }

    public boolean gruda() {
        return numeros.gruda;
    }

    public Especial especial() {
        return numeros.especial;
    }

    public int danoMinimo(int nivel) {
        return numeros.danoMinimo + nivel * numeros.crescimentoMinimo;
    }

    public int danoMaximo(int nivel) {
        return numeros.danoMaximo + nivel * numeros.crescimentoMaximo;
    }

    // Sangramento do tomahawk: 3-6, mais meio por nível no mínimo e 1 no máximo
    public static float sangramentoMinimo(int nivel) {
        return 3 + nivel / 2.0F;
    }

    public static float sangramentoMaximo(int nivel) {
        return 6 + nivel;
    }

    // ---- Combate ----

    // Dano de um acerto. Cada ponto de força acima do necessário soma de 0 até ele.
    // Anel do tiro certeiro: +1 nível de dano por bônus
    @Override
    public int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean deSurpresa) {
        int nivel = NivelDoItem.nivel(pilha);
        int minimo = Math.max(0, danoMinimo(nivel + heroi.bonusDoAnel(TipoDeAnel.TIRO_CERTEIRO)));
        int maximo = Math.max(0, danoMaximo(nivel + heroi.bonusDoAnel(TipoDeAnel.TIRO_CERTEIRO)));
        if (deSurpresa && numeros.surpresa > 0) {
            minimo = minimo + Math.round((maximo - minimo) * numeros.surpresa);
        }
        // A alteração (pedra de alteração) muda o dano sorteado, antes do bônus da força
        int dano = Alteracao.dano(pilha, Sorteio.normal(sorteio, minimo, maximo));
        int forcaSobrando = heroi.forca() - forcaNecessaria(pilha);
        if (forcaSobrando > 0) {
            dano += Sorteio.normal(sorteio, 0, forcaSobrando);
        }
        return dano;
    }

    @Override
    public float precisao(ItemStack pilha, DadosDoHeroi heroi) {
        return Equipamento.divisaoPelaForca(forcaFaltando(pilha, heroi.forca()));
    }

    // ---- Durabilidade (igual ao Shattered) ----

    public static float durabilidade(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.DURABILIDADE.get(), DURABILIDADE_MAXIMA);
    }

    public static void definirDurabilidade(ItemStack pilha, float valor) {
        if (valor >= DURABILIDADE_MAXIMA) {
            pilha.remove(ModComponentes.DURABILIDADE.get());
        } else {
            pilha.set(ModComponentes.DURABILIDADE.get(), valor);
        }
    }

    // Quantos acertos o conjunto aguenta: os do tipo, x1,5 por nível; projéteis duráveis (Caçadora) +50%/+75%;
    // anel do tiro certeiro, x1,2 por bônus
    public float usos(int nivel, int pontosDuraveis, int tiroCerteiro) {
        float usos = numeros.usos * (float) Math.pow(1.5, nivel);
        if (pontosDuraveis > 0) {
            usos *= 1.25F + 0.25F * pontosDuraveis;
        }
        return usos * (float) Math.pow(1.2, tiroCerteiro);
    }

    // Quanto cada acerto gasta da durabilidade (0 = dura pra sempre, com 100 usos ou mais).
    // Igual ao Shattered, a alteração muda os usos na proporção da velocidade (mais rápida, dura mais).
    public float gastoPorUso(int nivel, int pontosDuraveis, int tiroCerteiro) {
        return gastoPorUso(nivel, pontosDuraveis, tiroCerteiro, 1F);
    }

    public float gastoPorUso(int nivel, int pontosDuraveis, int tiroCerteiro, float fatorDeAtraso) {
        float usos = usos(nivel, pontosDuraveis, tiroCerteiro) / fatorDeAtraso;
        if (usos >= 100) {
            return 0;
        }
        return DURABILIDADE_MAXIMA / Math.round(usos) + 0.001F;
    }

    public float gastoPorUso(int nivel, int pontosDuraveis) {
        return gastoPorUso(nivel, pontosDuraveis, 0);
    }

    public float gastoPorUso(ItemStack pilha, DadosDoHeroi dados) {
        return gastoPorUso(NivelDoItem.nivel(pilha), dados.pontos(Talento.PROJETEIS_DURAVEIS), dados.bonusDoAnel(TipoDeAnel.TIRO_CERTEIRO),
                Alteracao.fatorDeAtraso(pilha));
    }

    // ---- Arremessar ----

    // Clique direito: arremessa uma do conjunto
    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!mundo.isClientSide() && jogador instanceof ServerPlayer heroi) {
            arremessar(heroi, pilha);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    public void arremessar(ServerPlayer jogador, ItemStack pilha) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        // A que voa é a mais gasta; as que ficam na mão estão inteiras (a durabilidade é do conjunto, igual ao Shattered)
        ItemStack voando = pilha.copyWithCount(1);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
            if (!pilha.isEmpty()) {
                definirDurabilidade(pilha, DURABILIDADE_MAXIMA);
            }
        }
        ArmaArremessada arma = new ArmaArremessada(jogador.level(), jogador, voando);
        arma.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 2.0F, 0.5F);
        jogador.level().addFreshEntity(arma);
        jogador.level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F, 0.6F);
        // Arremessar gasta 1 turno (pra todas as armas de arremesso); a shuriken sai na hora uma vez a cada 20 turnos
        int espera = Math.round(Turno.TICKS * Alteracao.fatorDeAtraso(voando));
        if (numeros.especial == Especial.INSTANTANEA && !dados.ativo(SHURIKEN_RECARGA, agora)) {
            espera = 4;
            dados.ativarPor(SHURIKEN_RECARGA, agora, Turno.ticks(19));
        }
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaDeArremesso) {
                jogador.getCooldowns().addCooldown(registrado.get(), espera);
            }
        }
    }

    // ---- Juntar o que voltou pro inventário ----

    // Igual ao Shattered: as armas de arremesso que voltam pra mochila se juntam de novo ao conjunto delas,
    // somando o que sobrou da durabilidade das duas pilhas (o que passar de 100 vira uma arma inteira a mais)
    // e o que já se sabe delas (identificação e maldição).
    public static void juntarPilhas(Player jogador) {
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.items.size(); i++) {
            ItemStack primeira = inventario.items.get(i);
            if (!(primeira.getItem() instanceof ArmaDeArremesso)) {
                continue;
            }
            for (int j = i + 1; j < inventario.items.size(); j++) {
                ItemStack outra = inventario.items.get(j);
                if (!mesmoConjunto(primeira, outra) || primeira.getCount() + outra.getCount() > primeira.getMaxStackSize()) {
                    continue;
                }
                juntar(primeira, outra);
                inventario.items.set(j, ItemStack.EMPTY);
            }
        }
    }

    // Mesma arma, mesmo nível e mesma maldição (mesmo que uma já esteja identificada e a outra não)
    public static boolean mesmoConjunto(ItemStack uma, ItemStack outra) {
        return uma.getItem() instanceof ArmaDeArremesso && uma.getItem() == outra.getItem() && NivelDoItem.nivel(uma) == NivelDoItem.nivel(outra)
                && Objects.equals(uma.get(ModComponentes.MALDICAO.get()), outra.get(ModComponentes.MALDICAO.get()));
    }

    // Junta a outra pilha na primeira
    public static void juntar(ItemStack primeira, ItemStack outra) {
        int quantidade = primeira.getCount() + outra.getCount();
        float duracao = durabilidade(primeira) + durabilidade(outra) - DURABILIDADE_MAXIMA;
        while (duracao <= 0 && quantidade > 1) {
            quantidade--;
            duracao += DURABILIDADE_MAXIMA;
        }
        // O que uma descobriu vale pro conjunto todo
        if (NivelDoItem.identificado(outra) && !NivelDoItem.identificado(primeira)) {
            NivelDoItem.identificar(null, primeira);
        } else if (!NivelDoItem.identificado(primeira)) {
            if (NivelDoItem.maldicaoConhecida(outra)) {
                primeira.remove(ModComponentes.MALDICAO_CONHECIDA.get());
            }
            float faltaNaPrimeira = primeira.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), USOS_PARA_IDENTIFICAR);
            float faltaNaOutra = outra.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), USOS_PARA_IDENTIFICAR);
            primeira.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), Math.min(faltaNaPrimeira, faltaNaOutra));
        }
        primeira.setCount(quantidade);
        definirDurabilidade(primeira, duracao);
    }

    // ---- Aparência ----

    // Brilha com encantamento
    @Override
    public boolean isFoil(ItemStack pilha) {
        return NivelDoItem.encantamento(pilha) != null || super.isFoil(pilha);
    }

    @Override
    public Component getName(ItemStack pilha) {
        return NivelDoItem.nome(pilha, super.getName(pilha));
    }

    // A barrinha embaixo do ícone mostra o desgaste do conjunto
    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return durabilidade(pilha) < DURABILIDADE_MAXIMA;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Math.round(13.0F * durabilidade(pilha) / DURABILIDADE_MAXIMA);
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return Mth.hsvToRgb(durabilidade(pilha) / DURABILIDADE_MAXIMA / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        boolean conhecida = NivelDoItem.identificado(pilha);
        int nivel = conhecida ? NivelDoItem.nivel(pilha) : 0;
        linhas.add(Component.translatable("dica.pixeldungeon.arremessar").withStyle(ChatFormatting.AQUA));
        linhas.add(Component.translatable("dica.pixeldungeon.tier", numeros.tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.dano" : "dica.pixeldungeon.dano_tipico", Alteracao.dano(pilha, danoMinimo(nivel)),
                Alteracao.dano(pilha, danoMaximo(nivel))).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.forca" : "dica.pixeldungeon.forca_tipica", forcaNecessaria(nivel) - maestria(pilha))
                .withStyle(ChatFormatting.GRAY));
        if (maestria(pilha) > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.maestria").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Alteracao alteracao = Alteracao.de(pilha);
        if (alteracao != null) {
            linhas.add(Component.translatable(alteracao.chaveDaDica()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (numeros.surpresa > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.surpresa_forte", Math.round(numeros.surpresa * 100)).withStyle(ChatFormatting.GRAY));
        }
        if (numeros.especial == Especial.SANGRA) {
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso.sangra", Math.round(sangramentoMinimo(nivel)), Math.round(sangramentoMaximo(nivel)))
                    .withStyle(ChatFormatting.GRAY));
        } else if (numeros.especial != Especial.NENHUM) {
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso." + numeros.especial.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        }
        if (!numeros.gruda && numeros.especial == Especial.NENHUM) {
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso.nao_gruda").withStyle(ChatFormatting.GRAY));
        }
        // Quantos acertos ainda aguenta antes de uma quebrar
        int pontosDuraveis = EstadoNoCliente.pontos(Talento.PROJETEIS_DURAVEIS);
        float gasto = gastoPorUso(nivel, pontosDuraveis, 0, Alteracao.fatorDeAtraso(pilha));
        if (gasto <= 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso.para_sempre").withStyle(ChatFormatting.GRAY));
        } else if (conhecida) {
            int total = Math.round(DURABILIDADE_MAXIMA / gasto);
            int restantes = (int) Math.ceil(durabilidade(pilha) / gasto);
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso.usos", Math.min(restantes, total), total).withStyle(ChatFormatting.GRAY));
        } else {
            linhas.add(Component.translatable("dica.pixeldungeon.arremesso.usos_tipicos", Math.round(DURABILIDADE_MAXIMA / gasto)).withStyle(ChatFormatting.GRAY));
        }
        linhas.add(Component.translatable("dica.pixeldungeon.arremesso.distancia").withStyle(ChatFormatting.DARK_GRAY));
        NivelDoItem.dicaDeIdentificacao(pilha, linhas);
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
