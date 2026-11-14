package com.louzada.pixeldungeon.entidade;

import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.PoteQuebrado;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Abelha (sai do pote de mel quando ele quebra): voa e defende o pote. Igual ao Shattered:
//   - quem estiver com o pote quebrado (no inventário) vira o alvo dela, onde estiver
//   - com o pote no chão, ela fica perto dele e ataca quem chegar a 3 quadradinhos dele (os outros inimigos primeiro)
//   - sem o pote (sumiu), ela vira um inimigo comum
// Números (nível = andar): vida (2 + nível) x 4, precisão e esquiva 9 + nível, dano de 1/10 a 1/4 da vida máxima. Não dá XP.
public class Abelha extends MobDoShattered {
    public static final Atributos ATRIBUTOS = atributosDoNivel(1);
    public static final Sons SONS = new Sons(SoundEvents.BEE_LOOP_AGGRESSIVE, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH);
    // 3 quadradinhos em volta do pote
    private static final double PERTO_DO_POTE = 12;
    private static final double PROCURA = 64;

    private int nivel = 1;
    // O número do pote que ela defende (0 = nenhum)
    private int idDoPote = 0;
    @Nullable
    private Vec3 lugarDoPote;
    @Nullable
    private UUID dono;

    public Abelha(EntityType<? extends Abelha> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static Atributos atributosDoNivel(int nivel) {
        int vida = (2 + nivel) * 4;
        return new Atributos(vida, 9 + nivel, 9 + nivel, vida / 10, vida / 4, 0, 0, 0, 30, 0.25);
    }

    public static AttributeSupplier.Builder atributosDaAbelha() {
        return atributosDoMinecraft(ATRIBUTOS).add(Attributes.FLYING_SPEED, 0.5);
    }

    public void definirNivel(int novo) {
        nivel = Math.max(1, novo);
        trocarAtributos(atributosDoNivel(nivel));
        var vida = getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(atributos().vida());
        }
        setHealth(getMaxHealth());
    }

    // Saiu de um pote que acabou de quebrar
    public void definirPote(int id, Vec3 lugar, @Nullable Player quemSegura) {
        idDoPote = id;
        lugarDoPote = quemSegura == null ? lugar : null;
        dono = quemSegura == null ? null : quemSegura.getUUID();
    }

    public int idDoPote() {
        return idDoPote;
    }

    // Onde está o pote que ela defende, se estiver no chão
    @Nullable
    public Vec3 lugarDoPote() {
        return lugarDoPote;
    }

    // Olha onde está o pote agora (ela olha sozinha de tempos em tempos)
    public void olharOPote() {
        if (level() instanceof ServerLevel mundo && idDoPote != 0) {
            procurarPote(mundo);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level mundo) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, mundo);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(true);
        navegacao.setCanPassDoors(true);
        return navegacao;
    }

    @Override
    protected Goal passeio() {
        return new WaterAvoidingRandomFlyingGoal(this, 0.8);
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    // Com o pote por perto, só caça quem está com ele ou perto dele
    @Override
    public boolean podeCacar(LivingEntity alvo) {
        if (dono != null) {
            return alvo.getUUID().equals(dono);
        }
        if (lugarDoPote != null) {
            return alvo.position().distanceTo(lugarDoPote) <= PERTO_DO_POTE;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel mundo) || !isAlive() || tickCount % 10 != 0 || idDoPote == 0 || aliado()) {
            return;
        }
        procurarPote(mundo);
        if (dono != null) {
            Entity quem = mundo.getEntity(dono);
            if (quem instanceof LivingEntity segurando && segurando.isAlive() && !(segurando instanceof Player jogador && (jogador.isCreative()
                    || jogador.isSpectator()))) {
                if (getTarget() != segurando) {
                    acordar();
                    setTarget(segurando);
                }
            }
            return;
        }
        if (lugarDoPote == null) {
            return;
        }
        LivingEntity alvo = getTarget();
        if (alvo != null && (!alvo.isAlive() || alvo.position().distanceTo(lugarDoPote) > PERTO_DO_POTE)) {
            setTarget(null);
            alvo = null;
        }
        if (alvo == null) {
            alvo = maisPertoDoPote(mundo);
            if (alvo != null) {
                acordar();
                setTarget(alvo);
            } else if (position().distanceTo(lugarDoPote) > 4) {
                // Sem ninguém pra atacar, volta pro pote
                getNavigation().moveTo(lugarDoPote.x, lugarDoPote.y + 1.5, lugarDoPote.z, 1.0);
            }
        }
    }

    // Onde está o pote: no chão (um item), ou com alguém (no inventário). Sumiu: a abelha vira um inimigo comum.
    private void procurarPote(ServerLevel mundo) {
        for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(PROCURA))) {
            if (PoteQuebrado.id(item.getItem()) == idDoPote) {
                lugarDoPote = item.position();
                dono = null;
                return;
            }
        }
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(PROCURA))) {
            if (PoteQuebrado.temNoInventario(jogador, idDoPote)) {
                dono = jogador.getUUID();
                lugarDoPote = null;
                return;
            }
        }
        idDoPote = 0;
        lugarDoPote = null;
        dono = null;
    }

    // O inimigo mais perto (perto do pote); se não tiver nenhum, o herói perto do pote
    @Nullable
    private LivingEntity maisPertoDoPote(ServerLevel mundo) {
        LivingEntity melhor = null;
        for (MobDoShattered outro : mundo.getEntitiesOfClass(MobDoShattered.class, getBoundingBox().inflate(PROCURA))) {
            if (outro != this && outro.isAlive() && !(outro instanceof Abelha) && !(outro instanceof Mimico) && !outro.isInvulnerable()
                    && outro.position().distanceTo(lugarDoPote) <= PERTO_DO_POTE
                    && (melhor == null || distanceToSqr(outro) < distanceToSqr(melhor))) {
                melhor = outro;
            }
        }
        if (melhor != null) {
            return melhor;
        }
        for (Player jogador : mundo.players()) {
            if (!jogador.isCreative() && !jogador.isSpectator() && jogador.isAlive() && jogador.position().distanceTo(lugarDoPote) <= PERTO_DO_POTE
                    && (melhor == null || distanceToSqr(jogador) < distanceToSqr(melhor))) {
                melhor = jogador;
            }
        }
        return melhor;
    }

    // A abelha pica: o inimigo picado vai atrás dela
    @Override
    public void aoAcertar(Entity alvo, float dano) {
        if (alvo instanceof MobDoShattered picado && picado.isAlive()) {
            picado.acordar();
            picado.setTarget(this);
        }
    }

    @Override
    public ItemStack sortearSaque() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nivel", nivel);
        tag.putInt("pote", idDoPote);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nivel = Math.max(1, tag.getInt("nivel"));
        trocarAtributos(atributosDoNivel(nivel));
        idDoPote = tag.getInt("pote");
    }

    // Amarelo (o sangue espirra dessa cor)
    @Override
    public int corDoSangue() {
        return 0xFFD500;
    }

    public void ajustarAoAndar() {
        definirNivel(SaqueDoAndar.andarDe(this));
    }
}
