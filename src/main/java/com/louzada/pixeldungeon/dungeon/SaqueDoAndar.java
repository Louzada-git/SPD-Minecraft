package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Os itens espalhados por cada andar, igual ao Shattered (RegularLevel.createItems):
//   - 3 itens em 60% dos andares, 4 em 30% e 5 em 10%
//   - cada item é ouro (10), poção (8), pergaminho (8), arma (2) ou semente (1), com esses pesos.
//     No Shattered também tem armaduras, varinhas, anéis... que o mod ainda não tem.
//   - a arma é de um tier sorteado pela região (no esgoto: 75% tier 2, 20% tier 3, 4% tier 4, 1% tier 5),
//     com nível +0 (75%), +1 (20%) ou +2 (5%), 30% amaldiçoada, e vem sem identificar
//   - cada item fica: num baú (4 em 20), dentro de um mímico (1 em 20, só a partir do andar 2) ou largado no chão
//   - todo andar também tem uma ração largada no chão
//   - em cada região caem 2 poções de força e 3 pergaminhos de melhoria, espalhados pelos andares
//   - andares de chefe não têm nada
// Onde tem item, a grama alta vira grama baixa (senão ninguém acharia o item), igual ao Shattered.
// Os itens no chão nunca somem sozinhos.
public class SaqueDoAndar {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    // Onde o item fica
    public enum Onde {
        CHAO, BAU, MIMICO
    }

    // Um quadradinho com itens
    public record Saque(int x, int y, List<ItemStack> itens, Onde onde) {
    }

    // Chances de cada tier de arma (1 a 5) em cada região (esgoto, prisão, cavernas, cidade, salões)
    private static final float[][] CHANCES_DOS_TIERS = {
            { 0, 75, 20, 4, 1 },
            { 0, 25, 50, 20, 5 },
            { 0, 0, 40, 50, 10 },
            { 0, 0, 20, 40, 40 },
            { 0, 0, 0, 20, 80 }
    };

    public static final String POCOES_DE_FORCA = "pocao_de_forca";
    public static final String PERGAMINHOS_DE_MELHORIA = "pergaminho_de_melhoria";

    // Sorteia os itens do andar e já apara a grama alta onde eles vão ficar (antes de construir o andar)
    public static List<Saque> sortear(PlantaDoAndar planta, int andar, DadosDaRun run) {
        List<Saque> saques = new ArrayList<>();
        if (Andares.ehAndarDeChefe(andar)) {
            return saques;
        }
        long semente = run.semente();
        Random sorteio = new Random(semente * 53 + andar * 11L);
        List<int[]> lugares = PopulacaoDoAndar.lugaresLivres(planta);
        int quantidade = 3 + sortearPeso(sorteio, new float[] { 6, 3, 1 });

        for (int i = 0; i < quantidade && !lugares.isEmpty(); i++) {
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            List<ItemStack> itens = new ArrayList<>();
            itens.add(itemAleatorio(sorteio, andar, semente));
            Onde onde = Onde.CHAO;
            int dado = sorteio.nextInt(20);
            if (dado >= 1 && dado <= 4) {
                onde = Onde.BAU;
            } else if (dado == 5) {
                // No andar 1 não tem mímico: vira um baú comum
                onde = andar > 1 ? Onde.MIMICO : Onde.BAU;
            }
            if (onde == Onde.MIMICO) {
                // Quem mata o mímico ganha um prêmio a mais
                itens.add(premioDoMimico(sorteio, andar));
            }
            saques.add(new Saque(lugar[0], lugar[1], itens, onde));
        }

        // A ração de todo andar, e as poções de força e os pergaminhos de melhoria da região (no chão)
        List<ItemStack> garantidos = new ArrayList<>();
        garantidos.add(new ItemStack(ModItens.RACAO.get()));
        if (precisaDePocaoDeForca(run, andar, sorteio)) {
            run.contarQueda(POCOES_DE_FORCA);
            garantidos.add(Identificacao.pocao(semente, TipoDePocao.FORCA));
        }
        if (precisaDePergaminhoDeMelhoria(run, andar, sorteio)) {
            run.contarQueda(PERGAMINHOS_DE_MELHORIA);
            garantidos.add(Identificacao.pergaminho(semente, TipoDePergaminho.MELHORIA));
        }
        for (ItemStack item : garantidos) {
            if (lugares.isEmpty()) {
                break;
            }
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            saques.add(new Saque(lugar[0], lugar[1], List.of(item), Onde.CHAO));
        }

        for (Saque saque : saques) {
            if (planta.grama(saque.x(), saque.y()) == PlantaDoAndar.GRAMA_ALTA) {
                planta.definirGrama(saque.x(), saque.y(), PlantaDoAndar.GRAMA);
            }
        }
        return saques;
    }

