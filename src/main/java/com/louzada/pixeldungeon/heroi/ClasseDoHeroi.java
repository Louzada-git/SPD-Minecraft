package com.louzada.pixeldungeon.heroi;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// As 6 classes do Shattered. Cada uma começa com a sua arma.
// (Os itens únicos de cada classe, como o selo do Guerreiro e a capa do Ladino, vêm junto com os itens.)
public enum ClasseDoHeroi {
    GUERREIRO("guerreiro", ModItens.ESPADA_CURTA_DESGASTADA),
    MAGO("mago", ModItens.CAJADO_DO_MAGO),
    LADINO("ladino", ModItens.ADAGA),
    CACADORA("cacadora", ModItens.LUVAS_CRAVEJADAS),
    DUELISTA("duelista", ModItens.FLORETE),
    CLERIGO("clerigo", ModItens.MACA);

    private String id;
    private Supplier<? extends Item> armaInicial;

    ClasseDoHeroi(String id, Supplier<? extends Item> armaInicial) {
        this.id = id;
        this.armaInicial = armaInicial;
    }

    public String id() {
        return id;
    }

    public String chaveDoNome() {
        return "classe.pixeldungeon." + id;
    }

    public String chaveDaDescricao() {
        return "classe.pixeldungeon." + id + ".descricao";
    }

    public Item armaInicial() {
        return armaInicial.get();
    }

    // O que a classe recebe ao começar a run
    public List<ItemStack> kitInicial() {
        return List.of(new ItemStack(armaInicial.get()), new ItemStack(ModItens.RACAO.get()));
    }

    @Nullable
    public static ClasseDoHeroi peloId(String id) {
        for (ClasseDoHeroi classe : values()) {
            if (classe.id.equals(id)) {
                return classe;
            }
        }
        return null;
    }
}
