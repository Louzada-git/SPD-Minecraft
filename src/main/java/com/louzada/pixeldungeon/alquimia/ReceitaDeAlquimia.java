package com.louzada.pixeldungeon.alquimia;

import net.minecraft.world.item.ItemStack;

// Uma receita do pote de alquimia (o "Recipe" do Shattered)
public interface ReceitaDeAlquimia {

    // Os ingredientes servem pra esta receita? (conta quantas casinhas estão ocupadas, igual ao Shattered)
    boolean serve(Ingredientes ingredientes);

    // Quanta energia alquímica gasta
    int custo(Ingredientes ingredientes);

    // O que aparece no lugar do resultado antes de misturar (sem gastar nada)
    ItemStack exemplo(Ingredientes ingredientes);

    // Mistura: gasta os ingredientes e devolve o que saiu
    ItemStack misturar(Ingredientes ingredientes);
}