    // Coloca os itens no andar já construído
    public static void colocar(ServerLevel mundo, BlockPos origem, int andar, List<Saque> saques) {
        int noChao = 0;
        int baus = 0;
        int mimicos = 0;
        for (Saque saque : saques) {
            if (saque.onde() == Onde.CHAO) {
                // Bem no meio do quadradinho
                for (ItemStack item : saque.itens()) {
                    soltar(mundo, origem.getX() + saque.x() * T + T / 2.0, origem.getY() + 1, origem.getZ() + saque.y() * T + T / 2.0, item);
                }
                noChao++;
            } else {
                // O baú fica num dos 4 blocos do meio do quadradinho (o outro é onde os inimigos nascem)
                BlockPos lugar = new BlockPos(origem.getX() + saque.x() * T + 1, origem.getY() + 1, origem.getZ() + saque.y() * T + 1);
                Direction frente = Direction.Plane.HORIZONTAL.getRandomDirection(mundo.getRandom());
                if (Baus.colocar(mundo, lugar, frente, saque.itens(), saque.onde() == Onde.MIMICO)) {
                    if (saque.onde() == Onde.MIMICO) {
                        mimicos++;
                    } else {
                        baus++;
                    }
                }
            }
        }
        PixelDungeon.LOGGER.info("Andar {}: {} itens ({} no chão, {} em baús, {} mímicos)", andar, saques.size(), noChao, baus, mimicos);
    }

    // ---- Sorteios ----

    // Igual ao Shattered: 2 poções de força por região. Elas caem de dois em dois andares
    // (uma nos andares 1-2 e outra nos 3-4), às vezes no primeiro, às vezes no segundo.
    private static boolean precisaDePocaoDeForca(DadosDaRun run, int andar, Random sorteio) {
        int faltamNaRegiao = 2 - (run.quedas(POCOES_DE_FORCA) - (andar / 5) * 2);
        if (faltamNaRegiao <= 0) {
            return false;
        }
        int andarNaRegiao = andar % 5;
        int deviamFaltar = 2 - andarNaRegiao / 2;
        if (andarNaRegiao % 2 == 1 && sorteio.nextInt(2) == 0) {
            deviamFaltar--;
        }
        return deviamFaltar < faltamNaRegiao;
    }

    // Igual ao Shattered: 3 pergaminhos de melhoria por região; a chance é (os que faltam) / (andares que faltam)
    private static boolean precisaDePergaminhoDeMelhoria(DadosDaRun run, int andar, Random sorteio) {
        int faltamNaRegiao = 3 - (run.quedas(PERGAMINHOS_DE_MELHORIA) - (andar / 5) * 3);
        if (faltamNaRegiao <= 0) {
            return false;
        }
        int andarNaRegiao = andar % 5;
        return sorteio.nextInt(5 - andarNaRegiao) < faltamNaRegiao;
    }

