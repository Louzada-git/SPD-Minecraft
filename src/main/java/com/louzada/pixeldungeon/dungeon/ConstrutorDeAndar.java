package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.bloco.ArmadilhaBloco;
import com.louzada.pixeldungeon.bloco.CapimAlto;
import com.louzada.pixeldungeon.bloco.FogoDeSacrificio;
import com.louzada.pixeldungeon.bloco.FundoDoPoco;
import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.bloco.PoteDeAlquimia;
import com.louzada.pixeldungeon.bloco.Respiradouro;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

// Transforma a planta de um andar em blocos de verdade no mundo.
//
// Corte de uma sala (de baixo pra cima):
//   y + 9      teto
//   y + 8      cornija (degraus de cabeça pra baixo encostados na parede)
//   y + 1 a 8  espaço livre (8 blocos); a parede tem rodapé no y + 1
//   y          chão (ou água, com um chão embaixo; ou os 16 blocos de uma armadilha)
//
// Os corredores são túneis mais baixos (6 blocos), com arcos no alto dos dois lados.
// As portas gigantes ficam no meio da parede entre dois cômodos, com a altura dos corredores (4 de largura, 6 de altura).
// As portas escondidas são construídas como parede; quando alguém acha uma, ela vira uma porta comum (revelarPorta).
public class ConstrutorDeAndar {
    // Atualiza só os jogadores, sem avisar os blocos vizinhos (bem mais rápido)
    private static final int ATUALIZACAO = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int ALTURA_DO_TETO = Andares.ALTURA_DA_PAREDE + 1;
    // Luz invisível de fundo (0 a 15); as lanternas dão a luz forte. No andar escuro, nada de lanternas e a luz de fundo é bem fraca.
    private static final int LUZ_DAS_SALAS = 10;
    private static final int LUZ_DOS_CORREDORES = 9;
    private static final int LUZ_DAS_SALAS_NO_ESCURO = 6;
    private static final int LUZ_DOS_CORREDORES_NO_ESCURO = 5;

    private ServerLevel nivel;
    private PlantaDoAndar planta;
    private BlockPos origem;
    private Random sorteio;
    private BlockPos.MutableBlockPos posicao = new BlockPos.MutableBlockPos();
    private final boolean escuro;
    private final int luzDasSalas;
    private final int luzDosCorredores;

    private BlockState parede = ModBlocos.PAREDE_ESGOTO.get().defaultBlockState();
    private BlockState chao = ModBlocos.CHAO_ESGOTO.get().defaultBlockState();
    private BlockState rodape = ModBlocos.RODAPE_ESGOTO.get().defaultBlockState();
    private BlockState pilar = ModBlocos.PILAR_ESGOTO.get().defaultBlockState();
    private BlockState moldura = ModBlocos.MOLDURA_ESGOTO.get().defaultBlockState();
    private BlockState lanterna = ModBlocos.LANTERNA_ESGOTO.get().defaultBlockState();
    private BlockState corrente = ModBlocos.CORRENTE_ESGOTO.get().defaultBlockState();
    private BlockState agua = Blocks.WATER.defaultBlockState();
    private BlockState escadaSubida = ModBlocos.ESCADA_SUBIDA.get().defaultBlockState();
    private BlockState escadaDescida = ModBlocos.ESCADA_DESCIDA.get().defaultBlockState();
    private BlockState capimAlto = ModBlocos.CAPIM_ALTO.get().defaultBlockState();
    private BlockState gramaBaixa = ModBlocos.GRAMA_BAIXA.get().defaultBlockState();
    private BlockState barril = ModBlocos.BARRIL_ESGOTO.get().defaultBlockState();
    private BlockState estante = ModBlocos.ESTANTE_ESGOTO.get().defaultBlockState();
    private BlockState brasas = ModBlocos.BRASAS.get().defaultBlockState();
    private BlockState fundoDoAbismo = ModBlocos.FUNDO_DO_ABISMO.get().defaultBlockState();
    private BlockState respiradouro = ModBlocos.RESPIRADOURO.get().defaultBlockState();

    private ConstrutorDeAndar(ServerLevel nivel, PlantaDoAndar planta, BlockPos origem, long semente) {
        this.nivel = nivel;
        this.planta = planta;
        this.origem = origem;
        this.sorteio = new Random(semente);
        this.escuro = planta.tema() == TemaDoAndar.Tema.ESCURO;
        this.luzDasSalas = escuro ? LUZ_DAS_SALAS_NO_ESCURO : LUZ_DAS_SALAS;
        this.luzDosCorredores = escuro ? LUZ_DOS_CORREDORES_NO_ESCURO : LUZ_DOS_CORREDORES;
    }

