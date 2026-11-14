package com.louzada.pixeldungeon.entidade;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.GlifosDoHeroi;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Base de todos os inimigos do Shattered.
// Cada inimigo tem os números dele (vida, precisão, esquiva, dano, armadura, XP) e:
//   - nasce dormindo quando o andar é criado ("z" em cima dele), e acorda quando nota um herói por perto ("!")
//   - quem ataca um inimigo dormindo ou distraído faz um ataque surpresa (sempre acerta)
//   - depois de apanhar, mostra uma barra de vida em cima da cabeça
//   - corrompido (encantamento de corrupção, varinha de corrupção), vira aliado dos heróis: ataca os outros inimigos,
//     segue o herói e, igual ao Shattered, perde 1% da vida máxima por turno
//   - alguns já nascem aliados (o guardião da terra e as guardas da varinha de proteção), sem perder vida
public abstract class MobDoShattered extends Monster implements Combatente {

    // Os números de um inimigo, iguais aos do Shattered.
    //   velocidade: velocidade de andar do Minecraft (0.25 = normal)
    //   nivelMaximo: herói acima desse nível não ganha mais XP com ele
    public record Atributos(int vida, int precisao, int esquiva, int danoMinimo, int danoMaximo,
            int armaduraMinima, int armaduraMaxima, int xp, int nivelMaximo, double velocidade) {
    }

    // Os sons do inimigo (por enquanto, sons de bichos do Minecraft)
    public record Sons(SoundEvent ambiente, SoundEvent ferido, SoundEvent morte) {
    }

    private static final int PEDACOS_DA_BARRA = 10;

    private Atributos atributos;
    private Sons sons;
    private boolean dormindo = false;
    // Talento golpe surpresa do Ladino: só vale no primeiro ataque surpresa em cada inimigo
    private boolean levouGolpeSurpresa = false;
    // Corrompido: quem corrompeu (os inimigos que ele mata contam pra esse herói) e o dano da corrupção que ainda não virou 1 de vida
    @Nullable
    private UUID donoDoAliado;
    private float corrupcaoAcumulada = 0;
    // Pra onde foi chamado (a armadilha de alarme): vai até lá se não estiver caçando ninguém
    @Nullable
    private Vec3 chamadoPara;

