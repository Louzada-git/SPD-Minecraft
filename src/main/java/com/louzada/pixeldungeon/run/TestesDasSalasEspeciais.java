package com.louzada.pixeldungeon.run;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.Barricada;
import com.louzada.pixeldungeon.bloco.FogoDeSacrificio;
import com.louzada.pixeldungeon.bloco.FundoDoPoco;
import com.louzada.pixeldungeon.bloco.PortaDeCristal;
import com.louzada.pixeldungeon.bloco.Respiradouro;
import com.louzada.pixeldungeon.dungeon.Abismo;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.InimigosDasSalas;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.InimigoDaSala;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.ItemPedido;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.PremioNoChao;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Recipiente;
import com.louzada.pixeldungeon.dungeon.Pocos;
import com.louzada.pixeldungeon.dungeon.Sacrificio;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.EstatuaAnimada;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.Piranha;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.entidade.Sentinela;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.ChaveDeCristal;
import com.louzada.pixeldungeon.item.EfeitosDePocao;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.PoteDeMel;
import com.louzada.pixeldungeon.item.PoteQuebrado;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

// Só no teste do GitHub: as salas especiais novas, os abismos, as piranhas e as abelhas.
//   1. a escolha das salas de cada andar (todas aparecem, as regras de "uma por andar", a sala do poço depois do piso frágil)
//   2. cada sala na planta (procurando uma semente que tenha ela) e os prêmios do cofre e do caminho de cristal
//   3. algumas dessas plantas construídas longe da dungeon: os blocos e os moradores de cada sala, e mexendo neles
//      (porta de cristal, barricada, fogo eterno, poço, respiradouro, fogo de sacrifício, piranha, estátua, sentinela)
//   4. o pote de mel e a abelha, e a queda no abismo
// O log mostra o resultado (o build.yml confere).
public class TestesDasSalasEspeciais {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int SEMENTES = 600;

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        testarEscolha();
        Map<ComodoEspecial.Tipo, long[]> ondeTem = procurarSalas();
        testarPlantas(ondeTem);
        testarSecretas();
        testarSaque(ondeTem, run);
        testarConstruidas(mundo, run, ondeTem);
        testarPoteDeMel(mundo, run);
        testarAbismo(mundo, run);
    }

    // ---- 1. Quais salas cada andar tem ----

    private static void testarEscolha() {
        Set<ComodoEspecial.Tipo> vistas = new HashSet<>();
        boolean umaDeCristal = true;
        boolean umaDePocao = true;
        boolean pocoDepoisDoPiso = true;
        boolean semPisoAntesDoChefe = true;
        for (long semente = 1; semente <= SEMENTES; semente++) {
            List<ComodoEspecial.Tipo> anterior = List.of();
            for (int andar = 1; andar <= 4; andar++) {
                List<ComodoEspecial.Tipo> doAndar = ComodoEspecial.doAndar(semente, andar);
                vistas.addAll(doAndar);
                umaDeCristal &= doAndar.stream().filter(tipo -> tipo.deCristal).count() <= 1;
                umaDePocao &= doAndar.stream().filter(tipo -> tipo.daPocao).count() <= 1;
                if (anterior.contains(ComodoEspecial.Tipo.PISO_FRAGIL) && !doAndar.contains(ComodoEspecial.Tipo.SALA_DO_POCO)) {
                    pocoDepoisDoPiso = false;
                }
                if (doAndar.contains(ComodoEspecial.Tipo.SALA_DO_POCO) && !anterior.contains(ComodoEspecial.Tipo.PISO_FRAGIL)) {
                    pocoDepoisDoPiso = false;
                }
                if (andar == 4 && doAndar.contains(ComodoEspecial.Tipo.PISO_FRAGIL)) {
                    semPisoAntesDoChefe = false;
                }
                anterior = doAndar;
            }
        }
        PixelDungeon.LOGGER.info("Teste da escolha das salas especiais: aparecem {} de {} tipos, uma de cristal por andar = {}, uma que dá poção por andar = {}, "
                + "sala do poço logo depois do piso frágil = {}, sem piso frágil antes do chefe = {}",
                vistas.size(), ComodoEspecial.Tipo.values().length, umaDeCristal, umaDePocao, pocoDepoisDoPiso, semPisoAntesDoChefe);
    }

    // A primeira semente e andar que têm cada sala
    private static Map<ComodoEspecial.Tipo, long[]> procurarSalas() {
        Map<ComodoEspecial.Tipo, long[]> ondeTem = new EnumMap<>(ComodoEspecial.Tipo.class);
        for (long semente = 1; semente <= SEMENTES && ondeTem.size() < ComodoEspecial.Tipo.values().length; semente++) {
            for (int andar = 1; andar <= 4; andar++) {
                for (ComodoEspecial.Tipo tipo : ComodoEspecial.doAndar(semente, andar)) {
                    ondeTem.putIfAbsent(tipo, new long[] { semente, andar });
                }
            }
        }
        return ondeTem;
    }

    private static PlantaDoAndar planta(Map<ComodoEspecial.Tipo, long[]> ondeTem, ComodoEspecial.Tipo tipo) {
        long[] onde = ondeTem.get(tipo);
        return onde == null ? null : GeradorDeAndar.gerar(onde[0], (int) onde[1]);
    }

    // ---- 2. Cada sala na planta ----

    private static void testarPlantas(Map<ComodoEspecial.Tipo, long[]> ondeTem) {
        Map<ComodoEspecial.Tipo, Boolean> certas = new EnumMap<>(ComodoEspecial.Tipo.class);
        for (ComodoEspecial.Tipo tipo : ComodoEspecial.Tipo.values()) {
            PlantaDoAndar planta = planta(ondeTem, tipo);
            certas.put(tipo, planta != null && planta.temSaida() && conferir(tipo, planta, ondeTem));
        }
        PixelDungeon.LOGGER.info("Teste das salas especiais na planta: piso frágil = {}, piscina = {}, sentinela = {}, estátua = {}, cofre de cristal = {}, "
                + "escolha de cristal = {}, sacrifício = {}, pedras = {}, depósito = {}, poço mágico = {}, gás tóxico = {}, fogo mágico = {}, "
                + "caminho de cristal = {}, sala do poço = {}, as antigas = {}",
                certas.get(ComodoEspecial.Tipo.PISO_FRAGIL), certas.get(ComodoEspecial.Tipo.PISCINA), certas.get(ComodoEspecial.Tipo.SALA_DA_SENTINELA),
                certas.get(ComodoEspecial.Tipo.SALA_DA_ESTATUA), certas.get(ComodoEspecial.Tipo.COFRE_DE_CRISTAL),
                certas.get(ComodoEspecial.Tipo.ESCOLHA_DE_CRISTAL), certas.get(ComodoEspecial.Tipo.SALA_DO_SACRIFICIO),
                certas.get(ComodoEspecial.Tipo.SALA_DAS_PEDRAS), certas.get(ComodoEspecial.Tipo.DEPOSITO), certas.get(ComodoEspecial.Tipo.POCO_MAGICO),
                certas.get(ComodoEspecial.Tipo.SALA_DE_GAS_TOXICO), certas.get(ComodoEspecial.Tipo.SALA_DO_FOGO_MAGICO),
                certas.get(ComodoEspecial.Tipo.CAMINHO_DE_CRISTAL), certas.get(ComodoEspecial.Tipo.SALA_DO_POCO),
                certas.get(ComodoEspecial.Tipo.ARSENAL) && certas.get(ComodoEspecial.Tipo.CRIPTA) && certas.get(ComodoEspecial.Tipo.JARDIM)
                        && certas.get(ComodoEspecial.Tipo.BIBLIOTECA) && certas.get(ComodoEspecial.Tipo.TESOURARIA)
                        && certas.get(ComodoEspecial.Tipo.ARMADILHAS) && certas.get(ComodoEspecial.Tipo.LABORATORIO));
        for (Map.Entry<ComodoEspecial.Tipo, Boolean> certa : certas.entrySet()) {
            if (!certa.getValue()) {
                PixelDungeon.LOGGER.info("Teste das salas especiais na planta: {} errada (semente e andar {})", certa.getKey(),
                        ondeTem.containsKey(certa.getKey()) ? ondeTem.get(certa.getKey())[0] + "/" + ondeTem.get(certa.getKey())[1] : "nenhum");
            }
        }
    }

    private static boolean conferir(ComodoEspecial.Tipo tipo, PlantaDoAndar planta, Map<ComodoEspecial.Tipo, long[]> ondeTem) {
        return switch (tipo) {
            case PISO_FRAGIL -> {
                long[] onde = ondeTem.get(tipo);
                PlantaDoAndar deBaixo = GeradorDeAndar.gerar(onde[0], (int) onde[1] + 1);
                yield planta.pisoFragil() != null && tiles(planta, PlantaDoAndar.ABISMO) > 0 && deBaixo.salaDoPoco() != null;
            }
            case PISCINA -> inimigosNasEspeciais(planta, InimigoDaSala.PIRANHA) == 3 && premios(planta, Premio.EQUIPAMENTO_DA_SALA, Recipiente.BAU) == 1
                    && tiles(planta, PlantaDoAndar.PEDESTAL) >= 1 && planta.pedidos(ItemPedido.INVISIBILIDADE) >= 1 && tiles(planta, PlantaDoAndar.AGUA) >= 9;
            case SALA_DA_SENTINELA -> inimigos(planta, InimigoDaSala.SENTINELA) == 1
                    && planta.inimigos().stream().allMatch(inimigo -> inimigo.tipo() != InimigoDaSala.SENTINELA || (inimigo.sala() != null && inimigo.extra() > 0))
                    && premios(planta, Premio.EQUIPAMENTO_DA_SENTINELA, Recipiente.BAU) == 1 && planta.pedidos(ItemPedido.RAPIDEZ) >= 1
                    && tiles(planta, PlantaDoAndar.ESTATUA) >= 2;
            case SALA_DA_ESTATUA -> inimigos(planta, InimigoDaSala.ESTATUA_ANIMADA) == 1 && tiles(planta, PlantaDoAndar.ESTATUA) >= 3 && planta.chavesDeFerro() >= 1;
            case COFRE_DE_CRISTAL -> premios(planta, Premio.PREMIO_DE_CRISTAL, Recipiente.BAU_DE_CRISTAL) == 2 && planta.pedidos(ItemPedido.CHAVE_DE_CRISTAL) == 1
                    && tiles(planta, PlantaDoAndar.PEDESTAL) >= 2;
            case ESCOLHA_DE_CRISTAL -> tiles(planta, PlantaDoAndar.PORTA_DE_CRISTAL) == 2 && premios(planta, Premio.PREMIO_ESCONDIDO, Recipiente.BAU) == 1
                    && premios(planta, Premio.POCAO_OU_PERGAMINHO, Recipiente.CHAO) >= 3 && premios(planta, Premio.POCAO_OU_PERGAMINHO, Recipiente.CHAO) <= 4
                    && planta.pedidos(ItemPedido.CHAVE_DE_CRISTAL) == 1;
            case SALA_DO_SACRIFICIO -> planta.fogoDeSacrificio() != null && tiles(planta, PlantaDoAndar.BRASAS) >= 8 && tiles(planta, PlantaDoAndar.ABISMO) > 0;
            case SALA_DAS_PEDRAS -> tiles(planta, PlantaDoAndar.ABISMO) > 0 && premios(planta, Premio.PEDRA_DA_SALA, Recipiente.CHAO) >= 2
                    && premios(planta, Premio.PEDRA_DA_SALA, Recipiente.CHAO) <= 3;
            case DEPOSITO -> tiles(planta, PlantaDoAndar.BARRICADA) == 1 && planta.pedidos(ItemPedido.CHAMA_LIQUIDA) >= 1
                    && entre(premios(planta, Premio.PREMIO_DO_DEPOSITO, Recipiente.CHAO) + premios(planta, Premio.POTE_DE_MEL, Recipiente.CHAO), 3, 4);
            case POCO_MAGICO -> planta.pocos().size() == 1 && tiles(planta, PlantaDoAndar.POCO) == 1;
            case SALA_DE_GAS_TOXICO -> tiles(planta, PlantaDoAndar.RESPIRADOURO) >= 3 && premios(planta, Premio.OURO_DOBRADO, Recipiente.OSSADA) == 1
                    && premios(planta, Premio.OURO_OU_CATALISADOR, Recipiente.BAU) == 2 && planta.pedidos(ItemPedido.PUREZA) >= 1;
            case SALA_DO_FOGO_MAGICO -> planta.fogoEterno().size() >= 5 && planta.pedidos(ItemPedido.GELO) >= 1
                    && entre(premios(planta, Premio.PREMIO_DO_DEPOSITO, Recipiente.CHAO) + premios(planta, Premio.POTE_DE_MEL, Recipiente.CHAO), 3, 4);
            case CAMINHO_DE_CRISTAL -> {
                boolean todos = true;
                for (int i = 0; i < 6; i++) {
                    todos &= premios(planta, Premio.values()[Premio.CAMINHO_1.ordinal() + i], Recipiente.CHAO) == 1;
                }
                yield todos && tiles(planta, PlantaDoAndar.PORTA_DE_CRISTAL) == 6 && planta.pedidos(ItemPedido.CHAVE_DE_CRISTAL) == 3;
            }
            case SALA_DO_POCO -> planta.salaDoPoco() != null && tiles(planta, PlantaDoAndar.POCO_VAZIO) == 1
                    && premios(planta, Premio.PREMIO_DA_OSSADA, Recipiente.OSSADA) == 1 && premios(planta, Premio.CHAVE_DE_CRISTAL, Recipiente.CHAO) == 1
                    && tiles(planta, PlantaDoAndar.PORTA_DE_CRISTAL) >= 1;
            default -> true;
        };
    }

    // ---- As salas secretas novas: o poço secreto e o esconderijo do mel ----

    private static void testarSecretas() {
        boolean poco = false;
        boolean mel = false;
        boolean achouPoco = false;
        boolean achouMel = false;
        for (long semente = 1; semente <= SEMENTES && (!achouPoco || !achouMel); semente++) {
            for (int andar = 2; andar <= 4; andar++) {
                List<ComodoSecreto.Tipo> secretas = ComodoSecreto.secretasDoAndar(semente, andar);
                if (!achouPoco && secretas.contains(ComodoSecreto.Tipo.POCO)) {
                    achouPoco = true;
                    PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                    poco = !planta.pocos().isEmpty() && tiles(planta, PlantaDoAndar.ABISMO) >= 8;
                }
                if (!achouMel && secretas.contains(ComodoSecreto.Tipo.MEL)) {
                    achouMel = true;
                    PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                    mel = inimigos(planta, InimigoDaSala.ABELHA) == 1 && premios(planta, Premio.POTE_DE_MEL, Recipiente.CHAO) >= 1
                            && premios(planta, Premio.BOMBA, Recipiente.CHAO) >= 1;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das salas secretas novas: poço secreto = {}, esconderijo do mel = {}", poco, mel);
    }

    // ---- O saque do cofre e do caminho de cristal ----

    private static void testarSaque(Map<ComodoEspecial.Tipo, long[]> ondeTem, DadosDaRun run) {
        boolean cofreDeTiposDiferentes = false;
        boolean cofreComChave = false;
        PlantaDoAndar cofre = planta(ondeTem, ComodoEspecial.Tipo.COFRE_DE_CRISTAL);
        if (cofre != null) {
            List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(cofre, (int) ondeTem.get(ComodoEspecial.Tipo.COFRE_DE_CRISTAL)[1], run);
            Set<Integer> categorias = new HashSet<>();
            int baus = 0;
            for (SaqueDoAndar.Saque saque : saques) {
                for (ItemStack item : saque.itens()) {
                    if (saque.onde() == SaqueDoAndar.Onde.BAU_DE_CRISTAL) {
                        baus++;
                        categorias.add(item.getItem() instanceof ItemAnel ? 1 : item.getItem() instanceof ArtefatoAchado ? 2 : 0);
                    }
                    cofreComChave |= item.getItem() instanceof ChaveDeCristal;
                }
            }
            cofreDeTiposDiferentes = baus == 2 && categorias.size() == 2;
        }

        boolean caminhoCerto = false;
        PlantaDoAndar caminho = planta(ondeTem, ComodoEspecial.Tipo.CAMINHO_DE_CRISTAL);
        if (caminho != null) {
            List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(caminho, (int) ondeTem.get(ComodoEspecial.Tipo.CAMINHO_DE_CRISTAL)[1], run);
            Set<String> pocoes = new HashSet<>();
            Set<String> pergaminhos = new HashSet<>();
            int chaves = 0;
            for (PremioNoChao premio : caminho.premios()) {
                if (premio.premio().ordinal() < Premio.CAMINHO_1.ordinal() || premio.premio().ordinal() > Premio.CAMINHO_6.ordinal()) {
                    continue;
                }
                for (SaqueDoAndar.Saque saque : saques) {
                    if (saque.x() == premio.x() && saque.y() == premio.y()) {
                        for (ItemStack item : saque.itens()) {
                            if (item.getItem() instanceof ItemPocao || item.getItem() instanceof ItemPocaoExotica) {
                                pocoes.add(String.valueOf(Identificacao.tipoDaPocao(run.semente(), item)));
                            } else if (item.getItem() instanceof ItemPergaminho || item.getItem() instanceof ItemPergaminhoExotico) {
                                pergaminhos.add(String.valueOf(Identificacao.tipoDoPergaminho(run.semente(), item)));
                            }
                        }
                    }
                }
            }
            for (SaqueDoAndar.Saque saque : saques) {
                for (ItemStack item : saque.itens()) {
                    if (item.getItem() instanceof ChaveDeCristal) {
                        chaves += item.getCount();
                    }
                }
            }
            caminhoCerto = pocoes.size() == 3 && pergaminhos.size() == 3 && chaves == 3;
        }
        PixelDungeon.LOGGER.info("Teste do saque das salas de cristal: cofre com 2 tipos diferentes = {}, chave de cristal no andar = {}, "
                + "caminho com 3 poções e 3 pergaminhos diferentes e 3 chaves = {}", cofreDeTiposDiferentes, cofreComChave, caminhoCerto);
    }

    // ---- 3. As salas construídas ----

    private static void testarConstruidas(ServerLevel mundo, DadosDaRun run, Map<ComodoEspecial.Tipo, long[]> ondeTem) {
        ComodoEspecial.Tipo[] tipos = { ComodoEspecial.Tipo.SALA_DAS_PEDRAS, ComodoEspecial.Tipo.ESCOLHA_DE_CRISTAL, ComodoEspecial.Tipo.DEPOSITO,
                ComodoEspecial.Tipo.SALA_DO_FOGO_MAGICO, ComodoEspecial.Tipo.SALA_DO_SACRIFICIO, ComodoEspecial.Tipo.SALA_DE_GAS_TOXICO,
                ComodoEspecial.Tipo.POCO_MAGICO, ComodoEspecial.Tipo.PISCINA, ComodoEspecial.Tipo.SALA_DA_ESTATUA, ComodoEspecial.Tipo.SALA_DA_SENTINELA };
        boolean blocosCertos = true;
        int construidas = 0;
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        Map<ComodoEspecial.Tipo, Construida> feitas = new EnumMap<>(ComodoEspecial.Tipo.class);
        for (int i = 0; i < tipos.length; i++) {
            long[] onde = ondeTem.get(tipos[i]);
            if (onde == null) {
                blocosCertos = false;
                continue;
            }
            PlantaDoAndar planta = GeradorDeAndar.gerar(onde[0], (int) onde[1]);
            BlockPos origem = new BlockPos(run.numeroDaRun() * Andares.DISTANCIA_ENTRE_RUNS + 500 + (i % 5) * 300, 140, 1500 + (i / 5) * 300);
            ConstrutorDeAndar.construir(mundo, planta, origem, onde[0]);
            InimigosDasSalas.criar(mundo, planta, origem, (int) onde[1], onde[0]);
            construidas++;
            if (!blocosIguaisAPlanta(mundo, planta, origem)) {
                blocosCertos = false;
                PixelDungeon.LOGGER.info("Teste das salas construídas: blocos errados na sala {}", tipos[i]);
            }
            feitas.put(tipos[i], new Construida(planta, origem));
        }
        PixelDungeon.LOGGER.info("Teste das salas construídas: {} construídas, blocos iguais à planta = {}", construidas, blocosCertos);

        testarPortaDeCristal(mundo, run, heroi, feitas.get(ComodoEspecial.Tipo.ESCOLHA_DE_CRISTAL));
        testarBarricadaEFogo(mundo, feitas.get(ComodoEspecial.Tipo.DEPOSITO), feitas.get(ComodoEspecial.Tipo.SALA_DO_FOGO_MAGICO));
        testarPocoERespiradouro(mundo, heroi, feitas.get(ComodoEspecial.Tipo.POCO_MAGICO), feitas.get(ComodoEspecial.Tipo.SALA_DE_GAS_TOXICO));
        testarSacrificio(mundo, run, feitas.get(ComodoEspecial.Tipo.SALA_DO_SACRIFICIO));
        testarMoradores(mundo, heroi, feitas.get(ComodoEspecial.Tipo.PISCINA), feitas.get(ComodoEspecial.Tipo.SALA_DA_ESTATUA),
                feitas.get(ComodoEspecial.Tipo.SALA_DA_SENTINELA));
    }

    private record Construida(PlantaDoAndar planta, BlockPos origem) {
        BlockPos bloco(int qx, int qy, int dx, int dy, int dz) {
            return new BlockPos(origem.getX() + qx * T + dx, origem.getY() + dy, origem.getZ() + qy * T + dz);
        }

        AABB caixa() {
            int tamanho = Andares.TAMANHO_DA_PLANTA * T;
            return new AABB(origem.getX(), origem.getY() - 3, origem.getZ(), origem.getX() + tamanho, origem.getY() + 10, origem.getZ() + tamanho);
        }
    }

    private static boolean blocosIguaisAPlanta(ServerLevel mundo, PlantaDoAndar planta, BlockPos origem) {
        Construida feita = new Construida(planta, origem);
        boolean certo = true;
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                switch (planta.tipo(qx, qy)) {
                    case PlantaDoAndar.ABISMO -> certo &= mundo.getBlockState(feita.bloco(qx, qy, 1, -2, 1)).is(ModBlocos.FUNDO_DO_ABISMO.get())
                            && mundo.getBlockState(feita.bloco(qx, qy, 1, 0, 1)).isAir();
                    case PlantaDoAndar.PORTA_DE_CRISTAL -> certo &= temNoQuadradinho(mundo, feita, qx, qy, ModBlocos.PORTA_DE_CRISTAL.get());
                    case PlantaDoAndar.BARRICADA -> certo &= temNoQuadradinho(mundo, feita, qx, qy, ModBlocos.BARRICADA.get());
                    case PlantaDoAndar.POCO -> {
                        BlockState fundo = mundo.getBlockState(feita.bloco(qx, qy, 1, -1, 1));
                        certo &= fundo.is(ModBlocos.FUNDO_DO_POCO.get()) && fundo.getValue(FundoDoPoco.CONTROLE)
                                && mundo.getBlockState(feita.bloco(qx, qy, 1, 0, 1)).is(Blocks.WATER);
                    }
                    case PlantaDoAndar.RESPIRADOURO -> {
                        BlockState respiradouro = mundo.getBlockState(feita.bloco(qx, qy, 0, 0, 0));
                        certo &= respiradouro.is(ModBlocos.RESPIRADOURO.get()) && respiradouro.getValue(Respiradouro.CONTROLE);
                    }
                    case PlantaDoAndar.PEDESTAL -> certo &= mundo.getBlockState(feita.bloco(qx, qy, 1, 0, 1)).is(ModBlocos.PILAR_ESGOTO.get());
                    case PlantaDoAndar.BRASAS -> certo &= mundo.getBlockState(feita.bloco(qx, qy, 0, 0, 0)).is(ModBlocos.BRASAS.get());
                    default -> {
                    }
                }
            }
        }
        for (int[] fogo : planta.fogoEterno()) {
            certo &= mundo.getBlockState(feita.bloco(fogo[0], fogo[1], 1, 2, 1)).is(ModBlocos.FOGO_ETERNO.get());
        }
        int[] altar = planta.fogoDeSacrificio();
        if (altar != null) {
            BlockState fogo = mundo.getBlockState(feita.bloco(altar[0], altar[1], 1, 1, 1));
            certo &= fogo.is(ModBlocos.FOGO_DE_SACRIFICIO.get()) && fogo.getValue(FogoDeSacrificio.CONTROLE);
        }
        return certo;
    }

    private static boolean temNoQuadradinho(ServerLevel mundo, Construida feita, int qx, int qy, Block bloco) {
        for (int i = 0; i < T; i++) {
            if (mundo.getBlockState(feita.bloco(qx, qy, 1, 1, i)).is(bloco) || mundo.getBlockState(feita.bloco(qx, qy, i, 1, 1)).is(bloco)) {
                return true;
            }
        }
        return false;
    }

    // Um bloco desse tipo no quadradinho (ou null)
    private static BlockPos achar(ServerLevel mundo, Construida feita, byte tipo, Block bloco) {
        PlantaDoAndar planta = feita.planta();
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                if (planta.tipo(qx, qy) != tipo) {
                    continue;
                }
                for (int dx = 0; dx < T; dx++) {
                    for (int dz = 0; dz < T; dz++) {
                        for (int dy = -1; dy <= 2; dy++) {
                            BlockPos lugar = feita.bloco(qx, qy, dx, dy, dz);
                            if (mundo.getBlockState(lugar).is(bloco)) {
                                return lugar;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void testarPortaDeCristal(ServerLevel mundo, DadosDaRun run, FakePlayer heroi, Construida feita) {
        boolean semChaveFechada = false;
        boolean abriu = false;
        boolean gastou = false;
        if (feita != null) {
            BlockPos porta = achar(mundo, feita, PlantaDoAndar.PORTA_DE_CRISTAL, ModBlocos.PORTA_DE_CRISTAL.get());
            if (porta != null) {
                heroi.getInventory().clearContent();
                semChaveFechada = !PortaDeCristal.abrir(mundo, porta, heroi) && mundo.getBlockState(porta).is(ModBlocos.PORTA_DE_CRISTAL.get());
                int pedacos = PortaDeCristal.pedacos(mundo, porta).size();
                heroi.getInventory().add(ChaveDeCristal.criar(run.numeroDaRun(), Andares.andarPelaAltura(porta.getY())));
                abriu = PortaDeCristal.abrir(mundo, porta, heroi) && pedacos == 24 && !mundo.getBlockState(porta).is(ModBlocos.PORTA_DE_CRISTAL.get());
                gastou = ChaveDeCristal.procurar(heroi, run.numeroDaRun(), Andares.andarPelaAltura(porta.getY())).isEmpty();
            }
        }
        PixelDungeon.LOGGER.info("Teste da porta de cristal: sem a chave continua fechada = {}, abriu a porta inteira = {}, gastou a chave = {}",
                semChaveFechada, abriu, gastou);
    }

    private static void testarBarricadaEFogo(ServerLevel mundo, Construida deposito, Construida salaDoFogo) {
        boolean queimou = false;
        if (deposito != null) {
            BlockPos barricada = achar(mundo, deposito, PlantaDoAndar.BARRICADA, ModBlocos.BARRICADA.get());
            if (barricada != null) {
                // Fogo no chão do lado da barricada
                BlockPos lado = null;
                for (BlockPos vizinho : new BlockPos[] { barricada.east(), barricada.west(), barricada.north(), barricada.south() }) {
                    if (mundo.getBlockState(vizinho).isAir()) {
                        lado = vizinho;
                        break;
                    }
                }
                if (lado != null) {
                    mundo.setBlockAndUpdate(lado, Blocks.FIRE.defaultBlockState());
                    queimou = achar(mundo, deposito, PlantaDoAndar.BARRICADA, ModBlocos.BARRICADA.get()) == null;
                }
            }
        }
        boolean geloApagou = false;
        boolean aguaApagou = false;
        if (salaDoFogo != null && !salaDoFogo.planta().fogoEterno().isEmpty()) {
            int[] fogo = salaDoFogo.planta().fogoEterno().get(0);
            BlockPos meio = salaDoFogo.bloco(fogo[0], fogo[1], 2, 1, 2);
            EfeitosDePocao.congelar(mundo, Vec3.atCenterOf(meio));
            geloApagou = true;
            for (int[] outro : salaDoFogo.planta().fogoEterno()) {
                geloApagou &= !mundo.getBlockState(salaDoFogo.bloco(outro[0], outro[1], 1, 2, 1)).is(ModBlocos.FOGO_ETERNO.get());
            }
            // De novo (a parede inteira), e agora com água do lado
            for (int[] outro : salaDoFogo.planta().fogoEterno()) {
                for (int dx = 0; dx < T; dx++) {
                    for (int dz = 0; dz < T; dz++) {
                        for (int dy = 1; dy <= 3; dy++) {
                            mundo.setBlockAndUpdate(salaDoFogo.bloco(outro[0], outro[1], dx, dy, dz), ModBlocos.FOGO_ETERNO.get().defaultBlockState());
                        }
                    }
                }
            }
            mundo.setBlockAndUpdate(salaDoFogo.bloco(fogo[0], fogo[1], 1, 4, 1), Blocks.WATER.defaultBlockState());
            aguaApagou = true;
            for (int[] outro : salaDoFogo.planta().fogoEterno()) {
                aguaApagou &= !mundo.getBlockState(salaDoFogo.bloco(outro[0], outro[1], 1, 2, 1)).is(ModBlocos.FOGO_ETERNO.get());
            }
        }
        PixelDungeon.LOGGER.info("Teste da barricada e do fogo eterno: a barricada queimou inteira = {}, o gelo apagou a parede de fogo = {}, a água apagou = {}",
                queimou, geloApagou, aguaApagou);
    }

    private static void testarPocoERespiradouro(ServerLevel mundo, FakePlayer heroi, Construida poco, Construida gas) {
        // A água da saúde: cura e tira a maldição do que está equipado
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        ItemStack arma = SaqueDoAndar.armaAleatoria(new Random(5), 1);
        arma.set(ModComponentes.MALDICAO.get(), Maldicao.doAlvo(Maldicao.Alvo.ARMA).get(0).id());
        heroi.setItemInHand(InteractionHand.MAIN_HAND, arma);
        heroi.setHealth(1);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.VENENO, 200));
        Pocos.beberSaude(heroi);
        boolean saude = heroi.getHealth() >= heroi.getMaxHealth() && !heroi.hasEffect(ModEfeitos.VENENO) && !NivelDoItem.amaldicoado(heroi.getMainHandItem());
        // A água da consciência: identifica o que está equipado
        ItemStack outra = SaqueDoAndar.armaAleatoria(new Random(6), 1);
        outra.set(ModComponentes.IDENTIFICADO.get(), false);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, outra);
        Pocos.beberConsciencia(heroi);
        boolean consciencia = NivelDoItem.identificado(heroi.getMainHandItem());
        boolean secou = false;
        if (poco != null && !poco.planta().pocos().isEmpty()) {
            PlantaDoAndar.PocoNaPlanta p = poco.planta().pocos().get(0);
            BlockPos controle = poco.bloco(p.x(), p.y(), 1, -1, 1);
            Pocos.secar(mundo, controle);
            secou = !mundo.getBlockState(controle.above()).is(Blocks.WATER) && !mundo.getBlockState(controle).is(ModBlocos.FUNDO_DO_POCO.get());
        }
        boolean gasSaiu = false;
        if (gas != null) {
            BlockPos respiradouro = achar(mundo, gas, PlantaDoAndar.RESPIRADOURO, ModBlocos.RESPIRADOURO.get());
            if (respiradouro != null) {
                NuvemDeGas nuvem = Respiradouro.soltarGas(mundo, respiradouro);
                gasSaiu = nuvem != null && nuvem.tipo() == NuvemDeGas.Tipo.TOXICO;
            }
        }
        PixelDungeon.LOGGER.info("Teste dos poços e do respiradouro: água da saúde = {}, água da consciência = {}, o poço secou = {}, saiu gás tóxico = {}",
                saude, consciencia, secou, gasSaiu);
    }

    private static void testarSacrificio(ServerLevel mundo, DadosDaRun run, Construida sala) {
        boolean marcou = false;
        boolean contou = false;
        boolean premio = false;
        if (sala != null && sala.planta().fogoDeSacrificio() != null) {
            int[] altar = sala.planta().fogoDeSacrificio();
            BlockPos controle = ConstrutorDeAndar.controleDoFogoDeSacrificio(sala.origem(), altar[0], altar[1]);
            int andar = Andares.andarPelaAltura(controle.getY());
            String chave = "sacrificio_" + andar;
            run.definirQueda(chave, 0);
            RatoMarsupial rato = rato(mundo, controle.offset(-1, 0, 2));
            Sacrificio.marcarEmVolta(mundo, controle);
            marcou = rato.hasEffect(ModEfeitos.MARCADO);
            int antes = Sacrificio.falta(run, andar);
            rato.hurt(mundo.damageSources().magic(), 1000);
            contou = Sacrificio.falta(run, andar) < antes;
            // Quase completo: o próximo completa e o fogo deixa a arma
            run.definirQueda(chave, Sacrificio.total(andar) - 1);
            RatoMarsupial outro = rato(mundo, controle.offset(2, 0, -1));
            Sacrificio.marcarEmVolta(mundo, controle);
            outro.hurt(mundo.damageSources().magic(), 1000);
            boolean apagou = !mundo.getBlockState(controle).is(ModBlocos.FOGO_DE_SACRIFICIO.get());
            boolean arma = false;
            for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, new AABB(controle).inflate(3))) {
                arma |= item.getItem().getItem() instanceof ArmaDoShattered && NivelDoItem.maldicaoConhecida(item.getItem());
            }
            premio = apagou && arma;
            run.definirQueda(chave, 0);
        }
        PixelDungeon.LOGGER.info("Teste do fogo de sacrifício: marcou o rato = {}, contou o sacrifício = {}, completou e deixou a arma = {}", marcou, contou, premio);
    }

    private static void testarMoradores(ServerLevel mundo, FakePlayer heroi, Construida piscina, Construida estatua, Construida sentinela) {
        // Piranhas: 3 na água, vivas; fora da água, morrem
        int piranhas = 0;
        boolean vivasNaAgua = false;
        boolean morreuNoSeco = false;
        if (piscina != null) {
            // (só as da piscina: o andar também pode ter um aquário, que é uma sala comum)
            List<Piranha> todas = mundo.getEntitiesOfClass(Piranha.class, piscina.caixa(), piranha -> {
                PlantaDoAndar.Sala sala = piscina.planta().salaEm(Math.floorDiv((int) Math.floor(piranha.getX()) - piscina.origem().getX(), T),
                        Math.floorDiv((int) Math.floor(piranha.getZ()) - piscina.origem().getZ(), T));
                return sala != null && sala.especial();
            });
            piranhas = todas.size();
            vivasNaAgua = !todas.isEmpty();
            for (Piranha piranha : todas) {
                for (int i = 0; i < 15; i++) {
                    piranha.tick();
                }
                vivasNaAgua &= piranha.isAlive();
            }
            if (!todas.isEmpty()) {
                Piranha fora = todas.get(0);
                BlockPos seco = piscina.origem().offset(0, 20, 0);
                mundo.setBlockAndUpdate(seco.below(), Blocks.STONE.defaultBlockState());
                fora.teleportTo(seco.getX() + 0.5, seco.getY(), seco.getZ() + 0.5);
                for (int i = 0; i < 15 && fora.isAlive(); i++) {
                    fora.tick();
                }
                morreuNoSeco = !fora.isAlive();
            }
        }
        // Estátua: parada até apanhar; derrotada, deixa a arma (encantada e sem maldição)
        boolean parada = false;
        boolean armaBoa = false;
        boolean acordou = false;
        boolean deixouArma = false;
        if (estatua != null) {
            List<EstatuaAnimada> estatuas = mundo.getEntitiesOfClass(EstatuaAnimada.class, estatua.caixa());
            if (!estatuas.isEmpty()) {
                EstatuaAnimada animada = estatuas.get(0);
                parada = animada.parada() && animada.isNoAi();
                armaBoa = animada.arma().getItem() instanceof ArmaDoShattered && NivelDoItem.encantamento(animada.arma()) != null
                        && !NivelDoItem.amaldicoado(animada.arma());
                RatoMarsupial rato = rato(mundo, animada.blockPosition());
                animada.hurt(mundo.damageSources().mobAttack(rato), 1);
                acordou = !animada.parada() && !animada.isNoAi();
                rato.discard();
                animada.hurt(mundo.damageSources().magic(), 10000);
                for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, animada.getBoundingBox().inflate(3))) {
                    deixouArma |= item.getItem().getItem() instanceof ArmaDoShattered;
                }
            }
        }
        // Sentinela: não apanha, e os raios dela machucam o herói
        boolean invulneravel = false;
        boolean raio = false;
        if (sentinela != null) {
            List<Sentinela> sentinelas = mundo.getEntitiesOfClass(Sentinela.class, sentinela.caixa());
            if (!sentinelas.isEmpty()) {
                Sentinela vermelha = sentinelas.get(0);
                invulneravel = !vermelha.hurt(mundo.damageSources().magic(), 100) && vermelha.isAlive();
                heroi.removeAllEffects();
                heroi.setHealth(heroi.getMaxHealth());
                // (o herói de teste não leva dano de verdade: confere se algum raio acerta)
                for (int i = 0; i < 10 && !raio; i++) {
                    raio = vermelha.disparar(mundo, heroi);
                }
                heroi.setHealth(heroi.getMaxHealth());
            }
        }
        PixelDungeon.LOGGER.info("Teste dos moradores das salas: {} piranhas, vivas na água = {}, morreu no seco = {}, estátua parada = {}, arma encantada = {}, "
                + "acordou ao apanhar = {}, deixou a arma = {}, sentinela não apanha = {}, o raio acerta = {}",
                piranhas, vivasNaAgua, morreuNoSeco, parada, armaBoa, acordou, deixouArma, invulneravel, raio);
    }

    // ---- 4. O pote de mel e a abelha; o abismo ----

    private static void testarPoteDeMel(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = run.andar(1).entrada().offset(2700, 0, 0);
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 40; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 4; y++) {
                    mundo.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        Vec3 lugar = Vec3.atBottomCenterOf(base);
        ItemStack pote = PoteDeMel.quebrar(mundo, lugar, null);
        int id = PoteQuebrado.id(pote);
        List<Abelha> abelhas = mundo.getEntitiesOfClass(Abelha.class, new AABB(base).inflate(3));
        boolean nasceu = abelhas.size() == 1 && id != 0 && abelhas.get(0).idDoPote() == id;
        boolean defende = false;
        boolean semPoteViraComum = false;
        if (!abelhas.isEmpty()) {
            Abelha abelha = abelhas.get(0);
            ItemEntity noChao = new ItemEntity(mundo, lugar.x, lugar.y, lugar.z, pote);
            mundo.addFreshEntity(noChao);
            abelha.olharOPote();
            RatoMarsupial perto = rato(mundo, base.offset(2, 0, 0));
            RatoMarsupial longe = rato(mundo, base.offset(0, 0, 30));
            defende = abelha.lugarDoPote() != null && abelha.podeCacar(perto) && !abelha.podeCacar(longe);
            perto.discard();
            longe.discard();
            noChao.discard();
            abelha.olharOPote();
            semPoteViraComum = abelha.idDoPote() == 0 && abelha.lugarDoPote() == null;
            abelha.discard();
        }
        PixelDungeon.LOGGER.info("Teste do pote de mel: a abelha nasceu com o pote = {}, defende só perto do pote = {}, sem o pote vira comum = {}",
                nasceu, defende, semPoteViraComum);
    }

    private static void testarAbismo(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        BlockPos entrada = run.andar(1).entrada();
        BlockPos fundo = new BlockPos(entrada.getX(), Andares.yDoAndar(1) - 2, entrada.getZ());
        // O herói: aleijado, sangrando e machucado
        // (o herói de teste não leva dano de verdade: o dano é conferido pela conta)
        heroi.setHealth(heroi.getMaxHealth());
        Abismo.cair(mundo, heroi, fundo);
        boolean aleijou = heroi.hasEffect(ModEfeitos.ALEIJADO) && heroi.hasEffect(ModEfeitos.SANGRANDO);
        boolean metade = true;
        for (int i = 0; i < 20; i++) {
            float vida = 5 + i * 2;
            metade &= Abismo.danoDaQueda(mundo.getRandom(), vida, 45) >= (int) vida / 2;
        }
        // Com a queda suave, nada
        heroi.removeAllEffects();
        heroi.setHealth(heroi.getMaxHealth());
        heroi.addEffect(new MobEffectInstance(ModEfeitos.QUEDA_SUAVE, 400));
        Abismo.aterrissar(heroi);
        boolean suave = !heroi.hasEffect(ModEfeitos.ALEIJADO) && !heroi.hasEffect(ModEfeitos.SANGRANDO);
        heroi.removeAllEffects();
        heroi.setHealth(heroi.getMaxHealth());
        // Onde cai: no andar de baixo
        BlockPos lugar = Abismo.lugarDeCair(run, 2, false, 3);
        boolean andarDeBaixo = Andares.andarPelaAltura(lugar.getY()) == 2;
        // Um item cai junto; um inimigo morre
        ItemEntity item = new ItemEntity(mundo, fundo.getX() + 0.5, fundo.getY() + 0.5, fundo.getZ() + 0.5, new ItemStack(Items.STICK));
        mundo.addFreshEntity(item);
        Abismo.cair(mundo, item, fundo);
        boolean itemCaiu = Andares.andarPelaAltura((int) Math.floor(item.getY())) == 2;
        item.discard();
        RatoMarsupial rato = rato(mundo, fundo);
        Abismo.cair(mundo, rato, fundo);
        boolean ratoMorreu = !rato.isAlive();
        // A sala do poço: quem cai do piso frágil vai pra ela
        boolean naSalaDoPoco = false;
        for (long semente = 1; semente <= SEMENTES && !naSalaDoPoco; semente++) {
            for (int andar = 2; andar <= 4 && !naSalaDoPoco; andar++) {
                if (!ComodoEspecial.doAndar(semente, andar).contains(ComodoEspecial.Tipo.SALA_DO_POCO)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                naSalaDoPoco = planta.salaDoPoco() != null && planta.salaDoPoco().largura() >= 4;
                break;
            }
        }
        PixelDungeon.LOGGER.info("Teste do abismo: a queda aleija e faz sangrar = {}, tira pelo menos metade da vida = {}, com a queda suave não = {}, "
                + "cai no andar de baixo = {}, o item cai junto = {}, o inimigo morre = {}, a sala do poço existe pra quem cai do piso frágil = {}",
                aleijou, metade, suave, andarDeBaixo, itemCaiu, ratoMorreu, naSalaDoPoco);
    }

    // ---- Ajudantes ----

    private static int tiles(PlantaDoAndar planta, byte tipo) {
        int total = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.tipo(x, y) == tipo) {
                    total++;
                }
            }
        }
        return total;
    }

    private static int premios(PlantaDoAndar planta, Premio premio, Recipiente recipiente) {
        int total = 0;
        for (PremioNoChao noChao : planta.premios()) {
            if (noChao.premio() == premio && noChao.recipiente() == recipiente) {
                total++;
            }
        }
        return total;
    }

    private static int inimigos(PlantaDoAndar planta, InimigoDaSala tipo) {
        int total = 0;
        for (PlantaDoAndar.InimigoNaPlanta inimigo : planta.inimigos()) {
            if (inimigo.tipo() == tipo) {
                total++;
            }
        }
        return total;
    }

    // Só os das salas especiais (o aquário, que é uma sala comum, também tem piranhas)
    private static int inimigosNasEspeciais(PlantaDoAndar planta, InimigoDaSala tipo) {
        int total = 0;
        for (PlantaDoAndar.InimigoNaPlanta inimigo : planta.inimigos()) {
            PlantaDoAndar.Sala sala = planta.salaEm(inimigo.x(), inimigo.y());
            if (inimigo.tipo() == tipo && sala != null && sala.especial()) {
                total++;
            }
        }
        return total;
    }

    private static boolean entre(int valor, int minimo, int maximo) {
        return valor >= minimo && valor <= maximo;
    }

    private static RatoMarsupial rato(ServerLevel mundo, BlockPos lugar) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        rato.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5);
        mundo.addFreshEntity(rato);
        return rato;
    }
}
