package com.louzada.pixeldungeon.item.artefato;

import java.util.List;

import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EfeitosDeVarinha;
import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Chave de crânio (o "SkeletonKey" do Shattered): vem cheia e recarrega com o tempo
// (1 carga a cada 120 turnos cheia, 60 vazia). Cargas: 3 + nível/2.
//   numa porta trancada (clique direito nela): 1 carga, destranca
//   numa porta comum: 2 cargas, tranca ("trancada pelo herói": nenhum inimigo passa; quem estiver nela sai empurrado).
//     A porta trancada pelo herói abre de graça com a chave.
//   em outro lugar (clique direito, mirando): 2 cargas, levanta paredes espectrais por 10 turnos na frente do herói
//     (3 quadradinhos; 5 na diagonal), empurrando os inimigos que estiverem nelas
//   sobe de nível (até o 10) usando: 2 de experiência por uso (mais 1 destrancando); precisa de mais de 4 + nível
public class ChaveDeCranio extends ArtefatoAchado {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final int ALTURA_DA_PAREDE = 6;
    // Os 8 quadradinhos em volta, em volta do relógio a partir do noroeste (igual ao Shattered): os pares são as diagonais
    private static final int[][] EM_VOLTA = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 } };

    public ChaveDeCranio(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "chave_de_cranio";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 3 + nivel(pilha) / 2;
    }

    @Override
    protected int cargasIniciais(ItemStack pilha) {
        return cargasMaximas(pilha);
    }

    @Override
    protected int corDaBarra() {
        return 0xDDDDCC;
    }

    @Override
    protected float aproveitamentoDeFora() {
        return 0.133F;
    }

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        int faltam = cargasMaximas(pilha) - cargas(pilha);
        if (faltam > 0) {
            carregar(pilha, energia(jogador) / (120F - faltam * 7.5F));
        }
    }

    private void ganharExperiencia(ServerPlayer jogador, ItemStack pilha, int quanto) {
        if (nivel(pilha) >= nivelMaximo()) {
            return;
        }
        definirExperiencia(pilha, experiencia(pilha) + quanto);
        if (experiencia(pilha) > 4 + nivel(pilha)) {
            definirExperiencia(pilha, experiencia(pilha) - (4 + nivel(pilha)));
            subirNivel(jogador, pilha);
        }
    }

    // ---- Nas portas ----

    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        BlockState estado = contexto.getLevel().getBlockState(contexto.getClickedPos());
        if (!(estado.getBlock() instanceof PortaGigante)) {
            return InteractionResult.PASS;
        }
        if (!(contexto.getPlayer() instanceof ServerPlayer jogador)) {
            return InteractionResult.SUCCESS;
        }
        if (!GerenciadorDoHeroi.emJogo(jogador)) {
            return InteractionResult.PASS;
        }
        BlockPos controle = PortaGigante.controleDe(estado, contexto.getClickedPos());
        return usarNaPorta(jogador, contexto.getItemInHand(), controle) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public boolean usarNaPorta(ServerPlayer jogador, ItemStack pilha, BlockPos controle) {
        ServerLevel mundo = jogador.serverLevel();
        BlockState estado = mundo.getBlockState(controle);
        if (!(estado.getBlock() instanceof PortaGigante porta)) {
            return false;
        }
        if (estado.getValue(PortaGigante.TRANCADA) && estado.getValue(PortaGigante.DO_HEROI)) {
            // Trancada pelo herói: abre de graça
            porta.destrancar(mundo, controle);
            return true;
        }
        if (estado.getValue(PortaGigante.TRANCADA)) {
            if (cargas(pilha) < 1) {
                avisar(jogador, "mensagem.pixeldungeon.chave_de_cranio.sem_carga_ferro");
                return false;
            }
            porta.destrancar(mundo, controle);
            definirCargas(pilha, cargas(pilha) - 1);
            ganharExperiencia(jogador, pilha, 3);
            Furtividade.revelar(jogador);
            return true;
        }
        if (cargas(pilha) < 2) {
            avisar(jogador, "mensagem.pixeldungeon.chave_de_cranio.sem_carga_trancar");
            return false;
        }
        if (!esvaziarAPorta(jogador, controle, estado.getValue(PortaGigante.EIXO))) {
            avisar(jogador, "mensagem.pixeldungeon.chave_de_cranio.sem_espaco");
            return false;
        }
        porta.trancarPeloHeroi(mundo, controle);
        definirCargas(pilha, cargas(pilha) - 2);
        ganharExperiencia(jogador, pilha, 2);
        Furtividade.revelar(jogador);
        return true;
    }

    // Quem está no vão da porta sai pro lado de lá (longe do herói). Devolve false se alguém não tiver pra onde ir.
    private static boolean esvaziarAPorta(ServerPlayer jogador, BlockPos controle, Direction.Axis eixo) {
        ServerLevel mundo = jogador.serverLevel();
        AABB espaco = PortaGigante.espacoDaPorta(controle, eixo);
        List<LivingEntity> dentro = mundo.getEntitiesOfClass(LivingEntity.class, espaco, viva -> viva.isAlive() && !viva.isSpectator());
        Vec3 meio = espaco.getCenter();
        for (LivingEntity viva : dentro) {
            double heroiDoLado = eixo == Direction.Axis.X ? jogador.getX() - meio.x : jogador.getZ() - meio.z;
            double sinal = heroiDoLado > 0 ? -1 : 1;
            if (viva == jogador) {
                sinal = -sinal;
            }
            Vec3 empurrao = eixo == Direction.Axis.X ? new Vec3(sinal * 2.5, 0, 0) : new Vec3(0, 0, sinal * 2.5);
            Vec3 destino = viva.position().add(empurrao);
            if (!mundo.noCollision(viva, viva.getBoundingBox().move(empurrao)) || viva.getBoundingBox().move(empurrao).intersects(espaco)) {
                return false;
            }
            viva.teleportTo(destino.x, destino.y, destino.z);
        }
        return true;
    }

    // ---- As paredes ----

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (cargas(pilha) < 2) {
            avisar(jogador, "mensagem.pixeldungeon.chave_de_cranio.sem_carga_parede");
            return false;
        }
        Vec3 olhar = jogador.getLookAngle();
        return levantarParedes(jogador, pilha, olhar.x, olhar.z);
    }

    // Levanta as paredes na direção (no chão) mais perto da que foi pedida
    public boolean levantarParedes(ServerPlayer jogador, ItemStack pilha, double dx, double dz) {
        int melhor = 0;
        double melhorProduto = -2;
        double tamanho = Math.max(1e-6, Math.sqrt(dx * dx + dz * dz));
        for (int i = 0; i < EM_VOLTA.length; i++) {
            double ox = EM_VOLTA[i][0];
            double oz = EM_VOLTA[i][1];
            double produto = (ox * dx + oz * dz) / (tamanho * Math.sqrt(ox * ox + oz * oz));
            if (produto > melhorProduto) {
                melhorProduto = produto;
                melhor = i;
            }
        }
        ServerLevel mundo = jogador.serverLevel();
        int qx = Math.floorDiv(jogador.getBlockX(), T);
        int qz = Math.floorDiv(jogador.getBlockZ(), T);
        int pes = jogador.getBlockY();
        int[] direcao = EM_VOLTA[melhor];
        if (!livre(mundo, qx + direcao[0], qz + direcao[1], pes)) {
            avisar(jogador, "mensagem.pixeldungeon.chave_de_cranio.lugar_invalido");
            return false;
        }
        int antes = (melhor + 7) % 8;
        int depois = (melhor + 1) % 8;
        Vec3 empurrao = new Vec3(direcao[0], 0, direcao[1]).normalize();
        parede(jogador, qx + direcao[0], qz + direcao[1], pes, empurrao);
        parede(jogador, qx + EM_VOLTA[antes][0], qz + EM_VOLTA[antes][1], pes, empurrao);
        parede(jogador, qx + EM_VOLTA[depois][0], qz + EM_VOLTA[depois][1], pes, empurrao);
        if (melhor % 2 == 0) {
            parede(jogador, qx + 2 * EM_VOLTA[antes][0], qz + 2 * EM_VOLTA[antes][1], pes, empurrao);
            parede(jogador, qx + 2 * EM_VOLTA[depois][0], qz + 2 * EM_VOLTA[depois][1], pes, empurrao);
        }
        definirCargas(pilha, cargas(pilha) - 2);
        ganharExperiencia(jogador, pilha, 2);
        Furtividade.revelar(jogador);
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.4F);
        return true;
    }

    // O quadradinho não é parede (tem espaço pra ficar em pé na altura dos pés)
    private static boolean livre(ServerLevel mundo, int qx, int qz, int pes) {
        BlockPos meio = new BlockPos(qx * T + T / 2, pes, qz * T + T / 2);
        return podeVirarParede(mundo.getBlockState(meio)) || mundo.getBlockState(meio).is(ModBlocos.PAREDE_DA_CHAVE.get());
    }

    // Só o que não é sólido vira parede (o ar, a grama, o musgo); portas e plantas ficam
    private static boolean podeVirarParede(BlockState estado) {
        return estado.isAir() || estado.is(ModBlocos.CAPIM_ALTO.get()) || estado.is(ModBlocos.GRAMA_BAIXA.get()) || estado.is(ModBlocos.MUSGO_ESGOTO.get());
    }

    // Uma parede num quadradinho inteiro (do chão até 6 blocos de altura), empurrando os inimigos que estiverem nele
    private static void parede(ServerPlayer jogador, int qx, int qz, int pes, Vec3 empurrao) {
        ServerLevel mundo = jogador.serverLevel();
        if (!livre(mundo, qx, qz, pes)) {
            return;
        }
        AABB quadradinho = new AABB(qx * T, pes, qz * T, qx * T + T, pes + ALTURA_DA_PAREDE, qz * T + T);
        for (MobDoShattered inimigo : mundo.getEntitiesOfClass(MobDoShattered.class, quadradinho, viva -> viva.isAlive() && !viva.aliado())) {
            EfeitosDeVarinha.empurrar(jogador, inimigo, empurrao, 1, false);
        }
        BlockState parede = ModBlocos.PAREDE_DA_CHAVE.get().defaultBlockState();
        for (int x = 0; x < T; x++) {
            for (int z = 0; z < T; z++) {
                for (int y = 0; y < ALTURA_DA_PAREDE; y++) {
                    BlockPos aqui = new BlockPos(qx * T + x, pes + y, qz * T + z);
                    BlockState agora = mundo.getBlockState(aqui);
                    if (!podeVirarParede(agora)) {
                        if (y > 0) {
                            break;
                        }
                        continue;
                    }
                    if (!mundo.getEntitiesOfClass(LivingEntity.class, new AABB(aqui), viva -> viva.isAlive() && !viva.isSpectator()).isEmpty()) {
                        continue;
                    }
                    mundo.setBlock(aqui, parede, 3);
                }
            }
        }
        mundo.sendParticles(ParticleTypes.END_ROD, qx * T + T / 2.0, pes + 1.5, qz * T + T / 2.0, 12, 1.2, 1.0, 1.2, 0.01);
    }
}
