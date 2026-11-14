package com.louzada.pixeldungeon.entidade;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

// Espectro (o "Wraith" do Shattered): uma alma penada que sai dos túmulos (e da ossada assombrada). Voa, morre com 1 golpe,
// mas é difícil de acertar: precisão 10 + andar, esquiva 5 vezes isso, dano de 1 + andar/2 a 2 + andar. Não dá XP nem deixa nada.
// Aparece numa nuvem de almas e só começa a agir 2 turnos depois, já caçando quem abriu o túmulo.
public class Espectro extends MobDoShattered {
    public static final Atributos ATRIBUTOS = atributosDoAndar(1);
    public static final Sons SONS = new Sons(SoundEvents.VEX_AMBIENT, SoundEvents.VEX_HURT, SoundEvents.VEX_DEATH);
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int TICKS_PRA_APARECER = Turno.ticks(2);

    private int andar = 1;
    private int aparecendo = 0;

    public Espectro(EntityType<? extends Espectro> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static Atributos atributosDoAndar(int andar) {
        int precisao = 10 + andar;
        return new Atributos(1, precisao, precisao * 5, 1 + andar / 2, 2 + andar, 0, 0, 0, 0, 0.25);
    }

    public static AttributeSupplier.Builder atributosDoEspectro() {
        return atributosDoMinecraft(ATRIBUTOS).add(Attributes.FLYING_SPEED, 0.5);
    }

    public void definirAndar(int novo) {
        andar = Math.max(1, novo);
        trocarAtributos(atributosDoAndar(andar));
    }

    public int andar() {
        return andar;
    }

    // ---- Onde eles aparecem ----

    // Os 4 lados de quem abriu o túmulo (um quadradinho pra cada lado), onde tiver espaço. Devolve quantos apareceram.
    public static int criarEmVolta(ServerLevel mundo, Vec3 centro, int andar) {
        int quantos = 0;
        int[][] lados = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] lado : lados) {
            Vec3 lugar = centro.add(lado[0] * T, 0, lado[1] * T);
            if (livre(mundo, lugar) && criarEm(mundo, lugar, andar) != null) {
                quantos++;
            }
        }
        return quantos;
    }

    // Num lugar, ou (se não der) num dos 8 quadradinhos em volta. Null se não tiver espaço nenhum.
    @Nullable
    public static Espectro criarPerto(ServerLevel mundo, Vec3 lugar, int andar) {
        if (livre(mundo, lugar)) {
            return criarEm(mundo, lugar, andar);
        }
        List<Vec3> livres = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Vec3 vizinho = lugar.add(dx * T, 0, dz * T);
                if ((dx != 0 || dz != 0) && livre(mundo, vizinho)) {
                    livres.add(vizinho);
                }
            }
        }
        if (livres.isEmpty()) {
            return null;
        }
        return criarEm(mundo, livres.get(mundo.getRandom().nextInt(livres.size())), andar);
    }

    private static boolean livre(ServerLevel mundo, Vec3 lugar) {
        // (sem parede e sem ninguém no caminho)
        return mundo.noCollision(ModEntidades.ESPECTRO.get().getSpawnAABB(lugar.x, lugar.y, lugar.z))
                && mundo.getEntitiesOfClass(LivingEntity.class, ModEntidades.ESPECTRO.get().getSpawnAABB(lugar.x, lugar.y, lugar.z)).isEmpty();
    }

    @Nullable
    private static Espectro criarEm(ServerLevel mundo, Vec3 lugar, int andar) {
        Espectro espectro = ModEntidades.ESPECTRO.get().create(mundo);
        if (espectro == null) {
            return null;
        }
        espectro.moveTo(lugar.x, lugar.y, lugar.z, mundo.getRandom().nextFloat() * 360.0F, 0.0F);
        espectro.definirAndar(andar);
        espectro.setPersistenceRequired();
        espectro.aparecendo = TICKS_PRA_APARECER;
        espectro.setNoAi(true);
        mundo.addFreshEntity(espectro);
        Particulas.explosao(mundo, ParticleTypes.SOUL, lugar.x, lugar.y + 1.0, lugar.z, 8, 0.4, 0.02);
        mundo.playSound(null, espectro.blockPosition(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.0F, 0.6F);
        return espectro;
    }

    // Quem pegou o item amaldiçoado da ossada assombrada: um espectro aparece do lado; se não tiver espaço, a maldição
    // tira metade da vida de quem pegou (igual ao Shattered)
    public static void assombrar(ServerLevel mundo, Player quem) {
        Espectro espectro = criarPerto(mundo, quem.position(), SaqueDoAndar.andarDe(quem));
        if (espectro == null) {
            quem.hurt(mundo.damageSources().magic(), quem.getHealth() / 2);
        }
        Particulas.explosao(mundo, ParticleTypes.SMOKE, quem.getX(), quem.getY() + 1.0, quem.getZ(), 12, 0.5, 0.02);
        mundo.playSound(null, quem.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    // Pegou o item amaldiçoado de uma ossada assombrada (ver SaqueDoAndar)
    public static void aoPegar(ItemEntityPickupEvent.Post evento) {
        ItemEntity noChao = evento.getItemEntity();
        if (!noChao.getPersistentData().getBoolean(SaqueDoAndar.ASSOMBRADO) || !(evento.getPlayer() instanceof ServerPlayer jogador)
                || !(jogador.level() instanceof ServerLevel mundo)) {
            return;
        }
        noChao.getPersistentData().remove(SaqueDoAndar.ASSOMBRADO);
        assombrar(mundo, jogador);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && aparecendo > 0 && --aparecendo == 0) {
            setNoAi(false);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level mundo) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, mundo);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(true);
        navegacao.setCanPassDoors(true);
        return navegacao;
    }

    @Override
    protected Goal passeio() {
        return new WaterAvoidingRandomFlyingGoal(this, 0.8);
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    public boolean mortoVivoOuDemonio() {
        return true;
    }

    @Override
    public int xpAoMorrer() {
        return 0;
    }

    // Sangue de sombra (cor do Shattered)
    @Override
    public int corDoSangue() {
        return 0x222222;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("andar", andar);
        tag.putInt("aparecendo", aparecendo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        definirAndar(tag.getInt("andar"));
        aparecendo = tag.getInt("aparecendo");
    }
}
