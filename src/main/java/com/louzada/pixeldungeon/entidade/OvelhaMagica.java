package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Ovelha mágica (o "Sheep" do Shattered, da armadilha de rebanho): fica parada no lugar, ruminando, até sumir
// sozinha depois de uns turnos (6, mais ou menos 2). Ninguém consegue machucar nem empurrar, e ela bloqueia a passagem.
// Igual ao Shattered, ela ativa até as armadilhas escondidas em que nascer em cima.
// Clique direito: "Bééé!".
public class OvelhaMagica extends PathfinderMob {
    private static final String[] BALIDOS = { "entidade.pixeldungeon.ovelha_magica.bee1", "entidade.pixeldungeon.ovelha_magica.bee2",
            "entidade.pixeldungeon.ovelha_magica.bee3", "entidade.pixeldungeon.ovelha_magica.bee4" };

    private int ticksRestantes = Turno.ticks(6);

    public OvelhaMagica(EntityType<? extends OvelhaMagica> tipo, Level mundo) {
        super(tipo, mundo);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    // Uma ovelha no lugar, que dura "turnos" turnos (mais ou menos 2)
    public static OvelhaMagica criar(ServerLevel mundo, Vec3 lugar, float turnos) {
        OvelhaMagica ovelha = new OvelhaMagica(ModEntidades.OVELHA_MAGICA.get(), mundo);
        ovelha.moveTo(lugar.x, lugar.y, lugar.z, mundo.getRandom().nextFloat() * 360.0F, 0.0F);
        ovelha.ticksRestantes = Turno.ticks(Math.max(1, turnos + (mundo.getRandom().nextFloat() * 4 - 2)));
        mundo.addFreshEntity(ovelha);
        Particulas.explosao(mundo, ParticleTypes.CLOUD, lugar.x, lugar.y + 0.5, lugar.z, 8, 0.4, 0.02);
        return ovelha;
    }

    public int ticksRestantes() {
        return ticksRestantes;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel mundo && --ticksRestantes <= 0) {
            Particulas.explosao(mundo, ParticleTypes.CLOUD, getX(), getY(0.5), getZ(), 10, 0.4, 0.02);
            mundo.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 0.8F, 1.0F);
            discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (!level().isClientSide() && mao == InteractionHand.MAIN_HAND) {
            NumerosFlutuantes.mostrarTraduzido(this, BALIDOS[getRandom().nextInt(BALIDOS.length)], NumerosFlutuantes.BRANCO);
            level().playSound(null, blockPosition(), SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.91F + getRandom().nextFloat() * 0.19F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // Nada machuca a ovelha (só o /kill), e nenhum efeito pega nela
    @Override
    public boolean isInvulnerableTo(DamageSource fonte) {
        return !fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance efeito) {
        return false;
    }

    // Parada que nem um bloco: não é empurrada e ninguém passa por ela
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity outro) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fonte) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ticks_restantes", ticksRestantes);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ticks_restantes")) {
            ticksRestantes = tag.getInt("ticks_restantes");
        }
    }
}
