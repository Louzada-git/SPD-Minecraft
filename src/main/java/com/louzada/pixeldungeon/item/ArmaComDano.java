package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;

import net.minecraft.util.RandomSource;

// Qualquer coisa que o herói usa pra bater com as regras do Shattered (armas de perto e o arco espiritual)
public interface ArmaComDano {
    // Dano sorteado de um golpe (surpresa: inimigo dormindo ou distraído)
    int sortearDano(RandomSource sorteio, DadosDoHeroi heroi, boolean surpresa);

    // Multiplicador de precisão (cai se faltar força)
    float precisaoCom(DadosDoHeroi heroi);
}
