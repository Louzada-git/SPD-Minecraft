package com.louzada.pixeldungeon.entidade;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.GerenciadorDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

// Goo: o chefe do esgoto (andar 5), com as regras do Shattered:
//   - na água ele se cura 1 de vida por turno (1 s): é preciso tirar ele da poça
//   - às vezes, em vez de atacar, ele "bombeia" (aparece "!!!"). Depois de 2 turnos solta um golpe
//     com 3x o dano e 2x a precisão, que alcança 2 quadradinhos. Quem se afasta antes cancela o golpe.
//   - com metade da vida ou menos fica furioso: mais dano, mais precisão, mais esquiva e bombeia mais
//   - 1 em cada 3 golpes deixa muco cáustico, que vai derretendo a vida até a pessoa entrar na água
//   - enquanto a luta acontece ninguém sobe a escada do andar, e a saída só abre quando ele morre
public class Goo extends MobDoShattered {
    // vida, precisão, esquiva, dano mínimo, dano máximo, armadura mínima, armadura máxima, XP, nível máximo, velocidade
    public static final Atributos ATRIBUTOS = new Atributos(100, 10, 8, 1, 8, 0, 2, 10, 29, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.SLIME_SQUISH, SoundEvents.SLIME_HURT, SoundEvents.SLIME_DEATH);

    // O golpe carregado alcança 2 quadradinhos (1 quadradinho = 4 blocos), contando do meio do Goo
    private static final double ALCANCE_DO_GOLPE_CARREGADO = 2 * Andares.TAMANHO_DO_QUADRADINHO + 1;
    // Distância em que os heróis contam como "na luta" (e veem a barra do chefe)
    private static final double DISTANCIA_DA_LUTA = 48;
    // Bombear leva 2 turnos antes de soltar o golpe
    private static final int TICKS_PARA_SOLTAR = 2 * 20;

    // Quanto o Goo já bombeou (0 = nada, 1 e 2 = inchando). O jogo do jogador usa pra desenhar ele inchado.
    private static final EntityDataAccessor<Integer> CARGA = SynchedEntityData.defineId(Goo.class, EntityDataSerializers.INT);

