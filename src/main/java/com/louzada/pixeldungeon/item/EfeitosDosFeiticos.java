package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.alquimia.ReceitasDaAlquimia;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Mira;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// O que cada feitiço do Shattered faz (ver TipoDeFeitico).
//   instável: um pergaminho qualquer (com inimigos à vista, um de luta; sem, um de explorar), sem identificar
//   alquimizar: escolhe um item e transforma em energia alquímica (sem precisar do pote)
//   infusão mágica: escolhe um equipamento e aprimora (+1) sem perder o encantamento ou o glifo (a maldição continua)
//   mudança de fase: o alvo na mira é teletransportado e fica paralisado por 10 turnos (menos chefes); sem alvo, não gasta
//   reciclar: escolhe uma poção, pergaminho, semente ou pedra rúnica e troca por outra do mesmo tipo
//   farol de retorno: marca o lugar; depois, volta pra ele de qualquer andar (só voltar gasta o feitiço)
//   agarrão telecinético: pega os itens do quadradinho na mira, ou as armas cravadas no inimigo na mira; sem nada, não gasta
public class EfeitosDosFeiticos {
    private static final double ALCANCE = 32;
    public static final String FAROL = "farol_de_retorno";
    private static final String FAROL_X = "farol_x";
    private static final String FAROL_Y = "farol_y";
    private static final String FAROL_Z = "farol_z";
    private static final String FAROL_ANDAR = "farol_andar";
    private static final String FAROL_RUN = "farol_run";

    // Os pergaminhos do feitiço instável e as chances (as do Shattered)
    private static final TipoDePergaminho[] PERGAMINHOS = { TipoDePergaminho.IDENTIFICACAO, TipoDePergaminho.REMOVER_MALDICAO,
            TipoDePergaminho.MAPEAMENTO_MAGICO, TipoDePergaminho.IMAGEM_ESPELHADA, TipoDePergaminho.RECARGA, TipoDePergaminho.CANCAO_DE_NINAR,
            TipoDePergaminho.RETRIBUICAO, TipoDePergaminho.FURIA, TipoDePergaminho.TELETRANSPORTE, TipoDePergaminho.TERROR,
            TipoDePergaminho.TRANSMUTACAO };
    private static final float[] CHANCES = { 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1 };

    // Lança o feitiço. Devolve true se ele já se gastou (os que pedem item se gastam depois, quando o item for escolhido).
    public static boolean lancar(ServerPlayer jogador, TipoDeFeitico tipo) {
        switch (tipo) {
            case INSTAVEL -> {
                instavel(jogador);
                return true;
            }
            case ALQUIMIZAR -> {
                return pedirItem(jogador, tipo, pilha -> !(pilha.getItem() instanceof ItemFeitico feitico && feitico.tipo() == TipoDeFeitico.ALQUIMIZAR)
                        && ReceitasDaAlquimia.energia(jogador, pilha) > 0, EfeitosDosFeiticos::alquimizar);
            }
            case INFUSAO_MAGICA -> {
                return pedirItem(jogador, tipo, NivelDoItem::podeMelhorar, EfeitosDosFeiticos::infundir);
            }
            case RECICLAR -> {
                return pedirItem(jogador, tipo, EfeitosDosFeiticos::reciclavel, EfeitosDosFeiticos::reciclar);
            }
            case MUDANCA_DE_FASE -> {
                return mudancaDeFase(jogador, Mira.inimigo(jogador, ALCANCE));
            }
            case FAROL_DE_RETORNO -> {
                return farol(jogador);
            }
            case AGARRAO_TELECINETICO -> {
                return agarrar(jogador, Mira.inimigo(jogador, ALCANCE), Mira.ponto(jogador, ALCANCE).subtract(jogador.getLookAngle().scale(0.2))) > 0;
            }
        }
        return false;
    }

    private static void avisar(ServerPlayer jogador, String chave, ChatFormatting cor, Object... numeros) {
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.feitico." + chave, numeros).withStyle(cor), true);
    }

