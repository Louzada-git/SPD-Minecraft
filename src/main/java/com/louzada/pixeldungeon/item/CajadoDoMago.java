package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Cajado do Mago: bate como arma (1-6) e leva uma varinha dentro (começa com a de míssil mágico). Igual ao Shattered:
//   - o cajado dá 1 carga a mais que a varinha sozinha (míssil mágico: 3 + 1 = 4)
//   - clique direito lança a varinha (gasta 1 turno e as cargas)
//   - cada nível (pergaminho de melhoria) melhora o cajado e a varinha juntos: +1 carga (até 10) e uma magia mais forte
//   - o Mago pode trocar a varinha de dentro por outra (agachado, com a varinha nova na mão e o cajado na outra):
//     o cajado fica com o maior nível dos dois (+1 se a varinha for melhor e o cajado já tiver nível), e a varinha
//     antiga se perde (a não ser com o talento preservação de varinha)
public class CajadoDoMago extends ArmaDoShattered {
    private static final int CARGA_DO_CAJADO = 1;
    private static final int COR_DA_BARRA = 0xB07CFF;
    // Trocar a varinha do cajado pede confirmação (clicar de novo em 5 segundos)
    private static final String CONFIRMAR_TROCA = "confirmar_troca_de_varinha";
    // Preservação de varinha: se já usou, e em que nível do herói
    private static final String PRESERVACAO_USADA = "preservacao_de_varinha_usada";
    private static final String PRESERVACAO_NIVEL = "preservacao_de_varinha_nivel";

    public CajadoDoMago(Properties propriedades) {
        super(propriedades, "cajado_do_mago", Numeros.doTier(1).dano(1, 6));
    }

    // A varinha que está dentro do cajado
    public static TipoDeVarinha varinha(ItemStack pilha) {
        TipoDeVarinha tipo = TipoDeVarinha.peloId(pilha.get(ModComponentes.VARINHA_DO_CAJADO.get()));
        return tipo == null ? TipoDeVarinha.MISSEL_MAGICO : tipo;
    }

    // ---- Cargas ----

    public static int cargasMaximas(ItemStack pilha) {
        return varinha(pilha).cargasMaximas(NivelDoItem.nivel(pilha), CARGA_DO_CAJADO);
    }

    public static int cargas(ItemStack pilha) {
        return Cargas.cargas(pilha, cargasMaximas(pilha));
    }

    private static void recarregar(ItemStack pilha, long agora) {
        Cargas.recarregar(pilha, agora, cargasMaximas(pilha), Varinha::ticksParaCarregar);
    }

    // Pergaminho de recarga: enche tudo na hora
    public static void encher(ItemStack pilha) {
        Cargas.somar(pilha, cargasMaximas(pilha), cargasMaximas(pilha));
    }