    private ServerBossEvent barra = new ServerBossEvent(Component.translatable("entity.pixeldungeon.goo"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private int ticksCarregando = 0;
    private boolean soltando = false;
    private boolean emLuta = false;
    private boolean estavaFurioso = false;
    private boolean jaGritou = false;

    public Goo(EntityType<? extends Goo> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
        // Ele adora água: anda por ela sem desviar
        setPathfindingMalus(PathType.WATER, 0.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    public static AttributeSupplier.Builder atributosDoGoo() {
        return atributosDoMinecraft(ATRIBUTOS)
                .add(Attributes.FOLLOW_RANGE, DISTANCIA_DA_LUTA)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                // Na água ele anda tão rápido quanto no chão
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(CARGA, 0);
    }

    public int carga() {
        return entityData.get(CARGA);
    }

    @Override
    protected boolean nadaNaAgua() {
        return false;
    }

    @Override
    protected Goal passeio() {
        return new RandomStrollGoal(this, 0.8);
    }

    // Igual ao Shattered: furioso com metade da vida ou menos
    public boolean furioso() {
        return getHealth() * 2 <= getMaxHealth();
    }

    // Está lutando com alguém (a escada de subida do andar fica bloqueada)
    public boolean lutando() {
        return emLuta && isAlive();
    }

    // ---- Números do combate (Shattered) ----

    @Override
    public int precisao() {
        int precisao = furioso() ? 15 : 10;
        return soltando ? precisao * 2 : precisao;
    }

    @Override
    public int esquiva() {
        return furioso() ? 12 : 8;
    }

    @Override
    public int sortearDano() {
        int maximo = furioso() ? 12 : 8;
        if (soltando) {
            return Sorteio.normal(getRandom(), 3, maximo * 3);
        }
        return Sorteio.normal(getRandom(), 1, maximo);
    }

    // 1 em cada 3 golpes que acertam deixa muco cáustico
    @Override
    public void aoAcertar(Entity alvo, float dano) {
        if (!(alvo instanceof LivingEntity criatura) || getRandom().nextInt(3) != 0) {
            return;
        }
        boolean jaTinha = criatura.hasEffect(ModEfeitos.MUCO_CAUSTICO);
        criatura.addEffect(new MobEffectInstance(ModEfeitos.MUCO_CAUSTICO, MucoCaustico.DURACAO));
        if (level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.SQUID_INK, criatura.getX(), criatura.getY(0.5), criatura.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
        }
        if (!jaTinha && criatura instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.muco_caustico.aviso").withStyle(ChatFormatting.RED), false);
        }
    }

    // ---- Ataque e golpe carregado ----

    // Chamado quando ele está colado em alguém e pode atacar (uma vez por turno)
    @Override
    public boolean doHurtTarget(Entity alvo) {
        if (soltando) {
            return super.doHurtTarget(alvo);
        }
        if (carga() > 0) {
            // Bombeando: o golpe sai sozinho quando terminar de carregar
            return false;
        }
        // Igual ao Shattered: 1 em 5 vezes (1 em 2 se estiver furioso) ele bombeia em vez de atacar
        if (getRandom().nextInt(furioso() ? 2 : 5) > 0) {
            return super.doHurtTarget(alvo);
        }
        comecarABombear();
        return false;
    }

    private void comecarABombear() {
        entityData.set(CARGA, 1);
        ticksCarregando = 0;
        NumerosFlutuantes.mostrar(this, "!!!", NumerosFlutuantes.LARANJA);
        avisarQuemEstaPerto(Component.translatable("goo.pixeldungeon.bombeando").withStyle(ChatFormatting.RED));
        playSound(SoundEvents.SLIME_JUMP, 1.5F, 0.5F);
    }

    private void pararDeBombear() {
        entityData.set(CARGA, 0);
        ticksCarregando = 0;
    }

    private void continuarBombeando() {
        if (!(getTarget() instanceof Player alvo) || !alvo.isAlive() || alvo.isSpectator() || !aoAlcance(alvo)) {
            // Igual ao Shattered: se o alvo saiu do alcance, o Goo desiste e vai atrás dele
            pararDeBombear();
            return;
        }
        ticksCarregando++;
        if (ticksCarregando % 5 == 0) {
            mostrarAlcance();
        }
        if (ticksCarregando == TICKS_PARA_SOLTAR / 2) {
            entityData.set(CARGA, 2);
            playSound(SoundEvents.SLIME_JUMP, 1.5F, 0.4F);
        }
        if (ticksCarregando >= TICKS_PARA_SOLTAR) {
            soltar(alvo);
        }
    }

    private void soltar(LivingEntity alvo) {
        soltando = true;
        doHurtTarget(alvo);
        soltando = false;
        pararDeBombear();
        playSound(SoundEvents.SLIME_ATTACK, 2.0F, 0.5F);
        if (level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.SQUID_INK, alvo.getX(), alvo.getY(0.5), alvo.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
        }
    }

    // Alcance do golpe carregado: até 2 quadradinhos, sem parede no meio
    private boolean aoAlcance(LivingEntity alvo) {
        double dx = alvo.getX() - getX();
        double dz = alvo.getZ() - getZ();
        return dx * dx + dz * dz <= ALCANCE_DO_GOLPE_CARREGADO * ALCANCE_DO_GOLPE_CARREGADO
                && Math.abs(alvo.getY() - getY()) < Andares.ALTURA_DA_PAREDE
                && hasLineOfSight(alvo);
    }

    // Um círculo de gosma escura no chão mostra até onde o golpe carregado alcança
    private void mostrarAlcance() {
        if (!(level() instanceof ServerLevel mundo)) {
            return;
        }
        int pontos = 32;
        for (int i = 0; i < pontos; i++) {
            double angulo = i * Math.PI * 2 / pontos;
            mundo.sendParticles(ParticleTypes.SQUID_INK, getX() + Math.cos(angulo) * ALCANCE_DO_GOLPE_CARREGADO, getY() + 0.2,
                    getZ() + Math.sin(angulo) * ALCANCE_DO_GOLPE_CARREGADO, 1, 0.0, 0.0, 0.0, 0.0);
        }
        mundo.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(0.6), getZ(), 6, 0.6, 0.4, 0.6, 0.02);
    }

    // Bombeando, ele fica parado no lugar
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (carga() > 0) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
            if (getTarget() != null) {
                getLookControl().setLookAt(getTarget());
            }
        }
    }

    // ---- Cada tick e cada turno ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        barra.setProgress(getHealth() / getMaxHealth());
        verificarFuria();
        if (carga() > 0) {
            continuarBombeando();
        }
        if (tickCount % 20 == 0) {
            umTurno();
        }
    }

    private void umTurno() {
        // Na água ele se cura (até dormindo, igual ao Shattered)
        if (isInWater() && getHealth() < getMaxHealth()) {
            heal(1.0F);
            if (level().getNearestPlayer(this, 32) != null) {
                NumerosFlutuantes.mostrar(this, "+1", NumerosFlutuantes.VERDE);
            }
        }
        atualizarLuta();
    }

