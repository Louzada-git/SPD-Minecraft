package com.louzada.pixeldungeon.registro;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Materiais de armadura: definem o desenho da armadura no corpo do jogador.
// A defesa do Minecraft fica zerada; quem calcula a defesa é o EventosDeCombate, com as regras do Shattered.
public class ModMateriais {
    public static final DeferredRegister<ArmorMaterial> MATERIAIS = DeferredRegister.create(Registries.ARMOR_MATERIAL, PixelDungeon.MODID);

    // Pano: o desenho da armadura de couro sem cor (fica clarinho)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PANO = material("pano", "leather", true, SoundEvents.ARMOR_EQUIP_LEATHER);
    // Couro: o couro marrom do Minecraft (a armadura de couro do mod está na tag "dyeable", que dá a cor)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> COURO = material("couro", "leather", true, SoundEvents.ARMOR_EQUIP_LEATHER);
    // Malha: a cota de malha do Minecraft
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MALHA = material("malha", "chainmail", false, SoundEvents.ARMOR_EQUIP_CHAIN);
    // Escamas: a armadura dourada (as escamas do Shattered são amareladas)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ESCAMAS = material("escamas", "gold", false, SoundEvents.ARMOR_EQUIP_GOLD);
    // Placas: a armadura de ferro
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLACAS = material("placas", "iron", false, SoundEvents.ARMOR_EQUIP_IRON);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String nome, String desenho, boolean tingivel, Holder<SoundEvent> som) {
        return MATERIAIS.register(nome, () -> new ArmorMaterial(defesaZerada(), 0, som, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace(desenho), "", tingivel)), 0.0F, 0.0F));
    }

    private static Map<ArmorItem.Type, Integer> defesaZerada() {
        Map<ArmorItem.Type, Integer> defesa = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type tipo : ArmorItem.Type.values()) {
            defesa.put(tipo, 0);
        }
        return defesa;
    }
}
