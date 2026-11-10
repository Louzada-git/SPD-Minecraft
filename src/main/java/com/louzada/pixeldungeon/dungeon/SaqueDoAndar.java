package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.PremioNoChao;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.ChaveDeFerro;
import com.louzada.pixeldungeon.item.Equipamento;
import com.louzada.pixeldungeon.item.GemaDeAnel;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModComponentes;
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
//   - cada item é ouro (10), poção (8), pergaminho (8), arma (2), armadura (2) ou semente (1), com esses pesos.
//     No Shattered também tem armaduras, varinhas, anéis... que o mod ainda não tem.
//   - a arma é de um tier sorteado pela região (no esgoto: 75% tier 2, 20% tier 3, 4% tier 4, 1% tier 5),
//     com nível +0 (75%), +1 (20%) ou +2 (5%), 30% amaldiçoada, e vem sem identificar
//   - cada item fica: num baú (4 em 20), dentro de um mímico (1 em 20, só a partir do andar 2) ou largado no chão
//   - todo andar também tem uma ração largada no chão
//   - em cada região caem 2 poções de força e 3 pergaminhos de melhoria, espalhados pelos andares
//   - cada porta trancada tem a chave de ferro dela largada numa sala comum
//   - as salas especiais (arsenal, tesouraria, biblioteca, jardim, cripta, armadilhas) e as secretas têm os prêmios delas
//   - a sala das armadilhas pede uma poção de levitação no chão do andar
//   - andares de chefe não têm nada
// Onde tem item, a grama alta vira grama baixa (senão ninguém acharia o item), igual ao Shattered.
// Os itens no chão nunca somem sozinhos.
public class SaqueDoAndar {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    // Onde o item fica (PLANTADA: a semente vira uma planta no chão)
    public enum Onde {
        CHAO, BAU, MIMICO, PLANTADA
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
    public static final String PETALAS = "petalas_da_rosa";

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
            itens.add(itemAleatorio(sorteio, andar, run));
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
        // A sala das armadilhas (igual ao Shattered): 2 em 3 vezes, o baú do fundo guarda um desses itens garantidos;
        // senão, uma arma ou armadura igual à do labirinto. E o andar ganha uma poção de levitação pra atravessar a sala.
        ItemStack premioDasArmadilhas = null;
        for (PremioNoChao premio : planta.premios()) {
            if (premio.premio() == Premio.PREMIO_DAS_ARMADILHAS && premioDasArmadilhas == null) {
                if (sorteio.nextInt(3) != 0 && !garantidos.isEmpty()) {
                    premioDasArmadilhas = garantidos.remove(sorteio.nextInt(garantidos.size()));
                } else {
                    premioDasArmadilhas = premioDoLabirinto(sorteio, andar);
                }
            }
        }
        for (int i = 0; i < planta.pocoesDeLevitacao(); i++) {
            garantidos.add(Identificacao.pocao(semente, TipoDePocao.LEVITACAO));
        }
        for (ItemStack item : garantidos) {
            if (lugares.isEmpty()) {
                break;
            }
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            saques.add(new Saque(lugar[0], lugar[1], List.of(item), Onde.CHAO));
        }

        // As pétalas da rosa seca: com a missão do fantasma completa e a rosa já achada, uma a cada 2 andares (igual ao Shattered)
        if (MissaoDoFantasma.completa(run) && run.quedas(chaveDoArtefato(ModItens.ROSA_SECA.get())) > 0) {
            int faltam = (int) Math.ceil(((andar / 2) - run.quedas(PETALAS)) / 3F);
            for (int i = 0; i < faltam && run.quedas(PETALAS) < 11 && !lugares.isEmpty(); i++) {
                int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
                saques.add(new Saque(lugar[0], lugar[1], List.of(new ItemStack(ModItens.PETALA_SECA.get())), Onde.CHAO));
                run.contarQueda(PETALAS);
            }
        }

        // As chaves de ferro das portas trancadas
        for (int i = 0; i < planta.chavesDeFerro() && !lugares.isEmpty(); i++) {
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            saques.add(new Saque(lugar[0], lugar[1], List.of(ChaveDeFerro.criar(run.numeroDaRun(), andar)), Onde.CHAO));
        }

