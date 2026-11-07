package com.louzada.pixeldungeon.heroi;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.entidade.ImagemEspelhada;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Pra onde o herói está mirando (o "escolha um alvo" do Shattered vira a mira do Minecraft)
public class Mira {

    // O inimigo na mira, até essa distância (paredes bloqueiam; outros jogadores não contam)
    @Nullable
    public static LivingEntity inimigo(ServerPlayer jogador, double alcance) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getViewVector(1.0F);
        Vec3 fim = olho.add(olhar.scale(alcance));
        HitResult bloco = jogador.level().clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        if (bloco.getType() != HitResult.Type.MISS) {
            fim = bloco.getLocation();
        }
        AABB caixa = jogador.getBoundingBox().expandTowards(olhar.scale(alcance)).inflate(1.0);
        EntityHitResult resultado = ProjectileUtil.getEntityHitResult(jogador, olho, fim, caixa,
                entidade -> entidade instanceof LivingEntity && entidade.isAlive() && !entidade.isSpectator() && !(entidade instanceof Player),
                olho.distanceToSqr(fim));
        return resultado != null && resultado.getEntity() instanceof LivingEntity alvo ? alvo : null;
    }

    // O aliado na mira (outro jogador vivo ou uma imagem espelhada), até essa distância
    @Nullable
    public static LivingEntity aliado(ServerPlayer jogador, double alcance) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getViewVector(1.0F);
        Vec3 fim = olho.add(olhar.scale(alcance));
        HitResult bloco = jogador.level().clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        if (bloco.getType() != HitResult.Type.MISS) {
            fim = bloco.getLocation();
        }
        AABB caixa = jogador.getBoundingBox().expandTowards(olhar.scale(alcance)).inflate(1.0);
        EntityHitResult resultado = ProjectileUtil.getEntityHitResult(jogador, olho, fim, caixa,
                entidade -> entidade.isAlive() && !entidade.isSpectator() && (entidade instanceof Player || entidade instanceof ImagemEspelhada),
                olho.distanceToSqr(fim));
        return resultado != null && resultado.getEntity() instanceof LivingEntity alvo ? alvo : null;
    }

    // O ponto onde a mira bate numa parede (ou o fim do alcance, se não bater em nada)
    public static Vec3 ponto(ServerPlayer jogador, double alcance) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getViewVector(1.0F);
        Vec3 fim = olho.add(olhar.scale(alcance));
        HitResult bloco = jogador.level().clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        if (bloco.getType() != HitResult.Type.MISS) {
            // Um pouco antes da parede, pra não ficar dentro dela
            return bloco.getLocation().subtract(olhar.scale(0.8));
        }
        return fim;
    }

    // Alcance dos golpes de perto
    public static double alcanceCorpoACorpo(ServerPlayer jogador) {
        return jogador.entityInteractionRange() + 0.5;
    }
}
