package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.Mira;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// O que cada varinha faz quando é lançada (as soltas e a que está no cajado do Mago), com os números do Shattered.
// O míssil mágico voa (MisselMagico); as outras acertam na hora, com um rastro de partículas:
//   raios que param no primeiro inimigo ou na parede (relâmpago, geada, luz prismática, corrosão, onda de choque, transfusão),
//   o da desintegração, que atravessa tudo, e os cones (rajada de fogo, maturação).
// Varinhas amaldiçoadas fazem uma coisa aleatória (e ruim) no lugar.
public class EfeitosDeVarinha {
    // 1 quadradinho do Shattered = 4 blocos
    private static final double QUADRADINHO = 4.0;
    // Alcance dos raios (o Shattered vai até a parede; aqui, 8 quadradinhos)
    private static final double ALCANCE = 32.0;

    // ---- Lançar ----

    public static void lancar(ServerPlayer jogador, TipoDeVarinha tipo, int nivel, int cargasUsadas) {
        switch (tipo) {
            case MISSEL_MAGICO -> new VarinhaDeMisselMagico().lancar(jogador, nivel);
            case RELAMPAGO -> relampago(jogador, nivel);
            case DESINTEGRACAO -> desintegracao(jogador, nivel);
            case RAJADA_DE_FOGO -> rajadaDeFogo(jogador, nivel, cargasUsadas);
            case CORROSAO -> corrosao(jogador, nivel);
            case ONDA_DE_CHOQUE -> ondaDeChoque(jogador, nivel);
            case GEADA -> geada(jogador, nivel);
            case LUZ_PRISMATICA -> luzPrismatica(jogador, nivel);
            case TRANSFUSAO -> transfusao(jogador, nivel);
            case MATURACAO -> maturacao(jogador, nivel, cargasUsadas);
        }
    }

    // ---- Raios ----

