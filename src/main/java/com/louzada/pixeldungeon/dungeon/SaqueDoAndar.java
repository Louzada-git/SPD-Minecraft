package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.PlantaDoShattered;
import com.louzada.pixeldungeon.bloco.Tumulo;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.PremioNoChao;
import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.ChaveDeCristal;
import com.louzada.pixeldungeon.item.ChaveDeFerro;
import com.louzada.pixeldungeon.item.Equipamento;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.GemaDeAnel;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.item.TipoDePedra;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModBlocos;
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
//   - cada item é ouro, poção, pergaminho, arma, armadura, semente, arma de arremesso, varinha, anel, artefato ou pedra rúnica,
//     com os pesos do Shattered (ver itemAleatorio)
//   - a arma é de um tier sorteado pela região (no esgoto: 75% tier 2, 20% tier 3, 4% tier 4, 1% tier 5),
//     com nível +0 (75%), +1 (20%) ou +2 (5%), 30% amaldiçoada, e vem sem identificar
//   - cada item fica: num baú (4 em 20), dentro de um mímico (1 em 20, só a partir do andar 2) ou largado no chão
//   - os acessórios mudam um pouco isso (ver Acessorios): o dente de mímico, os cristais exóticos, a luneta trincada
//   - todo andar também tem uma ração largada no chão
//   - o andar extenso (ver TemaDoAndar) tem 2 itens a mais e mais uma comida
//   - em cada região caem 2 poções de força e 3 pergaminhos de melhoria, espalhados pelos andares;
//     e uma pedra de intuição a mais em um dos andares 1 a 4, e o catalisador mágico também (o laboratório guarda ele)
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
        CHAO, BAU, MIMICO, PLANTADA, BAU_DE_CRISTAL, OSSADA, TUMULO
    }

    // Marca do item amaldiçoado de uma ossada assombrada (quem pega solta um espectro)
    public static final String ASSOMBRADO = "pixeldungeon_assombrado";

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
    public static final String PEDRA_DE_INTUICAO = "pedra_de_intuicao";
    public static final String CATALISADOR = "catalisador_magico";

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
        boolean extenso = planta.tema() == TemaDoAndar.Tema.GRANDE;
        if (extenso) {
            quantidade += 2;
        }

        for (int i = 0; i < quantidade && !lugares.isEmpty(); i++) {
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            List<ItemStack> itens = new ArrayList<>();
            itens.add(itemAleatorio(sorteio, andar, run));
            Onde onde = Onde.CHAO;
            int dado = sorteio.nextInt(20);
            if (dado >= 1 && dado <= 4) {
                onde = Onde.BAU;
                // Igual ao Shattered: com o dente de mímico, cada +1x de mímicos vira 25% de chance de o baú ser um mímico
                float mimicos = Acessorios.multiplicadorDeMimicos(Acessorios.daRun(TipoDeAcessorio.DENTE_DE_MIMICO));
                if (mimicos > 1 && sorteio.nextFloat() < (mimicos - 1) / 4F) {
                    onde = Onde.MIMICO;
                }
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

        // A comida de todo andar (igual ao Shattered: ração 4 vezes em 5, pastel 1 em 5), e as poções de força e
        // os pergaminhos de melhoria da região (no chão)
        List<ItemStack> garantidos = new ArrayList<>();
        garantidos.add(new ItemStack(sorteio.nextInt(5) == 0 ? ModItens.PASTEL.get() : ModItens.RACAO.get()));
        if (extenso) {
            garantidos.add(comidaAleatoria(sorteio));
        }
        if (precisaDePocaoDeForca(run, andar, sorteio)) {
            run.contarQueda(POCOES_DE_FORCA);
            garantidos.add(Identificacao.pocao(semente, TipoDePocao.FORCA));
        }
        if (precisaDePergaminhoDeMelhoria(run, andar, sorteio)) {
            run.contarQueda(PERGAMINHOS_DE_MELHORIA);
            garantidos.add(Identificacao.pergaminho(semente, TipoDePergaminho.MELHORIA));
        }
        // Igual ao Shattered, uma pedra de intuição a mais nos andares 1 a 4 (cai no máximo até o 4)
        if (andar < 5 && run.quedas(PEDRA_DE_INTUICAO) == 0 && (andar == 4 || sorteio.nextInt(4 - andar) == 0)) {
            run.contarQueda(PEDRA_DE_INTUICAO);
            garantidos.add(new ItemStack(ModItens.pedra(TipoDePedra.INTUICAO)));
        }
        // O catalisador mágico (igual ao Shattered): um por run, nos andares 1 a 4 (no 4, se ainda não caiu, cai de certeza).
        // (sorteado à parte, pra não mudar o resto do andar)
        ItemStack catalisador = null;
        if (andar < 5 && run.quedas(CATALISADOR) == 0 && (andar == 4 || new Random(semente * 71 + andar * 13L).nextInt(4 - andar) == 0)) {
            run.contarQueda(CATALISADOR);
            catalisador = new ItemStack(ModItens.CATALISADOR_MAGICO.get());
            garantidos.add(catalisador);
        }
        // O laboratório (igual ao Shattered): o catalisador vira o primeiro prêmio do laboratório; senão, a poção de força do andar
        ItemStack catalisadorDoLaboratorio = null;
        for (PremioNoChao premio : planta.premios()) {
            if (premio.premio() == Premio.PREMIO_DO_LABORATORIO && catalisador != null && catalisadorDoLaboratorio == null) {
                catalisadorDoLaboratorio = catalisador;
                garantidos.remove(catalisador);
            }
        }
        ItemStack forcaDoLaboratorio = null;
        boolean primeiroDoLaboratorio = catalisadorDoLaboratorio != null;
        for (PremioNoChao premio : planta.premios()) {
            if (premio.premio() == Premio.PREMIO_DO_LABORATORIO && primeiroDoLaboratorio) {
                // (o primeiro prêmio já é o catalisador)
                primeiroDoLaboratorio = false;
                continue;
            }
            if (premio.premio() == Premio.PREMIO_DO_LABORATORIO && forcaDoLaboratorio == null) {
                for (ItemStack item : garantidos) {
                    if (Identificacao.tipoDaPocao(semente, item) == TipoDePocao.FORCA) {
                        forcaDoLaboratorio = item;
                    }
                }
                garantidos.remove(forcaDoLaboratorio);
            }
        }
        // A sala das armadilhas (igual ao Shattered): 2 em 3 vezes, o baú do fundo guarda um desses itens garantidos;
        // senão, uma arma ou armadura igual à do labirinto. E o andar ganha uma poção de levitação pra atravessar a sala.
        ItemStack premioDasArmadilhas = null;
        for (PremioNoChao premio : planta.premios()) {
            if (premio.premio() == Premio.PREMIO_DAS_ARMADILHAS && premioDasArmadilhas == null) {
                if (sorteio.nextInt(3) != 0 && !garantidos.isEmpty()) {
                    // (igual ao Shattered: se o catalisador ainda estiver pra cair, é ele)
                    premioDasArmadilhas = garantidos.contains(catalisador) ? catalisador : garantidos.get(sorteio.nextInt(garantidos.size()));
                    garantidos.remove(premioDasArmadilhas);
                } else {
                    premioDasArmadilhas = premioDoLabirinto(sorteio, andar);
                }
            }
        }
        // As salas que guardam um dos itens que o andar ia ter (o "findPrizeItem" do Shattered): o catalisador primeiro
        Map<PremioNoChao, ItemStack> guardados = new HashMap<>();
        for (PremioNoChao premio : planta.premios()) {
            ItemStack guardado = switch (premio.premio()) {
                case PREMIO_DO_DEPOSITO -> sorteio.nextInt(3) != 0 ? tirarGuardado(garantidos, catalisador, null) : null;
                case EQUIPAMENTO_DA_SALA -> sorteio.nextInt(3) == 0 ? tirarGuardado(garantidos, catalisador, null) : null;
                case EQUIPAMENTO_DA_SENTINELA -> sorteio.nextInt(2) == 0 ? tirarGuardado(garantidos, catalisador, null) : null;
                case PEDRA_DA_SALA -> tirarGuardado(garantidos, catalisador, ItemPedraRunica.class);
                case OURO_OU_CATALISADOR -> catalisador != null && garantidos.remove(catalisador) ? catalisador : null;
                // As salas comuns que guardam prêmio: o anel grande e o baú suspeito sempre, o estudo metade das vezes
                case PREMIO_DO_ANEL, PREMIO_SUSPEITO -> tirarGuardado(garantidos, catalisador, null);
                case PREMIO_DO_ESTUDO -> sorteio.nextInt(2) == 0 ? tirarGuardado(garantidos, catalisador, null) : null;
                default -> null;
            };
            if (guardado != null) {
                guardados.put(premio, guardado);
            }
        }
        // Os itens que as salas pedem pro andar (a poção pra atravessar a sala, as chaves de cristal...)
        for (PlantaDoAndar.ItemPedido pedido : PlantaDoAndar.ItemPedido.values()) {
            for (int i = 0; i < planta.pedidos(pedido); i++) {
                garantidos.add(itemPedido(pedido, run, andar));
            }
        }
        for (ItemStack item : garantidos) {
            if (lugares.isEmpty()) {
                break;
            }
            int[] lugar = lugares.remove(sorteio.nextInt(lugares.size()));
            saques.add(new Saque(lugar[0], lugar[1], List.of(item), Onde.CHAO));
        }

        // A luneta trincada (acessório): 37,5% a 150% de chance de itens a mais pelo andar (sorteados à parte)
        int luneta = Acessorios.daRun(TipoDeAcessorio.LUNETA_TRINCADA);
        if (luneta >= 0) {
            Random dado = new Random(semente * 89 + andar * 17L);
            int extras = (int) (dado.nextFloat() + Acessorios.chanceDaLuneta(luneta));
            for (int i = 0; i < extras && !lugares.isEmpty(); i++) {
                int[] lugar = lugares.remove(dado.nextInt(lugares.size()));
                saques.add(new Saque(lugar[0], lugar[1], List.of(itemAleatorio(dado, andar, run)), Onde.CHAO));
            }
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
        Set<TipoDePocao> pocoesDoLaboratorio = EnumSet.noneOf(TipoDePocao.class);
        // O cofre de cristal (igual ao Shattered): cada baú de cristal é de um tipo diferente (varinha, anel ou artefato)
        List<Integer> salasDeCristal = new ArrayList<>(List.of(0, 1, 2));
        Collections.shuffle(salasDeCristal, sorteio);
        // O caminho de cristal: as 3 poções e os 3 pergaminhos, das mais comuns às mais raras (só sorteia se o andar tiver a sala)
        List<ItemStack> caminho = planta.premios().stream().anyMatch(premio -> premio.premio() == Premio.CAMINHO_1)
                ? caminhoDeCristal(sorteio, semente) : List.of();
        for (PremioNoChao premio : planta.premios()) {
            // (o meio do anel só tem prêmio se o andar tinha algum item pra guardar)
            if (premio.premio() == Premio.PREMIO_DO_ANEL && guardados.get(premio) == null) {
                continue;
            }
            if (premio.premio() == Premio.PREMIO_DO_LABORATORIO && catalisadorDoLaboratorio != null) {
                saques.add(new Saque(premio.x(), premio.y(), List.of(catalisadorDoLaboratorio), Onde.CHAO));
                catalisadorDoLaboratorio = null;
                continue;
            }
            if (premio.premio() == Premio.PREMIO_DO_LABORATORIO && forcaDoLaboratorio != null) {
                saques.add(new Saque(premio.x(), premio.y(), List.of(forcaDoLaboratorio), Onde.CHAO));
                forcaDoLaboratorio = null;
                continue;
            }
            saques.add(saqueDoPremio(premio, sorteio, andar, run, pilhasDoTesouro, premioDasArmadilhas, pocoesDoLaboratorio, guardados.get(premio),
                    salasDeCristal, caminho));
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
            @Nullable ItemStack premioDasArmadilhas, Set<TipoDePocao> pocoesDoLaboratorio, @Nullable ItemStack guardado, List<Integer> salasDeCristal,
            List<ItemStack> caminho) {
        long semente = run.semente();
        Onde onde = switch (premio.recipiente()) {
            case BAU -> Onde.BAU;
            case BAU_DE_CRISTAL -> Onde.BAU_DE_CRISTAL;
            case OSSADA -> Onde.OSSADA;
            case TUMULO -> Onde.TUMULO;
            default -> Onde.CHAO;
        };
        ItemStack item = switch (premio.premio()) {
            case ARMA -> armaAleatoria(sorteio, andar);
            case ARMADURA -> armaduraAleatoria(sorteio, andar);
            case OURO -> Ouro.sortear(sorteio, andar);
            case OURO_POUCO -> Ouro.pilha(5 + sorteio.nextInt(8));
            case PERGAMINHO -> Acessorios.talvezExotico(pergaminhoAleatorio(sorteio, semente), sorteio, semente);
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
            case ARBUSTO_DE_FRUTA -> new ItemStack(ModItens.FRUTA_SEM_GRACA.get());
            case CARNE_GRELHADA -> new ItemStack(ModItens.CARNE_GRELHADA.get());
            case CRISTAIS_DO_LABORATORIO -> new ItemStack(ModItens.CRISTAL_DE_ENERGIA.get(), 5);
            case CRISTAIS_DO_LABORATORIO_SECRETO -> new ItemStack(ModItens.CRISTAL_DE_ENERGIA.get(), 3 + sorteio.nextInt(3));
            case PREMIO_DO_LABORATORIO -> sorteio.nextBoolean() ? Acessorios.talvezExotica(pocaoAleatoria(sorteio, semente), sorteio, semente)
                    : pedraAleatoria(sorteio);
            case PAGINA_DO_GUIA -> new ItemStack(ModItens.PAGINA_DO_GUIA.get());
            case BOMBA -> new ItemStack(ModItens.bomba(TipoDeBomba.COMUM), sorteio.nextInt(4) == 0 ? 2 : 1);
            case BOMBA_DUPLA -> new ItemStack(ModItens.bomba(TipoDeBomba.COMUM), 2);
            case POCAO_DO_LABORATORIO_SECRETO -> Identificacao.pocao(semente, pocaoDoLaboratorioSecreto(sorteio, pocoesDoLaboratorio));
            case PREMIO_DO_DEPOSITO, MIUDEZA_DA_OSSADA -> guardado != null ? guardado : miudeza(sorteio, andar, semente);
            case POTE_DE_MEL -> new ItemStack(ModItens.POTE_DE_MEL.get());
            case PEDRA_DA_SALA -> guardado != null ? guardado : pedraAleatoria(sorteio);
            case EQUIPAMENTO_DA_SALA, EQUIPAMENTO_DA_SENTINELA -> guardado != null ? guardado : equipamentoDaSala(sorteio, andar);
            case PREMIO_DE_CRISTAL -> {
                int categoria = salasDeCristal.remove(0);
                salasDeCristal.add(categoria);
                yield premioDeCristal(sorteio, run, categoria);
            }
            case PREMIO_ESCONDIDO -> premioDeCristal(sorteio, run, sorteio.nextInt(3));
            case POCAO_OU_PERGAMINHO -> sorteio.nextBoolean() ? Acessorios.talvezExotica(pocaoAleatoria(sorteio, semente), sorteio, semente)
                    : Acessorios.talvezExotico(pergaminhoAleatorio(sorteio, semente), sorteio, semente);
            case OURO_DOBRADO -> {
                ItemStack ouro = Ouro.sortear(sorteio, andar);
                yield Ouro.pilha(Ouro.quantidade(ouro) * 2);
            }
            case OURO_OU_CATALISADOR -> guardado != null ? guardado : Ouro.sortear(sorteio, andar);
            case PREMIO_DA_OSSADA -> premioDaOssada(sorteio, andar, run);
            case CHAVE_DE_CRISTAL -> ChaveDeCristal.criar(run.numeroDaRun(), andar);
            case CAMINHO_1, CAMINHO_2, CAMINHO_3, CAMINHO_4, CAMINHO_5, CAMINHO_6 ->
                caminho.get(premio.premio().ordinal() - Premio.CAMINHO_1.ordinal()).copy();
            case PREMIO_DO_ANEL -> guardado != null ? guardado : ItemStack.EMPTY;
            case PREMIO_DO_ESTUDO -> guardado != null ? guardado
                    : sorteio.nextBoolean() ? Acessorios.talvezExotica(pocaoAleatoria(sorteio, semente), sorteio, semente)
                            : Acessorios.talvezExotico(pergaminhoAleatorio(sorteio, semente), sorteio, semente);
            case PREMIO_SUSPEITO -> guardado != null ? guardado : Ouro.sortear(sorteio, andar);
        };
        if (premio.premio() == Premio.PLANTA_ALEATORIA || premio.premio() == Premio.PLANTA_GRAMA_SOLAR || premio.premio() == Premio.PLANTA_ESTRELA
                || premio.premio() == Premio.ARBUSTO_DE_FRUTA) {
            onde = Onde.PLANTADA;
        }
        // Igual ao Shattered: a partir do andar 2, cada baú de ouro da tesouraria tem 1 chance em 5 de ser um mímico
        // (com o dente de mímico, 1,5x a 3x mais)
        if (onde == Onde.BAU && premio.premio() == Premio.OURO && andar > 1) {
            float mimicos = Acessorios.multiplicadorDeMimicos(Acessorios.daRun(TipoDeAcessorio.DENTE_DE_MIMICO));
            if (mimicos == 1 ? sorteio.nextInt(5) == 0 : sorteio.nextFloat() < mimicos / 5F) {
                onde = Onde.MIMICO;
            }
        }
        // O baú suspeito (igual ao Shattered): 1 em 3 vezes é um mímico (com o dente de mímico, mais), que guarda um prêmio a mais
        if (premio.premio() == Premio.PREMIO_SUSPEITO) {
            float mimicos = Acessorios.multiplicadorDeMimicos(Acessorios.daRun(TipoDeAcessorio.DENTE_DE_MIMICO));
            if (sorteio.nextFloat() < mimicos / 3F) {
                return new Saque(premio.x(), premio.y(), List.of(item, premioDoMimico(sorteio, andar)), Onde.MIMICO);
            }
        }
        return new Saque(premio.x(), premio.y(), List.of(item), onde);
    }

    // ---- Os prêmios das salas especiais novas ----

    // Tira um dos itens que o andar ia ter (o catalisador primeiro); com "tipo", só um item desse tipo
    @Nullable
    private static ItemStack tirarGuardado(List<ItemStack> garantidos, @Nullable ItemStack catalisador, @Nullable Class<?> tipo) {
        if (catalisador != null && garantidos.remove(catalisador)) {
            return catalisador;
        }
        for (ItemStack item : garantidos) {
            if (tipo == null || tipo.isInstance(item.getItem())) {
                garantidos.remove(item);
                return item;
            }
        }
        return null;
    }

    // O item que uma sala pede pro andar
    private static ItemStack itemPedido(PlantaDoAndar.ItemPedido pedido, DadosDaRun run, int andar) {
        long semente = run.semente();
        return switch (pedido) {
            case LEVITACAO -> Identificacao.pocao(semente, TipoDePocao.LEVITACAO);
            case CHAMA_LIQUIDA -> Identificacao.pocao(semente, TipoDePocao.CHAMA_LIQUIDA);
            case INVISIBILIDADE -> Identificacao.pocao(semente, TipoDePocao.INVISIBILIDADE);
            case GELO -> Identificacao.pocao(semente, TipoDePocao.GELO);
            case PUREZA -> Identificacao.pocao(semente, TipoDePocao.PUREZA);
            case RAPIDEZ -> Identificacao.pocao(semente, TipoDePocao.PRESSA);
            case CHAVE_DE_CRISTAL -> ChaveDeCristal.criar(run.numeroDaRun(), andar);
        };
    }

    // Uma poção, um pergaminho, uma comida ou ouro (o depósito, a sala do fogo mágico, a ossada)
    public static ItemStack miudeza(Random sorteio, int andar, long semente) {
        return switch (sorteio.nextInt(4)) {
            case 0 -> Acessorios.talvezExotica(pocaoAleatoria(sorteio, semente), sorteio, semente);
            case 1 -> Acessorios.talvezExotico(pergaminhoAleatorio(sorteio, semente), sorteio, semente);
            case 2 -> comidaAleatoria(sorteio);
            default -> Ouro.sortear(sorteio, andar);
        };
    }

    // A comida do chão, igual ao Shattered: ração (4) ou pastel (1)
    public static ItemStack comidaAleatoria(Random sorteio) {
        return new ItemStack(sorteio.nextInt(5) == 0 ? ModItens.PASTEL.get() : ModItens.RACAO.get());
    }

    // O equipamento da piscina e da sentinela (igual ao Shattered): do tier da região seguinte (arma 2 em 5, arremesso 1 em 5,
    // armadura 2 em 5), nunca amaldiçoado (e já se sabe disso), e 1 em 3 vezes com +1
    public static ItemStack equipamentoDaSala(Random sorteio, int andar) {
        int dado = sorteio.nextInt(5);
        ItemStack premio = dado <= 1 ? armaAleatoria(sorteio, andar + 5) : dado == 2 ? arremessoAleatorio(sorteio, andar + 5)
                : armaduraAleatoria(sorteio, andar + 5);
        premio.remove(ModComponentes.MALDICAO.get());
        premio.set(ModComponentes.MALDICAO_CONHECIDA.get(), true);
        if (sorteio.nextInt(3) == 0) {
            NivelDoItem.definirNivel(premio, NivelDoItem.nivel(premio) + 1);
        }
        return premio;
    }

    // Varinha (0), anel (1) ou artefato (2)
    public static ItemStack premioDeCristal(Random sorteio, DadosDaRun run, int categoria) {
        return switch (categoria) {
            case 0 -> varinhaAleatoria(sorteio);
            case 1 -> anelAleatorio(sorteio);
            default -> artefatoAleatorio(sorteio, run);
        };
    }

    // O prêmio da ossada da sala do poço (igual ao Shattered): um anel, um artefato, ou uma arma, arma de arremesso ou armadura
    public static ItemStack premioDaOssada(Random sorteio, int andar, DadosDaRun run) {
        return switch (sorteio.nextInt(3)) {
            case 0 -> anelAleatorio(sorteio);
            case 1 -> artefatoAleatorio(sorteio, run);
            default -> switch (sorteio.nextInt(5)) {
                case 0, 1 -> armaAleatoria(sorteio, andar);
                case 2 -> arremessoAleatorio(sorteio, andar);
                default -> armaduraAleatoria(sorteio, andar);
            };
        };
    }

    // O caminho de cristal (igual ao Shattered): 3 poções e 3 pergaminhos sem repetir; uma das poções é a de experiência,
    // ou um dos pergaminhos é o de transmutação. Na ordem: poção e pergaminho mais comuns, os do meio, os mais raros.
    public static List<ItemStack> caminhoDeCristal(Random sorteio, long semente) {
        List<TipoDePocao> pocoes = new ArrayList<>();
        List<TipoDePergaminho> pergaminhos = new ArrayList<>();
        if (sorteio.nextBoolean()) {
            pocoes.add(pocaoDiferente(sorteio, semente, pocoes));
            pergaminhos.add(TipoDePergaminho.TRANSMUTACAO);
        } else {
            pocoes.add(TipoDePocao.EXPERIENCIA);
            pergaminhos.add(pergaminhoDiferente(sorteio, semente, pergaminhos));
        }
        pocoes.add(pocaoDiferente(sorteio, semente, pocoes));
        pergaminhos.add(pergaminhoDiferente(sorteio, semente, pergaminhos));
        pocoes.add(pocaoDiferente(sorteio, semente, pocoes));
        pergaminhos.add(pergaminhoDiferente(sorteio, semente, pergaminhos));
        pocoes.sort((a, b) -> Float.compare(b.peso(), a.peso()));
        pergaminhos.sort((a, b) -> Float.compare(b.peso(), a.peso()));
        List<ItemStack> itens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            itens.add(Identificacao.pocao(semente, pocoes.get(i)));
            itens.add(Identificacao.pergaminho(semente, pergaminhos.get(i)));
        }
        return itens;
    }

    private static TipoDePocao pocaoDiferente(Random sorteio, long semente, List<TipoDePocao> jaTem) {
        while (true) {
            TipoDePocao tipo = Identificacao.tipoDaPocao(semente, pocaoAleatoria(sorteio, semente));
            if (!jaTem.contains(tipo)) {
                return tipo;
            }
        }
    }

    private static TipoDePergaminho pergaminhoDiferente(Random sorteio, long semente, List<TipoDePergaminho> jaTem) {
        while (true) {
            TipoDePergaminho tipo = Identificacao.tipoDoPergaminho(semente, pergaminhoAleatorio(sorteio, semente));
            if (!jaTem.contains(tipo)) {
                return tipo;
            }
        }
    }

    // As chances de cada poção do laboratório secreto (igual ao Shattered: as mais fracas são mais comuns); não repete na mesma sala
    private static final TipoDePocao[] POCOES_DO_LABORATORIO = { TipoDePocao.CURA, TipoDePocao.VISAO_MENTAL, TipoDePocao.GELO,
            TipoDePocao.CHAMA_LIQUIDA, TipoDePocao.GAS_TOXICO, TipoDePocao.PRESSA, TipoDePocao.INVISIBILIDADE, TipoDePocao.LEVITACAO,
            TipoDePocao.GAS_PARALISANTE, TipoDePocao.PUREZA, TipoDePocao.EXPERIENCIA };
    private static final float[] CHANCES_DO_LABORATORIO = { 1, 2, 3, 3, 3, 4, 4, 4, 4, 4, 6 };

    private static TipoDePocao pocaoDoLaboratorioSecreto(Random sorteio, Set<TipoDePocao> jaSairam) {
        float[] chances = CHANCES_DO_LABORATORIO.clone();
        for (int i = 0; i < chances.length; i++) {
            if (jaSairam.contains(POCOES_DO_LABORATORIO[i])) {
                chances[i] = 0;
            }
        }
        TipoDePocao tipo = POCOES_DO_LABORATORIO[sortearPeso(sorteio, chances)];
        jaSairam.add(tipo);
        return tipo;
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
                    } else if (item.getItem() instanceof FrutaSemGraca && PlantaDoShattered.daPraPlantar(mundo, lugar)) {
                        // O arbusto de fruta-sem-graça
                        mundo.setBlock(lugar, ModBlocos.ARBUSTO_DE_FRUTA.get().defaultBlockState(), 3);
                    }
                }
            } else if (saque.onde() == Onde.OSSADA) {
                // A ossada: um crânio no canto e os itens no meio do quadradinho. Igual ao Shattered, se tiver um item amaldiçoado
                // ela é assombrada: a maldição dele fica à vista, e quem pega ele solta um espectro (ver Espectro)
                Baus.colocarOssada(mundo, new BlockPos(origem.getX() + saque.x() * T, origem.getY() + 1, origem.getZ() + saque.y() * T), mundo.getRandom());
                boolean assombrada = false;
                for (ItemStack item : saque.itens()) {
                    ItemEntity noChaoDaOssada = soltar(mundo, origem.getX() + saque.x() * T + T / 2.0, origem.getY() + 1,
                            origem.getZ() + saque.y() * T + T / 2.0, item);
                    if (!assombrada && NivelDoItem.amaldicoado(item)) {
                        assombrada = true;
                        item.set(ModComponentes.MALDICAO_CONHECIDA.get(), true);
                        noChaoDaOssada.setItem(item);
                        noChaoDaOssada.getPersistentData().putBoolean(ASSOMBRADO, true);
                    }
                }
                noChao++;
            } else if (saque.onde() == Onde.TUMULO) {
                // O túmulo: uma lápide no meio do quadradinho com os itens dentro (quem abre solta os espectros)
                BlockPos lugar = new BlockPos(origem.getX() + saque.x() * T + 1, origem.getY() + 1, origem.getZ() + saque.y() * T + 1);
                if (Tumulo.colocar(mundo, lugar, Direction.Plane.HORIZONTAL.getRandomDirection(mundo.getRandom()), saque.itens())) {
                    baus++;
                }
            } else if (saque.onde() == Onde.BAU_DE_CRISTAL) {
                BlockPos lugar = new BlockPos(origem.getX() + saque.x() * T + 1, origem.getY() + 1, origem.getZ() + saque.y() * T + 1);
                if (Baus.colocarDeCristal(mundo, lugar, Direction.Plane.HORIZONTAL.getRandomDirection(mundo.getRandom()), saque.itens())) {
                    baus++;
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

    // Os pesos de cada tipo de item, igual ao Shattered (a média dos dois "baralhos" dele):
    // ouro 10, poção 8, pergaminho 8, arma 2, armadura 1,5, semente 1, arremesso 1,5, varinha 1, anel 0,5, artefato 0,5, pedra rúnica 1
    public static ItemStack itemAleatorio(Random sorteio, int andar, DadosDaRun run) {
        long semente = run.semente();
        return switch (sortearPeso(sorteio, new float[] { 10, 8, 8, 2, 1.5F, 1, 1.5F, 1, 0.5F, 0.5F, 1 })) {
            case 0 -> Ouro.sortear(sorteio, andar);
            case 1 -> Acessorios.talvezExotica(pocaoAleatoria(sorteio, semente), sorteio, semente);
            case 2 -> Acessorios.talvezExotico(pergaminhoAleatorio(sorteio, semente), sorteio, semente);
            case 3 -> armaAleatoria(sorteio, andar);
            case 4 -> armaduraAleatoria(sorteio, andar);
            case 5 -> sementeAleatoria(sorteio);
            case 6 -> arremessoAleatorio(sorteio, andar);
            case 7 -> varinhaAleatoria(sorteio);
            case 8 -> anelAleatorio(sorteio);
            case 9 -> artefatoAleatorio(sorteio, run);
            default -> pedraAleatoria(sorteio);
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

    // Uma pedra rúnica pelo peso de cada tipo (a de encantamento e a de alteração têm peso 0: só saem da alquimia)
    public static ItemStack pedraAleatoria(Random sorteio) {
        TipoDePedra[] tipos = TipoDePedra.values();
        float[] pesos = new float[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = tipos[i].peso();
        }
        return new ItemStack(ModItens.pedra(tipos[sortearPeso(sorteio, pesos)]));
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
