package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

// Transforma a planta de um andar em blocos de verdade no mundo.
//
// Corte de uma sala (de baixo pra cima):
//   y + 9      teto
//   y + 8      cornija (degraus de cabeça pra baixo encostados na parede)
//   y + 1 a 8  espaço livre (8 blocos); a parede tem rodapé no y + 1
//   y          chão (ou água, com um chão embaixo)
//
// Os corredores são túneis mais baixos (6 blocos), com arcos no alto dos dois lados.
public class ConstrutorDeAndar {
    // Atualiza só os jogadores, sem avisar os blocos vizinhos (bem mais rápido)
    private static final int ATUALIZACAO = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int ALTURA_DO_TETO = Andares.ALTURA_DA_PAREDE + 1;
    // Luz invisível de fundo (0 a 15); as lanternas dão a luz forte
    private static final int LUZ_DAS_SALAS = 10;
    private static final int LUZ_DOS_CORREDORES = 9;
    // Chance (em %) de nascer musgo num bloco de chão
    private static final int CHANCE_DE_MUSGO_PERTO_DA_AGUA = 30;
    private static final int CHANCE_DE_MUSGO = 4;

    private ServerLevel nivel;
    private PlantaDoAndar planta;
    private BlockPos origem;
    private Random sorteio;
    private BlockPos.MutableBlockPos posicao = new BlockPos.MutableBlockPos();

    private BlockState parede = ModBlocos.PAREDE_ESGOTO.get().defaultBlockState();
    private BlockState chao = ModBlocos.CHAO_ESGOTO.get().defaultBlockState();
    private BlockState rodape = ModBlocos.RODAPE_ESGOTO.get().defaultBlockState();
    private BlockState pilar = ModBlocos.PILAR_ESGOTO.get().defaultBlockState();
    private BlockState moldura = ModBlocos.MOLDURA_ESGOTO.get().defaultBlockState();
    private BlockState lanterna = ModBlocos.LANTERNA_ESGOTO.get().defaultBlockState();
    private BlockState corrente = ModBlocos.CORRENTE_ESGOTO.get().defaultBlockState();
    private BlockState musgo = ModBlocos.MUSGO_ESGOTO.get().defaultBlockState();
    private BlockState agua = Blocks.WATER.defaultBlockState();
    private BlockState escadaSubida = ModBlocos.ESCADA_SUBIDA.get().defaultBlockState();
    private BlockState escadaDescida = ModBlocos.ESCADA_DESCIDA.get().defaultBlockState();

    private ConstrutorDeAndar(ServerLevel nivel, PlantaDoAndar planta, BlockPos origem, long semente) {
        this.nivel = nivel;
        this.planta = planta;
        this.origem = origem;
        this.sorteio = new Random(semente);
    }

    // semente: decide onde cai o musgo (mesma semente = mesma decoração)
    public static void construir(ServerLevel nivel, PlantaDoAndar planta, BlockPos origem, long semente) {
        new ConstrutorDeAndar(nivel, planta, origem, semente).construirTudo();
    }

    // Bloco onde o jogador aparece ao chegar num quadradinho (do lado da escada)
    public static BlockPos pontoDeChegada(BlockPos origem, int qx, int qy) {
        return new BlockPos(origem.getX() + qx * T + 2, origem.getY() + 1, origem.getZ() + qy * T + 2);
    }

