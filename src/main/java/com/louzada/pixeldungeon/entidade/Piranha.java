package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Piranha (a piscina): só vive na água, nada rápido (o dobro do herói) e morde forte. Fora da água, morre na hora.
// Igual ao Shattered, os números crescem com o andar: vida 10 + 5 x andar, precisão 20 + 2 x andar, esquiva 10 + 2 x andar,
// dano andar a 4 + 2 x andar, armadura 0 a andar. Não dá XP e sempre deixa uma carne misteriosa. Não pega fogo.
public class Piranha extends MobDoShattered {
    public static final Atributos ATRIBUTOS = atributosDoAndar(1);
    public static final Sons SONS = new Sons(SoundEvents.COD_FLOP, SoundEvents.COD_HURT, SoundEvents.COD_DEATH);
    // Fora da água por tanto tempo, morre (uns instantes, pra não morrer por um respingo)
    private static final int TICKS_FORA_DA_AGUA = 10;

    private int andar = 1;
    private int foraDaAgua = 0;

    public Piranha(EntityType<? extends Piranha> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
        this.moveControl = new Nadar(this);
    }

    public static Atributos atributosDoAndar(int andar) {
        return new Atributos(10 + 5 * andar, 20 + 2 * andar, 10 + 2 * andar, andar, 4 + 2 * andar, 0, andar, 0, 30, 0.35);
    }

    public static AttributeSupplier.Builder atributosDaPiranha() {
        return atributosDoMinecraft(ATRIBUTOS);
    }

    public void definirAndar(int novo) {
        andar = Math.max(1, novo);
        trocarAtributos(atributosDoAndar(andar));
        var vida = getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(atributos().vida());
        }
        setHealth(getMaxHealth());
    }

    @Override
    protected PathNavigation createNavigation(Level mundo) {
        return new WaterBoundPathNavigation(this, mundo);
    }

    @Override
    protected Goal passeio() {
        return new RandomSwimmingGoal(this, 1.0, 20);
    }

    @Override
    protected boolean nadaNaAgua() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // Na água, anda do jeito dela (sem afundar nem boiar); fora, cai
    @Override
    public void travel(Vec3 vontade) {
        if (isControlledByLocalInstance() && isInWater()) {
            Vec3 movimento = getDeltaMovement();
            if (!onGround()) {
                movimento = movimento.add(0, -0.01, 0);
            }
            move(MoverType.SELF, movimento);
            setDeltaMovement(movimento.scale(0.8));
        } else {
            super.travel(vontade);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (isInWater()) {
            // Peixe não se afoga
            setAirSupply(getMaxAirSupply());
            foraDaAgua = 0;
        } else if (++foraDaAgua >= TICKS_FORA_DA_AGUA) {
            // Igual ao Shattered: fora da água (ou voando), a piranha morre
            hurt(damageSources().dryOut(), Float.MAX_VALUE);
        }
    }

    // Sempre deixa uma carne misteriosa
    @Override
    public ItemStack sortearSaque() {
        return new ItemStack(ModItens.CARNE_MISTERIOSA.get());
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("andar", andar);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        andar = Math.max(1, tag.getInt("andar"));
        trocarAtributos(atributosDoAndar(andar));
    }

    // Prateada (o sangue espirra dessa cor)
    @Override
    public int corDoSangue() {
        return 0xCC2222;
    }

    // O andar onde ela está (pra quem cria a piranha sem dizer o andar)
    public void ajustarAoAndar() {
        definirAndar(SaqueDoAndar.andarDe(this));
    }

    // Anda na água direto pro ponto do caminho, acelerando aos poucos (sem a física de boiar)
    private static class Nadar extends MoveControl {
        Nadar(Piranha piranha) {
            super(piranha);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO || !mob.isInWater() || mob.getNavigation().isDone()) {
                mob.setSpeed(0);
                mob.setZza(0);
                return;
            }
            double dx = wantedX - mob.getX();
            double dy = wantedY - mob.getY();
            double dz = wantedZ - mob.getZ();
            double distancia = Math.sqrt(dx * dx + dz * dz);
            if (distancia < 0.05) {
                operation = Operation.WAIT;
                return;
            }
            float giro = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), giro, 30.0F));
            mob.yBodyRot = mob.getYRot();
            double velocidade = speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double aceleracao = velocidade / 4;
            mob.setDeltaMovement(mob.getDeltaMovement().add(dx / distancia * aceleracao, Mth.clamp(dy, -0.5, 0.5) * 0.02, dz / distancia * aceleracao));
            mob.setSpeed((float) velocidade);
        }
    }
}
