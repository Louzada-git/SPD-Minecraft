package com.louzada.pixeldungeon.entidade;

import java.util.EnumSet;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Gnoll trapaceiro (o "GnollTrickster" do Shattered, da missão do fantasma no andar 3): só ataca de longe, jogando
// dardos, e foge de quem chega perto. Cada golpe seguido que acerta é pior: sorteia 0 a 3 + a sequência de acertos;
// passando de 2, envenena (o que passou de 2, em turnos); com 6 ou mais, põe fogo. Andar zera a sequência.
// Sempre deixa armas de arremesso (metade da quantidade, +0, sem maldição e já identificadas).
public class GnollTrapaceiro extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura (as do gnoll), XP, nível máximo, velocidade
    public static final Atributos ATRIBUTOS = new Atributos(20, 16, 5, 1, 6, 0, 2, 5, 8, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH);
    // Até onde ele joga (8 quadradinhos) e quão perto é "colado" (1 quadradinho e meio)
    private static final double ALCANCE = 32;
    private static final double PERTO = 6;

    private int sequencia = 0;

    public GnollTrapaceiro(EntityType<? extends GnollTrapaceiro> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new VagarAteOHeroi(this));
    }

    @Override
    protected Goal ataque() {
        return new AtaqueDeLonge(this);
    }

    @Override
    public boolean chefe() {
        return true;
    }

    public int sequencia() {
        return sequencia;
    }

    // Igual ao Shattered: quanto mais ele acerta seguido, pior
    @Override
    public void aoAcertar(Entity alvo, float dano) {
        if (!(alvo instanceof LivingEntity criatura)) {
            return;
        }
        sequencia++;
        int efeito = getRandom().nextInt(4) + sequencia;
        if (efeito > 2) {
            if (efeito >= 6 && !criatura.isOnFire()) {
                criatura.igniteForSeconds(Turno.ticks(8) / 20F);
            } else {
                EfeitosNoTick.envenenar(criatura, efeito - 2);
            }
        }
    }

    // Um dardo: o golpe de longe do Shattered (precisão 1,5x de longe)
    public boolean atirar(LivingEntity alvo) {
        if (!(level() instanceof ServerLevel mundo)) {
            return false;
        }
        Vec3 de = getEyePosition();
        Vec3 para = alvo.getBoundingBox().getCenter();
        Vec3 meio = para.subtract(de);
        int pontos = (int) Math.max(2, meio.length() * 2);
        for (int i = 1; i < pontos; i++) {
            Vec3 ponto = de.add(meio.scale((double) i / pontos));
            mundo.sendParticles(ParticleTypes.CRIT, ponto.x, ponto.y, ponto.z, 1, 0, 0, 0, 0);
        }
        mundo.playSound(null, blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.4F);
        swing(InteractionHand.MAIN_HAND);
        Golpe golpe = Golpe.de(this, Combatente.de(alvo)).aDistancia();
        float dano = Combate.resolver(golpe);
        if (dano == Combate.ERROU) {
            return false;
        }
        Combate.aplicar(alvo, dano, damageSources().mobAttack(this));
        return true;
    }

    // Sempre deixa armas de arremesso: +0, sem maldição, identificadas e metade da quantidade (arredondando pra cima)
    @Override
    public ItemStack sortearSaque() {
        ItemStack armas = SaqueDoAndar.arremessoAleatorio(new Random(getRandom().nextLong()), SaqueDoAndar.andarDe(this));
        if (armas.getItem() instanceof ArmaDeArremesso) {
            NivelDoItem.definirNivel(armas, 0);
            armas.remove(ModComponentes.MALDICAO.get());
            NivelDoItem.definirEncantamento(armas, null);
            NivelDoItem.identificar(null, armas);
            armas.setCount((armas.getCount() + 1) / 2);
        }
        return armas;
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        MissaoDoFantasma.aoMorrerOChefe(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("sequencia", sequencia);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sequencia = tag.getInt("sequencia");
    }

    // O ataque dele: de longe, uma vez por turno (lento: a cada 2); colado no alvo, foge em vez de atacar
    private static class AtaqueDeLonge extends Goal {
        private GnollTrapaceiro gnoll;
        @Nullable
        private LivingEntity alvo;
        private long proximoAtaque = 0;

        AtaqueDeLonge(GnollTrapaceiro gnoll) {
            this.gnoll = gnoll;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            alvo = gnoll.getTarget();
            return alvo != null && alvo.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            alvo = null;
            gnoll.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (alvo == null) {
                return;
            }
            gnoll.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            double distancia = gnoll.distanceTo(alvo);
            boolean ve = gnoll.hasLineOfSight(alvo);
            if (distancia < PERTO) {
                // Colado: foge (e andando, a sequência zera)
                gnoll.sequencia = 0;
                if (gnoll.getNavigation().isDone()) {
                    Vec3 longe = DefaultRandomPos.getPosAway(gnoll, 12, 4, alvo.position());
                    if (longe != null) {
                        gnoll.getNavigation().moveTo(longe.x, longe.y, longe.z, 1.1);
                    }
                }
                return;
            }
            if (!ve || distancia > ALCANCE) {
                gnoll.sequencia = 0;
                gnoll.getNavigation().moveTo(alvo, 1.0);
                return;
            }
            gnoll.getNavigation().stop();
            long agora = gnoll.level().getGameTime();
            if (agora < proximoAtaque || Furtividade.invisivel(alvo) || EfeitosNoTick.impedidoDeAtacar(gnoll)) {
                return;
            }
            proximoAtaque = agora + (gnoll.hasEffect(ModEfeitos.LENTO) ? 2 : 1) * Turno.TICKS;
            gnoll.atirar(alvo);
        }
    }
}
