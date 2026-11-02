package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.combate.Sorteio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Base de todos os inimigos do Shattered.
// Cada inimigo tem os números dele (vida, precisão, esquiva, dano, armadura, XP) e:
//   - nasce dormindo quando o andar é criado, e acorda quando nota um herói por perto (igual ao Shattered)
//   - quem ataca um inimigo dormindo ou distraído faz um ataque surpresa (sempre acerta)
public abstract class MobDoShattered extends Monster {

    // Os números de um inimigo, iguais aos do Shattered.
    //   velocidade: velocidade de andar do Minecraft (0.25 = normal)
    //   nivelMaximo: herói acima desse nível não ganha mais XP com ele
    public record Atributos(int vida, int precisao, int esquiva, int danoMinimo, int danoMaximo,
            int armaduraMinima, int armaduraMaxima, int xp, int nivelMaximo, double velocidade) {
    }

    private Atributos atributos;
    private boolean dormindo = false;

    protected MobDoShattered(EntityType<? extends MobDoShattered> tipo, Level mundo, Atributos atributos) {
        super(tipo, mundo);
        this.atributos = atributos;
        this.xpReward = 0;
    }

    // Atributos do Minecraft (vida, velocidade...) a partir dos números do Shattered
    public static AttributeSupplier.Builder atributosDoMinecraft(Atributos atributos) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, atributos.vida())
                .add(Attributes.MOVEMENT_SPEED, atributos.velocidade())
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        // Ataca uma vez por segundo, que é um turno do Shattered
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public Atributos atributos() {
        return atributos;
    }

    // ---- Números do combate ----

    public int sortearDano() {
        return Sorteio.normal(getRandom(), atributos.danoMinimo(), atributos.danoMaximo());
    }

    public int sortearArmadura() {
        return Sorteio.normal(getRandom(), atributos.armaduraMinima(), atributos.armaduraMaxima());
    }

    // Alguns inimigos aguentam golpes fortes de um jeito especial (o slime, por exemplo)
    public float reduzirDanoRecebido(float dano) {
        return dano;
    }

    // ---- Dormir e acordar ----

    public boolean dormindo() {
        return dormindo;
    }

    public void dormir() {
        dormindo = true;
        setNoAi(true);
    }

    public void acordar() {
        if (dormindo) {
            dormindo = false;
            setNoAi(false);
        }
    }

    // O herói pega o inimigo de surpresa se ele está dormindo ou não está de olho nesse herói
    public boolean surpreendidoPor(Player jogador) {
        return dormindo || getTarget() != jogador;
    }

    @Override
    public void tick() {
        super.tick();
        // Uma vez por segundo, quem está dormindo pode notar um herói por perto.
        // Igual ao Shattered: a chance é maior quanto mais perto (1 quadradinho = 4 blocos).
        if (dormindo && !level().isClientSide() && tickCount % 20 == 0) {
            for (Player jogador : level().players()) {
                if (jogador.isSpectator() || jogador.isCreative() || !hasLineOfSight(jogador)) {
                    continue;
                }
                double distanciaEmQuadradinhos = distanceTo(jogador) / 4.0;
                if (distanciaEmQuadradinhos > 8) {
                    continue;
                }
                if (getRandom().nextDouble() * Math.max(1.0, distanciaEmQuadradinhos) < 1.0) {
                    acordar();
                    setTarget(jogador);
                    break;
                }
            }
        }
    }

    // ---- O resto do Minecraft ----

    // Os inimigos da dungeon nunca somem sozinhos
    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("dormindo", dormindo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dormindo = tag.getBoolean("dormindo");
    }

    // Ajusta a vida pra quantidade de jogadores (mais gente = mais vida)
    public void ajustarVida(ServerLevel mundo, double multiplicador) {
        var vida = getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(Math.max(1, Math.round(atributos.vida() * multiplicador)));
            setHealth(getMaxHealth());
        }
    }
}