        // Os prêmios das salas especiais (o ouro do tesouro secreto é dividido entre as pilhas dele)
        int pilhasDoTesouro = 0;
        for (PremioNoChao premio : planta.premios()) {
            if (premio.premio() == Premio.OURO_DO_TESOURO) {
                pilhasDoTesouro++;
            }
        }
        for (PremioNoChao premio : planta.premios()) {
            saques.add(saqueDoPremio(premio, sorteio, andar, run, pilhasDoTesouro, premioDasArmadilhas));
        }

        for (Saque saque : saques) {
            if (planta.grama(saque.x(), saque.y()) == PlantaDoAndar.GRAMA_ALTA) {
                planta.definirGrama(saque.x(), saque.y(), PlantaDoAndar.GRAMA);
            }
        }
        return saques;
    }

    // O item de verdade de cada prêmio das salas especiais
    private static Saque saqueDoPremio(PremioNoChao premio, Random sorteio, int andar, DadosDaRun run, int pilhasDoTesouro,
            @Nullable ItemStack premioDasArmadilhas) {
        long semente = run.semente();
        Onde onde = premio.noBau() ? Onde.BAU : Onde.CHAO;
        ItemStack item = switch (premio.premio()) {
            case ARMA -> armaAleatoria(sorteio, andar);
            case ARMADURA -> armaduraAleatoria(sorteio, andar);
            case OURO -> Ouro.sortear(sorteio, andar);
            case OURO_POUCO -> Ouro.pilha(5 + sorteio.nextInt(8));
            case PERGAMINHO -> pergaminhoAleatorio(sorteio, semente);
            case PERGAMINHO_DE_IDENTIFICAR_OU_REMOVER ->
                Identificacao.pergaminho(semente, sorteio.nextBoolean() ? TipoDePergaminho.IDENTIFICACAO : TipoDePergaminho.REMOVER_MALDICAO);
            case ARMADURA_AMALDICOADA -> armaduraDaCripta(sorteio, andar);
            case RACAO_DE_SUPRIMENTOS -> new ItemStack(ModItens.RACAO_DE_SUPRIMENTOS.get());
            case OURO_DO_TESOURO -> ouroDoTesouro(sorteio, andar, pilhasDoTesouro);
            case PREMIO_DO_LABIRINTO -> premioDoLabirinto(sorteio, andar);
            case PREMIO_DAS_ARMADILHAS -> premioDasArmadilhas != null ? premioDasArmadilhas : premioDoLabirinto(sorteio, andar);
            case ITEM_ALEATORIO -> itemAleatorio(sorteio, andar, run);
            case ARREMESSO -> arremessoAleatorio(sorteio, andar);
            case PLANTA_ALEATORIA -> sementeAleatoria(sorteio);
            case PLANTA_GRAMA_SOLAR -> new ItemStack(ItemSemente.daPlanta(TipoDePlanta.GRAMA_SOLAR));
            case PLANTA_ESTRELA -> new ItemStack(ItemSemente.daPlanta(TipoDePlanta.FLOR_ESTRELA));
        };
        if (premio.premio() == Premio.PLANTA_ALEATORIA || premio.premio() == Premio.PLANTA_GRAMA_SOLAR || premio.premio() == Premio.PLANTA_ESTRELA) {
            onde = Onde.PLANTADA;
        }
        // Igual ao Shattered: a partir do andar 2, cada baú de ouro da tesouraria tem 1 chance em 5 de ser um mímico
        if (onde == Onde.BAU && premio.premio() == Premio.OURO && andar > 1 && sorteio.nextInt(5) == 0) {
            onde = Onde.MIMICO;
        }
        return new Saque(premio.x(), premio.y(), List.of(item), onde);
    }

