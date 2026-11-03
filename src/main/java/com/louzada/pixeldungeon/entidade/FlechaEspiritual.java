package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.heroi.CombateDoHeroi;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Flecha espiritual do arco da Caçadora: é um ataque físico (sorteia acerto, a armadura segura),
// some quando bate em algo e não pode ser pega de volta.
public class FlechaEspiritual extends ThrowableItemProjectile {
    private static final int DURACAO = 40;
    private static final int AZUL = 0x88DDFF;

    public FlechaEspiritual(EntityType<? extends FlechaEspiritual> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public FlechaEspiritual(Level mundo, LivingEntity dono) {
        super(ModEntidades.FLECHA_ESPIRITUAL.get(), dono, mundo);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.FLECHA_ESPIRITUAL.get();
    }

    // Voa reto, igual às flechas do Shattered
    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            Vec3 movimento = getDeltaMovement();
            for (int i = 0; i < 2; i++) {
                double recuo = i / 2.0;
                level().addParticle(Particulas.poeira(AZUL, 0.9F), getX() - movimento.x * recuo, getY() - movimento.y * recuo,
                        getZ() - movimento.z * recuo, 0.0, 0.0, 0.0);
            }
        } else if (tickCount > DURACAO) {
            discard();
        }
    }

    // Passa pelos outros heróis (sem fogo amigo)
    @Override
    protected boolean canHitEntity(Entity alvo) {
        if (alvo instanceof Player && getOwner() instanceof Player) {
            return false;
        }
        return super.canHitEntity(alvo);
    }

    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        super.onHitEntity(resultado);
        if (level().isClientSide() || !(resultado.getEntity() instanceof LivingEntity alvo)) {
            return;
        }
        if (getOwner() instanceof ServerPlayer jogador) {
            // As regras do herói: acerto, dano do arco, armadura e talentos (ataque consecutivo marca o alvo)
            float dano = CombateDoHeroi.calcular(jogador, alvo, ModItens.ARCO_ESPIRITUAL.get(), 1, 1, 1, 0, false, true);
            if (dano >= 0) {
                CombateDoHeroi.aplicar(alvo, dano, damageSources().thrown(this, jogador));
            }
        } else {
            alvo.hurt(damageSources().thrown(this, getOwner()), 2);
        }
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (!level().isClientSide() && !isRemoved()) {
            if (level() instanceof ServerLevel mundo) {
                Particulas.explosao(mundo, Particulas.poeira(AZUL, 1.2F), getX(), getY(), getZ(), 8, 0.2, 0.0);
                Particulas.explosao(mundo, ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 4, 0.1, 0.1);
            }
            playSound(SoundEvents.ARROW_HIT, 0.6F, 1.5F);
            discard();
        }
    }
}
