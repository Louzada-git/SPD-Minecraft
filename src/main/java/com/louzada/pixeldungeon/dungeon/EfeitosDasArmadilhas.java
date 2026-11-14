package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.entidade.FantasmaTriste;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.item.EfeitosDasBombas;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// O que cada armadilha faz quando ativa (igual ao Shattered; 1 quadradinho = 4x4 blocos, 1 turno = 2 segundos):
//   dardos: um dardo no alvo mais perto à vista (até 8 quadradinhos; quem está em cima da armadilha é o primeiro):
//     4 a 8 de dano menos a armadura. O envenenado também envenena por 8 + 2/3 do andar turnos.
//   resfriamento: ar congelante nos 3x3 quadradinhos em volta, por 10 turnos (resfria e depois congela)
//   elétrica: eletricidade nos 3x3 quadradinhos em volta, por 10 turnos (paralisa e dá choques)
//   gás tóxico / confusão: uma nuvem do gás em volta
//   alarme: todos os inimigos do andar acordam e vão até a armadilha
//   muco: muco cáustico em quem estiver nos 3x3 quadradinhos em volta (menos quem voa)
//   rebanho: ovelhas mágicas em todo lugar livre até 2 quadradinhos dela (somem em uns 6 turnos)
//   invocação: 1 a 3 inimigos do andar (1: 50%, 2: 25%, 3: 25%) nos quadradinhos do lado
//   teletransporte: tudo nos 3x3 quadradinhos em volta (criaturas e itens) vai pra lugares aleatórios do andar
//   portal: tudo nos 3x3 em volta vai sempre pro mesmo lugar do andar, quantas vezes for
//   agarradora: quem está em cima sangra (2 + andar / 2, menos metade da armadura) e fica aleijado por 10 turnos; não desarma
//   desmoronamento: pedras caem na sala inteira (ou 2 quadradinhos em volta, fora das salas): 5 + andar a 10 + 2 x andar
//     de dano menos a armadura, e paralisa por 10 turnos
public class EfeitosDasArmadilhas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    // O dano das armadilhas (a mensagem de morte é "foi morto por uma armadilha")
    public static final ResourceKey<DamageType> DANO_DE_ARMADILHA = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "armadilha"));
    // O dardo alcança até onde o herói enxerga: 8 quadradinhos (mais meio)
    private static final double ALCANCE_DO_DARDO = 8.5 * T;

    // O andar onde a armadilha está (null fora da dungeon da run: aí os efeitos que precisam do andar fazem o básico)
    private record Lugar(DadosDaRun run, int andar, BlockPos origem, PlantaDoAndar planta, int qx, int qy) {
    }

    public static void ativar(ServerLevel mundo, BlockPos controle, TipoDeArmadilha tipo, @Nullable Entity quem) {
        Vec3 centro = Armadilhas.centro(controle);
        switch (tipo) {
            case DARDOS, DARDOS_VENENOSOS -> dardo(mundo, controle, tipo == TipoDeArmadilha.DARDOS_VENENOSOS);
            case RESFRIAMENTO -> {
                NuvemDeGas nuvem = NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.CONGELANTE, 6.0F, 10);
                Particulas.explosao(mundo, ParticleTypes.SNOWFLAKE, centro.x, centro.y + 0.5, centro.z, 40, 2.0, 0.05);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
                // Igual ao Shattered, o primeiro sopro já pega quem está perto
                afetarAgora(mundo, nuvem);
            }
            case ELETRICA -> {
                NuvemDeGas nuvem = NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.ELETRICO, 6.0F, 10);
                Particulas.explosao(mundo, ParticleTypes.ELECTRIC_SPARK, centro.x, centro.y + 0.5, centro.z, 60, 2.0, 0.3);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.8F, 1.4F);
                afetarAgora(mundo, nuvem);
            }
            case GAS_TOXICO -> {
                NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.TOXICO, 5.0F, 10);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
            }
            case CONFUSAO -> {
                NuvemDeGas.soltar(mundo, centro, NuvemDeGas.Tipo.CONFUSAO, 5.0F, 10);
                mundo.playSound(null, BlockPos.containing(centro), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            case ALARME -> alarme(mundo, controle);
            case MUCO -> muco(mundo, controle);
            case REBANHO -> rebanho(mundo, controle);
            case INVOCACAO -> invocacao(mundo, controle);
            case TELETRANSPORTE -> teletransporte(mundo, controle);
            case PORTAL -> portal(mundo, controle);
            case AGARRADORA -> agarrar(mundo, controle, quem);
            case DESMORONAMENTO -> desmoronamento(mundo, controle);
            // Igual ao Shattered: fogo na armadilha e nos 8 quadradinhos em volta
            case FOGO -> EfeitosDasBombas.fogo(mundo, quadradinhosAte(mundo, controle, 1));
            // Igual ao Shattered: a explosão de uma bomba comum
            case EXPLOSIVA -> EfeitosDasBombas.explodir(mundo, TipoDeBomba.COMUM, controle, null);
        }
    }

    // ---- Dardos ----

    private static void dardo(ServerLevel mundo, BlockPos controle, boolean envenenado) {
        Vec3 boca = Armadilhas.centro(controle).add(0, 0.5, 0);
        // Quem está em cima da armadilha leva o dardo; senão, o mais perto que dá pra acertar daqui
        LivingEntity alvo = null;
        for (LivingEntity criatura : criaturasEmVolta(mundo, controle, 0)) {
            if (podeSerAtingido(criatura)) {
                alvo = criatura;
                break;
            }
        }
        if (alvo == null) {
            double maisPerto = Double.MAX_VALUE;
            AABB caixa = new AABB(boca, boca).inflate(ALCANCE_DO_DARDO, 6, ALCANCE_DO_DARDO);
            for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, caixa, EfeitosDasArmadilhas::podeSerAtingido)) {
                Vec3 meio = criatura.position().add(0, criatura.getBbHeight() * 0.5, 0);
                double distancia = meio.distanceTo(boca);
                // Quem está invisível conta como se estivesse no limite do alcance
                if (Furtividade.invisivel(criatura)) {
                    distancia = Math.max(distancia, ALCANCE_DO_DARDO);
                }
                if (!caminhoLivre(mundo, boca, meio, criatura)) {
                    continue;
                }
                if (distancia < maisPerto || (distancia == maisPerto && criatura instanceof Player)) {
                    alvo = criatura;
                    maisPerto = distancia;
                }
            }
            if (maisPerto > ALCANCE_DO_DARDO) {
                alvo = null;
            }
        }
        mundo.playSound(null, BlockPos.containing(boca), SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.4F);
        if (alvo == null) {
            return;
        }
        Vec3 meio = alvo.position().add(0, alvo.getBbHeight() * 0.5, 0);
        trilha(mundo, boca, meio, envenenado ? Particulas.poeira(0x55AA33, 0.8F) : ParticleTypes.CRIT);
        int dano = Sorteio.normal(mundo.getRandom(), 4, 8) - armadura(alvo);
        ferir(mundo, alvo, dano);
        mundo.playSound(null, alvo.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.BLOCKS, 1.0F, 0.8F + mundo.getRandom().nextFloat() * 0.45F);
        if (envenenado) {
            EfeitosNoTick.envenenar(alvo, 8 + Math.round(2 * andarDe(controle) / 3F));
        }
    }

    // Uma linha de partículas do ponto ao alvo (o dardo voando)
    private static void trilha(ServerLevel mundo, Vec3 de, Vec3 ate, ParticleOptions particula) {
        int passos = Math.max(2, (int) (de.distanceTo(ate) * 2));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = de.lerp(ate, i / (double) passos);
            mundo.sendParticles(particula, ponto.x, ponto.y, ponto.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static boolean caminhoLivre(ServerLevel mundo, Vec3 de, Vec3 ate, Entity alvo) {
        return mundo.clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, alvo)).getType() == HitResult.Type.MISS;
    }

    // ---- Nuvens ----

    // A nuvem recém-solta já age em quem está dentro (senão só agiria no fim do primeiro turno dela)
    private static void afetarAgora(ServerLevel mundo, NuvemDeGas nuvem) {
        float raio = nuvem.raio();
        for (LivingEntity criatura : mundo.getEntitiesOfClass(LivingEntity.class, nuvem.getBoundingBox().inflate(raio, 3, raio))) {
            if (criatura.isAlive() && !criatura.isSpectator() && !criatura.hasEffect(ModEfeitos.IMUNE_A_GAS)
                    && criatura.distanceToSqr(nuvem.getX(), criatura.getY(), nuvem.getZ()) <= raio * raio && !criativo(criatura)) {
                nuvem.afetar(criatura);
            }
        }
    }

    // ---- Alarme ----

    public static void alarme(ServerLevel mundo, BlockPos controle) {
        Vec3 centro = Armadilhas.centro(controle);
        Lugar lugar = lugar(mundo, controle);
        AABB andar = lugar != null ? PopulacaoDoAndar.caixaDoAndar(lugar.origem()) : new AABB(centro, centro).inflate(64, 8, 64);
        int chamados = 0;
        for (MobDoShattered inimigo : mundo.getEntitiesOfClass(MobDoShattered.class, andar)) {
            if (!inimigo.aliado() && inimigo.isAlive()) {
                inimigo.chamar(centro);
                chamados++;
            }
        }
        for (ServerPlayer jogador : mundo.players()) {
            if (jogador.position().distanceTo(centro) <= 40) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.armadilha_alarme").withStyle(ChatFormatting.RED), false);
            }
        }
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 4.0F, 1.2F);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
        Particulas.explosao(mundo, ParticleTypes.NOTE, centro.x, centro.y + 1.5, centro.z, 12, 1.2, 0.5);
        PixelDungeon.LOGGER.debug("Armadilha de alarme: {} inimigos chamados", chamados);
    }

    // ---- Muco ----

    private static void muco(ServerLevel mundo, BlockPos controle) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                Vec3 meio = Armadilhas.centro(controle.offset(dx * T, 0, dy * T));
                Particulas.explosao(mundo, Particulas.poeira(0x101010, 1.5F), meio.x, meio.y + 0.2, meio.z, 10, 1.0, 0.0);
            }
        }
        for (LivingEntity criatura : criaturasEmVolta(mundo, controle, 1)) {
            if (!Armadilhas.voa(criatura)) {
                criatura.addEffect(new MobEffectInstance(ModEfeitos.MUCO_CAUSTICO, MucoCaustico.DURACAO));
            }
        }
        mundo.playSound(null, BlockPos.containing(Armadilhas.centro(controle)), SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    // ---- Rebanho ----

    private static void rebanho(ServerLevel mundo, BlockPos controle) {
        int ovelhas = 0;
        for (BlockPos quadradinho : quadradinhosAte(mundo, controle, 2)) {
            if (!criaturasEmVolta(mundo, quadradinho, 0).isEmpty()) {
                continue;
            }
            OvelhaMagica ovelha = OvelhaMagica.criar(mundo, Armadilhas.centro(quadradinho), 6);
            ovelhas++;
            // Nasceu em cima de outra armadilha: ativa (a ovelha pisa forte, igual ao herói)
            if (!quadradinho.equals(controle)) {
                Armadilhas.acionarSeTiver(mundo, quadradinho, ovelha);
            }
        }
        Vec3 centro = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        PixelDungeon.LOGGER.debug("Armadilha de rebanho: {} ovelhas", ovelhas);
    }

    // ---- Invocação ----

    private static void invocacao(ServerLevel mundo, BlockPos controle) {
        Random sorteio = new Random(mundo.getRandom().nextLong());
        int quantos = 1;
        if (sorteio.nextInt(2) == 0) {
            quantos++;
            if (sorteio.nextInt(2) == 0) {
                quantos++;
            }
        }
        List<BlockPos> livres = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos quadradinho = controle.offset(dx * T, 0, dy * T);
                if ((dx != 0 || dy != 0) && quadradinhoLivre(mundo, quadradinho) && criaturasEmVolta(mundo, quadradinho, 0).isEmpty()) {
                    livres.add(quadradinho);
                }
            }
        }
        Collections.shuffle(livres, sorteio);
        int andar = andarDe(controle);
        Map<BlockPos, MobDoShattered> invocados = new HashMap<>();
        for (int i = 0; i < quantos && i < livres.size(); i++) {
            EntityType<? extends MobDoShattered> tipo = PopulacaoDoAndar.tipoAleatorio(andar, sorteio);
            MobDoShattered inimigo = PopulacaoDoAndar.invocar(mundo, tipo, BlockPos.containing(Armadilhas.centro(livres.get(i))));
            if (inimigo != null) {
                invocados.put(livres.get(i), inimigo);
                Particulas.explosao(mundo, ParticleTypes.REVERSE_PORTAL, inimigo.getX(), inimigo.getY(0.5), inimigo.getZ(), 30, 0.5, 0.1);
            }
        }
        // Quem nasceu em cima de outra armadilha ativa ela
        for (Map.Entry<BlockPos, MobDoShattered> invocado : invocados.entrySet()) {
            Armadilhas.acionarSeTiver(mundo, invocado.getKey(), invocado.getValue());
        }
        mundo.playSound(null, BlockPos.containing(Armadilhas.centro(controle)), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    // ---- Teletransporte e portal ----

    private static void teletransporte(ServerLevel mundo, BlockPos controle) {
        Lugar lugar = lugar(mundo, controle);
        for (LivingEntity criatura : criaturasEmVolta(mundo, controle, 1)) {
            if (!(criatura instanceof FantasmaTriste)) {
                Teleporte.paraLugarAleatorio(mundo, criatura);
            }
        }
        if (lugar == null) {
            return;
        }
        Random sorteio = new Random(mundo.getRandom().nextLong());
        for (ItemEntity item : itensEmVolta(mundo, controle, 1)) {
            BlockPos destino = PopulacaoDoAndar.lugarAleatorio(lugar.run().semente(), lugar.origem(), lugar.andar(), sorteio);
            if (destino != null) {
                Particulas.explosao(mundo, ParticleTypes.PORTAL, item.getX(), item.getY() + 0.3, item.getZ(), 12, 0.2, 0.2);
                item.teleportTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5);
                item.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    // O destino do portal: sempre o mesmo pra cada armadilha (sorteado pela semente da run e pelo lugar da armadilha)
    @Nullable
    public static BlockPos destinoDoPortal(ServerLevel mundo, BlockPos controle) {
        Lugar lugar = lugar(mundo, controle);
        if (lugar == null) {
            return null;
        }
        Random sorteio = new Random(lugar.run().semente() * 31 + controle.asLong());
        List<int[]> livres = lugar.planta().lugaresLivres(0);
        livres.removeIf(quadradinho -> Math.abs(quadradinho[0] - lugar.qx()) <= 2 && Math.abs(quadradinho[1] - lugar.qy()) <= 2);
        if (livres.isEmpty()) {
            return null;
        }
        int[] escolhido = livres.get(sorteio.nextInt(livres.size()));
        return Armadilhas.controle(lugar.origem(), escolhido[0], escolhido[1]);
    }

    private static void portal(ServerLevel mundo, BlockPos controle) {
        Lugar lugar = lugar(mundo, controle);
        BlockPos destino = destinoDoPortal(mundo, controle);
        if (lugar == null || destino == null) {
            return;
        }
        // O primeiro vai pro quadradinho do destino, os outros pros quadradinhos livres em volta dele
        List<BlockPos> vagas = new ArrayList<>();
        int dqx = Math.floorDiv(destino.getX() - lugar.origem().getX(), T);
        int dqy = Math.floorDiv(destino.getZ() - lugar.origem().getZ(), T);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && lugar.planta().passavel(dqx + dx, dqy + dy) && !lugar.planta().ehPorta(dqx + dx, dqy + dy)) {
                    vagas.add(Armadilhas.controle(lugar.origem(), dqx + dx, dqy + dy));
                }
            }
        }
        Collections.shuffle(vagas, new Random(mundo.getRandom().nextLong()));
        vagas.add(0, destino);
        vagas.removeIf(vaga -> !vaga.equals(destino) && !criaturasEmVolta(mundo, vaga, 0).isEmpty());
        for (LivingEntity criatura : criaturasEmVolta(mundo, controle, 1)) {
            if (criatura instanceof FantasmaTriste || vagas.isEmpty()) {
                continue;
            }
            BlockPos vaga = vagas.remove(0);
            Teleporte.para(mundo, criatura, BlockPos.containing(Armadilhas.centro(vaga)));
        }
        Vec3 meio = Armadilhas.centro(destino);
        for (ItemEntity item : itensEmVolta(mundo, controle, 1)) {
            Particulas.explosao(mundo, ParticleTypes.PORTAL, item.getX(), item.getY() + 0.3, item.getZ(), 12, 0.2, 0.2);
            item.teleportTo(meio.x, meio.y, meio.z);
            item.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ---- Agarradora ----

    private static void agarrar(ServerLevel mundo, BlockPos controle, @Nullable Entity quem) {
        LivingEntity preso = null;
        if (quem instanceof LivingEntity vivo && Armadilhas.emCima(vivo, controle) && !Armadilhas.voa(vivo)) {
            preso = vivo;
        } else {
            for (LivingEntity criatura : criaturasEmVolta(mundo, controle, 0)) {
                if (!Armadilhas.voa(criatura)) {
                    preso = criatura;
                    break;
                }
            }
        }
        Vec3 centro = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.2F, 0.7F);
        if (preso == null) {
            Particulas.explosao(mundo, ParticleTypes.CRIT, centro.x, centro.y + 0.3, centro.z, 10, 0.6, 0.1);
            return;
        }
        int sangue = Math.max(0, (2 + andarDe(controle) / 2) - armadura(preso) / 2);
        if (sangue > 0) {
            EfeitosNoTick.sangrar(preso, sangue);
        }
        preso.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, 10 * Turno.TICKS));
        Particulas.sangue(preso, Particulas.SANGUE_PADRAO, 4);
        Particulas.explosao(mundo, ParticleTypes.CRIT, preso.getX(), preso.getY() + 0.3, preso.getZ(), 12, 0.4, 0.1);
    }

    // ---- Desmoronamento ----

    private static void desmoronamento(ServerLevel mundo, BlockPos controle) {
        Lugar lugar = lugar(mundo, controle);
        List<BlockPos> quadradinhos = new ArrayList<>();
        Sala sala = lugar == null ? null : lugar.planta().salaEm(lugar.qx(), lugar.qy());
        if (sala != null) {
            for (int y = sala.y(); y < sala.y() + sala.altura(); y++) {
                for (int x = sala.x(); x < sala.x() + sala.largura(); x++) {
                    if (lugar.planta().passavel(x, y)) {
                        quadradinhos.add(Armadilhas.controle(lugar.origem(), x, y));
                    }
                }
            }
        } else {
            quadradinhos.addAll(quadradinhosAte(mundo, controle, 2));
        }
        int andar = andarDe(controle);
        BlockParticleOption pedra = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.TUFF.defaultBlockState());
        for (BlockPos quadradinho : quadradinhos) {
            Vec3 meio = Armadilhas.centro(quadradinho);
            mundo.sendParticles(pedra, meio.x, meio.y + 5, meio.z, 12, 1.5, 0.5, 1.5, 0.0);
            mundo.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), meio.x, meio.y + 0.2, meio.z, 8, 1.2,
                    0.1, 1.2, 0.0);
            for (LivingEntity criatura : criaturasEmVolta(mundo, quadradinho, 0)) {
                int dano = Sorteio.normal(mundo.getRandom(), 5 + andar, 10 + andar * 2) - armadura(criatura);
                ferir(mundo, criatura, dano);
                if (criatura.isAlive()) {
                    criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 10 * Turno.TICKS));
                }
            }
        }
        Vec3 centro = Armadilhas.centro(controle);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.5F);
        mundo.playSound(null, BlockPos.containing(centro), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
    }

    // ---- Ajudantes ----

    // O andar da run onde fica a armadilha
    @Nullable
    private static Lugar lugar(ServerLevel mundo, BlockPos controle) {
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        if (!run.ativa()) {
            return null;
        }
        Integer andar = PopulacaoDoAndar.andarEm(run, controle.getX() + 0.5, controle.getY() + 1, controle.getZ() + 0.5);
        if (andar == null || Andares.ehAndarDeChefe(andar)) {
            return null;
        }
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        if (origem.getY() != controle.getY()) {
            return null;
        }
        int qx = Math.floorDiv(controle.getX() - origem.getX(), T);
        int qy = Math.floorDiv(controle.getZ() - origem.getZ(), T);
        return new Lugar(run, andar, origem, PopulacaoDoAndar.planta(run.semente(), andar), qx, qy);
    }

    private static int andarDe(BlockPos controle) {
        return Math.max(1, Andares.andarPelaAltura(controle.getY() + 1));
    }

    // As criaturas nos quadradinhos até "raio" de distância (0 = só o da armadilha; 1 = os 3x3 em volta)
    public static List<LivingEntity> criaturasEmVolta(ServerLevel mundo, BlockPos controle, int raio) {
        AABB caixa = new AABB(controle.getX() - raio * T, controle.getY() + 0.5, controle.getZ() - raio * T, controle.getX() + (raio + 1) * T,
                controle.getY() + 3.5, controle.getZ() + (raio + 1) * T);
        return mundo.getEntitiesOfClass(LivingEntity.class, caixa,
                criatura -> criatura.isAlive() && !criatura.isSpectator() && !criativo(criatura) && pertoDoQuadradinho(criatura, controle, raio));
    }

    public static List<ItemEntity> itensEmVolta(ServerLevel mundo, BlockPos controle, int raio) {
        AABB caixa = new AABB(controle.getX() - raio * T, controle.getY() + 0.5, controle.getZ() - raio * T, controle.getX() + (raio + 1) * T,
                controle.getY() + 3.5, controle.getZ() + (raio + 1) * T);
        return mundo.getEntitiesOfClass(ItemEntity.class, caixa, item -> item.isAlive() && pertoDoQuadradinho(item, controle, raio));
    }

    private static boolean pertoDoQuadradinho(Entity entidade, BlockPos controle, int raio) {
        int dx = Math.floorDiv((int) Math.floor(entidade.getX()) - controle.getX(), T);
        int dz = Math.floorDiv((int) Math.floor(entidade.getZ()) - controle.getZ(), T);
        double dy = entidade.getY() - controle.getY();
        return Math.abs(dx) <= raio && Math.abs(dz) <= raio && dy >= 0.5 && dy <= 3.5;
    }

    private static boolean criativo(LivingEntity criatura) {
        return criatura instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator());
    }

    private static boolean podeSerAtingido(LivingEntity criatura) {
        return criatura.isAlive() && !criatura.isSpectator() && !criativo(criatura) && !(criatura instanceof FantasmaTriste)
                && !(criatura instanceof OvelhaMagica);
    }

    // Dá pra ficar em pé no meio do quadradinho? (chão firme embaixo, espaço livre pro corpo)
    public static boolean quadradinhoLivre(ServerLevel mundo, BlockPos controle) {
        BlockPos pe = BlockPos.containing(Armadilhas.centro(controle));
        return !mundo.getBlockState(pe.below()).getCollisionShape(mundo, pe.below()).isEmpty()
                && mundo.getBlockState(pe).getCollisionShape(mundo, pe).isEmpty()
                && mundo.getBlockState(pe.above()).getCollisionShape(mundo, pe.above()).isEmpty();
    }

    // Os quadradinhos livres até "distancia" passos (andando também na diagonal), sem atravessar paredes
    // (o "buildDistanceMap" do Shattered). O da armadilha vem primeiro.
    public static List<BlockPos> quadradinhosAte(ServerLevel mundo, BlockPos controle, int distancia) {
        List<BlockPos> achados = new ArrayList<>();
        Map<BlockPos, Integer> passos = new HashMap<>();
        Deque<BlockPos> fila = new ArrayDeque<>();
        passos.put(controle, 0);
        fila.add(controle);
        while (!fila.isEmpty()) {
            BlockPos aqui = fila.poll();
            achados.add(aqui);
            int dist = passos.get(aqui);
            if (dist >= distancia) {
                continue;
            }
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    BlockPos vizinho = aqui.offset(dx * T, 0, dy * T);
                    if ((dx != 0 || dy != 0) && !passos.containsKey(vizinho) && quadradinhoLivre(mundo, vizinho)) {
                        passos.put(vizinho, dist + 1);
                        fila.add(vizinho);
                    }
                }
            }
        }
        return achados;
    }

    // A armadura sorteada de quem leva o golpe (igual a um golpe comum)
    public static int armadura(LivingEntity alvo) {
        Combatente combatente = Combatente.de(alvo);
        return combatente.sortearArmadura(Golpe.de(combatente, combatente));
    }

    private static void ferir(ServerLevel mundo, LivingEntity alvo, int dano) {
        if (dano <= 0 || alvo instanceof OvelhaMagica) {
            return;
        }
        Combate.aplicar(alvo, dano, fonte(mundo));
    }

    public static DamageSource fonte(ServerLevel mundo) {
        return new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANO_DE_ARMADILHA));
    }
}