    // semente: decide a decoração (mesma semente = mesma decoração)
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
                    construirOQueTemEmCima(qx, qy);
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
        construirBeiradasDosAbismos();
        construirFogos();
    }

    // Uma porta escondida foi achada: a parede vira uma porta comum. A planta já tem que estar com a PORTA no lugar.
    public static void revelarPorta(ServerLevel nivel, PlantaDoAndar planta, BlockPos origem, int qx, int qy) {
        ConstrutorDeAndar construtor = new ConstrutorDeAndar(nivel, planta, origem, 0);
        construtor.abrirParede(qx, qy);
    }

    private void abrirParede(int qx, int qy) {
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                for (int dy = 1; dy <= ALTURA_DO_TETO; dy++) {
                    colocar(qx, qy, dx, dy, dz, Blocks.AIR.defaultBlockState());
                }
            }
        }
        construirQuadradinhoAberto(qx, qy);
        construirOQueTemEmCima(qx, qy);
        // Nos dois lados da porta, os blocos encostados nela: a cornija muda (num corredor ela some) e um pilar
        // de canto que estava encostado na "parede" sai do caminho
        int[][] lados = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] lado : lados) {
            int vx = qx + lado[0];
            int vy = qy + lado[1];
            if (!planta.aberto(vx, vy)) {
                continue;
            }
            for (int i = 0; i < T; i++) {
                int dx = lado[0] == 0 ? i : (lado[0] < 0 ? T - 1 : 0);
                int dz = lado[1] == 0 ? i : (lado[1] < 0 ? T - 1 : 0);
                ajustarBeiradaDoVizinho(vx, vy, dx, dz);
            }
        }
    }

    private void ajustarBeiradaDoVizinho(int qx, int qy, int dx, int dz) {
        int altura = alturaLivre(qx, qy);
        posicao.set(origem.getX() + qx * T + dx, origem.getY() + 1, origem.getZ() + qy * T + dz);
        if (nivel.getBlockState(posicao).is(pilar.getBlock())) {
            if (paredesEncostadas(qx, qy, dx, dz, 1).size() >= 2) {
                return;
            }
            // O pilar não é mais de canto: some (na água, o pé dele volta a ser água)
            if (planta.tipo(qx, qy) == PlantaDoAndar.AGUA) {
                colocar(qx, qy, dx, 0, dz, agua);
            }
            for (int dy = 1; dy < altura; dy++) {
                colocar(qx, qy, dx, dy, dz, Blocks.AIR.defaultBlockState());
            }
        }
        List<Direction> paredes = paredesEncostadas(qx, qy, dx, dz, altura);
        BlockState emCima = Blocks.AIR.defaultBlockState();
        if (paredes.size() == 1) {
            emCima = degrauDeCabecaPraBaixo(paredes.get(0));
        } else if (paredes.size() >= 2) {
            emCima = parede;
        }
        colocar(qx, qy, dx, altura, dz, emCima);
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

    // ---- Chão, teto, cornija, luz e grama ----

    private void construirQuadradinhoAberto(int qx, int qy) {
        byte tipo = planta.tipo(qx, qy);
        int alturaLivre = alturaLivre(qx, qy);
        boolean temLanterna = temLanterna(qx, qy);
        // O chão especial (as pontes, a biblioteca) é de tufo entalhado; o pedestal, de tijolos de tufo com o meio de pilar;
        // as brasas (em volta do fogo de sacrifício), de pedra-negra com ouro
        BlockState chaoDaqui = switch (tipo) {
            case PlantaDoAndar.CHAO_ESPECIAL, PlantaDoAndar.POTE, PlantaDoAndar.POCO, PlantaDoAndar.POCO_VAZIO -> moldura;
            case PlantaDoAndar.PEDESTAL -> rodape;
            case PlantaDoAndar.BRASAS -> brasas;
            default -> chao;
        };
        boolean poco = tipo == PlantaDoAndar.POCO || tipo == PlantaDoAndar.POCO_VAZIO;
        PlantaDoAndar.PocoNaPlanta pocoMagico = tipo == PlantaDoAndar.POCO ? planta.pocoEm(qx, qy) : null;
        // A armadilha toma o lugar do chão (os 16 blocos, cada um com um pedaço do desenho dela)
        TipoDeArmadilha armadilha = tipo == PlantaDoAndar.AGUA ? null : planta.armadilha(qx, qy);
        boolean escondida = armadilha != null && !planta.armadilhaVisivel(qx, qy);
        // (as da sala queimada já começam gastas)
        boolean desarmada = armadilha != null && planta.armadilhaDesarmada(qx, qy);

        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                boolean meio = dx >= 1 && dx <= 2 && dz >= 1 && dz <= 2;
                // Chão (a água fica num buraco de 1 bloco com chão embaixo)
                if (tipo == PlantaDoAndar.AGUA) {
                    colocar(qx, qy, dx, -1, dz, chao);
                    colocar(qx, qy, dx, 0, dz, agua);
                } else if (tipo == PlantaDoAndar.ABISMO) {
                    // O abismo: um buraco escuro (quem chega no fundo cai pro andar de baixo)
                    colocar(qx, qy, dx, 0, dz, Blocks.AIR.defaultBlockState());
                    colocar(qx, qy, dx, -1, dz, Blocks.AIR.defaultBlockState());
                    colocar(qx, qy, dx, -2, dz, fundoDoAbismo);
                } else if (poco && meio) {
                    // O poço: um buraco de 2x2 no meio, com a água mágica (o fundo diz qual é) ou seco
                    if (pocoMagico != null) {
                        colocar(qx, qy, dx, -1, dz, ModBlocos.FUNDO_DO_POCO.get().defaultBlockState()
                                .setValue(FundoDoPoco.SAUDE, pocoMagico.saude()).setValue(FundoDoPoco.CONTROLE, dx == 1 && dz == 1));
                        colocar(qx, qy, dx, 0, dz, agua);
                    } else {
                        colocar(qx, qy, dx, -1, dz, chao);
                        colocar(qx, qy, dx, 0, dz, Blocks.AIR.defaultBlockState());
                    }
                } else if (tipo == PlantaDoAndar.PEDESTAL && meio) {
                    colocar(qx, qy, dx, 0, dz, pilar);
                } else if (tipo == PlantaDoAndar.RESPIRADOURO) {
                    colocar(qx, qy, dx, 0, dz, respiradouro.setValue(Respiradouro.CONTROLE, dx == 0 && dz == 0));
                } else if (armadilha != null) {
                    colocar(qx, qy, dx, 0, dz, ModBlocos.ARMADILHA.get().pedaco(armadilha, escondida, desarmada, dx, dz));
                } else {
                    colocar(qx, qy, dx, 0, dz, chaoDaqui);
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

                // Grama (a alta ocupa 2 blocos: a parte de baixo e a de cima)
                byte grama = tipo == PlantaDoAndar.CHAO ? planta.grama(qx, qy) : PlantaDoAndar.SEM_GRAMA;
                if (grama == PlantaDoAndar.GRAMA_ALTA) {
                    colocar(qx, qy, dx, 1, dz, capimAlto.setValue(CapimAlto.HALF, DoubleBlockHalf.LOWER));
                    colocar(qx, qy, dx, 2, dz, capimAlto.setValue(CapimAlto.HALF, DoubleBlockHalf.UPPER));
                } else if (grama == PlantaDoAndar.GRAMA) {
                    colocar(qx, qy, dx, 1, dz, gramaBaixa);
                }
            }
        }

        if (armadilha != null && !desarmada) {
            // O controle da armadilha começa a olhar quem pisa
            nivel.scheduleTick(Armadilhas.controle(origem, qx, qy), ModBlocos.ARMADILHA.get(), ArmadilhaBloco.INTERVALO);
        }

        if (tipo == PlantaDoAndar.ABISMO) {
            // Em cima do abismo, só a luz de fundo
            colocar(qx, qy, 1, alturaLivre, 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, luzDasSalas));
            return;
        }
        if (!planta.passavel(qx, qy) || planta.ehPorta(qx, qy)) {
            // A porta, a estátua, a estante e os barris ocupam o meio do quadradinho: a luz vem dos vizinhos
            return;
        }
        if (temLanterna) {
            // Lanterna pendurada por duas correntes no meio do quadradinho
            colocar(qx, qy, 1, alturaLivre, 1, corrente);
            colocar(qx, qy, 1, alturaLivre - 1, 1, corrente);
            colocar(qx, qy, 1, alturaLivre - 2, 1, lanterna);
        } else {
            int forca = planta.ehCorredor(qx, qy) ? luzDosCorredores : luzDasSalas;
            colocar(qx, qy, 1, alturaLivre, 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, forca));
        }
    }

    // Salas têm 8 blocos de altura livre; corredores e portas, 6
    private int alturaLivre(int qx, int qy) {
        return planta.ehCorredor(qx, qy) || planta.ehPorta(qx, qy) ? Andares.ALTURA_DO_CORREDOR : Andares.ALTURA_DA_PAREDE;
    }

    // ---- Portas, estátuas, estantes e barris ----

    private void construirOQueTemEmCima(int qx, int qy) {
        switch (planta.tipo(qx, qy)) {
            case PlantaDoAndar.PORTA -> construirPorta(qx, qy, false);
            case PlantaDoAndar.PORTA_TRANCADA -> construirPorta(qx, qy, true);
            case PlantaDoAndar.PORTA_DE_CRISTAL -> construirPlanoDePorta(qx, qy, ModBlocos.PORTA_DE_CRISTAL.get().defaultBlockState());
            case PlantaDoAndar.BARRICADA -> construirPlanoDePorta(qx, qy, ModBlocos.BARRICADA.get().defaultBlockState());
            case PlantaDoAndar.ESTATUA -> construirEstatua(qx, qy);
            case PlantaDoAndar.ESTANTE -> construirEstante(qx, qy);
            case PlantaDoAndar.BARRIL -> construirBarris(qx, qy);
            case PlantaDoAndar.POTE -> construirPote(qx, qy);
            default -> {
            }
        }
    }

    // A porta gigante fica atravessada no quadradinho, a 1 bloco de um dos lados.
    // Se os vizinhos abertos estão a leste e a oeste, atravessa-se a porta andando no eixo X.
    private void construirPorta(int qx, int qy, boolean trancada) {
        boolean pelaX = planta.passavel(qx - 1, qy) && planta.passavel(qx + 1, qy);
        Direction.Axis eixo = pelaX ? Direction.Axis.X : Direction.Axis.Z;
        PortaGigante porta = ModBlocos.PORTA_GIGANTE.get();
        for (int coluna = 0; coluna < PortaGigante.LARGURA; coluna++) {
            for (int linha = 0; linha < PortaGigante.ALTURA; linha++) {
                colocar(qx, qy, pelaX ? 1 : coluna, 1 + linha, pelaX ? coluna : 1, porta.pedaco(eixo, coluna, linha, trancada));
            }
        }
        // Uma luz fraca do lado da porta (no alto, fora do caminho dela)
        colocar(qx, qy, pelaX ? 3 : 1, Andares.ALTURA_DO_CORREDOR, pelaX ? 1 : 3, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, luzDosCorredores));
        // O controle (coluna 0, linha 0) começa a olhar em volta
        nivel.scheduleTick(controleDaPorta(origem, planta, qx, qy), porta, 4);
    }

    // A porta de cristal e a barricada: uma "parede" fina no lugar da porta gigante, do mesmo tamanho
    private void construirPlanoDePorta(int qx, int qy, BlockState bloco) {
        boolean pelaX = planta.passavel(qx - 1, qy) && planta.passavel(qx + 1, qy);
        for (int coluna = 0; coluna < PortaGigante.LARGURA; coluna++) {
            for (int linha = 0; linha < PortaGigante.ALTURA; linha++) {
                colocar(qx, qy, pelaX ? 1 : coluna, 1 + linha, pelaX ? coluna : 1, bloco);
            }
        }
        colocar(qx, qy, pelaX ? 3 : 1, Andares.ALTURA_DO_CORREDOR, pelaX ? 1 : 3, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, luzDosCorredores));
    }

    // Onde fica o controle da porta do quadradinho (o pedaço da coluna 0, linha 0)
    public static BlockPos controleDaPorta(BlockPos origem, PlantaDoAndar planta, int qx, int qy) {
        boolean pelaX = planta.passavel(qx - 1, qy) && planta.passavel(qx + 1, qy);
        return new BlockPos(origem.getX() + qx * T + (pelaX ? 1 : 0), origem.getY() + 1, origem.getZ() + qy * T + (pelaX ? 0 : 1));
    }

    // A estátua: um pilar de tufo entalhado no meio do quadradinho, do chão ao teto
    private void construirEstatua(int qx, int qy) {
        int altura = alturaLivre(qx, qy);
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = 1; dz <= 2; dz++) {
                for (int dy = 1; dy <= altura; dy++) {
                    colocar(qx, qy, dx, dy, dz, pilar);
                }
            }
        }
    }

    // A estante: livros do chão ao teto, no quadradinho inteiro
    private void construirEstante(int qx, int qy) {
        int altura = alturaLivre(qx, qy);
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                for (int dy = 1; dy <= altura; dy++) {
                    colocar(qx, qy, dx, dy, dz, estante);
                }
            }
        }
    }

    // Os barris de armazenamento: uma pilha de 2 blocos de altura (ninguém pula), com 4 barris em cima no meio
    private void construirBarris(int qx, int qy) {
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                colocar(qx, qy, dx, 1, dz, barril);
                colocar(qx, qy, dx, 2, dz, barril);
                if (dx >= 1 && dx <= 2 && dz >= 1 && dz <= 2) {
                    colocar(qx, qy, dx, 3, dz, barril);
                }
            }
        }
    }

    // O pote de alquimia: 2x2 blocos no meio do quadradinho (cada um é um quarto do caldeirão)
    private void construirPote(int qx, int qy) {
        PoteDeAlquimia pote = ModBlocos.POTE_DE_ALQUIMIA.get();
        for (int parte = 0; parte < 4; parte++) {
            colocar(qx, qy, 1 + PoteDeAlquimia.dx(parte), 1, 1 + PoteDeAlquimia.dz(parte), pote.defaultBlockState().setValue(PoteDeAlquimia.PARTE, parte));
        }
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

    // Lanternas no centro de cada sala e, nas salas grandes, a cada 4 quadradinhos a partir do centro (no andar escuro, nenhuma)
    private boolean temLanterna(int qx, int qy) {
        if (escuro || planta.ehCorredor(qx, qy)) {
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

    // ---- Abismos e fogos ----

    // Embaixo dos vizinhos de um abismo, parede (senão o buraco mostra o vazio dos lados)
    private void construirBeiradasDosAbismos() {
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                if (planta.tipo(qx, qy) == PlantaDoAndar.ABISMO) {
                    continue;
                }
                boolean encosta = false;
                for (int vy = -1; vy <= 1 && !encosta; vy++) {
                    for (int vx = -1; vx <= 1; vx++) {
                        if (planta.dentro(qx + vx, qy + vy) && planta.tipo(qx + vx, qy + vy) == PlantaDoAndar.ABISMO) {
                            encosta = true;
                            break;
                        }
                    }
                }
                if (!encosta) {
                    continue;
                }
                for (int dx = 0; dx < T; dx++) {
                    for (int dz = 0; dz < T; dz++) {
                        colocar(qx, qy, dx, -1, dz, parede);
                        colocar(qx, qy, dx, -2, dz, parede);
                    }
                }
            }
        }
    }

    // O fogo eterno (uma parede de fogo de 3 blocos de altura em cada quadradinho) e o fogo de sacrifício (2x2 no altar)
    private void construirFogos() {
        BlockState fogoEterno = ModBlocos.FOGO_ETERNO.get().defaultBlockState();
        for (int[] lugar : planta.fogoEterno()) {
            for (int dx = 0; dx < T; dx++) {
                for (int dz = 0; dz < T; dz++) {
                    for (int dy = 1; dy <= 3; dy++) {
                        colocar(lugar[0], lugar[1], dx, dy, dz, fogoEterno);
                    }
                }
            }
        }
        int[] altar = planta.fogoDeSacrificio();
        if (altar != null) {
            BlockState fogo = ModBlocos.FOGO_DE_SACRIFICIO.get().defaultBlockState();
            for (int dx = 1; dx <= 2; dx++) {
                for (int dz = 1; dz <= 2; dz++) {
                    colocar(altar[0], altar[1], dx, 1, dz, fogo.setValue(FogoDeSacrificio.CONTROLE, dx == 1 && dz == 1));
                }
            }
            nivel.scheduleTick(controleDoFogoDeSacrificio(origem, altar[0], altar[1]), ModBlocos.FOGO_DE_SACRIFICIO.get(), 20);
        }
        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                if (planta.tipo(qx, qy) == PlantaDoAndar.RESPIRADOURO) {
                    posicao.set(origem.getX() + qx * T, origem.getY(), origem.getZ() + qy * T);
                    nivel.scheduleTick(posicao.immutable(), ModBlocos.RESPIRADOURO.get(), 5);
                }
                if (planta.tipo(qx, qy) == PlantaDoAndar.POCO) {
                    posicao.set(origem.getX() + qx * T + 1, origem.getY() - 1, origem.getZ() + qy * T + 1);
                    nivel.scheduleTick(posicao.immutable(), ModBlocos.FUNDO_DO_POCO.get(), 10);
                }
            }
        }
    }

    // O bloco do fogo de sacrifício que cuida de tudo
    public static BlockPos controleDoFogoDeSacrificio(BlockPos origem, int qx, int qy) {
        return new BlockPos(origem.getX() + qx * T + 1, origem.getY() + 1, origem.getZ() + qy * T + 1);
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
