package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.item.PoteDeMel;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Um pote de mel voando: quebra onde cair (solta a abelha e larga o pote quebrado no chão)
public class PoteArremessado extends ThrowableItemProjectile {

    public PoteArremessado(EntityType<? extends PoteArremessado> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public PoteArremessado(Level mundo, LivingEntity dono) {
        super(ModEntidades.POTE_ARREMESSADO.get(), dono, mundo);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.POTE_DE_MEL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (isRemoved() || !(level() instanceof ServerLevel mundo)) {
            return;
        }
        Vec3 lugar = resultado.getLocation();
        if (resultado instanceof BlockHitResult bloco) {
            lugar = lugar.add(Vec3.atLowerCornerOf(bloco.getDirection().getNormal()).scale(0.3));
        } else if (resultado instanceof EntityHitResult acerto) {
            // Bateu em alguém: o pote cai do lado
            lugar = acerto.getEntity().position().add(getDeltaMovement().normalize().scale(-0.8));
        }
        ItemEntity pote = new ItemEntity(mundo, lugar.x, lugar.y, lugar.z, PoteDeMel.quebrar(mundo, lugar, null));
        pote.setUnlimitedLifetime();
        mundo.addFreshEntity(pote);
        discard();
    }
}
