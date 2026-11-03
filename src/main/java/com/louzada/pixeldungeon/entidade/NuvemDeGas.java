package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
// Quem bebeu a poção de pureza não é afetado, e a pureza arremessada desfaz as nuvens.
public class NuvemDeGas extends Entity {
    public enum Tipo {
        TOXICO(0x8CC63F),
        PARALISANTE(0xFFE97F),
        CONFUSAO(0xC98BFF);

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
                level().addParticle(Particulas.poeira(tipo().cor(), 2.5F), getX() + Math.cos(angulo) * distancia,
                        getY() + random.nextDouble() * ALTURA * 0.6, getZ() + Math.sin(angulo) * distancia, 0.0, 0.01, 0.0);
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
        for (LivingEntity criatura : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(raio, ALTURA, raio))) {
            if (!criatura.isAlive() || criatura.isSpectator() || criatura.hasEffect(ModEfeitos.IMUNE_A_GAS)
                    || criatura.distanceToSqr(getX(), criatura.getY(), getZ()) > raio * raio) {
                continue;
            }
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
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TIPO, tag.getInt("tipo"));
        entityData.set(RAIO, tag.getFloat("raio"));
        ticksRestantes = tag.getInt("ticks_restantes");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("tipo", entityData.get(TIPO));
        tag.putFloat("raio", entityData.get(RAIO));
        tag.putInt("ticks_restantes", ticksRestantes);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
