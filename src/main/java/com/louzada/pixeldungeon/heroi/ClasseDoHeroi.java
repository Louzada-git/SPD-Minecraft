package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// As 6 classes do Shattered. Cada uma começa com a sua arma, armadura de pano, uma ração e o seu item único:
//   Guerreiro: selo quebrado (afixado na armadura)     Mago: varinha de míssil mágico (dentro do cajado)
//   Ladino: capa das sombras                            Caçadora: arco espiritual
//   Duelista: habilidades das armas (tecla V)           Clérigo: tomo sagrado
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

    // Chave do texto "começa com..." do menu
    public String chaveDoKit() {
        return "classe.pixeldungeon." + id + ".kit";
    }

    // O que a classe recebe ao começar a run (a armadura já vai vestida)
    public List<ItemStack> kitInicial() {
        ItemStack armadura = new ItemStack(ModItens.ARMADURA_DE_PANO.get());
        List<ItemStack> kit = new ArrayList<>(List.of(new ItemStack(armaInicial.get()), armadura, new ItemStack(ModItens.RACAO.get())));
        switch (this) {
            case GUERREIRO -> armadura.set(ModComponentes.SELO.get(), true);
            case LADINO -> kit.add(new ItemStack(ModItens.CAPA_DAS_SOMBRAS.get()));
            case CACADORA -> kit.add(new ItemStack(ModItens.ARCO_ESPIRITUAL.get()));
            case CLERIGO -> kit.add(new ItemStack(ModItens.TOMO_SAGRADO.get()));
            default -> {
            }
        }
        return kit;
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
