package com.louzada.pixeldungeon.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.dungeon.ConstrutorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as salas secretas, a busca, o mapeamento mágico, a previsão do ladino e o prêmio do labirinto.
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDaBusca {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.LADINO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // As salas secretas e as portas escondidas dos andares 2 a 4 (o andar 1 não tem nenhuma)
        int salas = 0;
        int[] porAndar = new int[5];
        List<int[]> portas = new ArrayList<>();
        for (int andar = 1; andar <= 4; andar++) {
            PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
            if (andar > 1) {
                salas += planta.salasSecretas();
            }
            for (int y = 0; y < planta.altura(); y++) {
                for (int x = 0; x < planta.largura(); x++) {
                    if (planta.tipo(x, y) == PlantaDoAndar.PORTA_ESCONDIDA && !run.portaAchada(andar, x, y)) {
                        porAndar[andar]++;
                        portas.add(new int[] { andar, x, y });
                    }
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das salas secretas: {} salas secretas nos andares 2 a 4, pelo menos 2 = {}; portas escondidas no andar 1 = {}, nos outros = {}",
                salas, salas >= 2, porAndar[1], porAndar[2] + porAndar[3] + porAndar[4]);
        if (portas.isEmpty()) {
            return;
        }

        // Procurar do lado de uma porta escondida: sempre acha, e a parede vira porta
        int[] porta = portas.get(0);
        int andar = porta[0];
        PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
        BlockPos controle = ConstrutorDeAndar.controleDaPorta(origem, planta, porta[1], porta[2]);
        BlockState antes = mundo.getBlockState(controle);
        boolean eraParede = !antes.isAir() && !antes.is(ModBlocos.PORTA_GIGANTE.get());
        int[][] lados = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] lado : lados) {
            if (planta.passavel(porta[1] + lado[0], porta[2] + lado[1])) {
                BlockPos lugar = ConstrutorDeAndar.pontoDeChegada(origem, porta[1] + lado[0], porta[2] + lado[1]);
                heroi.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
                break;
            }
        }
        int achou = Busca.procurar(heroi);
        boolean virouPorta = mundo.getBlockState(controle).is(ModBlocos.PORTA_GIGANTE.get());
        int deNovo = Busca.procurar(heroi);
        PixelDungeon.LOGGER.info("Teste da busca: achou {} porta(s) ao procurar do lado; antes era parede = {}, virou porta = {}, ficou salvo = {}, procurar de novo logo em seguida achou {}",
                achou, eraParede, virouPorta, run.portaAchada(andar, porta[1], porta[2]) && planta.tipo(porta[1], porta[2]) == PlantaDoAndar.PORTA, deNovo);

        // O mapeamento mágico revela todas as portas escondidas do andar
        int[] outra = null;
        for (int[] candidata : portas) {
            if (!run.portaAchada(candidata[0], candidata[1], candidata[2])) {
                outra = candidata;
                break;
            }
        }
        if (outra != null) {
            BlockPos entrada = run.andar(outra[0]).entrada();
            heroi.moveTo(entrada.getX() + 0.5, entrada.getY(), entrada.getZ() + 0.5, 0.0F, 0.0F);
            int revelou = Busca.revelarTodas(heroi).size();
            PlantaDoAndar plantaDaOutra = PopulacaoDoAndar.planta(run.semente(), outra[0]);
            int sobraram = 0;
            for (int y = 0; y < plantaDaOutra.altura(); y++) {
                for (int x = 0; x < plantaDaOutra.largura(); x++) {
                    if (plantaDaOutra.tipo(x, y) == PlantaDoAndar.PORTA_ESCONDIDA && !run.portaAchada(outra[0], x, y)) {
                        sobraram++;
                    }
                }
            }
            PixelDungeon.LOGGER.info("Teste do mapeamento das portas: revelou {} no andar {}, revelou alguma = {}, sobrou escondida = {}", revelou, outra[0],
                    revelou > 0, sobraram);
        }

        // Previsão do ladino: com +2 sempre avisa num andar com sala secreta; sem o talento, nunca
        int andarComSala = 0;
        for (int a = 2; a <= 4 && andarComSala == 0; a++) {
            // (no andar com segredos, igual ao Shattered, só avisa com 2 ou mais)
            PlantaDoAndar candidata = PopulacaoDoAndar.planta(run.semente(), a);
            if (candidata.salasSecretas() >= (candidata.tema() == TemaDoAndar.Tema.SEGREDOS ? 2 : 1)) {
                andarComSala = a;
            }
        }
        boolean semTalento = Busca.aoChegarNoAndar(heroi, andarComSala);
        dados.definirPontos(Talento.PREVISAO_DO_LADINO, 2);
        boolean comTalento = Busca.aoChegarNoAndar(heroi, andarComSala);
        boolean noAndar1 = Busca.aoChegarNoAndar(heroi, 1);
        dados.definirPontos(Talento.PREVISAO_DO_LADINO, 0);
        PixelDungeon.LOGGER.info("Teste da previsão do ladino: com +2 avisou = {}, sem o talento avisou = {}, no andar 1 avisou = {}", comTalento, semTalento,
                noAndar1);

        // O prêmio do labirinto: arma ou armadura, nunca amaldiçoada (e já se sabe disso)
        Random sorteio = new Random(7);
        int amaldicoados = 0;
        int desconhecidos = 0;
        int armaduras = 0;
        for (int i = 0; i < 200; i++) {
            ItemStack premio = SaqueDoAndar.premioDoLabirinto(sorteio, 3);
            if (NivelDoItem.amaldicoado(premio)) {
                amaldicoados++;
            }
            if (!NivelDoItem.maldicaoConhecida(premio)) {
                desconhecidos++;
            }
            if (premio.getItem() instanceof ArmaduraDoShattered) {
                armaduras++;
            }
        }
        PixelDungeon.LOGGER.info("Teste do labirinto: 200 prêmios, amaldiçoados = {}, maldição desconhecida = {}, metade armaduras = {}", amaldicoados, desconhecidos,
                armaduras >= 70 && armaduras <= 130);

        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
    }
}
