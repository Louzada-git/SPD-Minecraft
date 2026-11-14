package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.bloco.FogoEterno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Nuvem de gás do Shattered (poções arremessadas e plantas). Fica parada no lugar e, a cada turno,
// pega quem estiver dentro dela:
//   tóxico: 1 + andar / 5 de dano por turno
//   paralisante: paralisa por 10 turnos
//   confusão (poção de levitação arremessada): vertigem por 2 turnos
//   corrosivo (varinha da corrosão): dano que começa na força da nuvem e cresce 1 a cada turno
//   fedor (o rato fétido, quando apanha): paralisa por 2 turnos (o rato fétido não sente)
//   congelante (armadilha de resfriamento): resfria +3 turnos por turno (+5 na água), até 10; já no máximo, congela por 10 turnos
//     (quem já está congelado fica congelado mais um pouco)
//   elétrico (armadilha elétrica): paralisa pelos turnos que faltam pra nuvem acabar e, de 2 em 2 turnos, dá um choque
//     de 0 a 2 + andar / 5
//   neblina (poção da neblina): ninguém enxerga através dela: os inimigos dentro perdem o alvo de vista e o herói
//     dentro fica escondido
//   tempestade (poção de nuvens tempestuosas): chove; apaga o fogo de quem está dentro e do chão
// Quem bebeu a poção de pureza não é afetado, e a pureza arremessada desfaz as nuvens.
public class NuvemDeGas extends Entity {
    public enum Tipo {
        TOXICO(0x8CC63F),
        PARALISANTE(0xFFE97F),
        CONFUSAO(0xC98BFF),
        CORROSIVO(0xDD8833),
        FEDOR(0x4D6B2E),
        CONGELANTE(0xB2D6FF),
        ELETRICO(0xFFFF66),
        NEBLINA(0x707070),
        TEMPESTADE(0x8899BB);

        private int cor;

        Tipo(int cor) {
            this.cor = cor;
        }

        public int cor() {
            return cor;
        }
    }

