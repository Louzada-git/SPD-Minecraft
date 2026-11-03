package com.louzada.pixeldungeon.item;

import java.util.List;

import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// Armadura do Shattered: vai no peito e reduz o dano de cada golpe recebido.
//   defesa: sorteada de 0 até 2 x tier a cada golpe (pano = 0 a 2)
//   força necessária: 8 + 2 por tier; cada ponto que falta tira 2 de defesa
public class ArmaduraDoShattered extends ArmorItem {
    private int tier;
    private String chaveDaDescricao;

    public ArmaduraDoShattered(Holder<ArmorMaterial> material, Properties propriedades, String nome, int tier) {
        super(material, ArmorItem.Type.CHESTPLATE, propriedades.stacksTo(1));
        this.tier = tier;
        this.chaveDaDescricao = "item.pixeldungeon." + nome + ".descricao";
    }

    public int tier() {
        return tier;
    }

    public int defesaMinima() {
        return 0;
    }

    public int defesaMaxima() {
        return tier * 2;
    }

    public int forcaNecessaria() {
        return 8 + tier * 2;
    }

    public int sortearDefesa(RandomSource sorteio, int forcaDoHeroi) {
        int defesa = Sorteio.normal(sorteio, defesaMinima(), defesaMaxima());
        int forcaFaltando = forcaNecessaria() - forcaDoHeroi;
        if (forcaFaltando > 0) {
            defesa -= 2 * forcaFaltando;
        }
        return Math.max(0, defesa);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("dica.pixeldungeon.tier", tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dica.pixeldungeon.defesa", defesaMinima(), defesaMaxima()).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dica.pixeldungeon.forca", forcaNecessaria()).withStyle(ChatFormatting.GRAY));
        // Selo quebrado do Guerreiro: escudo de 3 + 2 x tier quando a vida cai pela metade
        if (pilha.getOrDefault(ModComponentes.SELO.get(), false)) {
            linhas.add(Component.translatable("dica.pixeldungeon.selo", 3 + 2 * tier).withStyle(ChatFormatting.GOLD));
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
