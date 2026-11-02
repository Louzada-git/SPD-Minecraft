package com.louzada.pixeldungeon.registro;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

    // Pano: usa o desenho da armadura de couro (marrom) por enquanto
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PANO = MATERIAIS.register("pano", () -> new ArmorMaterial(
            defesaZerada(), 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"), "", true)),
            0.0F, 0.0F));

    private static Map<ArmorItem.Type, Integer> defesaZerada() {
        Map<ArmorItem.Type, Integer> defesa = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type tipo : ArmorItem.Type.values()) {
            defesa.put(tipo, 0);
        }
        return defesa;
    }
}
