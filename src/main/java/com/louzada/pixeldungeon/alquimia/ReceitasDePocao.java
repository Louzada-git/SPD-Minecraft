package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.world.item.ItemStack;

// As receitas de poção do pote de alquimia
public class ReceitasDePocao {
    // Quantas poções de cura já saíram do pote nesta run (cada uma deixa a próxima mais difícil)
    public static final String CURAS_COZINHADAS = "curas_cozinhadas";

    // A poção de cada semente (o "SeedToPotion" do Shattered; a baga-podre, da poção de força, é de missão e o mod não tem)
    public static final Map<TipoDePlanta, TipoDePocao> POCAO_DA_SEMENTE = new EnumMap<>(TipoDePlanta.class);
    static {
        POCAO_DA_SEMENTE.put(TipoDePlanta.CEGUERVA, TipoDePocao.INVISIBILIDADE);
        POCAO_DA_SEMENTE.put(TipoDePlanta.MAGI_REAL, TipoDePocao.PUREZA);
        POCAO_DA_SEMENTE.put(TipoDePlanta.TERRAIZ, TipoDePocao.GAS_PARALISANTE);
        POCAO_DA_SEMENTE.put(TipoDePlanta.FOLHA_FANTASMA, TipoDePocao.VISAO_MENTAL);
        POCAO_DA_SEMENTE.put(TipoDePlanta.FLOR_DE_FOGO, TipoDePocao.CHAMA_LIQUIDA);
        POCAO_DA_SEMENTE.put(TipoDePlanta.MUDA_DE_GELO, TipoDePocao.GELO);
        POCAO_DA_SEMENTE.put(TipoDePlanta.SOFRIMUSGO, TipoDePocao.GAS_TOXICO);
        POCAO_DA_SEMENTE.put(TipoDePlanta.FLOR_ESTRELA, TipoDePocao.EXPERIENCIA);
        POCAO_DA_SEMENTE.put(TipoDePlanta.VINHA_TEMPESTUOSA, TipoDePocao.LEVITACAO);
        POCAO_DA_SEMENTE.put(TipoDePlanta.GRAMA_SOLAR, TipoDePocao.CURA);
        POCAO_DA_SEMENTE.put(TipoDePlanta.CARDO_LIGEIRO, TipoDePocao.PRESSA);
    }

    // 3 sementes: uma poção. Cada semente puxa pra poção dela; com sementes diferentes, às vezes sai uma poção
    // qualquer (2 tipos: 1 em 4; 3 tipos: 1 em 2). Com as 3 iguais, a poção já sai identificada.
    // Igual ao Shattered, cada poção de cura que sai do pote deixa a próxima mais rara (refaz o sorteio
    // com chance de 10% por cura já feita).
    public static class DeSementes implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            if (ingredientes.quantos() != 3) {
                return false;
            }
            for (ItemStack pilha : ingredientes.pilhas()) {
                if (!(pilha.getItem() instanceof ItemSemente semente) || !POCAO_DA_SEMENTE.containsKey(semente.planta())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 0;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ModItens.POCAO_ALEATORIA.get());
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            Random sorteio = new Random(ingredientes.jogador().getRandom().nextLong());
            List<TipoDePlanta> sementes = new ArrayList<>();
            List<TipoDePlanta> diferentes = new ArrayList<>();
            for (ItemStack pilha : ingredientes.pilhas()) {
                TipoDePlanta planta = ((ItemSemente) pilha.getItem()).planta();
                sementes.add(planta);
                if (!diferentes.contains(planta)) {
                    diferentes.add(planta);
                }
                pilha.shrink(1);
            }
            TipoDePocao tipo;
            if ((diferentes.size() == 2 && sorteio.nextInt(4) == 0) || (diferentes.size() == 3 && sorteio.nextInt(2) == 0)) {
                tipo = pocaoQualquer(sorteio);
            } else {
                tipo = POCAO_DA_SEMENTE.get(sementes.get(sorteio.nextInt(sementes.size())));
            }
            // (igual ao Shattered, o que fica identificado é a poção da semente, mesmo se a cura for sorteada de novo)
            if (diferentes.size() == 1) {
                Identificacao.identificar(ingredientes.jogador(), tipo);
            }
            int curas = ingredientes.run().quedas(CURAS_COZINHADAS);
            while (tipo == TipoDePocao.CURA && sorteio.nextInt(10) < curas) {
                tipo = pocaoQualquer(sorteio);
            }
            if (tipo == TipoDePocao.CURA) {
                ingredientes.run().contarQueda(CURAS_COZINHADAS);
            }
            return Identificacao.pocao(ingredientes.semente(), tipo);
        }
    }

    // 1 poção + 4 de energia: a poção exótica da mesma cor (o "PotionToExotic" do Shattered).
    // Ela fica conhecida se a poção comum já é.
    public static class ParaExotica implements ReceitaDeAlquimia {

        @Override
        public boolean serve(Ingredientes ingredientes) {
            return ingredientes.quantos() == 1 && ingredientes.get(0).getItem() instanceof ItemPocao;
        }

        @Override
        public int custo(Ingredientes ingredientes) {
            return 4;
        }

        @Override
        public ItemStack exemplo(Ingredientes ingredientes) {
            return new ItemStack(ItemPocaoExotica.daCor(((ItemPocao) ingredientes.get(0).getItem()).cor()));
        }

        @Override
        public ItemStack misturar(Ingredientes ingredientes) {
            ItemStack exotica = exemplo(ingredientes);
            ingredientes.get(0).shrink(1);
            return exotica;
        }
    }

    // Uma poção qualquer, com os pesos de quando ela cai no chão
    public static TipoDePocao pocaoQualquer(Random sorteio) {
        TipoDePocao[] tipos = TipoDePocao.values();
        float[] pesos = new float[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = tipos[i].peso();
        }
        return tipos[SaqueDoAndar.sortearPeso(sorteio, pesos)];
    }
}
