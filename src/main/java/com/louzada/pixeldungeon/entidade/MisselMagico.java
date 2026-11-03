package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.VarinhaDeMisselMagico;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// O míssil mágico da varinha (o Mago já começa com ela dentro do cajado).
// Igual ao Shattered: um raio de energia branca que sempre acerta quem estiver no caminho
// e dá 2-8 de dano mágico (a armadura não segura). Voa reto, sem cair, e some depois de 2 segundos.
// A luz guia do Clérigo é um raio igual a este, só que amarelo e iluminando quem acerta.
public class MisselMagico extends ThrowableProjectile {
    private static final int DURACAO = 40;
    private static final int BRANCO = 0xFFFFFF;

    // Nível da varinha (+1 de dano mínimo e +2 de máximo por nível)
    private int nivel = 0;

    public MisselMagico(EntityType<? extends MisselMagico> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public MisselMagico(Level mundo, LivingEntity dono, int nivel) {
        this(ModEntidades.MISSEL_MAGICO.get(), mundo, dono, nivel);
    }

    protected MisselMagico(EntityType<? extends MisselMagico> tipo, Level mundo, LivingEntity dono, int nivel) {
        super(tipo, dono, mundo);
        this.nivel = nivel;
    }

    // Cor do raio
    protected int cor() {
        return BRANCO;
    }

    // O dano do raio (o da varinha de míssil mágico no nível dela; a luz guia é igual a uma varinha +0)
    protected int sortearDano() {
        return VarinhaDeMisselMagico.sortearDano(random, nivel);
    }

    // Dano a mais que quem lançou ganha (refeição empoderadora do Mago, só na varinha)
    protected int bonusDeQuemLancou(ServerPlayer jogador) {
        return Talentos.bonusDaVarinha(jogador);
    }

    // O que mais acontece com quem foi atingido (a luz guia ilumina)
    protected void aoAtingir(LivingEntity alvo) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
    }

    // Magia não cai
    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // Rastro de luz branca, igual ao raio do Shattered
            Vec3 movimento = getDeltaMovement();
            for (int i = 0; i < 3; i++) {
                double recuo = i / 3.0;
                level().addParticle(Particulas.poeira(cor(), 1.3F), getX() - movimento.x * recuo, getY() - movimento.y * recuo,
                        getZ() - movimento.z * recuo, 0.0, 0.0, 0.0);
            }
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
        } else if (tickCount > DURACAO) {
            estourar();
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
        if (level().isClientSide()) {
            return;
        }
        Entity alvo = resultado.getEntity();
        // A magia sempre acerta, mesmo que o alvo tenha acabado de levar outro golpe
        if (alvo instanceof LivingEntity criatura) {
            criatura.invulnerableTime = 0;
        }
        int dano = sortearDano();
        if (getOwner() instanceof ServerPlayer jogador) {
            dano += bonusDeQuemLancou(jogador);
        }
        alvo.hurt(damageSources().indirectMagic(this, getOwner()), dano);
        if (alvo instanceof LivingEntity criatura && criatura.isAlive()) {
            aoAtingir(criatura);
        }
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (!level().isClientSide() && !isRemoved()) {
            estourar();
            discard();
        }
    }

    // Explosão de luz onde o míssil bate
    private void estourar() {
        if (level() instanceof ServerLevel mundo) {
            Particulas.explosao(mundo, ParticleTypes.END_ROD, getX(), getY(), getZ(), 10, 0.1, 0.12);
            Particulas.explosao(mundo, Particulas.poeira(cor(), 1.5F), getX(), getY(), getZ(), 12, 0.3, 0.0);
            mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.SHULKER_BULLET_HIT, getSoundSource(), 0.8F,
                    0.87F + random.nextFloat() * 0.28F);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nivel", nivel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nivel = tag.getInt("nivel");
    }
}
