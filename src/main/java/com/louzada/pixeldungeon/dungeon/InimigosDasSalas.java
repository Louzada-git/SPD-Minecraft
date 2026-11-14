package com.louzada.pixeldungeon.dungeon;

import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.InimigoNaPlanta;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.EstatuaAnimada;
import com.louzada.pixeldungeon.entidade.Piranha;
import com.louzada.pixeldungeon.entidade.Sentinela;
import com.louzada.pixeldungeon.item.PoteQuebrado;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Os moradores das salas especiais, que nascem junto com o andar: as piranhas da piscina, a estátua animada, a sentinela
// e a abelha do esconderijo do mel (com o pote quebrado dela)
public final class InimigosDasSalas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    private InimigosDasSalas() {
    }

    public static void criar(ServerLevel mundo, PlantaDoAndar planta, BlockPos origem, int andar, long semente) {
        Random sorteio = new Random(semente * 53 + andar * 13L);
        double multiplicador = PopulacaoDoAndar.multiplicadorDeVida(mundo.getServer());
        int criados = 0;
        for (InimigoNaPlanta inimigo : planta.inimigos()) {
            double x = origem.getX() + inimigo.x() * T + T / 2.0;
            double z = origem.getZ() + inimigo.y() * T + T / 2.0;
            switch (inimigo.tipo()) {
                case PIRANHA -> {
                    Piranha piranha = ModEntidades.PIRANHA.get().create(mundo);
                    if (piranha == null) {
                        continue;
                    }
                    piranha.moveTo(x, origem.getY() + 0.05, z, sorteio.nextFloat() * 360.0F, 0.0F);
                    piranha.definirAndar(andar);
                    piranha.ajustarVida(mundo, multiplicador);
                    piranha.setPersistenceRequired();
                    mundo.addFreshEntity(piranha);
                    piranha.dormir();
                }
                case ESTATUA_ANIMADA -> {
                    EstatuaAnimada estatua = ModEntidades.ESTATUA_ANIMADA.get().create(mundo);
                    if (estatua == null) {
                        continue;
                    }
                    estatua.moveTo(x, origem.getY() + 1, z, giroPara(inimigo, planta, x, z, origem), 0.0F);
                    estatua.preparar(andar, sorteio);
                    estatua.ajustarVida(mundo, multiplicador);
                    estatua.setPersistenceRequired();
                    mundo.addFreshEntity(estatua);
                }
                case ABELHA -> {
                    Abelha abelha = ModEntidades.ABELHA.get().create(mundo);
                    if (abelha == null) {
                        continue;
                    }
                    Vec3 lugar = new Vec3(x, origem.getY() + 1, z);
                    int id = 1 + sorteio.nextInt(Integer.MAX_VALUE - 1);
                    abelha.moveTo(x, origem.getY() + 1.5, z, sorteio.nextFloat() * 360.0F, 0.0F);
                    abelha.definirNivel(andar);
                    abelha.definirPote(id, lugar, null);
                    abelha.ajustarVida(mundo, multiplicador);
                    abelha.setPersistenceRequired();
                    mundo.addFreshEntity(abelha);
                    ItemEntity pote = new ItemEntity(mundo, lugar.x, lugar.y, lugar.z, PoteQuebrado.criar(id));
                    pote.setDeltaMovement(Vec3.ZERO);
                    pote.setUnlimitedLifetime();
                    mundo.addFreshEntity(pote);
                }
                case SENTINELA -> {
                    Sentinela sentinela = ModEntidades.SENTINELA.get().create(mundo);
                    Sala sala = inimigo.sala();
                    if (sentinela == null || sala == null) {
                        continue;
                    }
                    AABB area = new AABB(origem.getX() + sala.x() * T, origem.getY(), origem.getZ() + sala.y() * T,
                            origem.getX() + (sala.x() + sala.largura()) * T, origem.getY() + Andares.ALTURA_DA_PAREDE + 1,
                            origem.getZ() + (sala.y() + sala.altura()) * T);
                    float giro = giroPara(inimigo, planta, x, z, origem);
                    sentinela.moveTo(x, origem.getY() + 1, z, giro, 0.0F);
                    sentinela.setYHeadRot(giro);
                    sentinela.setYBodyRot(giro);
                    sentinela.preparar(andar, inimigo.extra(), area);
                    mundo.addFreshEntity(sentinela);
                }
            }
            criados++;
        }
        if (criados > 0) {
            PixelDungeon.LOGGER.info("Andar {}: {} moradores de salas especiais", andar, criados);
        }
    }

    // Virado pro meio da sala dele
    private static float giroPara(InimigoNaPlanta inimigo, PlantaDoAndar planta, double x, double z, BlockPos origem) {
        Sala sala = inimigo.sala() != null ? inimigo.sala() : planta.salaEm(inimigo.x(), inimigo.y());
        if (sala == null) {
            return 0;
        }
        double mx = origem.getX() + (sala.x() + sala.largura() / 2.0) * T;
        double mz = origem.getZ() + (sala.y() + sala.altura() / 2.0) * T;
        return (float) (Mth.atan2(mz - z, mx - x) * Mth.RAD_TO_DEG) - 90.0F;
    }
}
