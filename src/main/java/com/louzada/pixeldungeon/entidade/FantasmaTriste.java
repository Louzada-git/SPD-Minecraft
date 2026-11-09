package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// O fantasma triste (o "Ghost" do Shattered): aparece na sala da saída de um dos andares 2 a 4 e dá a missão.
// Ninguém consegue machucá-lo nem mexer nele; ele flutua devagar pela sala. Clique direito: conversar (ver MissaoDoFantasma).
public class FantasmaTriste extends PathfinderMob {

    public FantasmaTriste(EntityType<? extends FantasmaTriste> tipo, Level mundo) {
        super(tipo, mundo);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.15);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (jogador instanceof ServerPlayer heroi && mao == InteractionHand.MAIN_HAND) {
            MissaoDoFantasma.conversar(heroi, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // Nada machuca o fantasma (só o /kill)
    @Override
    public boolean isInvulnerableTo(DamageSource fonte) {
        return !fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance efeito) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity outro) {
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fonte) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }
}
