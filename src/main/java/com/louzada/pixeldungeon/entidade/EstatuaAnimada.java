package com.louzada.pixeldungeon.entidade;

import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Estátua animada (a sala da estátua): fica parada feito estátua até alguém bater nela (ou jogar algo ruim nela), e aí luta
// com a arma que segura: uma arma comum do andar, nunca amaldiçoada, sempre com um encantamento bom. Derrotada, deixa a arma.
// Números do Shattered: vida 15 + 5 x andar, esquiva 4 + andar, precisão (9 + andar) x a da arma, dano o da arma,
// armadura 0 a andar. Não dá XP.
public class EstatuaAnimada extends MobDoShattered {
    public static final Atributos ATRIBUTOS = atributosDoAndar(1);
    public static final Sons SONS = new Sons(SoundEvents.STONE_STEP, SoundEvents.STONE_HIT, SoundEvents.STONE_BREAK);

    private int andar = 1;
    private ItemStack arma = ItemStack.EMPTY;
    // Parada feito estátua (o "PASSIVE" do Shattered)
    private boolean parada = true;

    public EstatuaAnimada(EntityType<? extends EstatuaAnimada> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    public static Atributos atributosDoAndar(int andar) {
        return new Atributos(15 + 5 * andar, 9 + andar, 4 + andar, 1, 4, 0, andar, 0, 30, 0.25);
    }

    // Nasce com a arma (o sorteio decide qual), parada
    public void preparar(int novoAndar, Random sorteio) {
        andar = Math.max(1, novoAndar);
        trocarAtributos(atributosDoAndar(andar));
        arma = SaqueDoAndar.armaAleatoria(sorteio, andar);
        arma.remove(ModComponentes.MALDICAO.get());
        NivelDoItem.definirEncantamento(arma, Encantamento.sortear(sorteio));
        ficarParada();
    }

    public ItemStack arma() {
        return arma;
    }

    public boolean parada() {
        return parada;
    }

    private void ficarParada() {
        parada = true;
        setNoAi(true);
        setTarget(null);
    }

    // Alguém mexeu com ela: acorda pra lutar
    public void animar(@Nullable LivingEntity quem) {
        if (!parada) {
            return;
        }
        parada = false;
        setNoAi(false);
        if (quem != null) {
            setTarget(quem);
        }
    }

    @Override
    public boolean hurt(DamageSource fonte, float dano) {
        boolean doeu = super.hurt(fonte, dano);
        if (!level().isClientSide() && isAlive()) {
            animar(fonte.getEntity() instanceof LivingEntity quem ? quem : null);
        }
        return doeu;
    }

    @Override
    public boolean addEffect(MobEffectInstance efeito, @Nullable Entity fonte) {
        boolean pegou = super.addEffect(efeito, fonte);
        if (pegou && !level().isClientSide() && efeito.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            animar(fonte instanceof LivingEntity quem ? quem : null);
        }
        return pegou;
    }

    // Parada, ela não nota ninguém (não é sono: nada acorda ela além de mexer com ela)
    @Override
    public void acordar() {
        if (!parada) {
            super.acordar();
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity alvo) {
        super.setTarget(parada ? null : alvo);
    }

    @Override
    public float precisao(Golpe golpe) {
        float fator = arma.getItem() instanceof ArmaDoShattered tipo ? tipo.precisaoComForca(arma, 100) : 1;
        return precisao() * fator;
    }

    @Override
    public int sortearDano() {
        if (arma.getItem() instanceof ArmaDoShattered tipo) {
            int nivel = NivelDoItem.nivel(arma);
            return Sorteio.normal(getRandom(), tipo.danoMinimo(nivel), tipo.danoMaximo(nivel));
        }
        return super.sortearDano();
    }

    // Derrotada, deixa a arma
    @Override
    public void die(DamageSource fonte) {
        boolean jaEstavaMorta = dead;
        super.die(fonte);
        if (jaEstavaMorta || !dead || level().isClientSide() || arma.isEmpty()) {
            return;
        }
        ItemEntity noChao = spawnAtLocation(arma.copy());
        if (noChao != null) {
            noChao.setUnlimitedLifetime();
        }
        arma = ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("andar", andar);
        tag.putBoolean("parada", parada);
        if (!arma.isEmpty()) {
            tag.put("arma", arma.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        andar = Math.max(1, tag.getInt("andar"));
        trocarAtributos(atributosDoAndar(andar));
        parada = tag.getBoolean("parada");
        arma = tag.contains("arma") ? ItemStack.parse(registryAccess(), tag.getCompound("arma")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    // Pedra: espirra poeira cinza em vez de sangue
    @Override
    public int corDoSangue() {
        return 0x9A9A9A;
    }
}