    // A armadura do túmulo da cripta (igual ao Shattered): de um tier da região seguinte, sempre amaldiçoada
    // e já se sabe disso. Se ela não tinha maldição, ganha +1 de nível junto com a maldição.
    private static ItemStack armaduraDaCripta(Random sorteio, int andar) {
        ItemStack armadura = armaduraAleatoria(sorteio, andar + 5);
        if (!NivelDoItem.amaldicoado(armadura) && armadura.getItem() instanceof Equipamento equipamento) {
            NivelDoItem.definirNivel(armadura, NivelDoItem.nivel(armadura) + 1);
            List<Maldicao> possiveis = Maldicao.doAlvo(equipamento.alvoDasMaldicoes());
            armadura.set(ModComponentes.MALDICAO.get(), possiveis.get(sorteio.nextInt(possiveis.size())).id());
            // A maldição toma o lugar do glifo
            NivelDoItem.definirGlifo(armadura, null);
        }
        armadura.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        return armadura;
    }

    // O tesouro secreto (igual ao Shattered): não importa quantas pilhas sejam, o total é umas 8 pilhas normais de ouro
    private static ItemStack ouroDoTesouro(Random sorteio, int andar, int pilhas) {
        float proporcao = 8F / Math.max(1, pilhas);
        return Ouro.pilha(Math.round(Ouro.quantidade(Ouro.sortear(sorteio, andar)) * proporcao));
    }

    // O baú do fim do labirinto secreto (igual ao Shattered): metade das vezes uma arma, metade uma armadura,
    // de um tier da região seguinte, nunca amaldiçoada (e já se sabe disso), e 1 em 3 vem com +1 a mais
    public static ItemStack premioDoLabirinto(Random sorteio, int andar) {
        ItemStack premio = sorteio.nextInt(2) == 0 ? armaAleatoria(sorteio, andar + 5) : armaduraAleatoria(sorteio, andar + 5);
        premio.remove(ModComponentes.MALDICAO.get());
        premio.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        if (sorteio.nextInt(3) == 0) {
            NivelDoItem.definirNivel(premio, NivelDoItem.nivel(premio) + 1);
        }
        return premio;
    }

