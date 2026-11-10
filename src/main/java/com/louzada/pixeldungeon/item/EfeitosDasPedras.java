package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.bloco.ArmadilhaBloco;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// O que cada pedra rúnica faz (as "Runestone" do Shattered).
// As de arremesso fazem efeito no quadradinho onde caem (1 quadradinho = 4x4 blocos):
//   agressão: quem estiver ali vira o alvo de todos os inimigos que o virem, por 20 turnos (chefes não)
//   medo: quem estiver ali (menos o herói e os aliados) fica aterrorizado por 20 turnos e foge
//   sono profundo: o inimigo ali dorme um sono mágico, até alguém machucar ele
//   explosão: explode igual a uma bomba (nos 3x3 quadradinhos em volta): fere todo mundo, até o herói,
//             queima a grama e destrói os itens comuns do chão (as poções quebram)
//   teletransporte: o herói vai pra onde ela caiu (se acertou alguém, pro quadradinho de antes)
//   clarividência: revela as portas e as armadilhas escondidas até 20 quadradinhos dali, até através das paredes
//   rebanho: ovelhas mágicas em todos os quadradinhos livres até 2 de distância, por 8 turnos
//   choque: paralisa por 1 turno todo mundo até 2 quadradinhos dali (até o herói) e dá 1 carga + 1 por atingido
//           às varinhas de quem arremessou
// Depois do efeito, se não tem ninguém ali, a pedra aperta a armadilha à vista do quadradinho (igual a um item caindo).
// As de usar num item abrem a escolha do item (e as que precisam, uma janelinha de opções):
//   encantamento: arma ou armadura ganha um encantamento (ou glifo) sorteado, diferente do que tinha;
//                 no lugar da maldição, se tiver
//   detecção mágica: diz se o item (sem identificar) tem magia boa (nível ou encantamento) e/ou maldição
//   alteração: escolhe velocidade ou dano (armas), evasão ou defesa (armaduras), ou tirar a alteração (ver Alteracao)
//   intuição: palpite do tipo de uma poção ou pergaminho (comum ou exótico) ou anel desconhecido; se acertar, o tipo fica conhecido.
//             Igual ao Shattered, cada pedra serve pra dois palpites (a primeira vez não gasta a pedra).
public class EfeitosDasPedras {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    public static final int TURNOS_DA_AGRESSAO = 20;
    public static final int TURNOS_DO_MEDO = 20;
    public static final int DISTANCIA_DA_CLARIVIDENCIA = 20;
    // No estado do herói: já usou uma pedra de intuição (a próxima vez gasta)
    public static final String INTUICAO_USADA = "intuicao_usada";

    // ---- Arremessadas ----

    // A pedra caiu em "lugar" (antes: o ponto de onde a de teletransporte leva; atingido: em quem ela bateu)
    public static void cair(ServerLevel mundo, TipoDePedra tipo, Vec3 lugar, Vec3 antes, @Nullable Entity atingido, @Nullable Entity dono) {
        BlockPos controle = quadradinhoEm(mundo, lugar);
        LivingEntity alvo = atingido instanceof LivingEntity vivo ? vivo : quemEsta(mundo, controle, dono);
        switch (tipo) {
            case AGRESSAO -> agredir(mundo, alvo, controle);
            case MEDO -> amedrontar(mundo, alvo, controle);
            case SONO_PROFUNDO -> adormecer(mundo, alvo, controle);
            case EXPLOSAO -> explodir(mundo, controle, dono);
            case TELETRANSPORTE -> teletransportar(mundo, dono, antes);
            case CLARIVIDENCIA -> clarividencia(mundo, lugar, dono);
            case REBANHO -> rebanho(mundo, controle);
            case CHOQUE -> chocar(mundo, controle, dono);
            default -> {
            }
        }
        // Igual a um item caindo: aperta a armadilha à vista (as escondidas, só pisando)
        if (EfeitosDasArmadilhas.criaturasEmVolta(mundo, controle, 0).isEmpty()) {
            BlockState armadilha = Armadilhas.estado(mundo, controle);
            if (armadilha != null && !armadilha.getValue(ArmadilhaBloco.ESCONDIDA)) {
                Armadilhas.acionar(mundo, controle, null);
            }
        }
    }