    protected MobDoShattered(EntityType<? extends MobDoShattered> tipo, Level mundo, Atributos atributos, Sons sons) {
        super(tipo, mundo);
        this.atributos = atributos;
        this.sons = sons;
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
        if (nadaNaAgua()) {
            goalSelector.addGoal(1, new FloatGoal(this));
        }
        // Ataca uma vez por turno do Shattered (o gnoll trapaceiro ataca de longe)
        goalSelector.addGoal(2, ataque());
        goalSelector.addGoal(5, passeio());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        goalSelector.addGoal(4, new SeguirHeroi(this));
        goalSelector.addGoal(3, new IrAoChamado(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Não enxerga nada quando está cego. Herói invisível ele ainda segue, mas não consegue atacar (AtaqueDoShattered).
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                alvo -> !aliado() && enxerga() && podeCacar(alvo)));
        // As imagens espelhadas distraem os inimigos (o mímico só se importa com quem mexeu nele)
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ImagemEspelhada.class, 10, true, false,
                alvo -> !aliado() && enxerga() && podeCacar(alvo)));
        // Os aliados (corrompidos) caçam os inimigos, e os inimigos caçam os aliados
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MobDoShattered.class, 10, true, false,
                alvo -> enxerga() && alvo instanceof MobDoShattered outro && outro.aliado() != aliado() && !(outro instanceof Mimico)
                        && !(this instanceof Mimico) && podeCacar(alvo)));
    }

    // Esse alvo pode ser caçado agora? (as abelhas só caçam quem chega perto do pote delas)
    protected boolean podeCacar(LivingEntity alvo) {
        return true;
    }

    // Como ele ataca (de perto, por padrão)
    protected Goal ataque() {
        return new AtaqueDoShattered(this, 1.0, false);
    }

    // Chefe ou chefe de missão (o Goo, os da missão do fantasma): não pode ser corrompido, fica condenado
    public boolean chefe() {
        return false;
    }

    // Aliado sem ninguém pra caçar vai atrás do herói (o fantasma da rosa pode receber a ordem de ficar num lugar)
    protected boolean segueOHeroi() {
        return true;
    }

    private boolean enxerga() {
        return !hasEffect(ModEfeitos.CEGO) && !hasEffect(ModEfeitos.ATERRORIZADO);
    }

    // Como o inimigo anda à toa quando não está caçando ninguém (o enxame sobrescreve pra voar)
    protected Goal passeio() {
        return new WaterAvoidingRandomStrollGoal(this, 0.8);
    }

    // Quem nada fica pulando na água pra boiar (o Goo não: ele anda pela água como se fosse chão)
    protected boolean nadaNaAgua() {
        return true;
    }

    public Atributos atributos() {
        return atributos;
    }

    // Quem fica mais forte nos andares mais fundos (o mímico) troca os números depois de nascer
    protected void trocarAtributos(Atributos novos) {
        atributos = novos;
    }

    // Precisão e esquiva agora (o Goo muda as dele quando fica furioso)
    public int precisao() {
        return atributos.precisao();
    }

    public int esquiva() {
        return atributos.esquiva();
    }

    // XP que dá ao morrer (o enxame dividido não dá)
    public int xpAoMorrer() {
        return atributos.xp();
    }

    // Cor do sangue que espirra quando apanha (vermelho, por padrão), igual ao Shattered
    public int corDoSangue() {
        return Particulas.SANGUE_PADRAO;
    }

    // O que pode cair quando morre (nada, por padrão)
    public ItemStack sortearSaque() {
        return ItemStack.EMPTY;
    }

    // ---- Números do combate (o Combate pergunta pra cada inimigo) ----

    @Override
    public LivingEntity entidade() {
        return this;
    }

    @Override
    public float precisao(Golpe golpe) {
        return precisao();
    }

    // Dormindo não dá pra desviar
    @Override
    public float esquiva(Golpe golpe) {
        return dormindo ? 0 : esquiva();
    }

    @Override
    public float sortearDano(Golpe golpe) {
        return sortearDano();
    }

    @Override
    public int sortearArmadura(Golpe golpe) {
        return sortearArmadura();
    }

    @Override
    public boolean surpreendidoPor(Combatente atacante) {
        return atacante.entidade() instanceof Player jogador && surpreendidoPor(jogador);
    }

    @Override
    public float aoAcertar(Golpe golpe, float dano) {
        aoAcertar(golpe.alvo().entidade(), dano);
        return dano;
    }

    // O próximo golpe acerta sem sortear (a mordida do mímico escondido)
    public boolean acertoGarantido() {
        return false;
    }

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
        removeEffect(ModEfeitos.SONO_MAGICO);
    }

    // Sono mágico (pedra do sono profundo): dorme e não nota ninguém; só acorda se alguém machucar
    public void dormirProfundamente() {
        dormir();
        setTarget(null);
        addEffect(new MobEffectInstance(ModEfeitos.SONO_MAGICO, MobEffectInstance.INFINITE_DURATION));
    }

    // Notou um herói: acorda e vai atrás dele. Igual ao Shattered, aparece um "!" em cima.
    public void notar(Player jogador) {
        acordar();
        setTarget(jogador);
        NumerosFlutuantes.mostrar(this, "!", NumerosFlutuantes.VERMELHO);
    }

    // Chamado de longe (a armadilha de alarme, o "beckon" do Shattered): acorda, mostra o "!" e, se não estiver caçando ninguém,
    // vai até o lugar
    public void chamar(Vec3 lugar) {
        acordar();
        NumerosFlutuantes.mostrar(this, "!", NumerosFlutuantes.VERMELHO);
        chamadoPara = lugar;
    }

    @Nullable
    public Vec3 chamadoPara() {
        return chamadoPara;
    }

    // Chamado quando este inimigo acerta alguém (o rato albino faz sangrar, por exemplo)
    public void aoAcertar(Entity alvo, float dano) {
    }

    public boolean levouGolpeSurpresa() {
        return levouGolpeSurpresa;
    }

    public void marcarGolpeSurpresa() {
        levouGolpeSurpresa = true;
    }

    // O herói pega o inimigo de surpresa se ele está dormindo, não está de olho nesse herói,
    // ou se o herói está invisível (igual ao Shattered)
    public boolean surpreendidoPor(Player jogador) {
        return dormindo || getTarget() != jogador || Furtividade.invisivel(jogador);
    }

    @Override
    public void tick() {
        super.tick();
        // Corrompido: perde 1% da vida máxima por turno
        if (hasEffect(ModEfeitos.CORROMPIDO) && !level().isClientSide() && Turno.virou(this)) {
            corrupcaoAcumulada += getMaxHealth() / 100F;
            if (corrupcaoAcumulada >= 1) {
                int dano = (int) corrupcaoAcumulada;
                corrupcaoAcumulada -= dano;
                invulnerableTime = 0;
                hurt(damageSources().magic(), dano);
            }
        }
        // Uma vez por turno, quem está dormindo pode notar um herói por perto.
        // Igual ao Shattered: a chance é maior quanto mais perto (1 quadradinho = 4 blocos).
        if (dormindo && !level().isClientSide() && Turno.virou(this)) {
            boolean sonoMagico = hasEffect(ModEfeitos.SONO_MAGICO);
            // De vez em quando sobe um "z" em cima de quem dorme (só se tiver alguém por perto pra ver)
            if (tickCount % 60 == 0 && level().getNearestPlayer(this, 16) != null) {
                NumerosFlutuantes.mostrar(this, "z", NumerosFlutuantes.BRANCO);
            }
            for (Player jogador : level().players()) {
                if (sonoMagico || jogador.isSpectator() || jogador.isCreative() || Furtividade.invisivel(jogador) || !hasLineOfSight(jogador)
                        || hasEffect(ModEfeitos.CEGO)) {
                    continue;
                }
                double distanciaEmQuadradinhos = distanceTo(jogador) / 4.0;
                if (distanciaEmQuadradinhos > 8 || TalentosDoTier2.passosSilenciosos(jogador, distanciaEmQuadradinhos)) {
                    continue;
                }
                // Glifo do ofuscamento: o herói é mais difícil de notar
                if (getRandom().nextDouble() * Math.max(1.0, distanciaEmQuadradinhos + GlifosDoHeroi.furtividade(jogador)) < 1.0) {
                    notar(jogador);
                    break;
                }
            }
        }
    }

    // ---- Aliado (corrompido) ----

    // Morto-vivo ou demônio (a bomba sagrada fere mais; o esgoto ainda não tem nenhum)
    public boolean mortoVivoOuDemonio() {
        return false;
    }

    public boolean aliado() {
        return hasEffect(ModEfeitos.CORROMPIDO) || hasEffect(ModEfeitos.DOMINADO) || aliadoDeNascenca();
    }

    // Quem já nasce aliado (o guardião da terra, as guardas)
    protected boolean aliadoDeNascenca() {
        return false;
    }

    // O herói dono do aliado (o que o aliado mata conta pra ele)
    protected void definirDono(@Nullable Player dono) {
        donoDoAliado = dono == null ? null : dono.getUUID();
    }

    // Corrompe: vira aliado com a vida cheia e sem os efeitos ruins (igual ao Shattered)
    public void corromper(@Nullable Player dono) {
        acordar();
        for (MobEffectInstance efeito : List.copyOf(getActiveEffects())) {
            if (!efeito.getEffect().value().isBeneficial()) {
                removeEffect(efeito.getEffect());
            }
        }
        setHealth(getMaxHealth());
        addEffect(new MobEffectInstance(ModEfeitos.CORROMPIDO, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
        definirDono(dono);
        setTarget(null);
        setLastHurtByMob(null);
        setPersistenceRequired();
        Particulas.subindoEmVolta(this, Particulas.poeira(0x4B0F4B, 1.2F), 20, 0.0);
    }

    // Dominado (pergaminho do canto da sereia): vira aliado pra sempre, sem perder vida (e sem se curar)
    public void dominar(@Nullable Player dono) {
        acordar();
        addEffect(new MobEffectInstance(ModEfeitos.DOMINADO, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
        definirDono(dono);
        setTarget(null);
        setLastHurtByMob(null);
        setPersistenceRequired();
        Particulas.subindoEmVolta(this, ParticleTypes.HEART, 10, 0.0);
    }

    // O herói que corrompeu (pra contar o XP do que o aliado mata)
    @Nullable
    public UUID donoDoAliado() {
        return donoDoAliado;
    }

    // Aliado não persegue herói (nem quando um herói bate nele sem querer)
    @Override
    public void setTarget(@Nullable LivingEntity alvo) {
        if (aliado() && (alvo instanceof Player || alvo instanceof ImagemEspelhada)) {
            return;
        }
        super.setTarget(alvo);
    }

    // Aliado sem ninguém pra caçar vai atrás do herói mais perto
    private static class SeguirHeroi extends Goal {
        private MobDoShattered aliado;
        @Nullable
        private Player heroi;

        SeguirHeroi(MobDoShattered aliado) {
            this.aliado = aliado;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!aliado.aliado() || aliado.getTarget() != null || !aliado.segueOHeroi()) {
                return false;
            }
            heroi = aliado.level().getNearestPlayer(aliado, 48);
            return heroi != null && !heroi.isSpectator() && aliado.distanceTo(heroi) > 8;
        }

        @Override
        public boolean canContinueToUse() {
            return heroi != null && aliado.aliado() && aliado.getTarget() == null && aliado.segueOHeroi() && aliado.distanceTo(heroi) > 5;
        }

        @Override
        public void tick() {
            if (heroi != null && aliado.tickCount % 10 == 0) {
                aliado.getNavigation().moveTo(heroi, 1.1);
            }
        }

        @Override
        public void stop() {
            heroi = null;
            aliado.getNavigation().stop();
        }
    }

    // Foi chamado e não está caçando ninguém: anda até o lugar do chamado
    private static class IrAoChamado extends Goal {
        private MobDoShattered quem;

        IrAoChamado(MobDoShattered quem) {
            this.quem = quem;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return quem.chamadoPara != null && quem.getTarget() == null && !quem.dormindo() && !quem.aliado();
        }

        @Override
        public void start() {
            if (quem.chamadoPara != null) {
                quem.getNavigation().moveTo(quem.chamadoPara.x, quem.chamadoPara.y, quem.chamadoPara.z, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return quem.chamadoPara != null && quem.getTarget() == null && !quem.getNavigation().isDone();
        }

        @Override
        public void stop() {
            quem.chamadoPara = null;
        }
    }

    // ---- Barra de vida ----

    // Mostra a vida em cima da cabeça como uma barra (vermelho = vida que sobra), igual ao Shattered.
    // Com a vida cheia, a barra some.
    public void atualizarBarraDeVida() {
        if (getHealth() >= getMaxHealth() || isDeadOrDying()) {
            setCustomName(null);
            setCustomNameVisible(false);
            return;
        }
        int cheios = Math.max(1, Math.round(PEDACOS_DA_BARRA * getHealth() / getMaxHealth()));
        MutableComponent barra = Component.literal("■".repeat(cheios)).withStyle(ChatFormatting.RED)
                .append(Component.literal("■".repeat(PEDACOS_DA_BARRA - cheios)).withStyle(ChatFormatting.DARK_GRAY));
        setCustomName(barra);
        setCustomNameVisible(true);
    }

    // ---- Sons ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return dormindo ? null : sons.ambiente();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fonte) {
        return sons.ferido();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sons.morte();
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
        tag.putBoolean("levou_golpe_surpresa", levouGolpeSurpresa);
        if (donoDoAliado != null) {
            tag.putUUID("dono_do_aliado", donoDoAliado);
        }
        tag.putFloat("corrupcao_acumulada", corrupcaoAcumulada);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dormindo = tag.getBoolean("dormindo");
        levouGolpeSurpresa = tag.getBoolean("levou_golpe_surpresa");
        donoDoAliado = tag.hasUUID("dono_do_aliado") ? tag.getUUID("dono_do_aliado") : null;
        corrupcaoAcumulada = tag.getFloat("corrupcao_acumulada");
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
