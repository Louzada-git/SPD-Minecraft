package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Base de todos os inimigos do Shattered.
// Cada inimigo tem os números dele (vida, precisão, esquiva, dano, armadura, XP) e:
//   - nasce dormindo quando o andar é criado ("z" em cima dele), e acorda quando nota um herói por perto ("!")
//   - quem ataca um inimigo dormindo ou distraído faz um ataque surpresa (sempre acerta)
//   - depois de apanhar, mostra uma barra de vida em cima da cabeça
public abstract class MobDoShattered extends Monster {

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
        // Ataca uma vez por segundo, que é um turno do Shattered
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, passeio());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Não enxerga heróis invisíveis (capa das sombras, habilidade furtiva da adaga), nem enxerga nada quando está cego
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                alvo -> !Furtividade.invisivel(alvo) && !hasEffect(ModEfeitos.CEGO) && !hasEffect(ModEfeitos.ATERRORIZADO)));
        // As imagens espelhadas distraem os inimigos (o mímico só se importa com quem mexeu nele)
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ImagemEspelhada.class, 10, true, false,
                alvo -> !hasEffect(ModEfeitos.CEGO) && !hasEffect(ModEfeitos.ATERRORIZADO)));
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

    // ---- Números do combate ----

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
    }

    // Notou um herói: acorda e vai atrás dele. Igual ao Shattered, aparece um "!" em cima.
    public void notar(Player jogador) {
        acordar();
        setTarget(jogador);
        NumerosFlutuantes.mostrar(this, "!", NumerosFlutuantes.VERMELHO);
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

    // O herói pega o inimigo de surpresa se ele está dormindo ou não está de olho nesse herói
    public boolean surpreendidoPor(Player jogador) {
        return dormindo || getTarget() != jogador;
    }

    @Override
    public void tick() {
        super.tick();
        // Igual ao Shattered: quem estava caçando um herói que ficou invisível perde ele de vista
        if (!level().isClientSide() && tickCount % 10 == 0 && getTarget() instanceof Player alvo && Furtividade.invisivel(alvo)) {
            setTarget(null);
        }
        // Uma vez por segundo, quem está dormindo pode notar um herói por perto.
        // Igual ao Shattered: a chance é maior quanto mais perto (1 quadradinho = 4 blocos).
        if (dormindo && !level().isClientSide() && tickCount % 20 == 0) {
            // De vez em quando sobe um "z" em cima de quem dorme (só se tiver alguém por perto pra ver)
            if (tickCount % 60 == 0 && level().getNearestPlayer(this, 16) != null) {
                NumerosFlutuantes.mostrar(this, "z", NumerosFlutuantes.BRANCO);
            }
            for (Player jogador : level().players()) {
                if (jogador.isSpectator() || jogador.isCreative() || Furtividade.invisivel(jogador) || !hasLineOfSight(jogador)
                        || hasEffect(ModEfeitos.CEGO)) {
                    continue;
                }
                double distanciaEmQuadradinhos = distanceTo(jogador) / 4.0;
                if (distanciaEmQuadradinhos > 8) {
                    continue;
                }
                if (getRandom().nextDouble() * Math.max(1.0, distanciaEmQuadradinhos) < 1.0) {
                    notar(jogador);
                    break;
                }
            }
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dormindo = tag.getBoolean("dormindo");
        levouGolpeSurpresa = tag.getBoolean("levou_golpe_surpresa");
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