    private void construirTudo() {
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                if (planta.aberto(qx, qy)) {
                    construirQuadradinhoAberto(qx, qy);
                } else if (planta.encostaEmAberto(qx, qy)) {
                    // Parede só onde encosta em lugar aberto; o resto fica vazio (ninguém vê mesmo)
                    construirParede(qx, qy);
                }
            }
        }
        for (Sala sala : planta.salas()) {
            construirPilares(sala);
        }
        construirEscadas();
    }

    // ---- Paredes ----

    private void construirParede(int qx, int qy) {
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                for (int dy = 0; dy <= ALTURA_DO_TETO; dy++) {
                    colocar(qx, qy, dx, dy, dz, dy == 1 ? rodape : parede);
                }
            }
        }
    }

    // ---- Chão, teto, cornija, luz e musgo ----

    private void construirQuadradinhoAberto(int qx, int qy) {
        byte tipo = planta.tipo(qx, qy);
        int alturaLivre = alturaLivre(qx, qy);
        boolean temLanterna = temLanterna(qx, qy);
        boolean pertoDaAgua = encostaEmAgua(qx, qy);

        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                // Chão (a água fica num buraco de 1 bloco com chão embaixo)
                if (tipo == PlantaDoAndar.AGUA) {
                    colocar(qx, qy, dx, -1, dz, chao);
                    colocar(qx, qy, dx, 0, dz, agua);
                } else {
                    colocar(qx, qy, dx, 0, dz, chao);
                }

                // Teto (nos corredores, o espaço acima do túnel também é preenchido)
                for (int dy = alturaLivre + 1; dy <= ALTURA_DO_TETO; dy++) {
                    colocar(qx, qy, dx, dy, dz, parede);
                }

                // Cornija / arco: no alto, encostado na parede
                List<Direction> paredes = paredesEncostadas(qx, qy, dx, dz, alturaLivre);
                if (paredes.size() == 1) {
                    colocar(qx, qy, dx, alturaLivre, dz, degrauDeCabecaPraBaixo(paredes.get(0)));
                } else if (paredes.size() >= 2) {
                    // Canto: um bloco cheio fica melhor que um degrau torto
                    colocar(qx, qy, dx, alturaLivre, dz, parede);
                }

                // Musgo pelo chão, mais perto das poças
                if (tipo == PlantaDoAndar.CHAO) {
                    int chance = pertoDaAgua ? CHANCE_DE_MUSGO_PERTO_DA_AGUA : CHANCE_DE_MUSGO;
                    if (sorteio.nextInt(100) < chance) {
                        colocar(qx, qy, dx, 1, dz, musgo);
                    }
                }
            }
        }

        if (temLanterna) {
            // Lanterna pendurada por duas correntes no meio do quadradinho
            colocar(qx, qy, 1, alturaLivre, 1, corrente);
            colocar(qx, qy, 1, alturaLivre - 1, 1, corrente);
            colocar(qx, qy, 1, alturaLivre - 2, 1, lanterna);
        } else {
            int forca = planta.ehCorredor(qx, qy) ? LUZ_DOS_CORREDORES : LUZ_DAS_SALAS;
            colocar(qx, qy, 1, alturaLivre, 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, forca));
        }
    }

    // Salas têm 8 blocos de altura livre; corredores, 6
    private int alturaLivre(int qx, int qy) {
        return planta.ehCorredor(qx, qy) ? Andares.ALTURA_DO_CORREDOR : Andares.ALTURA_DA_PAREDE;
    }

    // O vizinho é "parede" nessa altura? (parede de verdade, ou um corredor, que é mais baixo que a sala)
    private boolean fechadoNaAltura(int qx, int qy, int altura) {
        return !planta.aberto(qx, qy) || alturaLivre(qx, qy) < altura;
    }

    // Lados do quadradinho em que este bloco encosta numa parede (0, 1 ou 2 lados)
    private List<Direction> paredesEncostadas(int qx, int qy, int dx, int dz, int altura) {
        List<Direction> lista = new ArrayList<>(2);
        if (dx == 0 && fechadoNaAltura(qx - 1, qy, altura)) {
            lista.add(Direction.WEST);
        }
        if (dx == T - 1 && fechadoNaAltura(qx + 1, qy, altura)) {
            lista.add(Direction.EAST);
        }
        if (dz == 0 && fechadoNaAltura(qx, qy - 1, altura)) {
            lista.add(Direction.NORTH);
        }
        if (dz == T - 1 && fechadoNaAltura(qx, qy + 1, altura)) {
            lista.add(Direction.SOUTH);
        }
        return lista;
    }

    // Degrau de cabeça pra baixo com a parte cheia virada pra parede
    private BlockState degrauDeCabecaPraBaixo(Direction ladoDaParede) {
        return ModBlocos.CORNIJA_ESGOTO.get().defaultBlockState()
                .setValue(StairBlock.FACING, ladoDaParede)
                .setValue(StairBlock.HALF, Half.TOP);
    }

    private boolean encostaEmAgua(int qx, int qy) {
        return planta.tipo(qx + 1, qy) == PlantaDoAndar.AGUA
                || planta.tipo(qx - 1, qy) == PlantaDoAndar.AGUA
                || planta.tipo(qx, qy + 1) == PlantaDoAndar.AGUA
                || planta.tipo(qx, qy - 1) == PlantaDoAndar.AGUA;
    }

    // Lanternas no centro de cada sala e, nas salas grandes, a cada 4 quadradinhos a partir do centro
    private boolean temLanterna(int qx, int qy) {
        if (planta.ehCorredor(qx, qy)) {
            return false;
        }
        for (Sala sala : planta.salas()) {
            boolean dentroDaSala = qx >= sala.x() && qx < sala.x() + sala.largura()
                    && qy >= sala.y() && qy < sala.y() + sala.altura();
            if (dentroDaSala) {
                return Math.floorMod(qx - sala.centroX(), 4) == 0 && Math.floorMod(qy - sala.centroY(), 4) == 0;
            }
        }
        return false;
    }

    // ---- Pilares nos cantos das salas ----

    private void construirPilares(Sala sala) {
        int ultimoX = sala.x() + sala.largura() - 1;
        int ultimoY = sala.y() + sala.altura() - 1;
        construirPilarSeForCanto(sala.x(), sala.y(), 0, 0);
        construirPilarSeForCanto(ultimoX, sala.y(), T - 1, 0);
        construirPilarSeForCanto(sala.x(), ultimoY, 0, T - 1);
        construirPilarSeForCanto(ultimoX, ultimoY, T - 1, T - 1);
    }

    // Só põe o pilar se o canto for um canto de verdade (com parede dos dois lados, sem corredor saindo dali)
    private void construirPilarSeForCanto(int qx, int qy, int dx, int dz) {
        if (!planta.aberto(qx, qy) || paredesEncostadas(qx, qy, dx, dz, 1).size() < 2) {
            return;
        }
        int inicio = planta.tipo(qx, qy) == PlantaDoAndar.AGUA ? 0 : 1;
        for (int dy = inicio; dy <= alturaLivre(qx, qy); dy++) {
            colocar(qx, qy, dx, dy, dz, pilar);
        }
    }

    // ---- Entrada e saída ----

    private void construirEscadas() {
        // Moldura de tufo entalhado no chão em volta das duas escadas
        if (planta.entradaX() >= 0) {
            construirMoldura(planta.entradaX(), planta.entradaY());
            colocar(planta.entradaX(), planta.entradaY(), 1, 1, 1, escadaSubida);
        }
        if (planta.temSaida()) {
            construirMoldura(planta.saidaX(), planta.saidaY());
            // O alçapão fica no lugar do chão, nivelado
            colocar(planta.saidaX(), planta.saidaY(), 1, 0, 1, escadaDescida);
        }
    }

    // Quadrado 3x3 de moldura no chão, com a escada no meio (bloco 1, 1 do quadradinho)
    private void construirMoldura(int qx, int qy) {
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                colocar(qx, qy, dx, 0, dz, moldura);
            }
        }
    }

    // Coloca um bloco: quadradinho (qx, qy) + deslocamento (dx, dz) dentro dele + altura dy acima do chão
    private void colocar(int qx, int qy, int dx, int dy, int dz, BlockState estado) {
        posicao.set(origem.getX() + qx * T + dx, origem.getY() + dy, origem.getZ() + qy * T + dz);
        nivel.setBlock(posicao, estado, ATUALIZACAO);
    }
}
