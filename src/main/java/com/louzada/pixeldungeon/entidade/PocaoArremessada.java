package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.item.EfeitosDePocao;
import com.louzada.pixeldungeon.item.IdentidadesDaRun;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Uma poção voando (agachado + clique direito). Quebra onde bater, igual ao Shattered:
// as poções de gás, gelo e chama soltam o efeito ali; as outras só espirram.
// Quem arremessou descobre o tipo se o efeito aparecer.
public class PocaoArremessada extends ThrowableItemProjectile {

    public PocaoArremessada(EntityType<? extends PocaoArremessada> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public PocaoArremessada(Level mundo, LivingEntity dono) {
        super(ModEntidades.POCAO_ARREMESSADA.get(), dono, mundo);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.POCOES.get(0).get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (level().isClientSide() || isRemoved() || !(level() instanceof ServerLevel mundo)) {
            return;
        }
        if (getItem().getItem() instanceof ItemPocao pocao) {
            TipoDePocao tipo = IdentidadesDaRun.tipoDaCor(Identificacao.sementeDaRun(mundo), pocao.cor());
            Vec3 lugar = resultado.getLocation();
            boolean apareceu = EfeitosDePocao.quebrar(mundo, lugar, tipo, pocao.cor(), getOwner());
            if (apareceu && getOwner() instanceof ServerPlayer jogador) {
                Identificacao.identificar(jogador, pocao.cor());
            }
        }
        discard();
    }
}
