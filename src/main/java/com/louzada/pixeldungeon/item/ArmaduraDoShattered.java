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

// Armadura do Shattered. Igual ao Shattered, o herói só veste uma (vai no peito) e ela reduz o dano de cada golpe:
//   defesa: sorteada de nível até tier x (2 + nível) a cada golpe (pano +0 = 0-2, placas +0 = 0-10)
//   força necessária: 10, 12, 14, 16 ou 18 (pelo tier), um pouco menos a cada nível. Cada ponto que falta tira 2 de defesa,
//     divide a esquiva por 1,5 e deixa o herói 1,2x mais lento (ver PesoDoEquipamento)
//   achada na dungeon, vem sem identificar e pode ser amaldiçoada (ver NivelDoItem e Maldicao); a amaldiçoada
//     não sai do corpo depois de vestida
public class ArmaduraDoShattered extends ArmorItem implements Equipamento {
    private int tier;
    private String chaveDaDescricao;

    public ArmaduraDoShattered(Holder<ArmorMaterial> material, Properties propriedades, String nome, int tier) {
        super(material, ArmorItem.Type.CHESTPLATE, propriedades.stacksTo(1));
        this.tier = tier;
        this.chaveDaDescricao = "item.pixeldungeon." + nome + ".descricao";
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Maldicao.Alvo alvoDasMaldicoes() {
        return Maldicao.Alvo.ARMADURA;
    }

    public int defesaMinima(int nivel) {
        return nivel;
    }

    public int defesaMaxima(int nivel) {
        return tier * (2 + nivel);
    }

    public int sortearDefesa(ItemStack pilha, RandomSource sorteio, int forcaDoHeroi) {
        int nivel = NivelDoItem.nivel(pilha);
        int defesa = Sorteio.normal(sorteio, defesaMinima(nivel), defesaMaxima(nivel)) - 2 * forcaFaltando(pilha, forcaDoHeroi);
        return Math.max(0, defesa);
    }

    // A esquiva de quem veste (cai se faltar força)
    public float fatorDeEsquiva(ItemStack pilha, int forcaDoHeroi) {
        return Equipamento.divisaoPelaForca(forcaFaltando(pilha, forcaDoHeroi));
    }

    // "Armadura de pano +1", "Armadura de malha de fedor"... só o que já foi descoberto
    @Override
    public Component getName(ItemStack pilha) {
        return NivelDoItem.nome(pilha, super.getName(pilha));
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        boolean conhecida = NivelDoItem.identificado(pilha);
        // Sem identificar, mostra os números de uma armadura +0 (os "típicos", igual ao Shattered)
        int nivel = conhecida ? NivelDoItem.nivel(pilha) : 0;
        linhas.add(Component.translatable("dica.pixeldungeon.tier", tier).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.defesa" : "dica.pixeldungeon.defesa_tipica", defesaMinima(nivel),
                defesaMaxima(nivel)).withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable(conhecida ? "dica.pixeldungeon.forca" : "dica.pixeldungeon.forca_tipica", forcaNecessaria(nivel))
                .withStyle(ChatFormatting.GRAY));
        NivelDoItem.dicaDeIdentificacao(pilha, linhas);
        // Selo quebrado do Guerreiro: escudo de 3 + 2 x tier quando a vida cai pela metade
        if (pilha.getOrDefault(ModComponentes.SELO.get(), false)) {
            linhas.add(Component.translatable("dica.pixeldungeon.selo", 3 + 2 * tier).withStyle(ChatFormatting.GOLD));
        }
        Textos.adicionarQuebrado(linhas, chaveDaDescricao, ChatFormatting.DARK_GRAY);
    }
}
