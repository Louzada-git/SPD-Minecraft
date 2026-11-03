package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.item.Odre;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Gota de orvalho (o "dewdrop" do Shattered). Cai da grama alta quando ela é pisoteada.
// Depois de um pulinho, ela voa sozinha até o herói mais perto que precisa dela:
//   - se o odre dele não estiver cheio, a gota entra no odre (até 20 gotas)
//   - se o odre estiver cheio, a gota cura 5% da vida (se faltar vida)
// Se ninguém precisa dela, fica no chão esperando.
public class GotaDeOrvalho extends Entity implements ItemSupplier {
    private static final double ALCANCE = 12;
    private static final int TEMPO_DO_PULO = 10;
    private static final int AZUL = 0xAADDFF;

    @Nullable
    private ItemStack desenho;

    public GotaDeOrvalho(EntityType<? extends GotaDeOrvalho> tipo, Level mundo) {
        super(tipo, mundo);
    }

    // Solta uma gota com um pulinho pra cima
    public static void soltar(ServerLevel mundo, double x, double y, double z) {
        GotaDeOrvalho gota = new GotaDeOrvalho(ModEntidades.GOTA_DE_ORVALHO.get(), mundo);
        gota.setPos(x, y, z);
        gota.setDeltaMovement((mundo.getRandom().nextDouble() - 0.5) * 0.15, 0.35, (mundo.getRandom().nextDouble() - 0.5) * 0.15);
        mundo.addFreshEntity(gota);
        mundo.playSound(null, x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8F, 1.6F);
    }

    // O desenho da gota (o item do Shattered)
    @Override
    public ItemStack getItem() {
        if (desenho == null) {
            desenho = new ItemStack(ModItens.GOTA_DE_ORVALHO.get());
        }
        return desenho;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 6 == 0) {
                level().addParticle(Particulas.poeira(AZUL, 0.6F), getRandomX(0.3), getY() + 0.1, getRandomZ(0.3), 0.0, 0.0, 0.0);
            }
            return;
        }

        ServerPlayer dono = tickCount > TEMPO_DO_PULO ? quemPrecisa() : null;
        if (dono != null) {
            // Voa reto até o herói, atravessando o que estiver no caminho
            noPhysics = true;
            Vec3 destino = dono.position().add(0, dono.getBbHeight() * 0.5, 0);
            Vec3 caminho = destino.subtract(position());
            if (caminho.lengthSqr() < 1.0) {
                absorver(dono);
                return;
            }
            double velocidade = Math.min(0.7, 0.2 + (tickCount - TEMPO_DO_PULO) * 0.02);
            setDeltaMovement(caminho.normalize().scale(velocidade));
        } else {
            // Cai e fica quietinha no chão
            noPhysics = false;
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0).scale(0.98));
            if (onGround()) {
                setDeltaMovement(getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    // O herói mais perto que tem lugar no odre, ou que está machucado
    @Nullable
    private ServerPlayer quemPrecisa() {
        ServerPlayer maisPerto = null;
        double menorDistancia = ALCANCE * ALCANCE;
        for (ServerPlayer jogador : ((ServerLevel) level()).players()) {
            if (!GerenciadorDoHeroi.emJogo(jogador) || !precisa(jogador)) {
                continue;
            }
            double distancia = jogador.distanceToSqr(this);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                maisPerto = jogador;
            }
        }
        return maisPerto;
    }

    private static boolean precisa(ServerPlayer jogador) {
        ItemStack odre = Odre.acharOdre(jogador);
        return (odre != null && !Odre.cheio(odre)) || jogador.getHealth() < jogador.getMaxHealth();
    }

    // A gota chegou no herói
    public void absorver(ServerPlayer jogador) {
        ItemStack odre = Odre.acharOdre(jogador);
        if (odre != null && !Odre.cheio(odre)) {
            Odre.adicionar(odre, 1);
            jogador.displayClientMessage(Component.translatable(Odre.cheio(odre) ? "mensagem.pixeldungeon.odre_cheio" : "mensagem.pixeldungeon.odre_gota")
                    .withStyle(Odre.cheio(odre) ? ChatFormatting.GREEN : ChatFormatting.AQUA), true);
            level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.6F);
        } else {
            // Odre cheio: a gota cura na hora (5% da vida máxima, igual ao Shattered)
            int cura = Math.min(Math.round(jogador.getMaxHealth() * 0.05F), Math.round(jogador.getMaxHealth() - jogador.getHealth()));
            cura = Math.max(1, cura);
            jogador.heal(cura);
            NumerosFlutuantes.mostrar(jogador, "+" + cura, NumerosFlutuantes.VERDE);
            level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
        if (level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
