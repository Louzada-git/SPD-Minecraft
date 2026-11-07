package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// Uma semente voando. Igual ao Shattered, a planta nasce onde ela cair
// (se cair em alguém, nasce embaixo dele e já é ativada).
public class SementeArremessada extends ThrowableItemProjectile {

    public SementeArremessada(EntityType<? extends SementeArremessada> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public SementeArremessada(Level mundo, LivingEntity dono) {
        super(ModEntidades.SEMENTE_ARREMESSADA.get(), dono, mundo);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.SEMENTES.get(0).get();
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        BlockPos lugar = blockPosition();
        if (resultado instanceof BlockHitResult bloco) {
            lugar = bloco.getBlockPos().relative(bloco.getDirection());
        } else if (resultado instanceof EntityHitResult entidade) {
            lugar = entidade.getEntity().blockPosition();
            // Projéteis improvisados (Guerreiro): acertou um inimigo em cheio
            if (entidade.getEntity() instanceof LivingEntity alvo && getOwner() instanceof ServerPlayer dono) {
                TalentosDoTier2.aoAcertarArremesso(dono, alvo);
            }
        }
        // Desce até achar chão (até 3 blocos)
        for (int i = 0; i < 3 && !level().getBlockState(lugar.below()).isFaceSturdy(level(), lugar.below(), Direction.UP); i++) {
            lugar = lugar.below();
        }
        boolean plantou = getItem().getItem() instanceof ItemSemente semente && ItemSemente.plantar(level(), lugar, semente.planta());
        if (!plantou) {
            // Não deu pra plantar: a semente cai no chão
            ItemEntity noChao = new ItemEntity(level(), getX(), getY(), getZ(), getItem().copy());
            noChao.setUnlimitedLifetime();
            level().addFreshEntity(noChao);
        }
        discard();
    }
}