    // Coloca os itens no andar já construído
    public static void colocar(ServerLevel mundo, BlockPos origem, int andar, List<Saque> saques) {
        int noChao = 0;
        int baus = 0;
        int mimicos = 0;
        for (Saque saque : saques) {
            if (saque.onde() == Onde.PLANTADA) {
                BlockPos lugar = new BlockPos(origem.getX() + saque.x() * T + 1, origem.getY() + 1, origem.getZ() + saque.y() * T + 1);
                for (ItemStack item : saque.itens()) {
                    if (item.getItem() instanceof ItemSemente semente) {
                        ItemSemente.plantar(mundo, lugar, semente.planta());
                    }
                }
            } else if (saque.onde() == Onde.CHAO) {
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

    // Ouro (10), poção (8), pergaminho (8), arma (2), armadura (2), semente (1), arma de arremesso (1), varinha (1) ou anel (1),
    // igual ao peso deles no Shattered
    // Os pesos de cada tipo de item, igual ao Shattered (a média dos dois "baralhos" dele):
    // ouro 10, poção 8, pergaminho 8, arma 2, armadura 1,5, semente 1, arremesso 1,5, varinha 1, anel 0,5, artefato 0,5
    public static ItemStack itemAleatorio(Random sorteio, int andar, DadosDaRun run) {
        long semente = run.semente();
        return switch (sortearPeso(sorteio, new float[] { 10, 8, 8, 2, 1.5F, 1, 1.5F, 1, 0.5F, 0.5F })) {
            case 0 -> Ouro.sortear(sorteio, andar);
            case 1 -> pocaoAleatoria(sorteio, semente);
            case 2 -> pergaminhoAleatorio(sorteio, semente);
            case 3 -> armaAleatoria(sorteio, andar);
            case 4 -> armaduraAleatoria(sorteio, andar);
            case 5 -> sementeAleatoria(sorteio);
            case 6 -> arremessoAleatorio(sorteio, andar);
            case 7 -> varinhaAleatoria(sorteio);
            case 8 -> anelAleatorio(sorteio);
            default -> artefatoAleatorio(sorteio, run);
        };
    }

    // Um artefato que ainda não apareceu nesta run (todos com a mesma chance); se já saíram todos, um anel, igual ao Shattered
    public static ItemStack artefatoAleatorio(Random sorteio, DadosDaRun run) {
        List<ArtefatoAchado> sobrando = new ArrayList<>();
        for (var registrado : ModItens.artefatos()) {
            ArtefatoAchado artefato = registrado.get();
            if (run.quedas(chaveDoArtefato(artefato)) == 0) {
                sobrando.add(artefato);
            }
        }
        if (sobrando.isEmpty()) {
            return anelAleatorio(sorteio);
        }
        ArtefatoAchado escolhido = sobrando.get(sorteio.nextInt(sobrando.size()));
        run.contarQueda(chaveDoArtefato(escolhido));
        return new ItemStack(escolhido);
    }

    public static String chaveDoArtefato(ArtefatoAchado artefato) {
        return "artefato_" + artefato.id();
    }

    // Um anel qualquer (todas as gemas têm a mesma chance), com nível e talvez amaldiçoado, sem identificar
    public static ItemStack anelAleatorio(Random sorteio) {
        GemaDeAnel[] gemas = GemaDeAnel.values();
        return ItemAnel.sortearDoChao(new ItemStack(ItemAnel.daGema(gemas[sorteio.nextInt(gemas.length)])), sorteio);
    }

    // Uma varinha qualquer (todas têm a mesma chance), com nível e talvez amaldiçoada, sem identificar
    public static ItemStack varinhaAleatoria(Random sorteio) {
        TipoDeVarinha[] tipos = TipoDeVarinha.values();
        return ItemVarinha.sortearDoChao(new ItemStack(ModItens.varinha(tipos[sorteio.nextInt(tipos.length)])), sorteio);
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

    // O prêmio extra do mímico, igual ao Shattered: ouro, arma, armadura, arma de arremesso ou anel
    public static ItemStack premioDoMimico(Random sorteio, int andar) {
        return switch (sorteio.nextInt(5)) {
            case 0 -> Ouro.sortear(sorteio, andar);
            case 1 -> armaAleatoria(sorteio, andar);
            case 2 -> arremessoAleatorio(sorteio, andar);
            case 3 -> anelAleatorio(sorteio);
            default -> armaduraAleatoria(sorteio, andar);
        };
    }

    // Uma arma do tier sorteado pela região do andar, com nível e talvez maldição, sem identificar
    public static ItemStack armaAleatoria(Random sorteio, int andar) {
        int regiao = Math.min(CHANCES_DOS_TIERS.length - 1, (andar - 1) / 5);
        int tier = 1 + sortearPeso(sorteio, CHANCES_DOS_TIERS[regiao]);
        return NivelDoItem.sortearDoChao(armaDoTier(sorteio, tier), sorteio, true);
    }

    // Uma armadura do tier sorteado pela região do andar (igual às armas), com nível e talvez maldição, sem identificar
    public static ItemStack armaduraAleatoria(Random sorteio, int andar) {
        int regiao = Math.min(CHANCES_DOS_TIERS.length - 1, (andar - 1) / 5);
        int tier = 1 + sortearPeso(sorteio, CHANCES_DOS_TIERS[regiao]);
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaduraDoShattered armadura && armadura.tier() == tier) {
                return NivelDoItem.sortearDoChao(new ItemStack(armadura), sorteio, true);
            }
        }
        return Ouro.sortear(sorteio, andar);
    }

    // Um conjunto de 3 armas de arremesso do tier sorteado pela região do andar (igual às armas),
    // com nível e talvez maldição, sem identificar
    public static ItemStack arremessoAleatorio(Random sorteio, int andar) {
        int regiao = Math.min(CHANCES_DOS_TIERS.length - 1, (andar - 1) / 5);
        int tier = 1 + sortearPeso(sorteio, CHANCES_DOS_TIERS[regiao]);
        List<Item> armas = new ArrayList<>();
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaDeArremesso arma && arma.tier() == tier) {
                armas.add(arma);
            }
        }
        if (armas.isEmpty()) {
            return Ouro.sortear(sorteio, andar);
        }
        ItemStack conjunto = new ItemStack(armas.get(sorteio.nextInt(armas.size())), ArmaDeArremesso.QUANTIDADE_PADRAO);
        return NivelDoItem.sortearDoChao(conjunto, sorteio, true);
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
