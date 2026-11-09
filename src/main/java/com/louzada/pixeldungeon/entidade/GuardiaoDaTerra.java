package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.heroi.ArmaduraDePedra;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// O guardião da terra (o "EarthGuardian" da varinha de terra viva): um aliado de pedra que luta do lado do herói.
//   vida máxima: 16 + 8 x nível da varinha; ele nasce com a armadura de pedra que o herói tinha, e cada disparo
//     da varinha nele (ou num inimigo enquanto ele existe) cura o guardião com o dano do disparo
//   esquiva: metade da do herói ((nível do herói + 4) / 2); precisão: 2 x esquiva + 5, igual à do herói
//   dano 2 a 4 + andar/2; armadura nível a 3 + 3 x nível da varinha
//   quando não tem mais nenhum inimigo à vista, volta pro herói, virando armadura de pedra com a vida que sobrou
public class GuardiaoDaTerra extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura, XP, nível máximo, velocidade (o resto muda com a varinha e o herói)
    public static final Atributos ATRIBUTOS = new Atributos(16, 7, 2, 2, 4, 0, 3, 0, 30, 0.3);
    public static final Sons SONS = new Sons(null, SoundEvents.STONE_HIT, SoundEvents.STONE_BREAK);
    // Até onde ele enxerga inimigos (8 quadradinhos)
    private static final double VISAO = 32.0;

    private int nivelDaVarinha = -1;
    private int esquivaDoHeroi = 2;
    // O herói que criou (não é salvo: depois de sair e entrar, ele é achado pelo dono do aliado)
    @Nullable
    private Player heroi;

    public GuardiaoDaTerra(EntityType<? extends GuardiaoDaTerra> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    protected boolean aliadoDeNascenca() {
        return true;
    }

    public int nivelDaVarinha() {
        return nivelDaVarinha;
    }

    // Nasce ou ganha mais vida (o "setInfo" do Shattered)
    public void receber(Player heroi, int nivelDaVarinha, int vidaASomar) {
        boolean novo = this.nivelDaVarinha < 0;
        if (nivelDaVarinha > this.nivelDaVarinha) {
            this.nivelDaVarinha = nivelDaVarinha;
            AttributeInstance vida = getAttribute(Attributes.MAX_HEALTH);
            if (vida != null) {
                vida.setBaseValue(16 + 8 * nivelDaVarinha);
            }
        }
        if (novo) {
            setHealth(Math.max(1, Math.min(getMaxHealth(), vidaASomar)));
        } else {
            setHealth(Math.min(getMaxHealth(), getHealth() + vidaASomar));
            if (vidaASomar > 0) {
                NumerosFlutuantes.mostrar(this, "+" + vidaASomar, NumerosFlutuantes.VERDE);
            }
        }
        esquivaDoHeroi = (RegrasDoHeroi.dados(heroi).nivel() + 4) / 2;
        this.heroi = heroi;
        definirDono(heroi);
        Particulas.subindoEmVolta(this, Particulas.poeira(0x805500, 1.2F), 8 + nivelDaVarinha / 2, 0.0);
    }

    @Override
    public int precisao() {
        return 2 * esquivaDoHeroi + 5;
    }

    @Override
    public int esquiva() {
        return esquivaDoHeroi;
    }

    @Override
    public int sortearDano() {
        int andar = Math.max(1, Andares.andarPelaAltura(getBlockY()));
        return Sorteio.normal(getRandom(), 2, 4 + andar / 2);
    }

    @Override
    public int sortearArmadura() {
        int nivel = Math.max(0, nivelDaVarinha);
        return Sorteio.normal(getRandom(), nivel, 3 + 3 * nivel);
    }

    @Override
    public int corDoSangue() {
        return 0x966400;
    }

    // Uma vez por turno: sem inimigo à vista, volta pro herói
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && Turno.virou(this)) {
            conferirInimigos();
        }
    }

    // Sem inimigo à vista, volta pro herói (devolve se voltou)
    public boolean conferirInimigos() {
        if (veInimigo()) {
            return false;
        }
        voltarProHeroi();
        return true;
    }

    private boolean veInimigo() {
        LivingEntity alvo = getTarget();
        if (alvo != null && alvo.isAlive() && hasLineOfSight(alvo)) {
            return true;
        }
        for (MobDoShattered outro : level().getEntitiesOfClass(MobDoShattered.class, getBoundingBox().inflate(VISAO),
                outro -> outro.isAlive() && !outro.aliado() && !(outro instanceof Mimico))) {
            if (distanceTo(outro) <= VISAO && hasLineOfSight(outro)) {
                setTarget(outro);
                return true;
            }
        }
        return false;
    }

    private void voltarProHeroi() {
        Player dono = dono();
        if (dono != null && dono.isAlive()) {
            ArmaduraDePedra.somar(dono, Math.max(0, nivelDaVarinha), Math.round(getHealth()));
        }
        Particulas.subindoEmVolta(this, Particulas.poeira(0x805500, 1.2F), 12, 0.05);
        discard();
    }

    @Nullable
    private Player dono() {
        if (heroi != null && !heroi.isRemoved()) {
            return heroi;
        }
        return donoDoAliado() == null ? null : level().getPlayerByUUID(donoDoAliado());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nivel_da_varinha", nivelDaVarinha);
        tag.putInt("esquiva_do_heroi", esquivaDoHeroi);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nivelDaVarinha = tag.getInt("nivel_da_varinha");
        esquivaDoHeroi = tag.getInt("esquiva_do_heroi");
    }
}
