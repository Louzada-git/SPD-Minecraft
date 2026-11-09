package com.louzada.pixeldungeon.entidade;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.artefato.RosaSeca;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// O fantasma da rosa seca (o "GhostHero" do Shattered): um aliado que luta do lado do herói dono da rosa.
//   vida máxima: 40 + 10 x nível da rosa; força: 13 + nível/2
//   precisão: nível do herói + 9; esquiva: nível do herói + 4 (a arma e a armadura que ele leva mudam isso, igual ao herói)
//   dano: o da arma que ele leva (mais o bônus da força que sobra), ou 1 a força - 8 sem arma; armadura: a da armadura dele
//   sem a rosa equipada pelo dono, perde 1 de vida por turno
//   ordens (com a rosa, ver RosaSeca): atacar um inimigo, guardar um lugar ou seguir o herói
public class FantasmaAliado extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura, XP, nível máximo, velocidade (o resto vem da rosa e do herói)
    public static final Atributos ATRIBUTOS = new Atributos(40, 10, 5, 1, 5, 0, 0, 0, 30, 0.3);
    public static final Sons SONS = new Sons(null, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH);
    private static final int FALAS = 6;

    // O fantasma de cada herói (só um por vez)
    private static final Map<UUID, FantasmaAliado> PELO_DONO = new HashMap<>();

    public enum Ordem {
        SEGUIR, GUARDAR, ATACAR
    }

    private Ordem ordem = Ordem.SEGUIR;
    @Nullable
    private BlockPos lugarGuardado;
    private int nivelDaRosa = 0;
    private int nivelDoHeroi = 1;
    private ItemStack arma = ItemStack.EMPTY;
    private ItemStack armadura = ItemStack.EMPTY;
    private boolean conversou = false;
    @Nullable
    private Player heroi;

    public FantasmaAliado(EntityType<? extends FantasmaAliado> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GuardarLugar(this));
    }

    @Override
    protected boolean aliadoDeNascenca() {
        return true;
    }

    @Override
    protected boolean segueOHeroi() {
        return ordem == Ordem.SEGUIR;
    }

    // O fantasma desse herói, se estiver vivo
    @Nullable
    public static FantasmaAliado doDono(Player dono) {
        FantasmaAliado fantasma = PELO_DONO.get(dono.getUUID());
        return fantasma != null && fantasma.isAlive() && !fantasma.isRemoved() ? fantasma : null;
    }

    // Nasce (ou é atualizado) com os números da rosa do herói
    public void receber(Player dono, ItemStack rosa) {
        heroi = dono;
        definirDono(dono);
        PELO_DONO.put(dono.getUUID(), this);
        atualizar(dono, rosa);
    }

    private void atualizar(Player dono, ItemStack rosa) {
        nivelDaRosa = ArtefatoAchado.nivel(rosa);
        arma = RosaSeca.arma(rosa);
        armadura = RosaSeca.armadura(rosa);
        nivelDoHeroi = RegrasDoHeroi.dados(dono).nivel();
        AttributeInstance vida = getAttribute(Attributes.MAX_HEALTH);
        if (vida != null && vida.getBaseValue() != 40 + 10 * nivelDaRosa) {
            vida.setBaseValue(40 + 10 * nivelDaRosa);
        }
    }

    public int forca() {
        return 13 + nivelDaRosa / 2;
    }

    public ItemStack arma() {
        return arma;
    }

    public ItemStack armadura() {
        return armadura;
    }

    public Ordem ordem() {
        return ordem;
    }

    // ---- Os números do combate ----

    @Override
    public int precisao() {
        float precisao = nivelDoHeroi + 9;
        if (arma.getItem() instanceof ArmaDoShattered tipo) {
            precisao *= tipo.precisaoComForca(arma, forca());
        }
        return Math.round(precisao);
    }

    @Override
    public int esquiva() {
        float esquiva = nivelDoHeroi + 4;
        if (armadura.getItem() instanceof ArmaduraDoShattered tipo) {
            esquiva *= tipo.fatorDeEsquiva(armadura, forca());
        }
        return Math.round(esquiva);
    }

    @Override
    public int sortearDano() {
        if (arma.getItem() instanceof ArmaDoShattered tipo) {
            return tipo.sortearDanoComForca(arma, getRandom(), forca());
        }
        return Sorteio.normal(getRandom(), 1, forca() - 8);
    }

    @Override
    public int sortearArmadura() {
        int armaduraSorteada = 0;
        if (armadura.getItem() instanceof ArmaduraDoShattered tipo) {
            armaduraSorteada += tipo.sortearDefesa(armadura, getRandom(), forca());
        }
        if (arma.getItem() instanceof ArmaDoShattered tipo) {
            armaduraSorteada += tipo.sortearDefesaComForca(arma, getRandom(), forca());
        }
        return armaduraSorteada;
    }

    @Override
    public int corDoSangue() {
        return 0xCCDDFF;
    }

    // ---- Todo turno ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive() || !Turno.virou(this)) {
            return;
        }
        Player dono = dono();
        if (dono != null) {
            PELO_DONO.put(dono.getUUID(), this);
        }
        ItemStack rosa = dono == null ? null : ArtefatoAchado.equipado(dono, RosaSeca.class);
        // Sem a rosa equipada, o fantasma vai se apagando
        if (rosa == null || !(dono instanceof ServerPlayer heroiNoServidor) || !GerenciadorDoHeroi.emJogo(heroiNoServidor)) {
            invulnerableTime = 0;
            hurt(damageSources().magic(), 1);
            return;
        }
        atualizar(dono, rosa);
        if (ordem == Ordem.ATACAR && (getTarget() == null || !getTarget().isAlive())) {
            ordem = Ordem.SEGUIR;
        }
    }

    @Nullable
    private Player dono() {
        if (heroi != null && !heroi.isRemoved()) {
            return heroi;
        }
        heroi = donoDoAliado() == null ? null : level().getPlayerByUUID(donoDoAliado());
        return heroi;
    }

    // ---- Ordens ----

    public void atacar(Entity alvo) {
        if (alvo instanceof MobDoShattered inimigo && !inimigo.aliado()) {
            ordem = Ordem.ATACAR;
            lugarGuardado = null;
            setTarget(inimigo);
            falar("fantasma_aliado.pixeldungeon.atacar");
        }
    }

    public void guardar(BlockPos lugar) {
        ordem = Ordem.GUARDAR;
        lugarGuardado = lugar;
        setTarget(null);
        getNavigation().moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, 1.2);
        falar("fantasma_aliado.pixeldungeon.guardar");
    }

    public void seguir() {
        ordem = Ordem.SEGUIR;
        lugarGuardado = null;
        setTarget(null);
        falar("fantasma_aliado.pixeldungeon.seguir");
    }

    // O que o fantasma diz aparece pro dono (igual ao "yell" do Shattered)
    public void falar(String chave, Object... argumentos) {
        Player dono = dono();
        if (dono != null) {
            dono.displayClientMessage(Component.translatable("fantasma_aliado.pixeldungeon.diz", Component.translatable(chave, argumentos))
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }

    // Conversar: na primeira vez ele se apresenta; depois, comenta o lugar
    @Override
    protected InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (!level().isClientSide() && mao == InteractionHand.MAIN_HAND && jogador == dono()) {
            if (!conversou) {
                conversou = true;
                falar("fantasma_aliado.pixeldungeon.apresentar");
            } else {
                falar("fantasma_aliado.pixeldungeon.esgoto_" + (1 + getRandom().nextInt(FALAS)));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void die(DamageSource fonte) {
        falar("fantasma_aliado.pixeldungeon.derrotado");
        Particulas.subindoEmVolta(this, ParticleTypes.SOUL, 12, 0.05);
        super.die(fonte);
    }

    @Override
    public void remove(RemovalReason motivo) {
        if (donoDoAliado() != null && PELO_DONO.get(donoDoAliado()) == this) {
            PELO_DONO.remove(donoDoAliado());
        }
        super.remove(motivo);
    }

    // Vai junto com o herói pra outro andar (aparece do lado dele)
    public static void acompanhar(ServerPlayer jogador) {
        FantasmaAliado fantasma = doDono(jogador);
        if (fantasma != null) {
            fantasma.teleportTo(jogador.getX() + 1.0, jogador.getY(), jogador.getZ());
            fantasma.getNavigation().stop();
            fantasma.setTarget(null);
            if (fantasma.ordem == Ordem.GUARDAR) {
                fantasma.ordem = Ordem.SEGUIR;
                fantasma.lugarGuardado = null;
            }
        }
    }

    // Mostra a vida que ganhou (a rosa cura o fantasma devagar)
    public void curarPelaRosa(int quanto) {
        if (quanto > 0 && getHealth() < getMaxHealth()) {
            heal(quanto);
            NumerosFlutuantes.mostrar(this, "+" + quanto, NumerosFlutuantes.VERDE);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ordem", ordem.name());
        if (lugarGuardado != null) {
            tag.put("lugar_guardado", NbtUtils.writeBlockPos(lugarGuardado));
        }
        tag.putInt("nivel_da_rosa", nivelDaRosa);
        tag.putInt("nivel_do_heroi", nivelDoHeroi);
        tag.putBoolean("conversou", conversou);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            ordem = Ordem.valueOf(tag.getString("ordem"));
        } catch (IllegalArgumentException semOrdem) {
            ordem = Ordem.SEGUIR;
        }
        lugarGuardado = NbtUtils.readBlockPos(tag, "lugar_guardado").orElse(null);
        nivelDaRosa = tag.getInt("nivel_da_rosa");
        nivelDoHeroi = Math.max(1, tag.getInt("nivel_do_heroi"));
        conversou = tag.getBoolean("conversou");
    }

    // Guardando um lugar: sem inimigo pra caçar, volta pra lá
    private static class GuardarLugar extends Goal {
        private FantasmaAliado fantasma;

        GuardarLugar(FantasmaAliado fantasma) {
            this.fantasma = fantasma;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fantasma.ordem == Ordem.GUARDAR && fantasma.lugarGuardado != null && fantasma.getTarget() == null
                    && fantasma.blockPosition().distSqr(fantasma.lugarGuardado) > 9;
        }

        @Override
        public void start() {
            BlockPos lugar = fantasma.lugarGuardado;
            if (lugar != null) {
                fantasma.getNavigation().moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, 1.1);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !fantasma.getNavigation().isDone();
        }
    }
}
