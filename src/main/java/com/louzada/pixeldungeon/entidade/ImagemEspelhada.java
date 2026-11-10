package com.louzada.pixeldungeon.entidade;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.Arma;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Imagem espelhada (pergaminho): uma cópia do herói que luta do lado dele, igual ao Shattered:
//   - tem 1 de vida (some com qualquer golpe)
//   - acerta com a precisão do herói e bate metade do dano da arma dele
//   - os inimigos atacam as imagens também (elas distraem)
//   - some se o herói ficar longe demais ou sair do jogo
public class ImagemEspelhada extends PathfinderMob implements Combatente {
    private static final EntityDataAccessor<Optional<UUID>> DONO = SynchedEntityData.defineId(ImagemEspelhada.class,
            EntityDataSerializers.OPTIONAL_UUID);
    // Prismática (pergaminho da imagem prismática): uma imagem mais forte, que dura até morrer
    private static final EntityDataAccessor<Boolean> PRISMATICA = SynchedEntityData.defineId(ImagemEspelhada.class,
            EntityDataSerializers.BOOLEAN);
    private static final double DISTANCIA_MAXIMA = 48;

    public ImagemEspelhada(EntityType<? extends ImagemEspelhada> tipo, Level mundo) {
        super(tipo, mundo);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder atributosDaImagem() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // Cria uma imagem do lado do herói
    @Nullable
    public static ImagemEspelhada criar(ServerLevel mundo, ServerPlayer dono) {
        ImagemEspelhada imagem = ModEntidades.IMAGEM_ESPELHADA.get().create(mundo);
        if (imagem == null) {
            return null;
        }
        double angulo = mundo.getRandom().nextDouble() * Math.PI * 2;
        imagem.moveTo(dono.getX() + Math.cos(angulo) * 1.5, dono.getY(), dono.getZ() + Math.sin(angulo) * 1.5, dono.getYRot(), 0.0F);
        if (!mundo.noCollision(imagem)) {
            imagem.moveTo(dono.getX(), dono.getY(), dono.getZ(), dono.getYRot(), 0.0F);
        }
        imagem.entityData.set(DONO, Optional.of(dono.getUUID()));
        imagem.setItemSlot(EquipmentSlot.MAINHAND, dono.getMainHandItem().copyWithCount(1));
        imagem.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mundo.addFreshEntity(imagem);
        Particulas.explosao(mundo, ParticleTypes.END_ROD, imagem.getX(), imagem.getY(1.0), imagem.getZ(), 8, 0.3, 0.05);
        return imagem;
    }

    // A imagem prismática do Shattered: vida 10 + 2,5 por nível do herói, dano de (2 + nível/4) a (4 + nível/2),
    // a precisão e a esquiva do herói e a armadura dele somada. Se o herói já tem uma, ela só se cura toda.
    @Nullable
    public static ImagemEspelhada criarPrismatica(ServerLevel mundo, ServerPlayer dono) {
        for (ImagemEspelhada outra : mundo.getEntitiesOfClass(ImagemEspelhada.class, dono.getBoundingBox().inflate(DISTANCIA_MAXIMA),
                imagem -> imagem.prismatica() && dono.getUUID().equals(imagem.dono()))) {
            outra.setHealth(outra.getMaxHealth());
            Particulas.subindoEmVolta(outra, ParticleTypes.HAPPY_VILLAGER, 8, 0.0);
            return outra;
        }
        ImagemEspelhada imagem = criar(mundo, dono);
        if (imagem != null) {
            imagem.entityData.set(PRISMATICA, true);
            int vida = 10 + (int) Math.floor(RegrasDoHeroi.dados(dono).nivel() * 2.5F);
            imagem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(vida);
            imagem.setHealth(vida);
            imagem.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            Particulas.explosao(mundo, ParticleTypes.END_ROD, imagem.getX(), imagem.getY(0.5), imagem.getZ(), 20, 0.4, 0.08);
        }
        return imagem;
    }

    public boolean prismatica() {
        return entityData.get(PRISMATICA);
    }

    @Nullable
    public UUID dono() {
        return entityData.get(DONO).orElse(null);
    }

    @Nullable
    public ServerPlayer jogadorDono() {
        UUID dono = dono();
        return dono != null && level() instanceof ServerLevel mundo ? mundo.getServer().getPlayerList().getPlayer(dono) : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(DONO, Optional.empty());
        construtor.define(PRISMATICA, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new AtaqueDoShattered(this, 1.2, true));
        goalSelector.addGoal(3, new SeguirDono(this));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, MobDoShattered.class, 5, true, false, alvo -> !(alvo instanceof Mimico)));
    }

    // ---- Números do combate (metade dos do herói dono) ----

    @Override
    public LivingEntity entidade() {
        return this;
    }

    @Override
    public float precisao(Golpe golpe) {
        return precisao();
    }

    @Override
    public float esquiva(Golpe golpe) {
        return esquiva();
    }

    @Override
    public float sortearDano(Golpe golpe) {
        return sortearDano();
    }

    // A prismática usa a armadura do herói
    @Override
    public int sortearArmadura(Golpe golpe) {
        ServerPlayer dono = jogadorDono();
        return prismatica() && dono != null ? EfeitosDasArmadilhas.armadura(dono) : 0;
    }

    // Metade do dano da arma do herói, arredondado pra cima (a prismática tem o dano dela)
    public int sortearDano() {
        ServerPlayer dono = jogadorDono();
        if (dono == null) {
            return 1;
        }
        if (prismatica()) {
            int nivel = RegrasDoHeroi.dados(dono).nivel();
            return Sorteio.normal(getRandom(), 2 + nivel / 4, 4 + nivel / 2);
        }
        ItemStack arma = dono.getMainHandItem();
        int dano = arma.getItem() instanceof Arma comDano ? comDano.sortearDano(arma, getRandom(), RegrasDoHeroi.dados(dono), false) : 1;
        return (dano + 1) / 2;
    }

    public float precisao() {
        ServerPlayer dono = jogadorDono();
        return dono == null ? 10 : RegrasDoHeroi.precisao(RegrasDoHeroi.dados(dono).nivel());
    }

    // Metade da esquiva do herói (mais 1); a prismática, a esquiva toda
    public float esquiva() {
        ServerPlayer dono = jogadorDono();
        DadosDoHeroi dados = dono == null ? null : RegrasDoHeroi.dados(dono);
        if (dados == null) {
            return 1;
        }
        return prismatica() ? RegrasDoHeroi.esquiva(dados.nivel()) : (1 + RegrasDoHeroi.esquiva(dados.nivel())) / 2F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0) {
            return;
        }
        ServerPlayer dono = jogadorDono();
        if (dono == null || dono.isSpectator() || dono.distanceTo(this) > DISTANCIA_MAXIMA) {
            desfazer();
        }
    }

    // Some num brilho
    public void desfazer() {
        if (level() instanceof ServerLevel mundo) {
            Particulas.explosao(mundo, ParticleTypes.END_ROD, getX(), getY(1.0), getZ(), 8, 0.3, 0.05);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID dono = dono();
        if (dono != null) {
            tag.putUUID("dono", dono);
        }
        tag.putBoolean("prismatica", prismatica());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("dono")) {
            entityData.set(DONO, Optional.of(tag.getUUID("dono")));
        }
        entityData.set(PRISMATICA, tag.getBoolean("prismatica"));
    }

    // Vai atrás do herói quando ele fica longe e não tem ninguém pra atacar
    private static class SeguirDono extends Goal {
        private ImagemEspelhada imagem;

        SeguirDono(ImagemEspelhada imagem) {
            this.imagem = imagem;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player dono = imagem.jogadorDono();
            return imagem.getTarget() == null && dono != null && dono.distanceTo(imagem) > 4;
        }

        @Override
        public void tick() {
            Player dono = imagem.jogadorDono();
            if (dono != null && imagem.getNavigation().isDone()) {
                BlockPos perto = dono.blockPosition();
                imagem.getNavigation().moveTo(perto.getX() + 0.5, perto.getY(), perto.getZ() + 0.5, 1.2);
            }
        }
    }
}
