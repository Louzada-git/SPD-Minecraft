package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.world.item.ItemStack;

// O que as armas e as armaduras do Shattered têm em comum:
//   - um tier (1 a 5), que diz a força necessária (10, 12, 14, 16 ou 18)
//   - um nível (+0, +1...), que o pergaminho de melhoria aumenta e que tira um pouco da força necessária
//   - achadas na dungeon, vêm sem identificar e podem ter uma maldição do tipo delas (ver NivelDoItem)
//   - quem não tem a força necessária usa mal (ver PesoDoEquipamento)
public interface Equipamento {
    int tier();

    // Que maldições ela pode ter (de arma ou de armadura)
    Maldicao.Alvo alvoDasMaldicoes();

    default int forcaNecessaria(int nivel) {
        return NivelDoItem.forcaNecessaria(tier(), nivel);
    }

    // Com a poção da maestria, o item pede 2 de força a menos
    default int forcaNecessaria(ItemStack pilha) {
        return forcaNecessaria(NivelDoItem.nivel(pilha)) - maestria(pilha);
    }

    // Quanto a poção da maestria tira da força necessária (0 ou 2)
    default int maestria(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.MAESTRIA.get(), false) ? 2 : 0;
    }

    // Quanto de força falta pro herói usar direito (0 = tem força suficiente)
    default int forcaFaltando(ItemStack pilha, int forcaDoHeroi) {
        return Math.max(0, forcaNecessaria(pilha) - forcaDoHeroi);
    }

    // Igual ao Shattered: cada ponto de força que falta divide a precisão (arma) ou a esquiva (armadura) por 1,5
    static float divisaoPelaForca(int faltando) {
        return faltando > 0 ? (float) (1.0 / Math.pow(1.5, faltando)) : 1.0F;
    }
}
