package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// O que cada poção exótica do Shattered faz (1 turno = 2 segundos, 1 quadradinho = 4 blocos).
// Beber:
//   maestria: escolhe uma arma ou armadura, que passa a pedir 2 de força a menos (uma vez por item)
//   blindagem: um escudo de 60% da vida máxima + 10 (que vai sumindo aos poucos, igual à blindagem do Guerreiro)
//   visão mágica: por 50 turnos, vê todo mundo até 12 quadradinhos, até através das paredes
//   bafo de dragão: cospe um cone de fogo pra onde está olhando (6 quadradinhos, 60 graus): queima e aleija por 5 turnos
//   vigor: anda 1,5x mais rápido por 100 turnos
//   armadura de barro: pele de pedra de 2 + nível/3 de armadura, que perde 1 a cada 50 turnos
//   limpeza: tira todos os efeitos ruins, mata a fome e deixa imune a efeitos ruins por 5 turnos
//   inspiração divina: +2 pontos de talento num tier escolhido (uma vez por tier)
// Arremessar (e as ruins também quebram em quem bebe):
//   congelamento instantâneo: congela e enraíza (10 turnos) todo mundo nos 3x3 quadradinhos, e a água vira gelo
//   gás corrosivo: uma nuvem de gás que corrói cada vez mais forte (começa em 2 + andar/5)
//   neblina: uma nuvem de fumaça que ninguém enxerga através
//   nuvens tempestuosas: uma nuvem de chuva que apaga o fogo, transforma o chão dos 3x3 quadradinhos em água
//                        e quebra as armadilhas dali
//   limpeza: limpa quem estiver onde ela quebrar
// As outras só espirram.
public class EfeitosDePocaoExotica {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    public static final int TURNOS_DA_VISAO_MAGICA = 50;
    public static final int ALCANCE_DA_VISAO_MAGICA = 12 * T;
    public static final int TURNOS_DO_VIGOR = 100;
    public static final int TURNOS_DA_ARMADURA_DE_BARRO = 50;
    public static final int TURNOS_DA_LIMPEZA = 5;
    // No estado do herói: os tiers que já ganharam os pontos da inspiração divina ("inspiracao_tier_1"...)
    public static final String INSPIRACAO = "inspiracao_tier_";

    // ---- Beber ----

