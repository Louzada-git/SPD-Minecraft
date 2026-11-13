package com.louzada.pixeldungeon.entidade;

import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.EfeitosDasArmadilhas;
import com.louzada.pixeldungeon.item.EfeitosDasBombas;
import com.louzada.pixeldungeon.item.EfeitosDasPedras;
import com.louzada.pixeldungeon.item.ItemBomba;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Uma bomba acesa no chão (o "Fuse" do Shattered): explode 2 turnos depois de cair, no quadradinho dela.
// Clique direito nela apaga o pavio (ela volta pro inventário).
// A bomba de barulho, quando o pavio acaba, vira um alarme: a cada 6 turnos chama todos os inimigos do andar,
// e só explode quando alguém pisa no quadradinho dela (aí não dá mais pra pegar).
public class BombaAcesa extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(BombaAcesa.class, EntityDataSerializers.ITEM_STACK);
    public static final int PAVIO = 2 * Turno.TICKS;
    private static final int TURNOS_DO_ALARME = 6;

    private int pavio = PAVIO;
    private boolean disparada = false;
    private int alarme = 0;
    @Nullable
    private UUID dono;

    public BombaAcesa(EntityType<? extends BombaAcesa> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public static BombaAcesa acender(ServerLevel mundo, Vec3 lugar, ItemStack bomba, @Nullable Entity dono) {
        BombaAcesa acesa = new BombaAcesa(ModEntidades.BOMBA_ACESA.get(), mundo);
        acesa.setPos(lugar.x, lugar.y, lugar.z);
        acesa.entityData.set(ITEM, bomba.copyWithCount(1));
        acesa.dono = dono != null ? dono.getUUID() : null;
        mundo.addFreshEntity(acesa);
        mundo.playSound(null, BlockPos.containing(lugar), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.8F, 1.2F);
        return acesa;
    }

    public TipoDeBomba tipo() {
        return getItem().getItem() instanceof ItemBomba bomba ? bomba.tipo() : TipoDeBomba.COMUM;
    }

    public boolean disparada() {
        return disparada;
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        construtor.define(ITEM, new ItemStack(ModItens.BOMBAS.get(0).get()));
    }

    @Override
    public void tick() {
        super.tick();
        // Cai até o chão
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(onGround() ? 0.5 : 0.98));
        if (level().isClientSide()) {
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0.0, 0.03, 0.0);
            }
            if (tickCount % 5 == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.45, getZ(), 0.0, 0.01, 0.0);
            }
            return;
        }
        ServerLevel mundo = (ServerLevel) level();
        if (disparada) {
            // Alarme da bomba de barulho: explode quando alguém pisa no quadradinho
            BlockPos controle = EfeitosDasPedras.quadradinhoEm(mundo, position());
            if (EfeitosDasArmadilhas.criaturasEmVolta(mundo, controle, 0).stream().anyMatch(criatura -> !criatura.isSpectator())) {
                explodir(mundo);
                return;
            }
            if (Turno.virou(this) && --alarme <= 0) {
                EfeitosDasArmadilhas.alarme(mundo, controle);
                alarme = TURNOS_DO_ALARME;
            }
            return;
        }
        if (--pavio <= 0) {
            if (tipo() == TipoDeBomba.BARULHO) {
                disparada = true;
                alarme = 0;
            } else {
                explodir(mundo);
            }
        }
    }

    // Explode agora (o pavio acabou, alguém pisou na de barulho, ou outra explosão pegou ela)
    public void explodir(ServerLevel mundo) {
        if (isRemoved()) {
            return;
        }
        discard();
        Entity quem = dono != null ? mundo.getEntity(dono) : null;
        EfeitosDasBombas.explodir(mundo, tipo(), EfeitosDasPedras.quadradinhoEm(mundo, position()), quem);
    }

    // Clique direito: apaga o pavio e pega a bomba de volta
    @Override
    public InteractionResult interact(Player jogador, InteractionHand mao) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (disparada) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bomba.disparada").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        ItemStack bomba = getItem().copy();
        if (!jogador.getInventory().add(bomba)) {
            jogador.drop(bomba, false);
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bomba.apagou").withStyle(ChatFormatting.GRAY), true);
        level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("item")) {
            entityData.set(ITEM, ItemStack.parseOptional(registryAccess(), tag.getCompound("item")));
        }
        pavio = tag.getInt("pavio");
        disparada = tag.getBoolean("disparada");
        alarme = tag.getInt("alarme");
        if (tag.hasUUID("dono")) {
            dono = tag.getUUID("dono");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("item", getItem().saveOptional(registryAccess()));
        tag.putInt("pavio", pavio);
        tag.putBoolean("disparada", disparada);
        tag.putInt("alarme", alarme);
        if (dono != null) {
            tag.putUUID("dono", dono);
        }
    }
}
