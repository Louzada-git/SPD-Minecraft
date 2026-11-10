package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.EfeitosDePocao;
import com.louzada.pixeldungeon.item.EfeitosDePocaoExotica;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.IdentidadesDaRun;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDePocaoExotica;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Uma poção voando (agachado + clique direito). Quebra onde bater, igual ao Shattered:
// as poções de gás, gelo e chama soltam o efeito ali; as outras só espirram. As exóticas também (ver EfeitosDePocaoExotica).
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
        // Projéteis improvisados (Guerreiro): acertou um inimigo em cheio
        if (resultado instanceof EntityHitResult acerto && acerto.getEntity() instanceof LivingEntity alvo && getOwner() instanceof ServerPlayer dono) {
            TalentosDoTier2.aoAcertarArremesso(dono, alvo);
        }
        if (getItem().getItem() instanceof ItemPocao pocao) {
            TipoDePocao tipo = IdentidadesDaRun.tipoDaCor(Identificacao.sementeDaRun(mundo), pocao.cor());
            Vec3 lugar = resultado.getLocation();
            boolean apareceu = EfeitosDePocao.quebrar(mundo, lugar, tipo, pocao.cor(), getOwner());
            if (apareceu && getOwner() instanceof ServerPlayer jogador) {
                Identificacao.identificar(jogador, pocao.cor());
            }
        } else if (getItem().getItem() instanceof ItemPocaoExotica exotica) {
            TipoDePocaoExotica tipo = TipoDePocaoExotica.doBase(IdentidadesDaRun.tipoDaCor(Identificacao.sementeDaRun(mundo), exotica.cor()));
            Entity atingido = resultado instanceof EntityHitResult acerto ? acerto.getEntity() : null;
            boolean apareceu = EfeitosDePocaoExotica.quebrar(mundo, resultado.getLocation(), tipo, exotica.cor(), getOwner(), atingido);
            if (apareceu && getOwner() instanceof ServerPlayer jogador) {
                Identificacao.identificar(jogador, exotica.cor());
            }
        } else if (getItem().getItem() instanceof FrutaSemGraca) {
            // A fruta-sem-graça cozida: as de poção ruim (e levitação e pureza) quebram igual à poção e deixam pedaços;
            // as outras caem inteiras
            TipoDePocao tipo = FrutaSemGraca.pocao(getItem());
            Vec3 lugar = resultado.getLocation();
            if (FrutaSemGraca.quebra(tipo)) {
                EfeitosDePocao.quebrar(mundo, lugar, tipo, IdentidadesDaRun.corDoTipo(Identificacao.sementeDaRun(mundo), tipo), getOwner());
                SaqueDoAndar.soltar(mundo, lugar.x, lugar.y, lugar.z, new ItemStack(ModItens.PEDACOS_DE_FRUTA.get()));
            } else {
                SaqueDoAndar.soltar(mundo, lugar.x, lugar.y, lugar.z, getItem().copyWithCount(1));
            }
        }
        discard();
    }
}
