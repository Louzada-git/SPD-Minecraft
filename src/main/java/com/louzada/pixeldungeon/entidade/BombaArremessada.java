package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.nbt.CompoundTag;
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

// Uma bomba voando. Onde ela cair: acesa, vira uma bomba acesa no chão (explode 2 turnos depois);
// apagada, só cai como item. Igual ao Shattered, ela não machuca quem acertar.
public class BombaArremessada extends ThrowableItemProjectile {
    private boolean acesa = true;

    public BombaArremessada(EntityType<? extends BombaArremessada> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public BombaArremessada(Level mundo, LivingEntity dono, boolean acesa) {
        super(ModEntidades.BOMBA_ARREMESSADA.get(), dono, mundo);
        this.acesa = acesa;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.BOMBAS.get(0).get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (level().isClientSide() || isRemoved() || !(level() instanceof ServerLevel mundo)) {
            return;
        }
        Vec3 lugar = resultado.getLocation();
        if (resultado instanceof BlockHitResult bloco) {
            // Um pouco pra fora do bloco em que bateu
            lugar = lugar.add(Vec3.atLowerCornerOf(bloco.getDirection().getNormal()).scale(0.3));
        } else if (resultado instanceof EntityHitResult acerto) {
            lugar = acerto.getEntity().position();
        }
        if (acesa) {
            BombaAcesa.acender(mundo, lugar, getItem().copyWithCount(1), getOwner());
        } else {
            ItemEntity item = new ItemEntity(mundo, lugar.x, lugar.y, lugar.z, getItem().copyWithCount(1));
            mundo.addFreshEntity(item);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("acesa", acesa);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        acesa = !tag.contains("acesa") || tag.getBoolean("acesa");
    }
}
