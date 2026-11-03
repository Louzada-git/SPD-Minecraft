package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

// Equipamento pesado demais pra força do herói atrapalha, igual ao Shattered. Pra cada ponto de força que falta:
//   arma: ataca 1,2x mais devagar (e erra mais e defende menos, ver ArmaDoShattered)
//   armadura: anda 1,2x mais devagar (e desvia menos e defende menos, ver ArmaduraDoShattered)
public class PesoDoEquipamento {
    private static final ResourceLocation ARMA_PESADA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "arma_pesada");
    private static final ResourceLocation ARMADURA_PESADA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "armadura_pesada");

    public static void atualizar(ServerPlayer jogador, DadosDoHeroi dados) {
        ItemStack naMao = jogador.getMainHandItem();
        int faltaPraArma = naMao.getItem() instanceof ArmaDoShattered arma ? arma.forcaFaltando(naMao, dados.forca()) : 0;
        ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
        int faltaPraArmadura = peito.getItem() instanceof ArmaduraDoShattered armadura ? armadura.forcaFaltando(peito, dados.forca()) : 0;
        aplicar(jogador, Attributes.ATTACK_SPEED, ARMA_PESADA, faltaPraArma);
        aplicar(jogador, Attributes.MOVEMENT_SPEED, ARMADURA_PESADA, faltaPraArmadura);
    }

    // Divide o atributo por 1,2 pra cada ponto que falta (sem faltar nada, tira o modificador)
    private static void aplicar(ServerPlayer jogador, Holder<Attribute> atributo, ResourceLocation id, int faltando) {
        AttributeInstance instancia = jogador.getAttribute(atributo);
        if (instancia == null) {
            return;
        }
        if (faltando <= 0) {
            instancia.removeModifier(id);
            return;
        }
        double fator = 1.0 / Math.pow(1.2, faltando);
        instancia.addOrUpdateTransientModifier(new AttributeModifier(id, fator - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
