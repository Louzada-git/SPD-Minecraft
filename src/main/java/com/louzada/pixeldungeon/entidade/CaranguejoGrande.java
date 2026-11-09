package com.louzada.pixeldungeon.entidade;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Caranguejo grande (o "GreatCrab" do Shattered, da missão do fantasma no andar 4): enorme e mais lento que os outros.
// Bloqueia com a garra todo golpe de quem ele está caçando (e as magias das varinhas dele), se estiver vendo essa pessoa,
// acordado e sem paralisia. Pra acertar, só pegando ele distraído (de surpresa, invisível, ou com outro chamando a atenção dele).
// Sempre deixa 2 carnes misteriosas.
public class CaranguejoGrande extends MobDoShattered {
    // vida, precisão, esquiva (ver esquiva()), dano, armadura (as do caranguejo), XP, nível máximo, velocidade
    public static final Atributos ATRIBUTOS = new Atributos(25, 12, 0, 1, 7, 0, 4, 6, 9, 0.2);
    public static final Sons SONS = new Sons(SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH);

    public CaranguejoGrande(EntityType<? extends CaranguejoGrande> tipo, Level mundo) {
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
        return 0xFFEA80;
    }

    // Está de olho nesse atacante (e pode bloquear)?
    public boolean bloqueia(@Nullable Entity atacante) {
        return atacante != null && atacante == getTarget() && !dormindo() && !EfeitosNoTick.semDefesa(this) && hasLineOfSight(atacante)
                && !(atacante instanceof LivingEntity vivo && Furtividade.invisivel(vivo));
    }

    // Igual ao Shattered: contra quem ele está caçando, desvia de tudo
    @Override
    public float esquiva(Golpe golpe) {
        if (bloqueia(golpe.atacante().entidade())) {
            avisarBloqueio(golpe.atacante().entidade());
            return Float.MAX_VALUE;
        }
        return super.esquiva(golpe);
    }

    // As magias das varinhas (e do Clérigo) de quem ele está caçando também
    public boolean bloqueiaMagia(@Nullable Entity atacante, DamageSource fonte) {
        boolean magia = fonte.is(DamageTypes.INDIRECT_MAGIC) || fonte.is(DamageTypes.MAGIC) || fonte.is(DamageTypeTags.IS_LIGHTNING);
        if (magia && bloqueia(atacante)) {
            avisarBloqueio(atacante);
            return true;
        }
        return false;
    }

    private void avisarBloqueio(Entity atacante) {
        NumerosFlutuantes.mostrarTraduzido(this, "combate.pixeldungeon.bloqueou", NumerosFlutuantes.BRANCO);
        level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.8F + getRandom().nextFloat() * 0.2F);
        if (atacante instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.caranguejo_grande.bloqueou").withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public ItemStack sortearSaque() {
        return new ItemStack(ModItens.CARNE_MISTERIOSA.get(), 2);
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        MissaoDoFantasma.aoMorrerOChefe(this);
    }
}
