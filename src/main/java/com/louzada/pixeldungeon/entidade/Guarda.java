package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// As guardas e sentinelas da varinha de proteção (o "Ward" do Shattered): aliados parados que disparam sozinhos
// nos inimigos à vista, uma vez por turno, sempre acertando (2 + nível a 8 + 4 x nível da varinha de dano).
//   nível 1 a 3 (guardas): 1 de vida; somem depois de 1, 3 ou 5 disparos
//   nível 4 a 6 (sentinelas): têm vida (35, 54 e 84 no máximo) e gastam 5, 6 ou 7 dela em cada disparo
//   enxergam 3 + nível quadradinhos; disparar a varinha numa guarda melhora ela (ou cura, se não sobrar energia)
//   o dono pode dispensar uma guarda: agachado, clique direito nela
public class Guarda extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura, XP, nível máximo, velocidade (o resto muda com o nível da guarda)
    public static final Atributos ATRIBUTOS = new Atributos(1, 0, 0, 0, 0, 0, 0, 0, 30, 0.0);
    public static final Sons SONS = new Sons(null, SoundEvents.AMETHYST_BLOCK_HIT, SoundEvents.AMETHYST_BLOCK_BREAK);
    private static final EntityDataAccessor<Integer> NIVEL = SynchedEntityData.defineId(Guarda.class, EntityDataSerializers.INT);
    private static final int COR = 0x8822FF;

    private int nivelDaVarinha = 0;
    private int disparos = 0;

    public Guarda(EntityType<? extends Guarda> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(NIVEL, 1);
    }

    // Não anda e não persegue ninguém: quem dispara é o tick
    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean aliadoDeNascenca() {
        return true;
    }

    // De 1 a 6 (1 a 3 são guardas, 4 a 6 sentinelas)
    public int nivel() {
        return entityData.get(NIVEL);
    }

    public int disparos() {
        return disparos;
    }

    public int nivelDaVarinha() {
        return nivelDaVarinha;
    }

    // Nasce uma guarda nova
    public void nascer(Player dono, int nivelDaVarinha) {
        this.nivelDaVarinha = nivelDaVarinha;
        definirDono(dono);
        setPersistenceRequired();
        brilhar();
    }

    // ---- Melhorar e curar (o "upgrade" e o "wandHeal" do Shattered) ----

    public void melhorar(int nivelDaVarinha) {
        this.nivelDaVarinha = Math.max(this.nivelDaVarinha, nivelDaVarinha);
        switch (nivel()) {
            case 3 -> {
                vidaMaxima(35);
                setHealth(15 + (5 - disparos) * 4);
            }
            case 4 -> {
                vidaMaxima(54);
                setHealth(getHealth() + 19);
            }
            case 5 -> {
                vidaMaxima(84);
                setHealth(getHealth() + 30);
            }
            case 6 -> curar(nivelDaVarinha, 1.0F);
            default -> {
            }
        }
        if (nivel() < 6) {
            entityData.set(NIVEL, nivel() + 1);
        }
        brilhar();
    }

    public void curar(int nivelDaVarinha, float fator) {
        this.nivelDaVarinha = Math.max(this.nivelDaVarinha, nivelDaVarinha);
        int cura = switch (nivel()) {
            case 2 -> Math.round(fator);
            case 3 -> Math.round((1 + getRandom().nextInt(2)) * fator);
            case 4 -> Math.round(9 * fator);
            case 5 -> Math.round(12 * fator);
            case 6 -> Math.round(16 * fator);
            default -> 0;
        };
        if (cura <= 0) {
            return;
        }
        // As guardas "recuperam" disparos; as sentinelas, vida
        if (nivel() <= 3) {
            disparos = Math.max(0, disparos - cura);
        } else {
            setHealth(Math.min(getMaxHealth(), getHealth() + cura));
        }
        NumerosFlutuantes.mostrar(this, "+" + cura, NumerosFlutuantes.VERDE);
        brilhar();
    }

    private void vidaMaxima(int vida) {
        AttributeInstance atributo = getAttribute(Attributes.MAX_HEALTH);
        if (atributo != null) {
            atributo.setBaseValue(vida);
        }
    }

    private void brilhar() {
        Particulas.subindoEmVolta(this, Particulas.poeira(COR, 1.0F), 4 + 2 * nivel(), 0.03);
    }

    // ---- Disparar ----

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (!level().isClientSide() && isAlive() && Turno.virou(this)) {
            agir();
        }
    }

    // O turno da guarda: dispara no inimigo mais perto à vista (se tiver)
    public void agir() {
        LivingEntity alvo = escolherAlvo();
        if (alvo != null) {
            disparar(alvo);
        }
    }

    // O inimigo mais perto à vista, até 3 + nível quadradinhos
    @Nullable
    private LivingEntity escolherAlvo() {
        double visao = (3 + nivel()) * 4.0;
        LivingEntity melhor = null;
        double menor = Double.MAX_VALUE;
        for (MobDoShattered outro : level().getEntitiesOfClass(MobDoShattered.class, getBoundingBox().inflate(visao),
                outro -> outro.isAlive() && !outro.aliado() && !(outro instanceof Mimico))) {
            double distancia = distanceTo(outro);
            if (distancia <= visao && distancia < menor && hasLineOfSight(outro)) {
                menor = distancia;
                melhor = outro;
            }
        }
        return melhor;
    }

    private void disparar(LivingEntity alvo) {
        ServerLevel mundo = (ServerLevel) level();
        int dano = Sorteio.normal(getRandom(), 2 + nivelDaVarinha, 8 + 4 * nivelDaVarinha);
        // Magia "de longe": quem dispara é a guarda (o que ela mata conta pro dono dela)
        Holder<DamageType> magia = mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.INDIRECT_MAGIC);
        alvo.invulnerableTime = 0;
        alvo.hurt(new DamageSource(magia, null, this), dano);
        // O raio roxo
        Vec3 inicio = getEyePosition();
        Vec3 fim = alvo.getBoundingBox().getCenter();
        Vec3 caminho = fim.subtract(inicio);
        int passos = Math.max(1, (int) (caminho.length() * 3));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = inicio.add(caminho.scale(i / (double) passos));
            mundo.sendParticles(Particulas.poeira(COR, 1.0F), ponto.x, ponto.y, ponto.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.NEUTRAL, 0.6F, 1.6F);
        disparos++;
        // Guardas somem depois de 1, 3 ou 5 disparos; sentinelas gastam vida
        switch (nivel()) {
            case 4 -> gastarVida(5);
            case 5 -> gastarVida(6);
            case 6 -> gastarVida(7);
            default -> {
                if (disparos >= 2 * nivel() - 1) {
                    sumir();
                }
            }
        }
    }

    private void gastarVida(int quanto) {
        invulnerableTime = 0;
        hurt(damageSources().magic(), quanto);
    }

    // Some na hora, sem a animação de morrer
    public void sumir() {
        Particulas.subindoEmVolta(this, Particulas.poeira(COR, 1.2F), 10, 0.05);
        discard();
    }

    // ---- Números do combate (as sentinelas desviam e têm armadura) ----

    @Override
    public int esquiva() {
        return nivel() > 3 ? 4 + andar() : 0;
    }

    @Override
    public int sortearArmadura() {
        if (nivel() <= 3) {
            return 0;
        }
        return Math.round(Sorteio.normal(getRandom(), 0, 3 + andar() / 2) / (7F - nivel()));
    }

    private int andar() {
        return Math.max(1, Andares.andarPelaAltura(getBlockY()));
    }

    // Igual ao Shattered: não dorme, não se assusta, não fica tonta e não pode ser corrompida
    @Override
    public boolean canBeAffected(MobEffectInstance efeito) {
        if (efeito.is(ModEfeitos.SONO_MAGICO) || efeito.is(ModEfeitos.SONOLENTO) || efeito.is(ModEfeitos.ATERRORIZADO) || efeito.is(ModEfeitos.VERTIGEM)
                || efeito.is(ModEfeitos.CORROMPIDO) || efeito.is(ModEfeitos.FURIOSO) || efeito.is(ModEfeitos.ENCANTADO)) {
            return false;
        }
        return super.canBeAffected(efeito);
    }

    // Parada: nada empurra
    @Override
    public boolean isPushable() {
        return false;
    }

    // O dono dispensa a guarda: agachado, clique direito nela
    @Override
    protected InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (jogador.isShiftKeyDown() && jogador.getUUID().equals(donoDoAliado())) {
            if (!level().isClientSide()) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.guarda_dispensada"), true);
                sumir();
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return super.mobInteract(jogador, mao);
    }

    @Override
    public Component getName() {
        return hasCustomName() ? super.getName() : Component.translatable("entity.pixeldungeon.guarda." + nivel());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nivel", nivel());
        tag.putInt("nivel_da_varinha", nivelDaVarinha);
        tag.putInt("disparos", disparos);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NIVEL, Math.max(1, Math.min(6, tag.getInt("nivel"))));
        nivelDaVarinha = tag.getInt("nivel_da_varinha");
        disparos = tag.getInt("disparos");
    }
}
