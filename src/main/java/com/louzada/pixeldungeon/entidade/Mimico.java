package com.louzada.pixeldungeon.entidade;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

// Mímico (do andar 2 em diante): um baú que na verdade é um monstro, igual ao Shattered.
// Escondido, ele é um baú de verdade no chão (ver Baus), sem nenhuma diferença.
// Quem tenta abrir ou bate nele leva uma mordida que sempre acerta (2 + 2 × nível de dano),
// e aí ele vira esta criatura e luta. Quando morre, solta tudo o que guardava:
// o item que estava no "baú" e mais um prêmio.
// Números do Shattered (nível = andar): vida (1 + nível) × 6, precisão 6 + nível, esquiva 2 + nível / 2,
// dano 1 + nível a 2 + 2 × nível, armadura 0 a 1 + nível / 2. Não dá XP.
public class Mimico extends MobDoShattered {
    public static final Atributos ATRIBUTOS = atributosDoNivel(1);
    public static final Sons SONS = new Sons(SoundEvents.CHEST_CLOSE, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundEvents.WOOD_BREAK);

    private int nivel = 1;
    private List<ItemStack> itens = new ArrayList<>();
    // Está dando a mordida de quando estava escondido
    private boolean mordendo = false;

    public Mimico(EntityType<? extends Mimico> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    public static Atributos atributosDoNivel(int nivel) {
        return new Atributos((1 + nivel) * 6, 6 + nivel, 2 + nivel / 2, 1 + nivel, 2 + 2 * nivel, 0, 1 + nivel / 2, 0, 30, 0.25);
    }

    public void definirNivel(int novoNivel) {
        nivel = Math.max(1, novoNivel);
        trocarAtributos(atributosDoNivel(nivel));
    }

    public int nivel() {
        return nivel;
    }

    public List<ItemStack> itens() {
        return itens;
    }

    // Alguém mexeu num baú que era um mímico: o baú some, o mímico aparece no lugar dele e morde quem mexeu
    @Nullable
    public static Mimico revelar(ServerLevel mundo, BlockPos lugar, ChestBlockEntity bau, @Nullable ServerPlayer quemMexeu) {
        Mimico mimico = ModEntidades.MIMICO.get().create(mundo);
        if (mimico == null) {
            return null;
        }
        // Tira os itens do baú antes de apagar ele (senão eles caem no chão)
        for (int i = 0; i < bau.getContainerSize(); i++) {
            ItemStack item = bau.removeItemNoUpdate(i);
            if (!item.isEmpty()) {
                mimico.itens.add(item);
            }
        }
        float giro = bau.getBlockState().hasProperty(ChestBlock.FACING) ? bau.getBlockState().getValue(ChestBlock.FACING).toYRot() : 0.0F;
        mundo.setBlock(lugar, Blocks.AIR.defaultBlockState(), 3);

        mimico.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, giro, 0.0F);
        mimico.setYHeadRot(giro);
        mimico.setYBodyRot(giro);
        mimico.definirNivel(SaqueDoAndar.andarDe(mimico));
        mimico.ajustarVida(mundo, PopulacaoDoAndar.multiplicadorDeVida(mundo.getServer()));
        mimico.setPersistenceRequired();
        mundo.addFreshEntity(mimico);

        // Igual ao Shattered: estrelinhas, o som do mímico e o aviso
        Particulas.explosao(mundo, ParticleTypes.CRIT, mimico.getX(), mimico.getY(0.5), mimico.getZ(), 12, 0.4, 0.1);
        mundo.playSound(null, lugar, SoundEvents.CHEST_OPEN, SoundSource.HOSTILE, 1.0F, 0.6F);
        NumerosFlutuantes.mostrar(mimico, "!", NumerosFlutuantes.VERMELHO);
        for (ServerPlayer jogador : mundo.getEntitiesOfClass(ServerPlayer.class, mimico.getBoundingBox().inflate(16))) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.mimico_revelado").withStyle(ChatFormatting.RED), true);
        }

        if (quemMexeu != null && !Furtividade.invisivel(quemMexeu)) {
            mimico.setTarget(quemMexeu);
            mimico.morder(quemMexeu);
        }
        return mimico;
    }

    // A mordida de quando estava escondido: sempre acerta, com o dano máximo
    public void morder(ServerPlayer alvo) {
        mordendo = true;
        swing(InteractionHand.MAIN_HAND);
        doHurtTarget(alvo);
        mordendo = false;
    }

    @Override
    public boolean acertoGarantido() {
        return mordendo;
    }

    @Override
    public int sortearDano() {
        return mordendo ? 2 + 2 * nivel : super.sortearDano();
    }

    // Morreu: solta tudo o que guardava (e os itens nunca somem do chão)
    @Override
    public void die(DamageSource fonte) {
        boolean jaEstavaMorto = dead;
        super.die(fonte);
        if (jaEstavaMorto || !dead || level().isClientSide()) {
            return;
        }
        for (ItemStack item : itens) {
            ItemEntity noChao = spawnAtLocation(item);
            if (noChao != null) {
                noChao.setUnlimitedLifetime();
            }
        }
        itens.clear();
    }

    // Madeira: espirra lasquinhas marrons em vez de sangue
    @Override
    public int corDoSangue() {
        return 0x8B5A2B;
    }

    // ---- Salvar e carregar ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nivel", nivel);
        HolderLookup.Provider registros = registryAccess();
        ListTag lista = new ListTag();
        for (ItemStack item : itens) {
            if (!item.isEmpty()) {
                lista.add(item.save(registros));
            }
        }
        tag.put("itens", lista);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        definirNivel(tag.getInt("nivel"));
        HolderLookup.Provider registros = registryAccess();
        itens.clear();
        ListTag lista = tag.getList("itens", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            ItemStack.parse(registros, lista.getCompound(i)).ifPresent(itens::add);
        }
    }
}
