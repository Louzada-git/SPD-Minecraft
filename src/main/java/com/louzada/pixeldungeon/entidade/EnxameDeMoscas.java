package com.louzada.pixeldungeon.entidade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

// Enxame de moscas (andares 3 e 4): voa, e cada golpe que leva divide ele em dois enxames menores.
// Igual ao Shattered: se a vida for pelo menos o dano + 2, metade do que sobrar vira um enxame novo.
// Os enxames que nasceram de uma divisão não dão XP.
public class EnxameDeMoscas extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(50, 10, 5, 1, 4, 0, 0, 3, 9, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.BEE_LOOP_AGGRESSIVE, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH);

    // 0 = o enxame original; 1, 2, 3... = pedaços
    private int geracao = 0;

    public EnxameDeMoscas(EntityType<? extends EnxameDeMoscas> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder atributosDoEnxame() {
        return atributosDoMinecraft(ATRIBUTOS).add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected PathNavigation createNavigation(Level mundo) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, mundo);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(true);
        navegacao.setCanPassDoors(true);
        return navegacao;
    }

    @Override
    protected Goal passeio() {
        return new WaterAvoidingRandomFlyingGoal(this, 0.8);
    }

    // Moscas não se machucam caindo
    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    public int xpAoMorrer() {
        return geracao == 0 ? super.xpAoMorrer() : 0;
    }

    // Chamado antes de levar o golpe: se dá pra dividir, nasce um enxame novo do lado
    public void dividir(float dano) {
        if (!(level() instanceof ServerLevel mundo) || getHealth() < dano + 2) {
            return;
        }
        int vidaDoNovo = (int) ((getHealth() - dano) / 2);
        if (vidaDoNovo < 1) {
            return;
        }
        EnxameDeMoscas novo = (EnxameDeMoscas) getType().create(mundo);
        if (novo == null) {
            return;
        }
        novo.geracao = geracao + 1;
        novo.moveTo(getX() + (getRandom().nextDouble() - 0.5), getY(), getZ() + (getRandom().nextDouble() - 0.5), getYRot(), 0.0F);
        novo.setPersistenceRequired();
        mundo.addFreshEntity(novo);
        // A vida máxima continua a do enxame grande, então a barra mostra o tamanho do pedaço
        var vidaMaxima = getAttribute(Attributes.MAX_HEALTH);
        var vidaMaximaDoNovo = novo.getAttribute(Attributes.MAX_HEALTH);
        if (vidaMaxima != null && vidaMaximaDoNovo != null) {
            vidaMaximaDoNovo.setBaseValue(vidaMaxima.getBaseValue());
        }
        novo.setHealth(vidaDoNovo);
        novo.setTarget(getTarget());
        novo.atualizarBarraDeVida();
        setHealth(getHealth() - vidaDoNovo);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("geracao", geracao);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        geracao = tag.getInt("geracao");
    }
}