    private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(NuvemDeGas.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RAIO = SynchedEntityData.defineId(NuvemDeGas.class, EntityDataSerializers.FLOAT);
    private static final double ALTURA = 3.0;

    private int ticksRestantes = 10 * Turno.TICKS;
    // Gás corrosivo: o dano do primeiro turno
    private int forca = 2;

    public NuvemDeGas(EntityType<? extends NuvemDeGas> tipo, Level mundo) {
        super(tipo, mundo);
        noPhysics = true;
    }

    public static NuvemDeGas soltar(ServerLevel mundo, Vec3 lugar, Tipo tipo, float raio, int turnos) {
        NuvemDeGas nuvem = new NuvemDeGas(ModEntidades.NUVEM_DE_GAS.get(), mundo);
        nuvem.setPos(lugar.x, lugar.y, lugar.z);
        nuvem.entityData.set(TIPO, tipo.ordinal());
        nuvem.entityData.set(RAIO, raio);
        nuvem.ticksRestantes = turnos * Turno.TICKS;
        mundo.addFreshEntity(nuvem);
        return nuvem;
    }

    public void definirForca(int valor) {
        forca = valor;
    }

    public Tipo tipo() {
        return Tipo.values()[Math.floorMod(entityData.get(TIPO), Tipo.values().length)];
    }

    public float raio() {
        return entityData.get(RAIO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        construtor.define(TIPO, 0);
        construtor.define(RAIO, 4.0F);
    }

    @Override
    public void tick() {
        super.tick();
        float raio = raio();
        if (level().isClientSide()) {
            // Fumaça da cor do gás espalhada pela nuvem
            for (int i = 0; i < Math.max(2, (int) raio); i++) {
                double angulo = random.nextDouble() * Math.PI * 2;
                double distancia = Math.sqrt(random.nextDouble()) * raio;
                double x = getX() + Math.cos(angulo) * distancia;
                double y = getY() + random.nextDouble() * ALTURA * 0.6;
                double z = getZ() + Math.sin(angulo) * distancia;
                if (tipo() == Tipo.ELETRICO) {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.0, 0.0);
                } else if (tipo() == Tipo.CONGELANTE) {
                    level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, -0.01, 0.0);
                } else if (tipo() == Tipo.NEBLINA) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.005, 0.0);
                    level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0, 0.003, 0.0);
                } else if (tipo() == Tipo.TEMPESTADE) {
                    level().addParticle(ParticleTypes.FALLING_WATER, x, getY() + ALTURA * 0.9, z, 0.0, 0.0, 0.0);
                    level().addParticle(Particulas.poeira(tipo().cor(), 2.5F), x, getY() + ALTURA * 0.8, z, 0.0, 0.0, 0.0);
                } else {
                    level().addParticle(Particulas.poeira(tipo().cor(), 2.5F), x, y, z, 0.0, 0.01, 0.0);
                }
            }
            return;
        }
        if (--ticksRestantes <= 0) {
            discard();
            return;
        }
        if (tickCount % Turno.TICKS != 0) {
            return;
        }
        // O gelo e a chuva apagam o fogo eterno (a parede inteira)
        if ((tipo() == Tipo.CONGELANTE || tipo() == Tipo.TEMPESTADE) && level() instanceof ServerLevel servidor) {
            FogoEterno.apagarPerto(servidor, position(), raio + 1);
        }
        // A chuva apaga o fogo do chão
        if (tipo() == Tipo.TEMPESTADE) {
            BlockPos meio = blockPosition();
            int alcance = (int) Math.ceil(raio);
            for (BlockPos posicao : BlockPos.betweenClosed(meio.offset(-alcance, -1, -alcance), meio.offset(alcance, 2, alcance))) {
                if (level().getBlockState(posicao).is(BlockTags.FIRE)) {
                    level().removeBlock(posicao, false);
                }
            }
        }
        for (LivingEntity criatura : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(raio, ALTURA, raio))) {
            if (!criatura.isAlive() || criatura.isSpectator() || criatura.hasEffect(ModEfeitos.IMUNE_A_GAS)
                    || (tipo() == Tipo.TOXICO && criatura.hasEffect(ModEfeitos.ESSENCIA_TOXICA))
                    || criatura.distanceToSqr(getX(), criatura.getY(), getZ()) > raio * raio) {
                continue;
            }
            afetar(criatura);
        }
    }

    // O que a nuvem faz num turno em quem está dentro dela
    public void afetar(LivingEntity criatura) {
        switch (tipo()) {
            case TOXICO -> {
                int andar = Math.max(1, Andares.andarPelaAltura(criatura.getBlockY()));
                criatura.invulnerableTime = 0;
                criatura.hurt(damageSources().magic(), 1 + andar / 5);
            }
            case PARALISANTE -> {
                MobEffectInstance atual = criatura.getEffect(ModEfeitos.PARALISADO);
                if (atual == null || atual.getDuration() < 10 * Turno.TICKS) {
                    criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 10 * Turno.TICKS));
                }
            }
            case CONFUSAO -> EfeitosNoTick.tontear(criatura, 2 * Turno.TICKS);
            case CORROSIVO -> {
                criatura.invulnerableTime = 0;
                criatura.hurt(damageSources().magic(), forca + tickCount / Turno.TICKS);
            }
            case FEDOR -> {
                MobEffectInstance atual = criatura.getEffect(ModEfeitos.PARALISADO);
                if (!(criatura instanceof RatoFetido) && (atual == null || atual.getDuration() < 2 * Turno.TICKS)) {
                    criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, 2 * Turno.TICKS));
                }
            }
            case CONGELANTE -> congelar(criatura);
            case ELETRICO -> eletrocutar(criatura);
            case NEBLINA -> {
                if (criatura instanceof MobDoShattered inimigo) {
                    inimigo.setTarget(null);
                    inimigo.addEffect(new MobEffectInstance(ModEfeitos.CEGO, 2 * Turno.TICKS));
                } else if (criatura instanceof ServerPlayer jogador) {
                    Furtividade.ficarInvisivel(jogador, 2 * Turno.TICKS);
                }
            }
            case TEMPESTADE -> criatura.clearFire();
        }
    }

    // O "Freezing" do Shattered
    private void congelar(LivingEntity criatura) {
        criatura.clearFire();
        MobEffectInstance congelado = criatura.getEffect(ModEfeitos.CONGELADO);
        if (congelado != null) {
            criatura.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, Math.max(congelado.getDuration(), 2 * Turno.TICKS)));
            return;
        }
        MobEffectInstance frio = criatura.getEffect(ModEfeitos.RESFRIADO);
        int antes = frio == null ? 0 : frio.getDuration();
        int maximo = 10 * Turno.TICKS;
        int depois = Math.min(maximo, Math.max(antes, antes + (criatura.isInWater() ? 5 : 3) * Turno.TICKS));
        if (depois > antes) {
            criatura.addEffect(new MobEffectInstance(ModEfeitos.RESFRIADO, depois));
        }
        criatura.setTicksFrozen(Math.max(criatura.getTicksFrozen(), 100));
        if (frio != null && depois >= maximo) {
            criatura.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, 10 * Turno.TICKS));
            criatura.setTicksFrozen(Math.max(criatura.getTicksFrozen(), 160));
        }
    }

    // O "Electricity" do Shattered
    private void eletrocutar(LivingEntity criatura) {
        int turnosQueFaltam = Math.max(1, Turno.turnos(ticksRestantes));
        if (!criatura.hasEffect(ModEfeitos.PARALISADO)) {
            criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, turnosQueFaltam * Turno.TICKS));
        }
        if (turnosQueFaltam % 2 == 1) {
            int andar = Math.max(1, Andares.andarPelaAltura(criatura.getBlockY()));
            int dano = Math.round(random.nextFloat() * (2 + andar / 5F));
            if (dano > 0) {
                criatura.invulnerableTime = 0;
                criatura.hurt(damageSources().lightningBolt(), dano);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TIPO, tag.getInt("tipo"));
        entityData.set(RAIO, tag.getFloat("raio"));
        ticksRestantes = tag.getInt("ticks_restantes");
        forca = tag.contains("forca") ? tag.getInt("forca") : 2;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("tipo", entityData.get(TIPO));
        tag.putFloat("raio", entityData.get(RAIO));
        tag.putInt("ticks_restantes", ticksRestantes);
        tag.putInt("forca", forca);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