    // O quadradinho onde fica esse ponto (o bloco de chão do canto noroeste, o "controle" das armadilhas).
    // Num andar da run segue a grade do andar; fora dele (a plataforma dos testes), a grade de 4 em 4 do mundo.
    public static BlockPos quadradinhoEm(ServerLevel mundo, Vec3 lugar) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        Integer andar = run.ativa() ? PopulacaoDoAndar.andarEm(run, lugar.x, lugar.y, lugar.z) : null;
        if (andar != null) {
            BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
            return Armadilhas.controle(origem, Math.floorDiv(Mth.floor(lugar.x) - origem.getX(), T), Math.floorDiv(Mth.floor(lugar.z) - origem.getZ(), T));
        }
        BlockPos pe = BlockPos.containing(lugar);
        for (int i = 0; i < 4 && mundo.getBlockState(pe.below()).getCollisionShape(mundo, pe.below()).isEmpty(); i++) {
            pe = pe.below();
        }
        return new BlockPos(Math.floorDiv(pe.getX(), T) * T, pe.getY() - 1, Math.floorDiv(pe.getZ(), T) * T);
    }

    // Quem está no quadradinho (o mais perto do meio; quem arremessou não conta)
    @Nullable
    private static LivingEntity quemEsta(ServerLevel mundo, BlockPos controle, @Nullable Entity dono) {
        Vec3 meio = Armadilhas.centro(controle);
        LivingEntity maisPerto = null;
        for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, controle, 0)) {
            if (criatura != dono && (maisPerto == null || criatura.distanceToSqr(meio) < maisPerto.distanceToSqr(meio))) {
                maisPerto = criatura;
            }
        }
        return maisPerto;
    }

    private static void agredir(ServerLevel mundo, @Nullable LivingEntity alvo, BlockPos controle) {
        if (alvo != null && !(alvo instanceof MobDoShattered inimigo && inimigo.chefe())) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.AGRESSAO, TURNOS_DA_AGRESSAO * Turno.TICKS));
            Particulas.subindoEmVolta(alvo, ParticleTypes.ANGRY_VILLAGER, 6, 0.0);
        }
        Vec3 meio = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void amedrontar(ServerLevel mundo, @Nullable LivingEntity alvo, BlockPos controle) {
        if (alvo instanceof MobDoShattered inimigo && !inimigo.aliado()) {
            inimigo.acordar();
            inimigo.addEffect(new MobEffectInstance(ModEfeitos.ATERRORIZADO, TURNOS_DO_MEDO * Turno.TICKS));
            inimigo.setTarget(null);
        }
        Vec3 meio = Armadilhas.centro(controle);
        Particulas.explosao(mundo, Particulas.poeira(0xFF0000, 1.5F), meio.x, meio.y + 0.8, meio.z, 30, 1.2, 0.05);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    private static void adormecer(ServerLevel mundo, @Nullable LivingEntity alvo, BlockPos controle) {
        if (alvo instanceof MobDoShattered inimigo) {
            inimigo.dormirProfundamente();
            Particulas.subindoEmVolta(inimigo, ParticleTypes.NOTE, 5, 0.0);
        }
        Vec3 meio = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // Igual à bomba do Shattered: dano de (4 + andar) a (12 + 3 x andar), menos a armadura, em todo mundo nos 3x3 quadradinhos
    public static void explodir(ServerLevel mundo, BlockPos controle, @Nullable Entity dono) {
        Vec3 meio = Armadilhas.centro(controle);
        int andar = andarDe(mundo, meio);
        long semente = Identificacao.sementeDaRun(mundo);
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 1)) {
            Vec3 centro = Armadilhas.centro(quadradinho);
            mundo.sendParticles(ParticleTypes.LARGE_SMOKE, centro.x, centro.y + 0.5, centro.z, 6, 1.0, 0.4, 1.0, 0.02);
            queimarGrama(mundo, quadradinho);
            for (ItemEntity item : EfeitosDasArmadilhas.itensEmVolta(mundo, quadradinho, 0)) {
                ItemStack pilha = item.getItem();
                if (!aguentaExplosao(pilha, semente)) {
                    if (pilha.getItem() instanceof ItemPocao pocao) {
                        EfeitosDePocao.quebrar(mundo, item.position(), IdentidadesDaRun.tipoDaCor(semente, pocao.cor()), pocao.cor(), dono);
                    }
                    item.discard();
                }
            }
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                if (criatura instanceof OvelhaMagica) {
                    continue;
                }
                int dano = Sorteio.normal(mundo.getRandom(), 4 + andar, 12 + 3 * andar) - EfeitosDasArmadilhas.armadura(criatura);
                if (dano > 0) {
                    Combate.aplicar(criatura, dano, mundo.damageSources().explosion(null, dono));
                }
            }
        }
        mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, meio.x, meio.y + 0.5, meio.z, 1, 0, 0, 0, 0);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    // Igual ao Shattered: os itens únicos e os equipamentos aguentam a explosão (as chaves, a poção de força,
    // o pergaminho de melhoria, a pedra de encantamento, o selo, o odre, os artefatos...)
    private static boolean aguentaExplosao(ItemStack pilha, long semente) {
        if (pilha.getItem() instanceof Equipamento || pilha.getItem() instanceof ArtefatoAchado || pilha.getItem() instanceof ChaveDeFerro
                || pilha.getItem() instanceof SeloQuebrado || pilha.getItem() instanceof Odre || pilha.getItem() instanceof ArcoEspiritual
                || pilha.getItem() instanceof TomoSagrado || pilha.getItem() instanceof CapaDasSombras) {
            return true;
        }
        if (pilha.getItem() instanceof ItemPedraRunica pedra) {
            return pedra.tipo() == TipoDePedra.ENCANTAMENTO;
        }
        return Identificacao.tipoDaPocao(semente, pilha) == TipoDePocao.FORCA
                || Identificacao.tipoDoPergaminho(semente, pilha) == TipoDePergaminho.MELHORIA;
    }

    // A grama (alta e baixa) do quadradinho pega fogo e some
    private static void queimarGrama(ServerLevel mundo, BlockPos controle) {
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos posicao = controle.offset(dx, dy, dz);
                    BlockState estado = mundo.getBlockState(posicao);
                    if (estado.is(ModBlocos.CAPIM_ALTO.get()) || estado.is(ModBlocos.GRAMA_BAIXA.get())) {
                        mundo.setBlock(posicao, Blocks.AIR.defaultBlockState(), 3);
                        mundo.sendParticles(ParticleTypes.FLAME, posicao.getX() + 0.5, posicao.getY() + 0.3, posicao.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
                    }
                }
            }
        }
    }

    private static int andarDe(ServerLevel mundo, Vec3 lugar) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        Integer andar = run.ativa() ? PopulacaoDoAndar.andarEm(run, lugar.x, lugar.y, lugar.z) : null;
        return andar == null ? 1 : Math.max(1, andar);
    }

    // Leva quem arremessou pro quadradinho onde a pedra caiu. Devolve pra onde foi (null se não deu).
    @Nullable
    public static BlockPos teletransportar(ServerLevel mundo, @Nullable Entity dono, Vec3 antes) {
        if (!(dono instanceof LivingEntity quem) || !quem.isAlive()) {
            return null;
        }
        BlockPos controle = quadradinhoEm(mundo, antes);
        BlockPos destino;
        if (EfeitosDasArmadilhas.quadradinhoLivre(mundo, controle)) {
            destino = BlockPos.containing(Armadilhas.centro(controle));
        } else {
            destino = BlockPos.containing(antes);
            for (int i = 0; i < 3 && mundo.getBlockState(destino.below()).getCollisionShape(mundo, destino.below()).isEmpty(); i++) {
                destino = destino.below();
            }
        }
        Teleporte.para(mundo, quem, destino);
        return destino;
    }

    private static void clarividencia(ServerLevel mundo, Vec3 lugar, @Nullable Entity dono) {
        int achou = Busca.revelarEmVolta(mundo, lugar, DISTANCIA_DA_CLARIVIDENCIA);
        Particulas.explosao(mundo, ParticleTypes.END_ROD, lugar.x, lugar.y + 1, lugar.z, 40, 3.0, 0.05);
        mundo.playSound(null, BlockPos.containing(lugar), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.6F);
        if (dono instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable(achou > 0 ? "efeito.pixeldungeon.clarividencia" : "efeito.pixeldungeon.clarividencia.nada",
                    achou).withStyle(ChatFormatting.AQUA), true);
        }
    }

    // Uma ovelha em cada quadradinho livre (sem ninguém) até 2 passos dali, por 8 turnos (mais ou menos 2)
    public static int rebanho(ServerLevel mundo, BlockPos controle) {
        int ovelhas = 0;
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 2)) {
            if (!EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0).isEmpty()) {
                continue;
            }
            OvelhaMagica ovelha = OvelhaMagica.criar(mundo, Armadilhas.centro(quadradinho), 8);
            ovelhas++;
            if (!quadradinho.equals(controle)) {
                Armadilhas.acionarSeTiver(mundo, quadradinho, ovelha);
            }
        }
        Vec3 meio = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        return ovelhas;
    }

    // Devolve quantos foram atingidos
    public static int chocar(ServerLevel mundo, BlockPos controle, @Nullable Entity dono) {
        int atingidos = 0;
        Vec3 meio = Armadilhas.centro(controle);
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 2)) {
            for (LivingEntity criatura : EfeitosDasArmadilhas.criaturasEmVolta(mundo, quadradinho, 0)) {
                MobEffectInstance antes = criatura.getEffect(ModEfeitos.PARALISADO);
                if (antes == null || antes.getDuration() < Turno.TICKS) {
                    criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.TICKS));
                }
                Particulas.explosao(mundo, ParticleTypes.ELECTRIC_SPARK, criatura.getX(), criatura.getY(0.5), criatura.getZ(), 12, 0.4, 0.1);
                atingidos++;
            }
        }
        Particulas.explosao(mundo, ParticleTypes.ELECTRIC_SPARK, meio.x, meio.y + 0.3, meio.z, 20, 0.8, 0.1);
        mundo.playSound(null, BlockPos.containing(meio), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
        if (atingidos > 0 && dono instanceof ServerPlayer jogador) {
            Glifo.carregarVarinhas(jogador, 1 + atingidos);
            Particulas.subindoEmVolta(jogador, ParticleTypes.ELECTRIC_SPARK, 10, 0.1);
        }
        return atingidos;
    }

    // ---- As de usar num item ----

    // Clique direito com a pedra: abre a escolha do item
    public static void usar(ServerPlayer jogador, TipoDePedra tipo) {
        Predicate<ItemStack> serve = switch (tipo) {
            case ENCANTAMENTO, ALTERACAO -> EfeitosDasPedras::encantavel;
            case DETECCAO_MAGICA -> EfeitosDasPedras::detectavel;
            case INTUICAO -> pilha -> !tiposDesconhecidos(jogador, pilha).isEmpty();
            default -> pilha -> false;
        };
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, tipo.nomeDoItem(), serve, (quem, espaco) -> escolheuItem(quem, tipo, espaco));
        if (!abriu) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pedra_sem_item").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // Arma (de perto ou de arremesso) ou armadura (o "enchantable" do Shattered)
    public static boolean encantavel(ItemStack pilha) {
        return pilha.getItem() instanceof Equipamento equipamento
                && (equipamento.alvoDasMaldicoes() == Maldicao.Alvo.ARMA || equipamento.alvoDasMaldicoes() == Maldicao.Alvo.ARMADURA);
    }

    // Equipamento (arma, armadura, varinha, anel) que ainda tem o que descobrir
    public static boolean detectavel(ItemStack pilha) {
        return NivelDoItem.podeIdentificar(pilha) && (!NivelDoItem.identificado(pilha) || !NivelDoItem.maldicaoConhecida(pilha));
    }

    private static void escolheuItem(ServerPlayer jogador, TipoDePedra tipo, int espaco) {
        ItemStack pilha = jogador.getInventory().getItem(espaco);
        switch (tipo) {
            case ENCANTAMENTO -> {
                if (gastar(jogador, tipo)) {
                    encantar(jogador, pilha);
                }
            }
            case DETECCAO_MAGICA -> {
                if (gastar(jogador, tipo)) {
                    detectar(jogador, pilha);
                }
            }
            case ALTERACAO -> perguntarAlteracao(jogador, espaco);
            case INTUICAO -> perguntarPalpite(jogador, espaco);
            default -> {
            }
        }
    }

    // Um encantamento (ou glifo) sorteado, diferente do que o item já tinha. A maldição, se tiver, dá lugar a ele.
    public static void encantar(ServerPlayer jogador, ItemStack pilha) {
        Random sorteio = new Random(jogador.getRandom().nextLong());
        if (NivelDoItem.amaldicoado(pilha)) {
            NivelDoItem.removerMaldicao(pilha);
        }
        boolean armadura = NivelDoItem.ehArmadura(pilha);
        if (armadura) {
            Glifo atual = NivelDoItem.glifo(pilha);
            Glifo novo = Glifo.sortear(sorteio);
            while (novo == atual) {
                novo = Glifo.sortear(sorteio);
            }
            NivelDoItem.definirGlifo(pilha, novo);
        } else {
            Encantamento atual = NivelDoItem.encantamento(pilha);
            Encantamento novo = Encantamento.sortear(sorteio);
            while (novo == atual) {
                novo = Encantamento.sortear(sorteio);
            }
            NivelDoItem.definirEncantamento(pilha, novo);
        }
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 30, 0.2);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.displayClientMessage(Component.translatable(armadura ? "mensagem.pixeldungeon.pedra_encantamento.armadura"
                : "mensagem.pixeldungeon.pedra_encantamento.arma").withStyle(ChatFormatting.GREEN), false);
    }

    // Descobre se é amaldiçoado e diz se tem magia boa (nível ou encantamento) e/ou ruim (maldição)
    public static void detectar(ServerPlayer jogador, ItemStack pilha) {
        pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        boolean ruim = NivelDoItem.amaldicoado(pilha);
        boolean boa = NivelDoItem.nivel(pilha) > 0 || NivelDoItem.encantado(pilha);
        String chave;
        ChatFormatting cor;
        if (boa && ruim) {
            chave = "ambas";
            cor = ChatFormatting.GOLD;
        } else if (boa) {
            chave = "boa";
            cor = ChatFormatting.GREEN;
        } else if (ruim) {
            chave = "ruim";
            cor = ChatFormatting.RED;
        } else {
            chave = "nenhuma";
            cor = ChatFormatting.GRAY;
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pedra_deteccao_magica." + chave).withStyle(cor), false);
    }

    // ---- Alteração ----

    // As alterações que dá pra escolher pra esse item (null = tirar a que tem)
    public static List<Alteracao> opcoesDeAlteracao(ItemStack pilha) {
        List<Alteracao> opcoes = new ArrayList<>();
        Alteracao atual = Alteracao.de(pilha);
        boolean armadura = NivelDoItem.ehArmadura(pilha);
        for (Alteracao alteracao : Alteracao.values()) {
            if (alteracao.daArma() != armadura && alteracao != atual) {
                opcoes.add(alteracao);
            }
        }
        if (atual != null) {
            opcoes.add(null);
        }
        return opcoes;
    }

    private static void perguntarAlteracao(ServerPlayer jogador, int espaco) {
        ItemStack pilha = jogador.getInventory().getItem(espaco);
        List<Alteracao> opcoes = opcoesDeAlteracao(pilha);
        List<Component> botoes = new ArrayList<>();
        for (Alteracao alteracao : opcoes) {
            botoes.add(Component.translatable(alteracao == null ? "item.pixeldungeon.pedra_alteracao.remover" : alteracao.chaveDoBotao()));
        }
        Component texto = Component.translatable("item.pixeldungeon.pedra_alteracao.escolha");
        if (Alteracao.de(pilha) != null) {
            texto = Component.translatable("item.pixeldungeon.pedra_alteracao.escolha_ja_alterado");
        }
        ItemStack copia = pilha.copy();
        EscolhaDeOpcao.pedir(jogador, pilha.getHoverName(), texto, pilha.copyWithCount(1), botoes, (quem, indice) -> {
            ItemStack agora = quem.getInventory().getItem(espaco);
            if (indice < 0 || !ItemStack.isSameItemSameComponents(agora, copia) || !gastar(quem, TipoDePedra.ALTERACAO)) {
                return;
            }
            alterar(quem, agora, opcoes.get(indice));
        });
    }

    public static void alterar(ServerPlayer jogador, ItemStack pilha, @Nullable Alteracao alteracao) {
        Alteracao.definir(pilha, alteracao);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 20, 0.2);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---- Intuição ----

    // Os tipos que o herói ainda não conhece, do mesmo jeito de item (vazio: o item já é conhecido, ou não é poção, pergaminho nem anel)
    public static List<Object> tiposDesconhecidos(ServerPlayer jogador, ItemStack pilha) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        List<Object> tipos = new ArrayList<>();
        if (pilha.getItem() instanceof ItemPocao pocao && !Identificacao.conhece(jogador, pocao.cor())) {
            for (TipoDePocao tipo : TipoDePocao.values()) {
                if (!dados.conhece(tipo.chaveDeConhecimento())) {
                    tipos.add(tipo);
                }
            }
        } else if (pilha.getItem() instanceof ItemPocaoExotica exotica && !Identificacao.conhece(jogador, exotica.cor())) {
            // A exótica: os palpites são as exóticas das poções desconhecidas
            for (TipoDePocao tipo : TipoDePocao.values()) {
                if (!dados.conhece(tipo.chaveDeConhecimento())) {
                    tipos.add(TipoDePocaoExotica.doBase(tipo));
                }
            }
        } else if (pilha.getItem() instanceof ItemPergaminho pergaminho && !Identificacao.conhece(jogador, pergaminho.runa())) {
            for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
                if (!dados.conhece(tipo.chaveDeConhecimento())) {
                    tipos.add(tipo);
                }
            }
        } else if (pilha.getItem() instanceof ItemPergaminhoExotico exotico && !Identificacao.conhece(jogador, exotico.runa())) {
            for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
                if (!dados.conhece(tipo.chaveDeConhecimento())) {
                    tipos.add(TipoDePergaminhoExotico.doBase(tipo));
                }
            }
        } else if (pilha.getItem() instanceof ItemAnel anel && !Identificacao.conhece(jogador, anel.gema())) {
            for (TipoDeAnel tipo : TipoDeAnel.values()) {
                if (!dados.conhece(tipo.chaveDeConhecimento())) {
                    tipos.add(tipo);
                }
            }
        }
        return tipos;
    }

    private static String chaveDoNome(Object tipo) {
        if (tipo instanceof TipoDePocao pocao) {
            return pocao.chaveDoNome();
        }
        if (tipo instanceof TipoDePergaminho pergaminho) {
            return pergaminho.chaveDoNome();
        }
        if (tipo instanceof TipoDePocaoExotica exotica) {
            return exotica.chaveDoNome();
        }
        if (tipo instanceof TipoDePergaminhoExotico exotico) {
            return exotico.chaveDoNome();
        }
        return ((TipoDeAnel) tipo).chaveDoNome();
    }

    // O tipo de verdade do item
    @Nullable
    private static Object tipoDe(ServerPlayer jogador, ItemStack pilha) {
        if (pilha.getItem() instanceof ItemPocao pocao) {
            return Identificacao.tipoDa(jogador, pocao.cor());
        }
        if (pilha.getItem() instanceof ItemPergaminho pergaminho) {
            return Identificacao.tipoDa(jogador, pergaminho.runa());
        }
        if (pilha.getItem() instanceof ItemPocaoExotica exotica) {
            return TipoDePocaoExotica.daCor(jogador, exotica.cor());
        }
        if (pilha.getItem() instanceof ItemPergaminhoExotico exotico) {
            return TipoDePergaminhoExotico.daRuna(jogador, exotico.runa());
        }
        if (pilha.getItem() instanceof ItemAnel anel) {
            return Identificacao.tipoDa(jogador, anel.gema());
        }
        return null;
    }

    private static void perguntarPalpite(ServerPlayer jogador, int espaco) {
        ItemStack pilha = jogador.getInventory().getItem(espaco);
        List<Object> tipos = tiposDesconhecidos(jogador, pilha);
        List<Component> botoes = new ArrayList<>();
        for (Object tipo : tipos) {
            botoes.add(Component.translatable(chaveDoNome(tipo)));
        }
        Item item = pilha.getItem();
        EscolhaDeOpcao.pedir(jogador, pilha.getHoverName(), Component.translatable("item.pixeldungeon.pedra_intuicao.palpite"), pilha.copyWithCount(1),
                botoes, (quem, indice) -> {
                    ItemStack agora = quem.getInventory().getItem(espaco);
                    if (indice < 0 || !agora.is(item)) {
                        return;
                    }
                    palpitar(quem, agora, tipos.get(indice));
                });
    }

    // Dá o palpite; devolve se acertou. A primeira vez não gasta a pedra; a segunda gasta.
    public static boolean palpitar(ServerPlayer jogador, ItemStack pilha, Object palpite) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.inteiro(INTUICAO_USADA) > 0) {
            if (!gastar(jogador, TipoDePedra.INTUICAO)) {
                return false;
            }
            dados.definir(INTUICAO_USADA, 0);
        } else {
            dados.definir(INTUICAO_USADA, 1);
        }
        boolean acertou = palpite.equals(tipoDe(jogador, pilha));
        if (acertou) {
            Identificacao.identificarItem(jogador, pilha);
            Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 12, 0.05);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else {
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        jogador.displayClientMessage(Component.translatable(acertou ? "mensagem.pixeldungeon.pedra_intuicao.certo" : "mensagem.pixeldungeon.pedra_intuicao.errado")
                .withStyle(acertou ? ChatFormatting.AQUA : ChatFormatting.GRAY), false);
        return acertou;
    }

    // ---- Ajudantes ----

    // Tira uma pedra desse tipo do inventário (no criativo, não tira)
    public static boolean gastar(Player jogador, TipoDePedra tipo) {
        if (jogador.getAbilities().instabuild) {
            return true;
        }
        Inventory inventario = jogador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            if (inventario.getItem(i).getItem() instanceof ItemPedraRunica pedra && pedra.tipo() == tipo) {
                inventario.getItem(i).shrink(1);
                return true;
            }
        }
        return false;
    }
}