    private void verificarFuria() {
        boolean agora = furioso();
        if (agora && !estavaFurioso) {
            estavaFurioso = true;
            barra.setColor(BossEvent.BossBarColor.RED);
            NumerosFlutuantes.mostrarTraduzido(this, "goo.pixeldungeon.furioso", NumerosFlutuantes.LARANJA);
            gritar("goo.pixeldungeon.gluuurp");
        } else if (!agora && estavaFurioso) {
            // Se curou na água e passou da metade da vida: acalma
            estavaFurioso = false;
            barra.setColor(BossEvent.BossBarColor.GREEN);
        }
        // Furioso, ele espirra gosma pra todo lado
        if (estavaFurioso && tickCount % 10 == 0 && level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(0.8), getZ(), 2, 0.5, 0.2, 0.5, 0.05);
        }
    }

    // Começa a luta quando ele vai atrás de um herói; acaba quando não sobra herói vivo por perto.
    // Quem está por perto (até os mortos, que assistem) vê a barra de vida do chefe.
    private void atualizarLuta() {
        List<ServerPlayer> perto = jogadoresPerto();
        boolean temHeroi = false;
        for (ServerPlayer jogador : perto) {
            if (jogador.isAlive() && !jogador.isSpectator() && !jogador.isCreative()) {
                temHeroi = true;
            }
        }
        boolean temAlvo = !dormindo() && getTarget() instanceof Player alvo && alvo.isAlive() && !alvo.isSpectator();

        if (!emLuta && temAlvo) {
            emLuta = true;
            if (!jaGritou) {
                jaGritou = true;
                gritar("goo.pixeldungeon.notou");
            }
        } else if (emLuta && !temHeroi) {
            emLuta = false;
            pararDeBombear();
            setTarget(null);
        }

        for (ServerPlayer jogador : new ArrayList<>(barra.getPlayers())) {
            if (!emLuta || !perto.contains(jogador)) {
                barra.removePlayer(jogador);
            }
        }
        if (emLuta) {
            for (ServerPlayer jogador : perto) {
                barra.addPlayer(jogador);
            }
        }
    }

    // Jogadores no mesmo andar, perto do Goo
    private List<ServerPlayer> jogadoresPerto() {
        return level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(DISTANCIA_DA_LUTA, 6, DISTANCIA_DA_LUTA));
    }

    // O Goo "fala" no chat, igual ao Shattered ("Goo: GULP-GULP!")
    private void gritar(String chave) {
        avisarQuemEstaPerto(Component.translatable("mensagem.pixeldungeon.fala", getType().getDescription(), Component.translatable(chave))
                .withStyle(ChatFormatting.RED));
    }

    private void avisarQuemEstaPerto(Component mensagem) {
        for (ServerPlayer jogador : jogadoresPerto()) {
            jogador.displayClientMessage(mensagem, false);
        }
    }

    // O chefe não tem a barrinha em cima da cabeça: a vida dele fica na barra do chefe, no alto da tela
    @Override
    public void atualizarBarraDeVida() {
        barra.setProgress(getHealth() / getMaxHealth());
    }

    // ---- Morte ----

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        if (level().isClientSide() || !dead) {
            return;
        }
        gritar("goo.pixeldungeon.derrotado");
        barra.removeAllPlayers();

        // Igual ao Shattered: 60% de chance de 2 bolhas de gosma, 30% de 3 e 10% de 4
        int sorteio = getRandom().nextInt(10);
        int bolhas = sorteio < 6 ? 2 : sorteio < 9 ? 3 : 4;
        for (int i = 0; i < bolhas; i++) {
            spawnAtLocation(new ItemStack(ModItens.BOLHA_DE_GOSMA.get()));
        }

        if (level() instanceof ServerLevel mundo) {
            GerenciadorDaRun.aoDerrotarChefe(mundo, this);
        }
    }

    @Override
    public void remove(RemovalReason motivo) {
        super.remove(motivo);
        barra.removeAllPlayers();
    }

    // ---- O resto do Minecraft ----

    // Voz mais grossa que a do slime
    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("gritou", jaGritou);
        tag.putBoolean("furioso", estavaFurioso);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        jaGritou = tag.getBoolean("gritou");
        estavaFurioso = tag.getBoolean("furioso");
        if (estavaFurioso) {
            barra.setColor(BossEvent.BossBarColor.RED);
        }
    }

    // Sangue gosma preta (cor do Shattered)
    @Override
    public int corDoSangue() {
        return 0x000000;
    }
}