    // ---- Os que pedem um item ----

    // Abre a escolha do item; quando ele for escolhido, faz o efeito e gasta um feitiço desses do inventário.
    // Sem nenhum item que sirva, avisa e não gasta.
    private static boolean pedirItem(ServerPlayer jogador, TipoDeFeitico tipo, java.util.function.Predicate<ItemStack> serve,
            java.util.function.BiPredicate<ServerPlayer, ItemStack> efeito) {
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, tipo.nomeDoItem(), serve, (quem, espaco) -> {
            ItemStack escolhido = quem.getInventory().getItem(espaco);
            if (efeito.test(quem, escolhido)) {
                gastarUm(quem, tipo);
            }
        });
        if (!abriu) {
            avisar(jogador, "sem_item", ChatFormatting.GRAY);
        }
        return false;
    }

    private static void gastarUm(ServerPlayer jogador, TipoDeFeitico tipo) {
        if (jogador.getAbilities().instabuild) {
            return;
        }
        Item feitico = ModItens.feitico(tipo);
        for (int i = 0; i < jogador.getInventory().getContainerSize(); i++) {
            ItemStack pilha = jogador.getInventory().getItem(i);
            if (pilha.is(feitico)) {
                pilha.shrink(1);
                return;
            }
        }
    }

    // Alquimizar: a pilha inteira vira energia (igual a energizar no pote)
    public static boolean alquimizar(ServerPlayer jogador, ItemStack pilha) {
        int energia = ReceitasDaAlquimia.energizar(jogador, pilha);
        if (energia <= 0) {
            return false;
        }
        EnergiaAlquimica.ganhar(jogador, energia);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // Infusão mágica
    public static boolean infundir(ServerPlayer jogador, ItemStack pilha) {
        if (!NivelDoItem.podeMelhorar(pilha)) {
            return false;
        }
        NivelDoItem.infundir(pilha);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 30, 0.3);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        avisar(jogador, "infundiu", ChatFormatting.GREEN);
        return true;
    }

    public static boolean reciclavel(ItemStack pilha) {
        return pilha.getItem() instanceof ItemPocao || pilha.getItem() instanceof ItemPocaoExotica || pilha.getItem() instanceof ItemPergaminho
                || pilha.getItem() instanceof ItemPergaminhoExotico || pilha.getItem() instanceof ItemSemente || pilha.getItem() instanceof ItemPedraRunica;
    }

    // Reciclar: troca um por outro do mesmo tipo (diferente dele). Os exóticos continuam exóticos.
    public static boolean reciclar(ServerPlayer jogador, ItemStack pilha) {
        if (!reciclavel(pilha)) {
            return false;
        }
        long semente = Identificacao.sementeDaRun(jogador);
        Random sorteio = new Random(jogador.getRandom().nextLong());
        ItemStack novo;
        do {
            if (pilha.getItem() instanceof ItemPocao || pilha.getItem() instanceof ItemPocaoExotica) {
                novo = SaqueDoAndar.pocaoAleatoria(sorteio, semente);
                if (pilha.getItem() instanceof ItemPocaoExotica) {
                    novo = new ItemStack(ItemPocaoExotica.daCor(((ItemPocao) novo.getItem()).cor()));
                }
            } else if (pilha.getItem() instanceof ItemPergaminho || pilha.getItem() instanceof ItemPergaminhoExotico) {
                novo = SaqueDoAndar.pergaminhoAleatorio(sorteio, semente);
                if (pilha.getItem() instanceof ItemPergaminhoExotico) {
                    novo = new ItemStack(ItemPergaminhoExotico.daRuna(((ItemPergaminho) novo.getItem()).runa()));
                }
            } else if (pilha.getItem() instanceof ItemSemente) {
                novo = SaqueDoAndar.sementeAleatoria(sorteio);
            } else {
                novo = SaqueDoAndar.pedraAleatoria(sorteio);
            }
        } while (novo.getItem() == pilha.getItem());
        pilha.shrink(1);
        Component nome = novo.getHoverName();
        if (!jogador.getInventory().add(novo)) {
            jogador.drop(novo, false);
        }
        Particulas.subindoEmVolta(jogador, ParticleTypes.PORTAL, 20, 0.3);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        avisar(jogador, "reciclou", ChatFormatting.GREEN, nome);
        return true;
    }

    // ---- Instável ----

    public static TipoDePergaminho instavel(ServerPlayer jogador) {
        boolean emLuta = !EfeitosDePergaminho.visiveis(jogador).isEmpty();
        TipoDePergaminho tipo;
        do {
            tipo = sortear(jogador.getRandom().nextFloat());
        } while (emLuta ? !deLuta(tipo) : !deExplorar(tipo));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (tipo.escolheItem()) {
            if (!EscolhaDeItem.pedir(jogador, tipo, null, false)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_sem_item").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            EfeitosDePergaminho.ler(jogador, tipo);
        }
        return tipo;
    }

    private static TipoDePergaminho sortear(float sorteio) {
        float total = 0;
        for (float chance : CHANCES) {
            total += chance;
        }
        float escolha = sorteio * total;
        for (int i = 0; i < CHANCES.length; i++) {
            if (escolha < CHANCES[i]) {
                return PERGAMINHOS[i];
            }
            escolha -= CHANCES[i];
        }
        return PERGAMINHOS[PERGAMINHOS.length - 1];
    }

    public static boolean deLuta(TipoDePergaminho tipo) {
        return tipo == TipoDePergaminho.IMAGEM_ESPELHADA || tipo == TipoDePergaminho.RECARGA || tipo == TipoDePergaminho.CANCAO_DE_NINAR
                || tipo == TipoDePergaminho.RETRIBUICAO || tipo == TipoDePergaminho.FURIA || tipo == TipoDePergaminho.TELETRANSPORTE
                || tipo == TipoDePergaminho.TERROR;
    }

    public static boolean deExplorar(TipoDePergaminho tipo) {
        return tipo == TipoDePergaminho.IDENTIFICACAO || tipo == TipoDePergaminho.REMOVER_MALDICAO || tipo == TipoDePergaminho.MAPEAMENTO_MAGICO
                || tipo == TipoDePergaminho.RECARGA || tipo == TipoDePergaminho.CANCAO_DE_NINAR || tipo == TipoDePergaminho.TELETRANSPORTE
                || tipo == TipoDePergaminho.TRANSMUTACAO;
    }

    // ---- Mudança de fase ----

    public static boolean mudancaDeFase(ServerPlayer jogador, @Nullable LivingEntity alvo) {
        if (alvo == null) {
            avisar(jogador, "sem_alvo", ChatFormatting.GRAY);
            return false;
        }
        if (!Teleporte.paraLugarAleatorio(jogador.serverLevel(), alvo)) {
            return false;
        }
        boolean chefe = alvo instanceof MobDoShattered mob && mob.chefe();
        if (alvo instanceof MobDoShattered mob) {
            mob.setTarget(null);
        }
        if (!chefe) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 10 * Turno.TICKS));
        }
        return true;
    }

    // ---- Farol de retorno ----

    // Sem farol marcado: marca aqui (não gasta). Com farol: pergunta se marca de novo ou se volta (voltar gasta um).
    // Igual ao Shattered, o farol continua marcado depois de voltar (só some quando o último feitiço é usado).
    private static boolean farol(ServerPlayer jogador) {
        if (!temFarol(jogador)) {
            marcarFarol(jogador);
            return false;
        }
        List<Component> opcoes = new ArrayList<>();
        opcoes.add(Component.translatable("tela.pixeldungeon.farol_de_retorno.marcar"));
        opcoes.add(Component.translatable("tela.pixeldungeon.farol_de_retorno.voltar"));
        EscolhaDeOpcao.pedir(jogador, Component.translatable("item.pixeldungeon.feitico_farol_de_retorno"),
                Component.translatable("tela.pixeldungeon.farol_de_retorno", RegrasDoHeroi.dados(jogador).inteiro(FAROL_ANDAR)),
                new ItemStack(ModItens.feitico(TipoDeFeitico.FAROL_DE_RETORNO)), opcoes, (quem, escolha) -> {
                    if (escolha == 0) {
                        marcarFarol(quem);
                    } else if (escolha == 1) {
                        voltarAoFarol(quem);
                    }
                });
        return false;
    }

    public static void marcarFarol(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        BlockPos aqui = jogador.blockPosition();
        int andar = SaqueDoAndar.andarDe(jogador);
        dados.definir(FAROL, true);
        dados.definir(FAROL_X, aqui.getX());
        dados.definir(FAROL_Y, aqui.getY());
        dados.definir(FAROL_Z, aqui.getZ());
        dados.definir(FAROL_ANDAR, andar);
        dados.definir(FAROL_RUN, DadosDaRun.pegar(jogador.server).numeroDaRun());
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 20, 0.1);
        jogador.level().playSound(null, aqui, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
        avisar(jogador, "farol_marcado", ChatFormatting.AQUA, andar);
    }

    // Volta pro farol (e gasta um feitiço). Não dá pra sair de um andar de chefe enquanto ele luta (o andar está selado).
    public static boolean voltarAoFarol(ServerPlayer jogador) {
        if (!temFarol(jogador)) {
            return false;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int andarAqui = SaqueDoAndar.andarDe(jogador);
        if (andarAqui != dados.inteiro(FAROL_ANDAR) && Andares.ehAndarDeChefe(andarAqui)
                && GerenciadorDaRun.chefeLutando(jogador.serverLevel(), andarAqui)) {
            avisar(jogador, "farol_selado", ChatFormatting.RED);
            return false;
        }
        BlockPos destino = new BlockPos(dados.inteiro(FAROL_X), dados.inteiro(FAROL_Y), dados.inteiro(FAROL_Z));
        Teleporte.para(jogador.serverLevel(), jogador, destino);
        gastarUm(jogador, TipoDeFeitico.FAROL_DE_RETORNO);
        if (!temFeitico(jogador, TipoDeFeitico.FAROL_DE_RETORNO)) {
            dados.definir(FAROL, false);
        }
        return true;
    }

    // Tem farol marcado nesta run
    public static boolean temFarol(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        return dados.sim(FAROL) && dados.inteiro(FAROL_RUN) == DadosDaRun.pegar(jogador.server).numeroDaRun();
    }

    private static boolean temFeitico(ServerPlayer jogador, TipoDeFeitico tipo) {
        return jogador.getInventory().contains(new ItemStack(ModItens.feitico(tipo)));
    }

    // ---- Agarrão telecinético ----

    // Devolve quantos itens pegou
    public static int agarrar(ServerPlayer jogador, @Nullable LivingEntity alvo, Vec3 ponto) {
        List<ItemStack> pegos = new ArrayList<>();
        if (alvo != null && ArmaArremessada.espetados(alvo) > 0) {
            pegos.addAll(ArmaArremessada.tirarEspetados(alvo));
        } else {
            ServerLevel mundo = jogador.serverLevel();
            BlockPos controle = EfeitosDasPedras.quadradinhoEm(mundo, ponto);
            for (ItemEntity item : EfeitosDasArmadilhas.itensEmVolta(mundo, controle, 0)) {
                pegos.add(item.getItem().copy());
                item.discard();
            }
            Vec3 meio = Armadilhas.centro(controle);
            mundo.sendParticles(ParticleTypes.END_ROD, meio.x, meio.y + 0.5, meio.z, 12, 1.0, 0.3, 1.0, 0.05);
        }
        if (pegos.isEmpty()) {
            avisar(jogador, "sem_alvo", ChatFormatting.GRAY);
            return 0;
        }
        for (ItemStack pilha : pegos) {
            if (!jogador.getInventory().add(pilha)) {
                jogador.drop(pilha, false);
            }
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        return pegos.size();
    }
}
