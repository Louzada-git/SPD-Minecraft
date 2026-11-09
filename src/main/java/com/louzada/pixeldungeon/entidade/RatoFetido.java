package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.efeito.MucoCaustico;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// Rato fétido (o "FetidRat" do Shattered, da missão do fantasma no andar 2): um rato podre e forte.
//   1 em cada 3 golpes deixa muco cáustico (que vai derretendo a vida até a pessoa entrar na água)
//   cada vez que apanha, solta uma nuvem de fedor que paralisa por 2 turnos (ele não sente o fedor)
public class RatoFetido extends MobDoShattered {
    // vida, precisão, esquiva, dano, armadura (a do rato + 0 a 2), XP, nível máximo, velocidade
    public static final Atributos ATRIBUTOS = new Atributos(20, 12, 5, 1, 4, 0, 3, 4, 5, 0.25);
    public static final Sons SONS = new Sons(SoundEvents.BAT_AMBIENT, SoundEvents.BAT_HURT, SoundEvents.BAT_DEATH);

    public RatoFetido(EntityType<? extends RatoFetido> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new VagarAteOHeroi(this));
    }

    @Override
    public boolean chefe() {
        return true;
    }

    @Override
    public int corDoSangue() {
        return 0x335522;
    }

    @Override
    public void aoAcertar(Entity alvo, float dano) {
        if (!(alvo instanceof LivingEntity criatura) || getRandom().nextInt(3) != 0) {
            return;
        }
        boolean jaTinha = criatura.hasEffect(ModEfeitos.MUCO_CAUSTICO);
        criatura.addEffect(new MobEffectInstance(ModEfeitos.MUCO_CAUSTICO, MucoCaustico.DURACAO));
        if (!jaTinha && criatura instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.muco_caustico.aviso").withStyle(ChatFormatting.RED), false);
        }
    }

    // Apanhou: solta fedor
    @Override
    public boolean hurt(DamageSource fonte, float dano) {
        boolean machucou = super.hurt(fonte, dano);
        if (machucou && level() instanceof ServerLevel mundo && fonte.getEntity() != null) {
            NuvemDeGas.soltar(mundo, position(), NuvemDeGas.Tipo.FEDOR, 5.0F, 4);
        }
        return machucou;
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        MissaoDoFantasma.aoMorrerOChefe(this);
    }
}
