package com.louzada.pixeldungeon.dungeon;

import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

// Transforma a planta de um andar em blocos de verdade no mundo.
//
// Corte de um quadradinho aberto (de baixo pra cima):
//   y + 9      teto (parede)
//   y + 8      luz invisível
//   y + 1 a 8  espaço livre (8 blocos)
//   y          chão (ou água, com um chão embaixo)
public class ConstrutorDeAndar {
    // Atualiza só os jogadores, sem avisar os blocos vizinhos (bem mais rápido)
    private static final int ATUALIZACAO = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // Força da luz em cada quadradinho (0 a 15)
    private static final int FORCA_DA_LUZ = 11;

    public static void construir(ServerLevel nivel, PlantaDoAndar planta, BlockPos origem) {
        BlockState parede = ModBlocos.PAREDE_ESGOTO.get().defaultBlockState();
        BlockState chao = ModBlocos.CHAO_ESGOTO.get().defaultBlockState();
        BlockState agua = Blocks.WATER.defaultBlockState();
        BlockState luz = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, FORCA_DA_LUZ);
        BlockState escadaSubida = ModBlocos.ESCADA_SUBIDA.get().defaultBlockState();
        BlockState escadaDescida = ModBlocos.ESCADA_DESCIDA.get().defaultBlockState();

        int tamanho = Andares.TAMANHO_DO_QUADRADINHO;
        int alturaDoTeto = Andares.ALTURA_DA_PAREDE + 1;
        int yChao = origem.getY();
        BlockPos.MutableBlockPos posicao = new BlockPos.MutableBlockPos();

        for (int qy = 0; qy < planta.altura(); qy++) {
            for (int qx = 0; qx < planta.largura(); qx++) {
                byte tipo = planta.tipo(qx, qy);
                int x0 = origem.getX() + qx * tamanho;
                int z0 = origem.getZ() + qy * tamanho;

                if (tipo == PlantaDoAndar.PAREDE) {
                    // Parede só onde encosta em lugar aberto; o resto fica vazio (ninguém vê mesmo)
                    if (!planta.encostaEmAberto(qx, qy)) {
                        continue;
                    }
                    for (int dx = 0; dx < tamanho; dx++) {
                        for (int dz = 0; dz < tamanho; dz++) {
                            for (int dy = 0; dy <= alturaDoTeto; dy++) {
                                nivel.setBlock(posicao.set(x0 + dx, yChao + dy, z0 + dz), parede, ATUALIZACAO);
                            }
                        }
                    }
                    continue;
                }

                // Quadradinho aberto: chão (ou água) e teto
                for (int dx = 0; dx < tamanho; dx++) {
                    for (int dz = 0; dz < tamanho; dz++) {
                        if (tipo == PlantaDoAndar.AGUA) {
                            nivel.setBlock(posicao.set(x0 + dx, yChao - 1, z0 + dz), chao, ATUALIZACAO);
                            nivel.setBlock(posicao.set(x0 + dx, yChao, z0 + dz), agua, ATUALIZACAO);
                        } else {
                            nivel.setBlock(posicao.set(x0 + dx, yChao, z0 + dz), chao, ATUALIZACAO);
                        }
                        nivel.setBlock(posicao.set(x0 + dx, yChao + alturaDoTeto, z0 + dz), parede, ATUALIZACAO);
                    }
                }
                nivel.setBlock(posicao.set(x0 + 1, yChao + Andares.ALTURA_DA_PAREDE, z0 + 1), luz, ATUALIZACAO);

                // Escadas ficam no bloco (1, 1) do quadradinho; quem chega fica no bloco (2, 2), do lado
                if (tipo == PlantaDoAndar.ENTRADA) {
                    nivel.setBlock(posicao.set(x0 + 1, yChao + 1, z0 + 1), escadaSubida, ATUALIZACAO);
                } else if (tipo == PlantaDoAndar.SAIDA) {
                    // O alçapão fica no lugar do chão, nivelado
                    nivel.setBlock(posicao.set(x0 + 1, yChao, z0 + 1), escadaDescida, ATUALIZACAO);
                }
            }
        }
    }

    // Bloco onde o jogador aparece ao chegar num quadradinho (do lado da escada)
    public static BlockPos pontoDeChegada(BlockPos origem, int qx, int qy) {
        int tamanho = Andares.TAMANHO_DO_QUADRADINHO;
        return new BlockPos(origem.getX() + qx * tamanho + 2, origem.getY() + 1, origem.getZ() + qy * tamanho + 2);
    }
}
