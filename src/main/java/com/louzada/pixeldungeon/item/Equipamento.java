package com.louzada.pixeldungeon.item;

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

    default int forcaNecessaria(ItemStack pilha) {
        return forcaNecessaria(NivelDoItem.nivel(pilha));
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