    // Uma vez por turno, no servidor, vê se encheu alguma carga
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && Turno.virou(dono)) {
            recarregar(pilha, mundo.getGameTime());
        }
    }

    // ---- Lançar ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return cargas(pilha) > 0 ? InteractionResultHolder.success(pilha) : InteractionResultHolder.fail(pilha);
        }
        long agora = mundo.getGameTime();
        recarregar(pilha, agora);
        int cargas = cargas(pilha);
        if (cargas <= 0) {
            heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_sem_carga").withStyle(ChatFormatting.GRAY), true);
            mundo.playSound(null, heroi.getX(), heroi.getY(), heroi.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.fail(pilha);
        }

        // Bateria de escudo: agachado, as cargas viram escudo
        if (TalentosDoTier2.bateriaDeEscudo(heroi, pilha, cargas, cargasMaximas(pilha))) {
            heroi.getCooldowns().addCooldown(this, Turno.TICKS);
            return InteractionResultHolder.success(pilha);
        }

        TipoDeVarinha tipo = varinha(pilha);
        int usadas = tipo.cargasPorDisparo(cargas);
        // Talentos do Mago (magia prolongada, barreira de apoio) e a invisibilidade acaba
        Talentos.aoUsarVarinha(heroi, cargas, usadas, true);
        Furtividade.revelar(heroi);
        // Poder inscrito: depois de ler um pergaminho, alguns disparos saem com +2 níveis
        EfeitosDeVarinha.lancar(heroi, tipo, NivelDoItem.nivel(pilha) + TalentosDoTier2.niveisInscritos(heroi), usadas);
        Cargas.gastar(pilha, agora, cargasMaximas(pilha), usadas);
        // Lançar gasta um turno
        heroi.getCooldowns().addCooldown(this, Turno.TICKS);
        return InteractionResultHolder.success(pilha);
    }

    // ---- Trocar a varinha de dentro ----

    // Primeiro avisa o que vai acontecer; clicando de novo em 5 segundos, troca
    public static void pedirParaColocar(ServerPlayer jogador, ItemStack cajado, ItemStack varinha) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        if (!dados.ativo(CONFIRMAR_TROCA, agora)) {
            dados.ativarPor(CONFIRMAR_TROCA, agora, 100);
            String chave = podePreservar(dados) ? "mensagem.pixeldungeon.cajado.confirmar_preservando" : "mensagem.pixeldungeon.cajado.confirmar";
            jogador.displayClientMessage(Component.translatable(chave, Component.translatable(varinha(cajado).chaveDoNome())).withStyle(ChatFormatting.YELLOW),
                    false);
            return;
        }
        dados.desativar(CONFIRMAR_TROCA);
        colocar(jogador, cajado, varinha);
    }

    // Preservação de varinha: +1 uma vez só; +2 de novo a cada nível do herói
    private static boolean podePreservar(DadosDoHeroi dados) {
        int pontos = dados.pontos(Talento.PRESERVACAO_DE_VARINHA);
        if (pontos <= 0) {
            return false;
        }
        return !dados.sim(PRESERVACAO_USADA) || (pontos >= 2 && dados.nivel() > dados.inteiro(PRESERVACAO_NIVEL));
    }

    // Põe a varinha no cajado, igual ao Shattered
    public static void colocar(ServerPlayer jogador, ItemStack cajado, ItemStack varinha) {
        if (!(varinha.getItem() instanceof ItemVarinha nova)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        recarregar(cajado, agora);
        ItemVarinha.recarregar(varinha, agora);
        TipoDeVarinha antiga = varinha(cajado);
        int cargasAntigas = cargas(cajado);
        // Preservação de varinha: a antiga volta pra mochila, +0
        if (podePreservar(dados)) {
            ItemStack devolvida = new ItemStack(ModItens.varinha(antiga));
            if (!jogador.getInventory().add(devolvida)) {
                jogador.drop(devolvida, false);
            }
            dados.definir(PRESERVACAO_USADA, true);
            dados.definir(PRESERVACAO_NIVEL, dados.nivel());
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.cajado.preservou").withStyle(ChatFormatting.AQUA), false);
        }
        // O cajado fica com o maior nível; se a varinha for melhor e o cajado já tiver nível, guarda +1
        int nivelDoCajado = NivelDoItem.nivel(cajado);
        int nivelDaVarinha = NivelDoItem.nivel(varinha);
        int nivel = Math.max(nivelDoCajado, nivelDaVarinha);
        if (nivelDaVarinha >= nivelDoCajado && nivelDoCajado > 0) {
            nivel++;
        }
        NivelDoItem.definirNivel(cajado, nivel);
        cajado.set(ModComponentes.VARINHA_DO_CAJADO.get(), nova.tipo().id());
        Cargas.definir(cajado, agora, cargasMaximas(cajado), ItemVarinha.cargas(varinha) + cargasAntigas);
        // Varinha amaldiçoada deixa o cajado amaldiçoado
        if (NivelDoItem.amaldicoado(varinha) && !NivelDoItem.amaldicoado(cajado)) {
            List<Maldicao> maldicoes = Maldicao.doAlvo(Maldicao.Alvo.ARMA);
            cajado.set(ModComponentes.MALDICAO.get(), maldicoes.get(jogador.getRandom().nextInt(maldicoes.size())).id());
            cajado.remove(ModComponentes.MALDICAO_CONHECIDA.get());
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arma_amaldicoada").withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        varinha.shrink(1);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.cajado.colocou", Component.translatable(nova.tipo().chaveDoNome()))
                .withStyle(ChatFormatting.AQUA), false);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 20, 0.5);
    }

    // ---- Aparência ----

    // A barrinha embaixo do ícone mostra as cargas (como o "3/4" do Shattered)
    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Cargas.larguraDaBarra(pilha, cargasMaximas(pilha));
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return COR_DA_BARRA;
    }

    // Mudar as cargas não faz o cajado "piscar" na mão
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack depois, boolean trocouDeEspaco) {
        return trocouDeEspaco || !ItemStack.isSameItem(antes, depois);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDeVarinha tipo = varinha(pilha);
        linhas.add(Component.translatable("dica.pixeldungeon.cajado.varinha", Component.translatable(tipo.chaveDoNome())).withStyle(ChatFormatting.AQUA));
        ItemVarinha.efeito(tipo, NivelDoItem.nivel(pilha), cargas(pilha), linhas);
        linhas.add(Component.translatable("dica.pixeldungeon.cargas", cargas(pilha), cargasMaximas(pilha)).withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(pilha, contexto, linhas, opcoes);
    }
}
