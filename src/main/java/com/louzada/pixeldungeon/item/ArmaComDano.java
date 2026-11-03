package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

// Qualquer coisa que o herói usa pra bater com as regras do Shattered (armas de perto e o arco espiritual).
// A pilha é a arma de verdade (cada uma pode ter um nível diferente).
public interface ArmaComDano {
    // Dano sorteado de um golpe (surpresa: inimigo dormindo ou distraído)
    int sortearDano(ItemStack pilha, RandomSource sorteio, DadosDoHeroi heroi, boolean surpresa);

    // Multiplicador de precisão (cai se faltar força)
    float precisaoCom(ItemStack pilha, DadosDoHeroi heroi);
}
