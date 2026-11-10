package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.EfeitosDasPedras;
import com.louzada.pixeldungeon.item.EfeitosDePergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Uma pedra rúnica voando. O efeito acontece onde ela cair (no quadradinho dela) e a pedra some.
// Se ela bate numa parede, vale o quadradinho da frente da parede; se bate em alguém, o quadradinho dele
// (a de teletransporte, igual ao Shattered, leva pro quadradinho de antes de quem ela acertou).
public class PedraArremessada extends ThrowableItemProjectile {

    public PedraArremessada(EntityType<? extends PedraArremessada> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public PedraArremessada(Level mundo, LivingEntity dono) {
        super(ModEntidades.PEDRA_ARREMESSADA.get(), dono, mundo);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.PEDRAS.get(0).get();
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
        Entity atingido = null;
        Vec3 antes = position();
        if (resultado instanceof BlockHitResult bloco) {
            // Um pouco pra fora do bloco em que bateu
            lugar = lugar.add(Vec3.atLowerCornerOf(bloco.getDirection().getNormal()).scale(0.3));
            antes = lugar;
        } else if (resultado instanceof EntityHitResult acerto) {
            atingido = acerto.getEntity();
            lugar = atingido.position();
            // Projéteis improvisados (Guerreiro): acertou um inimigo em cheio
            if (atingido instanceof LivingEntity alvo && getOwner() instanceof ServerPlayer dono) {
                TalentosDoTier2.aoAcertarArremesso(dono, alvo);
            }
        }
        if (getOwner() instanceof LivingEntity dono && EfeitosDePergaminhoExotico.semMagia(dono)) {
            // Igual ao Shattered: imune à magia, a pedra só cai no chão (dá pra pegar de volta)
            spawnAtLocation(getItem().copyWithCount(1));
        } else if (getItem().getItem() instanceof ItemPedraRunica pedra) {
            EfeitosDasPedras.cair(mundo, pedra.tipo(), lugar, antes, atingido, getOwner());
        }
        discard();
    }
}
