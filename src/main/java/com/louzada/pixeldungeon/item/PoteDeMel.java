package com.louzada.pixeldungeon.item;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.PoteArremessado;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// O pote de mel do Shattered (o depósito e a sala do fogo mágico às vezes têm um): jogado (clique direito), quebra onde cair
// e solta uma abelha furiosa, que defende o pote quebrado. Quebrado agachado (clique direito agachado), a abelha sai do lado e o
// pote quebrado fica com quem quebrou (e a abelha vai atrás dele).
public class PoteDeMel extends Item {

    public PoteDeMel(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (mundo instanceof ServerLevel servidor) {
            if (jogador.isShiftKeyDown()) {
                ItemStack pote = quebrar(servidor, jogador.position().add(jogador.getLookAngle().multiply(1.5, 0, 1.5)), jogador);
                if (!jogador.getInventory().add(pote)) {
                    jogador.drop(pote, false);
                }
            } else {
                PoteArremessado arremessado = new PoteArremessado(mundo, jogador);
                arremessado.setItem(pilha.copyWithCount(1));
                arremessado.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 1.2F, 0.5F);
                mundo.addFreshEntity(arremessado);
                mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
            }
        }
        jogador.getCooldowns().addCooldown(this, Turno.TICKS);
        if (!jogador.getAbilities().instabuild) {
            pilha.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(pilha, mundo.isClientSide());
    }

    // O pote quebra nesse lugar: sai uma abelha (do nível do andar) e sobra o pote quebrado, com o número que liga os dois.
    // quemSegura: quem vai ficar com o pote quebrado (null = o pote fica no chão).
    public static ItemStack quebrar(ServerLevel mundo, Vec3 lugar, @Nullable Player quemSegura) {
        mundo.playSound(null, lugar.x, lugar.y, lugar.z, SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS, 1.0F, 1.0F);
        mundo.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.HONEY_BOTTLE)), lugar.x, lugar.y + 0.3, lugar.z, 20,
                0.3, 0.2, 0.3, 0.1);
        int id = 1 + mundo.getRandom().nextInt(Integer.MAX_VALUE - 1);
        Abelha abelha = ModEntidades.ABELHA.get().create(mundo);
        if (abelha != null) {
            abelha.moveTo(lugar.x, lugar.y + 0.5, lugar.z, mundo.getRandom().nextFloat() * 360.0F, 0.0F);
            abelha.definirNivel(SaqueDoAndar.andarDe(abelha));
            abelha.definirPote(id, lugar, quemSegura);
            abelha.setPersistenceRequired();
            mundo.addFreshEntity(abelha);
            mundo.playSound(null, lugar.x, lugar.y, lugar.z, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return PoteQuebrado.criar(id);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag bandeira) {
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon.pote_de_mel.descricao", ChatFormatting.GRAY);
        linhas.add(Component.translatable("dica.pixeldungeon.pote_de_mel").withStyle(ChatFormatting.DARK_GRAY));
    }
}