    // Ouro (10), poção (8), pergaminho (8), arma (2) ou semente (1), igual ao peso deles no Shattered
    public static ItemStack itemAleatorio(Random sorteio, int andar, long semente) {
        return switch (sortearPeso(sorteio, new float[] { 10, 8, 8, 2, 1 })) {
            case 0 -> Ouro.sortear(sorteio, andar);
            case 1 -> pocaoAleatoria(sorteio, semente);
            case 2 -> pergaminhoAleatorio(sorteio, semente);
            case 3 -> armaAleatoria(sorteio, andar);
            default -> sementeAleatoria(sorteio);
        };
    }

    // Uma poção pelo peso de cada tipo (a de força tem peso 0: só cai de propósito)
    public static ItemStack pocaoAleatoria(Random sorteio, long semente) {
        TipoDePocao[] tipos = TipoDePocao.values();
        float[] pesos = new float[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = tipos[i].peso();
        }
        return Identificacao.pocao(semente, tipos[sortearPeso(sorteio, pesos)]);
    }

    public static ItemStack pergaminhoAleatorio(Random sorteio, long semente) {
        TipoDePergaminho[] tipos = TipoDePergaminho.values();
        float[] pesos = new float[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = tipos[i].peso();
        }
        return Identificacao.pergaminho(semente, tipos[sortearPeso(sorteio, pesos)]);
    }

    public static ItemStack sementeAleatoria(Random sorteio) {
        TipoDePlanta[] tipos = TipoDePlanta.values();
        float[] pesos = new float[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = tipos[i].peso();
        }
        return new ItemStack(ItemSemente.daPlanta(tipos[sortearPeso(sorteio, pesos)]));
    }

    // O prêmio extra do mímico. No Shattered é ouro, arma, armadura, arma de arremesso ou anel; aqui, ouro ou arma.
    public static ItemStack premioDoMimico(Random sorteio, int andar) {
        return sorteio.nextBoolean() ? Ouro.sortear(sorteio, andar) : armaAleatoria(sorteio, andar);
    }

    // Uma arma do tier sorteado pela região do andar, com nível e talvez maldição, sem identificar
    public static ItemStack armaAleatoria(Random sorteio, int andar) {
        int regiao = Math.min(CHANCES_DOS_TIERS.length - 1, (andar - 1) / 5);
        int tier = 1 + sortearPeso(sorteio, CHANCES_DOS_TIERS[regiao]);
        return NivelDoItem.sortearDoChao(armaDoTier(sorteio, tier), sorteio, true);
    }

    // Qualquer arma do tier (menos o cajado do Mago, que só o Mago tem)
    public static ItemStack armaDoTier(Random sorteio, int tier) {
        List<Item> armas = new ArrayList<>();
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaDoShattered arma && arma.tier() == tier && !(arma instanceof CajadoDoMago)) {
                armas.add(arma);
            }
        }
        if (armas.isEmpty()) {
            return Ouro.sortear(sorteio, tier * 5);
        }
        return new ItemStack(armas.get(sorteio.nextInt(armas.size())));
    }

    // Sorteia uma posição da lista, com chance proporcional ao peso de cada uma
    public static int sortearPeso(Random sorteio, float[] pesos) {
        float total = 0;
        for (float peso : pesos) {
            total += peso;
        }
        float dado = sorteio.nextFloat() * total;
        for (int i = 0; i < pesos.length; i++) {
            dado -= pesos[i];
            if (dado < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }

    // ---- Ajudantes ----

    // Solta um item parado no chão, que nunca some sozinho
    public static ItemEntity soltar(ServerLevel mundo, double x, double y, double z, ItemStack item) {
        ItemEntity noChao = new ItemEntity(mundo, x, y, z, item, 0.0, 0.0, 0.0);
        noChao.setUnlimitedLifetime();
        mundo.addFreshEntity(noChao);
        return noChao;
    }

    // Em que andar está alguém (pra saber quanto ouro uma pilha tem, por exemplo)
    public static int andarDe(Entity entidade) {
        int andar = Andares.andarPelaAltura((int) Math.floor(entidade.getY()));
        return Math.max(1, Math.min(Andares.ULTIMO_ANDAR_LIBERADO, andar));
    }
}