    public static void beber(ServerPlayer jogador, CorDePocao cor) {
        TipoDePocaoExotica tipo = TipoDePocaoExotica.daCor(jogador, cor);
        boolean conhecia = Identificacao.conhece(jogador, cor);
        ServerLevel mundo = jogador.serverLevel();
        // As duas que pedem uma escolha: se a poção já é conhecida, só é gasta quando o herói escolhe
        if (tipo == TipoDePocaoExotica.MAESTRIA) {
            maestria(jogador, cor, conhecia);
            return;
        }
        if (tipo == TipoDePocaoExotica.INSPIRACAO_DIVINA) {
            inspiracao(jogador, cor, conhecia);
            return;
        }
        Identificacao.identificar(jogador, cor);
        gastar(jogador, cor);
        mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.2F);
        TalentosDoTier2.aoUsarPocao(jogador, 1);
        switch (tipo) {
            case BLINDAGEM -> Escudos.darBarreira(jogador, Math.round(0.6F * jogador.getMaxHealth() + 10));
            case VISAO_MAGICA -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.VISAO_MAGICA, TURNOS_DA_VISAO_MAGICA * Turno.TICKS));
                jogador.removeEffect(ModEfeitos.CEGO);
                verAtravesDasParedes(jogador);
                avisar(jogador, "efeito.pixeldungeon.visao_magica", ChatFormatting.AQUA);
            }
            case BAFO_DE_DRAGAO -> bafoDeDragao(jogador);
            case VIGOR -> {
                jogador.addEffect(new MobEffectInstance(ModEfeitos.VIGOR, TURNOS_DO_VIGOR * Turno.TICKS));
                avisar(jogador, "efeito.pixeldungeon.vigor", ChatFormatting.YELLOW);
            }
            case ARMADURA_DE_BARRO -> {
                int nivel = 2 + RegrasDoHeroi.dados(jogador).nivel() / 3;
                jogador.addEffect(new MobEffectInstance(ModEfeitos.ARMADURA_DE_BARRO, TURNOS_DA_ARMADURA_DE_BARRO * Turno.TICKS, nivel - 1));
                Particulas.subindoEmVolta(jogador, Particulas.poeira(0x8A6A3A, 1.5F), 20, 0.0);
                avisar(jogador, "efeito.pixeldungeon.armadura_de_barro", ChatFormatting.GOLD);
            }
            case LIMPEZA -> {
                limpar(jogador);
                avisar(jogador, "efeito.pixeldungeon.limpeza", ChatFormatting.LIGHT_PURPLE);
            }
            // As ruins quebram em volta de quem bebeu
            default -> quebrar(mundo, jogador.position(), tipo, cor, jogador, null);
        }
    }

    // ---- Arremessar ----

    // Quebrou nesse lugar (atingido: em quem bateu). Devolve true se o efeito apareceu (aí quem arremessou descobre o tipo).
    public static boolean quebrar(ServerLevel mundo, Vec3 lugar, TipoDePocaoExotica tipo, CorDePocao cor, @Nullable Entity dono, @Nullable Entity atingido) {
        mundo.playSound(null, BlockPos.containing(lugar), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        Particulas.explosao(mundo, Particulas.poeira(cor.cor(), 1.5F), lugar.x, lugar.y + 0.3, lugar.z, 20, 0.6, 0.05);
        switch (tipo) {
            case CONGELAMENTO_INSTANTANEO -> congelamentoInstantaneo(mundo, lugar);
            case GAS_CORROSIVO -> {
                int andar = Math.max(1, Andares.andarPelaAltura(BlockPos.containing(lugar).getY()));
                NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.CORROSIVO, 6.0F, 8).definirForca(2 + andar / 5);
            }
            case NEBLINA -> NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.NEBLINA, 7.0F, 20);
            case NUVENS_TEMPESTUOSAS -> tempestade(mundo, lugar);
            case LIMPEZA -> {
                LivingEntity alvo = atingido instanceof LivingEntity vivo ? vivo : maisPerto(mundo, lugar);
                if (alvo == null) {
                    return espirrou(dono);
                }
                limpar(alvo);
            }
            default -> {
                return espirrou(dono);
            }
        }
        return true;
    }

    private static boolean espirrou(@Nullable Entity dono) {
        if (dono instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pocao_espirrou").withStyle(ChatFormatting.GRAY), true);
        }
        return false;
    }

    @Nullable
    private static LivingEntity maisPerto(ServerLevel mundo, Vec3 lugar) {
        LivingEntity perto = null;
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(lugar, 3, 3, 3), LivingEntity::isAlive)) {
            if (perto == null || criatura.distanceToSqr(lugar) < perto.distanceToSqr(lugar)) {
                perto = criatura;
            }
        }
        return perto;
    }

    // ---- Cada uma ----

    // Congela (10 turnos, 30 na água) e enraíza (10 turnos) quem estiver nos 3x3 quadradinhos; a água vira gelo e o fogo apaga
    public static void congelamentoInstantaneo(ServerLevel mundo, Vec3 lugar) {
        EfeitosDePocao.congelar(mundo, lugar);
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(lugar, 3 * T, 6, 3 * T))) {
            if (criatura.isSpectator()) {
                continue;
            }
            int turnos = criatura.isInWater() ? 30 : 10;
            criatura.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, Math.max(turnos * Turno.TICKS, duracao(criatura, ModEfeitos.CONGELADO))));
            criatura.addEffect(new MobEffectInstance(ModEfeitos.ENRAIZADO, 10 * Turno.TICKS));
        }
    }

    private static int duracao(LivingEntity criatura, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> efeito) {
        MobEffectInstance atual = criatura.getEffect(efeito);
        return atual == null ? 0 : atual.getDuration();
    }

    // Uma nuvem de chuva: apaga o fogo e o chão dos 3x3 quadradinhos em volta vira água (igual ao Shattered, pra sempre)
    public static void tempestade(ServerLevel mundo, Vec3 lugar) {
        NuvemDeGas.soltar(mundo, lugar, NuvemDeGas.Tipo.TEMPESTADE, 7.0F, 12);
        BlockPos centro = BlockPos.containing(lugar);
        for (BlockPos posicao : BlockPos.betweenClosed(centro.offset(-7, -2, -7), centro.offset(7, 2, 7))) {
            if (mundo.getBlockState(posicao).is(BlockTags.FIRE)) {
                mundo.removeBlock(posicao, false);
            }
        }
        BlockPos controle = EfeitosDasPedras.quadradinhoEm(mundo, lugar);
        for (BlockPos quadradinho : EfeitosDasArmadilhas.quadradinhosAte(mundo, controle, 1)) {
            // As armadilhas são esmagadas (quebram); o chão comum vira água
            if (Armadilhas.estado(mundo, quadradinho) != null) {
                Armadilhas.desarmar(mundo, quadradinho);
            } else {
                virarAgua(mundo, quadradinho);
            }
        }
        mundo.playSound(null, centro, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 1.5F, 1.0F);
    }

    // O chão comum (sem armadilha) de um quadradinho de um andar vira água (a água fica num buraco de 1 bloco, igual às outras)
    public static boolean virarAgua(ServerLevel mundo, BlockPos controle) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        Vec3 meio = Armadilhas.centro(controle);
        Integer andar = run.ativa() ? PopulacaoDoAndar.andarEm(run, meio.x, meio.y, meio.z) : null;
        if (andar == null || Armadilhas.estado(mundo, controle) != null) {
            return false;
        }
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        int qx = Math.floorDiv(controle.getX() - origem.getX(), T);
        int qy = Math.floorDiv(controle.getZ() - origem.getZ(), T);
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        if (planta.tipo(qx, qy) != PlantaDoAndar.CHAO || origem.getY() != controle.getY()) {
            return false;
        }
        BlockState chao = ModBlocos.CHAO_ESGOTO.get().defaultBlockState();
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                BlockPos posicao = controle.offset(dx, 0, dz);
                if (!mundo.getBlockState(posicao).is(ModBlocos.CHAO_ESGOTO.get())) {
                    continue;
                }
                mundo.setBlock(posicao.below(), chao, 2);
                mundo.setBlock(posicao, Blocks.WATER.defaultBlockState(), 3);
                for (int dy = 1; dy <= 2; dy++) {
                    BlockState acima = mundo.getBlockState(posicao.above(dy));
                    if (acima.is(ModBlocos.CAPIM_ALTO.get()) || acima.is(ModBlocos.GRAMA_BAIXA.get()) || acima.is(BlockTags.FIRE)) {
                        mundo.setBlock(posicao.above(dy), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        planta.definir(qx, qy, PlantaDoAndar.AGUA);
        planta.definirGrama(qx, qy, PlantaDoAndar.SEM_GRAMA);
        return true;
    }

    // Todo mundo até 12 quadradinhos brilha (dá pra ver através das paredes) por um turno e meio
    public static void verAtravesDasParedes(ServerPlayer jogador) {
        for (LivingEntity criatura : jogador.level().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(ALCANCE_DA_VISAO_MAGICA),
                criatura -> criatura != jogador && criatura.isAlive())) {
            if (criatura.distanceToSqr(jogador) <= ALCANCE_DA_VISAO_MAGICA * ALCANCE_DA_VISAO_MAGICA) {
                criatura.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, Turno.TICKS * 3 / 2, 0, false, false));
            }
        }
    }

    // Um cone de fogo de 6 quadradinhos e 60 graus pra onde o herói olha
    public static int bafoDeDragao(ServerPlayer jogador) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 olhar = jogador.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0, olhar.z);
        if (frente.lengthSqr() < 1.0E-4) {
            frente = Vec3.directionFromRotation(0, jogador.getYRot());
        }
        frente = frente.normalize();
        double alcance = 6 * T;
        double cosseno = Math.cos(Math.toRadians(30));
        int queimados = 0;
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(alcance),
                criatura -> criatura != jogador && criatura.isAlive() && !criatura.isSpectator())) {
            Vec3 ate = criatura.position().subtract(jogador.position());
            Vec3 plano = new Vec3(ate.x, 0, ate.z);
            double distancia = plano.length();
            if (distancia > alcance || distancia < 0.01 || plano.normalize().dot(frente) < cosseno || !jogador.hasLineOfSight(criatura)) {
                continue;
            }
            criatura.igniteForSeconds(8 * Turno.TICKS / 20F);
            criatura.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, 5 * Turno.TICKS));
            queimados++;
        }
        // O fogo no chão (perto do herói não, pra não queimar ele mesmo)
        for (double distancia = T + 1; distancia <= alcance; distancia += 2) {
            double passo = 2.0 / distancia;
            for (double angulo = -Math.toRadians(30); angulo <= Math.toRadians(30); angulo += passo) {
                Vec3 direcao = frente.yRot((float) angulo);
                Vec3 ponto = jogador.position().add(direcao.scale(distancia));
                BlockPos posicao = BlockPos.containing(ponto.x, jogador.getY() + 1, ponto.z);
                for (int i = 0; i < 4 && !mundo.getBlockState(posicao.below()).isFaceSturdy(mundo, posicao.below(), Direction.UP); i++) {
                    posicao = posicao.below();
                }
                if (mundo.getBlockState(posicao).isAir() && mundo.getBlockState(posicao.below()).isFaceSturdy(mundo, posicao.below(), Direction.UP)
                        && temLinha(jogador, ponto)) {
                    mundo.setBlockAndUpdate(posicao, BaseFireBlock.getState(mundo, posicao));
                }
                if (mundo.getRandom().nextInt(3) == 0) {
                    mundo.sendParticles(ParticleTypes.FLAME, ponto.x, jogador.getEyeY() - 0.3, ponto.z, 2, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return queimados;
    }

    // O fogo não atravessa paredes
    private static boolean temLinha(ServerPlayer jogador, Vec3 ponto) {
        Vec3 de = jogador.getEyePosition();
        Vec3 ate = new Vec3(ponto.x, jogador.getEyeY() - 0.5, ponto.z);
        return jogador.level().clip(new net.minecraft.world.level.ClipContext(de, ate, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, jogador)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    // Tira os efeitos ruins (menos a corrupção dos aliados), apaga o fogo, mata a fome do herói e deixa imune a efeitos ruins por 5 turnos
    public static void limpar(LivingEntity criatura) {
        for (MobEffectInstance efeito : List.copyOf(criatura.getActiveEffects())) {
            if (efeito.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !efeito.is(ModEfeitos.CORROMPIDO)) {
                criatura.removeEffect(efeito.getEffect());
            }
        }
        criatura.clearFire();
        criatura.setTicksFrozen(0);
        if (criatura instanceof ServerPlayer jogador) {
            // O "Hunger.satisfy(STARVING)" do Shattered: uma ração e meia
            jogador.getFoodData().eat(15, 0.6F);
        }
        criatura.addEffect(new MobEffectInstance(ModEfeitos.PURIFICADO, TURNOS_DA_LIMPEZA * Turno.TICKS));
        Particulas.subindoEmVolta(criatura, Particulas.poeira(0xFF4CD2, 1.5F), 20, 0.05);
        criatura.level().playSound(null, criatura.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    // ---- As que pedem uma escolha ----

    // Arma (de perto ou de arremesso) ou armadura que ainda não recebeu a maestria
    public static boolean maestravel(ItemStack pilha) {
        return EfeitosDasPedras.encantavel(pilha) && !pilha.getOrDefault(ModComponentes.MAESTRIA.get(), false);
    }

    private static void maestria(ServerPlayer jogador, CorDePocao cor, boolean conhecia) {
        if (!conhecia) {
            // Desconhecida: o herói descobre o que é e a poção já foi (mesmo se ele desistir de escolher)
            Identificacao.identificar(jogador, cor);
            gastar(jogador, cor);
        }
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, "pocao_maestria", EfeitosDePocaoExotica::maestravel, (quem, espaco) -> {
            if (conhecia && !gastar(quem, cor)) {
                return;
            }
            aplicarMaestria(quem, quem.getInventory().getItem(espaco));
        });
        if (!abriu) {
            avisar(jogador, "mensagem.pixeldungeon.pocao_maestria.sem_item", ChatFormatting.GRAY);
        }
    }

    public static void aplicarMaestria(ServerPlayer jogador, ItemStack pilha) {
        pilha.set(ModComponentes.MAESTRIA.get(), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.2F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.HAPPY_VILLAGER, 10, 0.1);
        jogador.displayClientMessage(Component.translatable(NivelDoItem.ehArmadura(pilha) ? "mensagem.pixeldungeon.pocao_maestria.armadura"
                : "mensagem.pixeldungeon.pocao_maestria.arma").withStyle(ChatFormatting.GREEN), false);
        TalentosDoTier2.aoUsarPocao(jogador, 2);
    }

    // Os tiers que ainda podem ganhar os pontos da inspiração divina
    public static List<Integer> tiersSemInspiracao(DadosDoHeroi dados) {
        List<Integer> tiers = new ArrayList<>();
        for (int tier = 1; tier <= Talento.TIERS; tier++) {
            if (dados.inteiro(INSPIRACAO + tier) == 0) {
                tiers.add(tier);
            }
        }
        return tiers;
    }

    private static void inspiracao(ServerPlayer jogador, CorDePocao cor, boolean conhecia) {
        if (!conhecia) {
            Identificacao.identificar(jogador, cor);
            gastar(jogador, cor);
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        List<Integer> tiers = tiersSemInspiracao(dados);
        if (tiers.isEmpty()) {
            avisar(jogador, "mensagem.pixeldungeon.pocao_inspiracao_divina.sem_pontos", ChatFormatting.GRAY);
            return;
        }
        List<Component> botoes = new ArrayList<>();
        for (int tier : tiers) {
            botoes.add(Component.translatable("dica.pixeldungeon.tier", tier));
        }
        ItemStack icone = new ItemStack(ItemPocaoExotica.daCor(cor));
        EscolhaDeOpcao.pedir(jogador, Component.translatable(TipoDePocaoExotica.INSPIRACAO_DIVINA.chaveDoNome()),
                Component.translatable("item.pixeldungeon.pocao_exotica.inspiracao_divina.escolha"), icone, botoes, (quem, indice) -> {
                    if (indice < 0 || (conhecia && !gastar(quem, cor))) {
                        return;
                    }
                    inspirar(quem, tiers.get(indice));
                });
    }

    public static void inspirar(ServerPlayer jogador, int tier) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(INSPIRACAO + tier, 1);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFF00, 1.5F), 24, 0.1);
        NumerosFlutuantes.mostrarTraduzido(jogador, "mensagem.pixeldungeon.pocao_inspiracao_divina.bonus", NumerosFlutuantes.AMARELO);
        avisar(jogador, "mensagem.pixeldungeon.pocao_inspiracao_divina.bonus", ChatFormatting.GREEN);
        TalentosDoTier2.aoUsarPocao(jogador, 2);
        RedeDoHeroi.enviarEstado(jogador, true);
    }

    // Os pontos de talento a mais que a inspiração divina deu nesse tier (0 ou 2)
    public static int pontosDaInspiracao(DadosDoHeroi dados, int tier) {
        return dados.inteiro(INSPIRACAO + tier) > 0 ? 2 : 0;
    }

    // ---- Ajudantes ----

    // Tira uma poção exótica dessa cor do inventário (no criativo, não tira)
    public static boolean gastar(ServerPlayer jogador, CorDePocao cor) {
        if (jogador.getAbilities().instabuild) {
            return true;
        }
        Inventory inventario = jogador.getInventory();
        ItemStack naMao = jogador.getMainHandItem();
        if (naMao.getItem() instanceof ItemPocaoExotica pocao && pocao.cor() == cor) {
            naMao.shrink(1);
            return true;
        }
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            if (inventario.getItem(i).getItem() instanceof ItemPocaoExotica pocao && pocao.cor() == cor) {
                inventario.getItem(i).shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void avisar(ServerPlayer jogador, String chave, ChatFormatting cor) {
        jogador.displayClientMessage(Component.translatable(chave).withStyle(cor), true);
    }
}
