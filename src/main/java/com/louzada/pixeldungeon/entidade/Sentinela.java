package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.HeroiEmCombate;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A sentinela (a sala da sentinela): não anda, não apanha e não morre. Quem pisa no chão especial da sala dela, à vista,
// leva raios: ela carrega por um tempo (o tamanho do caminho perigoso / 3) e depois dispara um raio por "passo".
// Sair do chão especial (ou da vista dela) zera a carga. Números do Shattered: precisão 20 + 2 x andar,
// dano 2 + andar / 2 a 4 + andar.
// (No Shattered a carga conta em turnos do herói, e o herói anda 1 quadradinho por turno; no Minecraft se anda mais ou
// menos 1 quadradinho por segundo, então aqui cada "passo" é 1 segundo.)
public class Sentinela extends Mob {
    private static final int TICKS_POR_PASSO = 20;

    private int andar = 1;
    private float espera = 1;
    private float carga = 1;
    // O lado de dentro da sala (em blocos)
    @Nullable
    private AABB sala;

    public Sentinela(EntityType<? extends Sentinela> tipo, Level mundo) {
        super(tipo, mundo);
        setNoAi(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributosDaSentinela() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void preparar(int novoAndar, float novaEspera, AABB novaSala) {
        andar = Math.max(1, novoAndar);
        espera = novaEspera;
        carga = novaEspera;
        sala = novaSala;
    }

    @Override
    public boolean hurt(DamageSource fonte, float dano) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance efeito) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel mundo) || sala == null) {
            return;
        }
        ServerPlayer alvo = alvo(mundo);
        if (alvo == null) {
            carga = espera;
            return;
        }
        if (carga == espera && tickCount % 4 == 0) {
            Particulas.subindoEmVolta(this, Particulas.poeira(0xFF4444, 1.0F), 2, 0.02);
        }
        carga -= 1F / TICKS_POR_PASSO;
        if (carga <= 0.001F) {
            carga = 1;
            disparar(mundo, alvo);
        }
    }

    // Um herói no chão especial da sala, à vista
    @Nullable
    private ServerPlayer alvo(ServerLevel mundo) {
        ServerPlayer melhor = null;
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, sala)) {
            if (jogador.isCreative() || jogador.isSpectator() || !jogador.isAlive() || !hasLineOfSight(jogador)) {
                continue;
            }
            BlockPos pe = BlockPos.containing(jogador.getX(), jogador.getY() - 0.2, jogador.getZ());
            if (!mundo.getBlockState(pe).is(ModBlocos.MOLDURA_ESGOTO.get())) {
                continue;
            }
            if (melhor == null || distanceToSqr(jogador) < distanceToSqr(melhor)) {
                melhor = jogador;
            }
        }
        return melhor;
    }

    // Um raio no herói (sorteia se acerta, igual ao Shattered); devolve se acertou
    public boolean disparar(ServerLevel mundo, ServerPlayer alvo) {
        Vec3 inicio = getEyePosition();
        Vec3 fim = alvo.getBoundingBox().getCenter();
        Vec3 caminho = fim.subtract(inicio);
        int passos = Math.max(1, (int) (caminho.length() * 3));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = inicio.add(caminho.scale(i / (double) passos));
            mundo.sendParticles(Particulas.poeira(0xFF3333, 1.0F), ponto.x, ponto.y, ponto.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 0.8F, 0.6F);
        float esquiva = EfeitosNoTick.semDefesa(alvo) ? 0 : new HeroiEmCombate(alvo).esquiva(null) * Combate.pelosEfeitos(alvo);
        if (!Combate.acertou(getRandom(), 20 + 2 * andar, esquiva)) {
            Combate.mostrarErro(alvo, null);
            return false;
        }
        int dano = Sorteio.normal(getRandom(), 2 + andar / 2, 4 + andar);
        Holder<DamageType> magia = mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.INDIRECT_MAGIC);
        alvo.invulnerableTime = 0;
        Combate.aplicar(alvo, dano, new DamageSource(magia, this, this));
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("andar", andar);
        tag.putFloat("espera", espera);
        if (sala != null) {
            tag.putDouble("x0", sala.minX);
            tag.putDouble("y0", sala.minY);
            tag.putDouble("z0", sala.minZ);
            tag.putDouble("x1", sala.maxX);
            tag.putDouble("y1", sala.maxY);
            tag.putDouble("z1", sala.maxZ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        andar = Math.max(1, tag.getInt("andar"));
        espera = tag.getFloat("espera");
        carga = espera;
        if (tag.contains("x0")) {
            sala = new AABB(tag.getDouble("x0"), tag.getDouble("y0"), tag.getDouble("z0"), tag.getDouble("x1"), tag.getDouble("y1"), tag.getDouble("z1"));
        }
    }

    // O andar em que ela está (pra quem cria a sentinela sem dizer)
    public int andar() {
        return andar;
    }

    public void ajustarAoAndar() {
        andar = SaqueDoAndar.andarDe(this);
    }
}
