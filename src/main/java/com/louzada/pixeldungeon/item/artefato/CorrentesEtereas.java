package com.louzada.pixeldungeon.item.artefato;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Correntes etéreas (o "EtherealChains" do Shattered): vêm com 5 cargas e recarregam com o tempo até 5 + 2 x nível
// (1 carga a cada 40 turnos, mais rápido quanto mais vazias); ganhar experiência também carrega (e passa desse limite).
//   lançar (clique direito, mirando): num inimigo, puxa ele pra perto do herói; numa parede, puxa o herói até lá.
//     Gasta 1 carga por quadradinho (4 blocos) de distância.
//   sobe de nível com a experiência do herói (até o 5)
public class CorrentesEtereas extends ArtefatoAchado {
    private static final double ALCANCE = 48;
    private static final int QUADRADINHO = 4;

    public CorrentesEtereas(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "correntes_etereas";
    }

    @Override
    public int nivelMaximo() {
        return 5;
    }

    // O limite de recarga com o tempo (a experiência passa dele)
    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 5 + 2 * nivel(pilha);
    }

    @Override
    protected int cargasIniciais(ItemStack pilha) {
        return 5;
    }

    @Override
    protected int corDaBarra() {
        return 0x99CCFF;
    }

    // Soma sem o limite (o resto quebrado fica guardado)
    private static void somar(ItemStack pilha, float quanto) {
        float parcial = parcial(pilha) + quanto;
        int cargas = cargas(pilha);
        while (parcial >= 1) {
            parcial--;
            cargas++;
        }
        definirCargas(pilha, cargas);
        definirParcial(pilha, parcial);
    }

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        int alvo = cargasMaximas(pilha);
        if (cargas(pilha) < alvo) {
            somar(pilha, energia(jogador) / (40F - (alvo - cargas(pilha)) * 2F));
        }
    }

    // Recarga de fora: metade de carga por turno, até o dobro do limite
    @Override
    public void recarregarDeFora(ServerPlayer jogador, ItemStack pilha, float quanto) {
        if (cargas(pilha) < cargasMaximas(pilha) * 2) {
            somar(pilha, 0.5F * quanto);
        }
    }

    @Override
    public void aoGanharXp(ServerPlayer jogador, ItemStack pilha, float porcao) {
        if (porcao <= 0) {
            return;
        }
        definirExperiencia(pilha, experiencia(pilha) + Math.round(porcao * 100));
        // Passando do limite, a experiência carrega mais devagar
        int limite = cargasMaximas(pilha);
        if (cargas(pilha) > limite) {
            porcao *= (float) limite / cargas(pilha);
        }
        somar(pilha, porcao * 6);
        int precisa = 100 + nivel(pilha) * 100;
        if (experiencia(pilha) > precisa && nivel(pilha) < nivelMaximo()) {
            definirExperiencia(pilha, experiencia(pilha) - precisa);
            subirNivel(jogador, pilha);
        }
    }

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (cargas(pilha) < 1) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.sem_carga");
            return false;
        }
        ServerLevel mundo = jogador.serverLevel();
        Vec3 olho = jogador.getEyePosition();
        Vec3 fim = olho.add(jogador.getLookAngle().scale(ALCANCE));
        BlockHitResult bloco = mundo.clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        Vec3 ate = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult criatura = ProjectileUtil.getEntityHitResult(mundo, jogador, olho, ate, new AABB(olho, ate).inflate(1.0),
                entidade -> entidade instanceof LivingEntity viva && viva.isAlive() && entidade != jogador && !entidade.isSpectator(), 0.3F);
        if (criatura != null) {
            return puxarInimigo(jogador, pilha, criatura.getEntity());
        }
        if (bloco.getType() == HitResult.Type.BLOCK) {
            return puxarHeroi(jogador, pilha, bloco) != null;
        }
        return false;
    }

    // Puxa a criatura pro primeiro lugar livre no caminho, saindo do herói
    public boolean puxarInimigo(ServerPlayer jogador, ItemStack pilha, Entity alvo) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 inicio = jogador.position();
        Vec3 direcao = alvo.position().subtract(inicio);
        double distancia = direcao.horizontalDistance();
        if (distancia < 2) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.nada");
            return false;
        }
        Vec3 passo = new Vec3(direcao.x / distancia, 0, direcao.z / distancia);
        Vec3 destino = null;
        for (double andado = 1.5; andado < distancia - 0.5; andado += 0.5) {
            Vec3 lugar = inicio.add(passo.scale(andado));
            Vec3 noChao = noChao(mundo, alvo, lugar);
            if (noChao != null) {
                destino = noChao;
                break;
            }
        }
        if (destino == null) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.nada");
            return false;
        }
        int custo = quadradinhos(alvo.position(), destino);
        if (custo > cargas(pilha)) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.sem_carga");
            return false;
        }
        rastro(mundo, jogador.position(), alvo.position());
        alvo.teleportTo(destino.x, destino.y, destino.z);
        alvo.setDeltaMovement(Vec3.ZERO);
        gastar(jogador, pilha, custo);
        if (alvo instanceof MobDoShattered inimigo) {
            inimigo.setTarget(jogador);
        }
        return true;
    }

    // Puxa o herói até a parede (tem que ter uma parede do lado de onde ele vai parar). Devolve onde ele parou (null se não deu).
    @Nullable
    public Vec3 puxarHeroi(ServerPlayer jogador, ItemStack pilha, BlockHitResult bloco) {
        if (jogador.hasEffect(ModEfeitos.ENRAIZADO)) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.enraizado");
            return null;
        }
        ServerLevel mundo = jogador.serverLevel();
        BlockPos fora = bloco.getBlockPos().relative(bloco.getDirection());
        Vec3 lugar = Vec3.atBottomCenterOf(fora);
        Vec3 destino = noChao(mundo, jogador, lugar);
        if (destino == null) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.na_parede");
            return null;
        }
        if (!temParedePerto(mundo, BlockPos.containing(destino))) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.nada_pra_agarrar");
            return null;
        }
        int custo = quadradinhos(jogador.position(), destino);
        if (custo > cargas(pilha)) {
            avisar(jogador, "mensagem.pixeldungeon.correntes_etereas.sem_carga");
            return null;
        }
        rastro(mundo, jogador.position(), destino);
        jogador.teleportTo(destino.x, destino.y, destino.z);
        jogador.setDeltaMovement(Vec3.ZERO);
        jogador.fallDistance = 0;
        gastar(jogador, pilha, custo);
        return destino;
    }

    private void gastar(ServerPlayer jogador, ItemStack pilha, int custo) {
        definirCargas(pilha, cargas(pilha) - custo);
        Furtividade.revelar(jogador);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // Quantos quadradinhos do Shattered entre os dois lugares (pelo menos 1)
    public static int quadradinhos(Vec3 de, Vec3 para) {
        double maior = Math.max(Math.abs(de.x - para.x), Math.abs(de.z - para.z));
        return Math.max(1, (int) Math.round(maior / QUADRADINHO));
    }

    // Um lugar onde a criatura cabe em pé, descendo até 3 blocos até achar o chão
    @Nullable
    private static Vec3 noChao(ServerLevel mundo, Entity quem, Vec3 lugar) {
        for (int desce = 0; desce <= 3; desce++) {
            Vec3 tentativa = lugar.subtract(0, desce, 0);
            AABB caixa = quem.getDimensions(quem.getPose()).makeBoundingBox(tentativa);
            if (!mundo.noCollision(quem, caixa)) {
                return null;
            }
            if (!mundo.noCollision(quem, caixa.move(0, -0.1, 0))) {
                return tentativa;
            }
        }
        return null;
    }

    // Igual ao Shattered: precisa de uma parede em volta (aqui, até um quadradinho de distância, na altura do corpo)
    private static boolean temParedePerto(ServerLevel mundo, BlockPos pes) {
        for (Direction lado : Direction.Plane.HORIZONTAL) {
            for (int passo = 1; passo <= QUADRADINHO; passo++) {
                BlockPos perto = pes.above().relative(lado, passo);
                if (!mundo.getBlockState(perto).getCollisionShape(mundo, perto).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void rastro(ServerLevel mundo, Vec3 de, Vec3 para) {
        Vec3 meio = para.subtract(de);
        int pontos = (int) Math.max(2, meio.length() * 2);
        for (int i = 0; i <= pontos; i++) {
            Vec3 ponto = de.add(meio.scale((double) i / pontos)).add(0, 1, 0);
            mundo.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ponto.x, ponto.y, ponto.z, 1, 0, 0, 0, 0);
        }
        Particulas.explosao(mundo, ParticleTypes.SOUL, para.x, para.y + 1, para.z, 6, 0.3, 0.02);
    }
}
