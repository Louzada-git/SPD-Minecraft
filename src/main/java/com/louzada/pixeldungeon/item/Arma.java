package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

// Qualquer coisa com que o herói ataca seguindo as regras do Shattered (as armas de perto e o arco espiritual).
// A pilha é a arma de verdade (cada uma pode ter um nível diferente). Quem usa isso é o combate (HeroiEmCombate).
public interface Arma {
    // Dano sorteado de um golpe (surpresa: inimigo dormindo, distraído, ou o herói invisível)
    int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean surpresa);

    // Multiplicador de precisão (1 = normal; cai se faltar força)
    float precisao(ItemStack pilha, DadosDoHeroi heroi);

    // Defesa que a arma dá a quem segura (o florete e a katana dão um pouco)
    default int sortearDefesa(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi) {
        return 0;
    }
}