    // Relâmpago: 5-10 (+1/+5) no alvo e em quem estiver a 1 quadradinho dele (2 na água), pulando de um pro outro.
    // O dano se divide: cada um leva 40% + 60% / (quantos foram atingidos); na água, todos levam tudo.
    private static void relampago(ServerPlayer jogador, int nivel) {
        Vec3 fim = Mira.ponto(jogador, ALCANCE);
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        if (alvo != null) {
            fim = alvo.getBoundingBox().getCenter();
        }
        rastro(jogador, fim, ParticleTypes.ELECTRIC_SPARK, 3);
        som(jogador, fim, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
        if (alvo == null) {
            return;
        }
        List<LivingEntity> atingidos = new ArrayList<>(List.of(alvo));
        for (int i = 0; i < atingidos.size(); i++) {
            LivingEntity atual = atingidos.get(i);
            double alcance = (atual.isInWater() ? 2 : 1) * QUADRADINHO;
            for (LivingEntity perto : inimigosEmVolta(jogador, atual.position(), alcance)) {
                if (!atingidos.contains(perto)) {
                    atingidos.add(perto);
                    linha(jogador.serverLevel(), atual.getBoundingBox().getCenter(), perto.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 3);
                }
            }
        }
        float parte = alvo.isInWater() ? 1.0F : 0.4F + 0.6F / atingidos.size();
        boolean primeiro = true;
        for (LivingEntity atingido : atingidos) {
            int dano = Math.round(TipoDeVarinha.RELAMPAGO.sortearDano(jogador.getRandom(), nivel, 1) * parte);
            ferir(jogador, atingido, dano, primeiro);
            primeiro = false;
        }
    }

    // Desintegração: atravessa inimigos e paredes até 6 + 2 x nível quadradinhos. Cada inimigo a mais no caminho
    // e cada 3 quadradinhos de parede atravessados contam como +1 nível pro dano de todos.
    private static void desintegracao(ServerPlayer jogador, int nivel) {
        double alcance = (6 + 2 * nivel) * QUADRADINHO;
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getViewVector(1.0F);
        Vec3 fim = olho.add(olhar.scale(alcance));
        // Quantos quadradinhos de parede o raio atravessa
        int paredes = 0;
        for (double d = 0; d < alcance; d += QUADRADINHO) {
            BlockPos lugar = BlockPos.containing(olho.add(olhar.scale(d)));
            if (!jogador.level().getBlockState(lugar).getCollisionShape(jogador.level(), lugar).isEmpty()) {
                paredes++;
            }
        }
        List<LivingEntity> atingidos = new ArrayList<>();
        for (LivingEntity criatura : jogador.level().getEntitiesOfClass(LivingEntity.class, new AABB(olho, fim).inflate(1.0), criatura -> podeAtingir(jogador, criatura))) {
            if (criatura.getBoundingBox().inflate(0.4).clip(olho, fim).isPresent()) {
                atingidos.add(criatura);
            }
        }
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.DESINTEGRACAO.cor(), 1.4F), 4);
        som(jogador, jogador.position(), SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.8F);
        int nivelDoDano = nivel + Math.max(0, atingidos.size() - 1) + paredes / 3;
        boolean primeiro = true;
        for (LivingEntity atingido : atingidos) {
            ferir(jogador, atingido, TipoDeVarinha.DESINTEGRACAO.sortearDano(jogador.getRandom(), nivelDoDano, 1), primeiro);
            primeiro = false;
        }
    }

    // Geada: 2-8 (+1/+5) e resfriado por 2 + nível turnos (4 + nível na água). Quem já está resfriado leva menos;
    // quem está congelado não sente nada.
    private static void geada(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = raioAteOAlvo(jogador, TipoDeVarinha.GEADA, ParticleTypes.SNOWFLAKE);
        som(jogador, jogador.position(), SoundEvents.GLASS_BREAK, 0.5F, 1.8F);
        if (alvo == null || alvo.hasEffect(ModEfeitos.CONGELADO)) {
            return;
        }
        int dano = TipoDeVarinha.GEADA.sortearDano(jogador.getRandom(), nivel, 1);
        MobEffectInstance frio = alvo.getEffect(ModEfeitos.RESFRIADO);
        if (frio != null) {
            float turnos = Math.min(10, frio.getDuration() / (float) Turno.TICKS);
            dano = (int) Math.round(dano * Math.pow(0.9333, turnos));
        }
        ferir(jogador, alvo, dano, true);
        if (alvo.isAlive()) {
            int turnos = (alvo.isInWater() ? 4 : 2) + nivel;
            alvo.addEffect(new MobEffectInstance(ModEfeitos.RESFRIADO, Turno.ticks(turnos)));
            alvo.setTicksFrozen(Math.max(alvo.getTicksFrozen(), 100));
        }
    }

    // Luz prismática: 1-5 (+1/+3) e às vezes cega (sorteio de 0 a 4 + nível dar 3 ou mais) por 2 + nível/3 turnos
    private static void luzPrismatica(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = raioAteOAlvo(jogador, TipoDeVarinha.LUZ_PRISMATICA, ParticleTypes.END_ROD);
        som(jogador, jogador.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
        if (alvo == null) {
            return;
        }
        ferir(jogador, alvo, TipoDeVarinha.LUZ_PRISMATICA.sortearDano(jogador.getRandom(), nivel, 1), true);
        if (alvo.isAlive() && jogador.getRandom().nextInt(5 + nivel) >= 3) {
            EfeitosNoTick.cegar(alvo, Turno.ticks(2 + nivel / 3.0F));
        }
    }

    // Corrosão: uma nuvem de gás corrosivo onde o raio para (o dano começa em 2 + nível e cresce a cada turno dentro dela)
    private static void corrosao(ServerPlayer jogador, int nivel) {
        Vec3 lugar = ondeORaioPara(jogador);
        rastro(jogador, lugar, Particulas.poeira(TipoDeVarinha.CORROSAO.cor(), 1.2F), 3);
        som(jogador, lugar, SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.6F);
        NuvemDeGas nuvem = NuvemDeGas.soltar(jogador.serverLevel(), lugar.subtract(0, 1.0, 0), NuvemDeGas.Tipo.CORROSIVO, 6.0F, 5 + nivel);
        nuvem.definirForca(2 + nivel);
    }

    // Onda de choque: explode onde o raio para, com 1-3 (+1/+3) em todo mundo a 1 quadradinho.
    // O alvo voa 3 + nível quadradinhos pra frente; os de volta, 1,5 + nível/2 pra longe do centro.
    // Quem bate numa parede no caminho leva mais dano e fica paralisado um pouco.
    private static void ondaDeChoque(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 centro = alvo != null ? alvo.position() : ondeORaioPara(jogador);
        rastro(jogador, centro.add(0, 0.5, 0), Particulas.poeira(TipoDeVarinha.ONDA_DE_CHOQUE.cor(), 1.2F), 3);
        ServerLevel mundo = jogador.serverLevel();
        Particulas.explosao(mundo, ParticleTypes.EXPLOSION, centro.x, centro.y + 0.5, centro.z, 3, 1.0, 0.0);
        som(jogador, centro, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.4F);
        Vec3 frente = jogador.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        for (LivingEntity atingido : inimigosEmVolta(jogador, centro, 1.5 * QUADRADINHO)) {
            ferir(jogador, atingido, TipoDeVarinha.ONDA_DE_CHOQUE.sortearDano(jogador.getRandom(), nivel, 1), atingido == alvo);
            if (!atingido.isAlive()) {
                continue;
            }
            if (atingido == alvo) {
                empurrar(jogador, atingido, frente, nivel + 3);
            } else {
                Vec3 longe = atingido.position().subtract(centro).multiply(1, 0, 1);
                empurrar(jogador, atingido, longe.lengthSqr() < 0.01 ? frente : longe.normalize(), Math.round(1.5F + nivel / 2.0F));
            }
        }
    }

    // Empurra uns quadradinhos pra uma direção, parando antes de uma parede (e aí leva dano e fica paralisado)
    private static void empurrar(ServerPlayer jogador, LivingEntity criatura, Vec3 direcao, int quadradinhos) {
        if (criatura.hasEffect(ModEfeitos.ENRAIZADO) || quadradinhos <= 0) {
            return;
        }
        Vec3 inicio = criatura.position().add(0, criatura.getBbHeight() / 2, 0);
        Vec3 fim = inicio.add(direcao.scale(quadradinhos * QUADRADINHO));
        HitResult parede = criatura.level().clip(new ClipContext(inicio, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, criatura));
        boolean bateu = parede.getType() != HitResult.Type.MISS;
        Vec3 destino = bateu ? parede.getLocation().subtract(direcao.scale(criatura.getBbWidth() / 2 + 0.3)) : fim;
        double andou = destino.distanceTo(inicio);
        criatura.teleportTo(destino.x, criatura.getY(), destino.z);
        if (bateu) {
            int distancia = (int) Math.round(andou / QUADRADINHO);
            if (distancia > 0) {
                criatura.invulnerableTime = 0;
                criatura.hurt(fonte(jogador), distancia + jogador.getRandom().nextInt(distancia + 1));
                criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.ticks(1 + distancia / 2.0F)));
            }
        }
    }

    // Transfusão: num aliado na mira, tira 5% da vida de quem lança e cura o aliado nisso + 3 x nível (a sobra vira blindagem).
    // Num inimigo, ele fica encantado (não ataca) por 5 turnos e quem lança ganha 5 + nível de blindagem.
    private static void transfusao(ServerPlayer jogador, int nivel) {
        LivingEntity aliado = Mira.aliado(jogador, ALCANCE);
        LivingEntity inimigo = aliado == null ? Mira.inimigo(jogador, ALCANCE) : null;
        LivingEntity alvo = aliado != null ? aliado : inimigo;
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.TRANSFUSAO.cor(), 1.2F), 3);
        som(jogador, fim, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
        if (aliado != null) {
            int custo = Math.round(jogador.getMaxHealth() * 0.05F);
            int cura = custo + 3 * nivel;
            float sobra = cura - (aliado.getMaxHealth() - aliado.getHealth());
            aliado.heal(cura);
            if (sobra > 0 && aliado instanceof ServerPlayer outro) {
                Escudos.darBarreira(outro, Math.round(sobra));
            }
            Particulas.subindoEmVolta(aliado, ParticleTypes.HEART, 4, 0.0);
            jogador.hurt(jogador.damageSources().magic(), custo);
        } else if (inimigo != null) {
            Escudos.darBarreira(jogador, 5 + nivel);
            inimigo.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, Turno.ticks(5)));
            Particulas.subindoEmVolta(inimigo, ParticleTypes.HEART, 4, 0.0);
            TalentosDoTier2.aoAcertarComVarinha(jogador, inimigo);
        }
    }

    // ---- Cones ----

    // Rajada de fogo: cone de 5/7/9 quadradinhos (1/2/3 cargas) que põe fogo no chão e em quem estiver nele;
    // com 2 cargas também aleija por 4 turnos, com 3 paralisa por 4 turnos
    private static void rajadaDeFogo(ServerPlayer jogador, int nivel, int cargasUsadas) {
        double alcance = (3 + 2 * cargasUsadas) * QUADRADINHO;
        double angulo = 30 + 20 * cargasUsadas;
        ServerLevel mundo = jogador.serverLevel();
        for (Vec3 ponto : pontosDoCone(jogador, alcance, angulo)) {
            Particulas.explosao(mundo, ParticleTypes.FLAME, ponto.x, ponto.y + 0.5, ponto.z, 6, 0.8, 0.02);
            // Fogo no chão (menos no quadradinho colado no herói)
            if (ponto.distanceTo(jogador.position()) > QUADRADINHO * 1.5) {
                BlockPos lugar = BlockPos.containing(ponto);
                if (mundo.getBlockState(lugar).isAir() && mundo.getBlockState(lugar.below()).isFaceSturdy(mundo, lugar.below(), Direction.UP)) {
                    mundo.setBlockAndUpdate(lugar, BaseFireBlock.getState(mundo, lugar));
                }
            }
        }
        som(jogador, jogador.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
        boolean primeiro = true;
        for (LivingEntity atingido : noCone(jogador, alcance, angulo)) {
            ferir(jogador, atingido, TipoDeVarinha.RAJADA_DE_FOGO.sortearDano(jogador.getRandom(), nivel, cargasUsadas), primeiro);
            primeiro = false;
            if (!atingido.isAlive()) {
                continue;
            }
            atingido.igniteForSeconds(8);
            atingido.removeEffect(ModEfeitos.CONGELADO);
            if (cargasUsadas == 2) {
                atingido.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, Turno.ticks(4)));
            } else if (cargasUsadas >= 3) {
                atingido.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.ticks(4)));
            }
        }
    }

    // Maturação: cone de 4/6/8 quadradinhos (1/2/3 cargas). Cresce grama alta em (3,67 + nível/3) x cargas quadradinhos
    // e enraíza quem estiver no cone por 4 x cargas turnos.
    private static void maturacao(ServerPlayer jogador, int nivel, int cargasUsadas) {
        double alcance = (2 + 2 * cargasUsadas) * QUADRADINHO;
        double angulo = 20 + 10 * cargasUsadas;
        ServerLevel mundo = jogador.serverLevel();
        int grama = Math.round((3.67F + nivel / 3.0F) * cargasUsadas);
        List<Vec3> pontos = pontosDoCone(jogador, alcance, angulo);
        // Mais perto primeiro, igual ao Shattered (a grama cresce ao longo do raio)
        pontos.sort(Comparator.comparingDouble(ponto -> ponto.distanceToSqr(jogador.position())));
        for (Vec3 ponto : pontos) {
            Particulas.explosao(mundo, ParticleTypes.HAPPY_VILLAGER, ponto.x, ponto.y + 0.5, ponto.z, 4, 0.8, 0.0);
            if (grama > 0 && ponto.distanceTo(jogador.position()) > QUADRADINHO) {
                int x = (int) Math.floor(ponto.x / QUADRADINHO) * (int) QUADRADINHO;
                int z = (int) Math.floor(ponto.z / QUADRADINHO) * (int) QUADRADINHO;
                if (Grama.crescer(mundo, x, jogador.getBlockY(), z)) {
                    grama--;
                }
            }
        }
        som(jogador, jogador.position(), SoundEvents.GRASS_PLACE, 1.0F, 0.8F);
        for (LivingEntity atingido : noCone(jogador, alcance, angulo)) {
            TalentosDoTier2.prolongar(atingido, ModEfeitos.ENRAIZADO, 4 * cargasUsadas, 0);
            TalentosDoTier2.aoAcertarComVarinha(jogador, atingido);
        }
    }

    // O centro de cada quadradinho dentro do cone (na altura dos pés do herói)
    private static List<Vec3> pontosDoCone(ServerPlayer jogador, double alcance, double angulo) {
        List<Vec3> pontos = new ArrayList<>();
        Vec3 pe = jogador.position();
        int raio = (int) Math.ceil(alcance / QUADRADINHO);
        int tileX = (int) Math.floor(pe.x / QUADRADINHO);
        int tileZ = (int) Math.floor(pe.z / QUADRADINHO);
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dz = -raio; dz <= raio; dz++) {
                Vec3 ponto = new Vec3((tileX + dx + 0.5) * QUADRADINHO, pe.y, (tileZ + dz + 0.5) * QUADRADINHO);
                if (dentroDoCone(jogador, ponto, alcance, angulo) && enxerga(jogador, ponto.add(0, 1.0, 0))) {
                    pontos.add(ponto);
                }
            }
        }
        return pontos;
    }

    private static List<LivingEntity> noCone(ServerPlayer jogador, double alcance, double angulo) {
        List<LivingEntity> lista = new ArrayList<>();
        for (LivingEntity criatura : jogador.level().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(alcance),
                criatura -> podeAtingir(jogador, criatura))) {
            Vec3 centro = criatura.getBoundingBox().getCenter();
            if (dentroDoCone(jogador, centro, alcance, angulo) && enxerga(jogador, centro)) {
                lista.add(criatura);
            }
        }
        return lista;
    }

    private static boolean dentroDoCone(ServerPlayer jogador, Vec3 ponto, double alcance, double angulo) {
        Vec3 ate = ponto.subtract(jogador.getEyePosition()).multiply(1, 0, 1);
        double distancia = ate.length();
        if (distancia > alcance) {
            return false;
        }
        if (distancia < QUADRADINHO * 0.75) {
            return true;
        }
        Vec3 olhar = jogador.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        double cosseno = ate.normalize().dot(olhar);
        return cosseno >= Math.cos(Math.toRadians(angulo / 2.0));
    }

    private static boolean enxerga(ServerPlayer jogador, Vec3 ponto) {
        Vec3 olho = jogador.getEyePosition();
        return jogador.level().clip(new ClipContext(olho, ponto, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador)).getType() == HitResult.Type.MISS;
    }

    // ---- Varinha amaldiçoada ----

    // Igual ao Shattered, a magia de uma varinha amaldiçoada sai aleatória (e quase sempre ruim):
    // nada, fogo ou gelo onde o raio para, gás tóxico, grama em volta, ou quem lançou é teletransportado
    public static void lancarAmaldicoada(ServerPlayer jogador) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 lugar = ondeORaioPara(jogador);
        rastro(jogador, lugar, Particulas.poeira(0x5A1E5A, 1.3F), 3);
        String mensagem;
        switch (jogador.getRandom().nextInt(6)) {
            case 0 -> {
                EfeitosDePocao.incendiar(mundo, lugar);
                mensagem = "fogo";
            }
            case 1 -> {
                EfeitosDePocao.congelar(mundo, lugar);
                mensagem = "gelo";
            }
            case 2 -> {
                NuvemDeGas.soltar(mundo, lugar.subtract(0, 1.0, 0), NuvemDeGas.Tipo.TOXICO, 6.0F, 10);
                mensagem = "gas";
            }
            case 3 -> {
                BlockPos pe = jogador.blockPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = (Math.floorDiv(pe.getX(), 4) + dx) * 4;
                        int z = (Math.floorDiv(pe.getZ(), 4) + dz) * 4;
                        Grama.crescer(mundo, x, pe.getY(), z);
                    }
                }
                mensagem = "grama";
            }
            case 4 -> {
                Teleporte.paraLugarAleatorio(mundo, jogador);
                mensagem = "teletransporte";
            }
            default -> mensagem = "nada";
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_amaldicoada." + mensagem).withStyle(ChatFormatting.DARK_PURPLE),
                false);
        som(jogador, jogador.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 0.6F);
    }

    // ---- Ajudantes ----

    // Dano mágico da varinha (a armadura não segura; quem lançou leva o crédito da morte)
    private static void ferir(ServerPlayer jogador, LivingEntity alvo, int dano, boolean alvoPrincipal) {
        // Refeição empoderadora: +2/+3 no alvo principal dos próximos 3 disparos
        if (alvoPrincipal) {
            dano += Talentos.bonusDaVarinha(jogador);
        }
        alvo.invulnerableTime = 0;
        if (dano > 0) {
            alvo.hurt(fonte(jogador), dano);
        }
        // Visão arcana do Mago
        if (alvo.isAlive()) {
            TalentosDoTier2.aoAcertarComVarinha(jogador, alvo);
        }
    }

    // Dano de magia "de longe" (sem ser corpo a corpo: não passa pelo golpe do Shattered)
    public static DamageSource fonte(ServerPlayer jogador) {
        return new DamageSource(jogador.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.INDIRECT_MAGIC),
                null, jogador);
    }

    // Quem a magia pode atingir: tudo que está vivo, menos os heróis e as imagens espelhadas
    private static boolean podeAtingir(ServerPlayer jogador, LivingEntity criatura) {
        return criatura.isAlive() && criatura != jogador && !(criatura instanceof Player) && !(criatura instanceof ImagemEspelhada) && !criatura.isSpectator();
    }

    private static List<LivingEntity> inimigosEmVolta(ServerPlayer jogador, Vec3 centro, double alcance) {
        return jogador.level().getEntitiesOfClass(LivingEntity.class, new AABB(centro, centro).inflate(alcance),
                criatura -> podeAtingir(jogador, criatura) && criatura.position().distanceTo(centro) <= alcance);
    }

    // Um raio que para no primeiro inimigo (ou na parede): o rastro e quem foi atingido
    @Nullable
    private static LivingEntity raioAteOAlvo(ServerPlayer jogador, TipoDeVarinha tipo, ParticleOptions particula) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(tipo.cor(), 1.2F), 3);
        rastro(jogador, fim, particula, 1);
        return alvo;
    }

    // Onde o raio para: no primeiro inimigo, ou um pouco antes da parede
    private static Vec3 ondeORaioPara(ServerPlayer jogador) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        return alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
    }

    private static void rastro(ServerPlayer jogador, Vec3 fim, ParticleOptions particula, int porBloco) {
        Vec3 inicio = jogador.getEyePosition().add(jogador.getViewVector(1.0F).scale(0.8)).subtract(0, 0.3, 0);
        linha(jogador.serverLevel(), inicio, fim, particula, porBloco);
    }

    private static void linha(ServerLevel mundo, Vec3 inicio, Vec3 fim, ParticleOptions particula, int porBloco) {
        Vec3 caminho = fim.subtract(inicio);
        int passos = Math.max(1, (int) (caminho.length() * porBloco));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = inicio.add(caminho.scale(i / (double) passos));
            mundo.sendParticles(particula, ponto.x, ponto.y, ponto.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void som(ServerPlayer jogador, Vec3 lugar, SoundEvent som, float volume, float tom) {
        jogador.level().playSound(null, lugar.x, lugar.y, lugar.z, som, SoundSource.PLAYERS, volume, tom);
    }
}
