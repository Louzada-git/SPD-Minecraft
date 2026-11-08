package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.Aneis;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Um anel do Shattered (um item pra cada gema). Igual ao Shattered:
//   - cada gema esconde um tipo de anel, sorteado pela run ("anel de rubi" pode ser o da pressa)
//   - o herói usa até 2 anéis, nas casinhas de anel do inventário (clique direito põe e tira)
//   - o bônus é o nível + 1; dois anéis iguais somam (ver Aneis)
//   - vêm +0 (2/3), +1 (4/15) ou +2 (1/15), sem identificar; usando, se identificam depois de 1 nível de experiência
//     (a intuição do ladrão do Ladino acelera e já revela o tipo)
//   - 30% vêm amaldiçoados: o efeito fica ao contrário e o anel gruda no dedo
public class ItemAnel extends Item implements Equipamento {
    private GemaDeAnel gema;

    public ItemAnel(Properties propriedades, GemaDeAnel gema) {
        super(propriedades.stacksTo(1));
        this.gema = gema;
    }

    public GemaDeAnel gema() {
        return gema;
    }

    public static ItemAnel daGema(GemaDeAnel gema) {
        return ModItens.ANEIS.get(gema.ordinal()).get();
    }

    // Anéis não pedem força
    @Override
    public int tier() {
        return 1;
    }

    @Override
    public Maldicao.Alvo alvoDasMaldicoes() {
        return Maldicao.Alvo.ANEL;
    }

    // As casinhas de anel são as da calça e da bota do Minecraft (o Shattered não tem essas peças)
    @Override
    public boolean canEquip(ItemStack pilha, EquipmentSlot lugar, LivingEntity quem) {
        return lugar == Aneis.PRIMEIRO || lugar == Aneis.SEGUNDO;
    }

    @Nullable
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack pilha) {
        return Aneis.PRIMEIRO;
    }

    // ---- Achados na dungeon ----

    // Igual ao Shattered: +0 (2/3), +1 (4/15), +2 (1/15); 30% amaldiçoados; vem sem identificar
    public static ItemStack sortearDoChao(ItemStack pilha, Random sorteio) {
        int nivel = 0;
        if (sorteio.nextInt(3) == 0) {
            nivel++;
            if (sorteio.nextInt(5) == 0) {
                nivel++;
            }
        }
        NivelDoItem.definirNivel(pilha, nivel);
        if (sorteio.nextFloat() < 0.3F) {
            pilha.set(ModComponentes.MALDICAO.get(), Maldicao.ANEL_AMALDICOADO.id());
        }
        pilha.set(ModComponentes.IDENTIFICADO.get(), false);
        pilha.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 1.0F);
        return pilha;
    }

    // ---- Pôr e tirar ----

    // Clique direito com o anel na mão: põe numa casinha de anel livre
    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return InteractionResultHolder.success(pilha);
        }
        for (EquipmentSlot lugar : new EquipmentSlot[] { Aneis.PRIMEIRO, Aneis.SEGUNDO }) {
            if (heroi.getItemBySlot(lugar).isEmpty()) {
                heroi.setItemSlot(lugar, pilha.copy());
                pilha.setCount(0);
                mundo.playSound(null, heroi.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
                Aneis.aoPor(heroi, heroi.getItemBySlot(lugar));
                return InteractionResultHolder.success(pilha);
            }
        }
        heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.anel.sem_lugar").withStyle(ChatFormatting.GRAY), true);
        return InteractionResultHolder.fail(pilha);
    }

    // ---- Aparência ----

    // "Anel de rubi" até descobrir o tipo; depois "Anel da pressa +1" (com o nível, se identificado)
    @Override
    public Component getName(ItemStack pilha) {
        TipoDeAnel conhecido = ConhecimentoDoCliente.anel(gema);
        Component nome = conhecido != null ? Component.translatable(conhecido.chaveDoNome()) : super.getName(pilha);
        return NivelDoItem.nome(pilha, nome);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        TipoDeAnel conhecido = ConhecimentoDoCliente.anel(gema);
        linhas.add(Component.translatable("dica.pixeldungeon.anel.por").withStyle(ChatFormatting.AQUA));
        if (conhecido != null) {
            boolean identificado = NivelDoItem.identificado(pilha);
            int bonus = identificado ? Aneis.bonus(pilha) : 1;
            Textos.adicionarQuebrado(linhas, "dica.pixeldungeon.anel." + conhecido.id(), ChatFormatting.LIGHT_PURPLE, Aneis.textoDoBonus(conhecido, bonus));
            if (!identificado) {
                linhas.add(Component.translatable("dica.pixeldungeon.anel.tipico").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        NivelDoItem.dicaDeIdentificacao(pilha, linhas);
        Textos.adicionarQuebrado(linhas, conhecido != null ? conhecido.chaveDaDescricao() : "item.pixeldungeon.anel.desconhecido.descricao",
                ChatFormatting.DARK_GRAY);
    }
}
