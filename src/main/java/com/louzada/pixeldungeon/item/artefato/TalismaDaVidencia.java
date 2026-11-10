package com.louzada.pixeldungeon.item.artefato;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.dungeon.Busca;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.item.ArtefatoAchado;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Talismã da vidência (o "TalismanOfForesight" do Shattered): carrega devagar com o tempo até 100
// (0,05 + 0,005 x nível por turno), e 10 por porta escondida e 2 por armadilha escondida que a busca achar.
// Equipado, deixa o herói apreensivo quando tem uma armadilha escondida a até 3 quadradinhos, à vista.
//   vidência (clique direito, mirando): olha num cone pra onde o herói mira, até onde ele mira (no máximo 5 + 2 x nível
//     quadradinhos). O cone abre 200° e fecha 8% por quadradinho de distância; passa pelas portas e para nas paredes.
//     Revela as portas e as armadilhas escondidas e faz os inimigos nele brilharem através das paredes por 5 + 2 x nível turnos.
//     Gasta 3 + 1,08 por quadradinho de distância.
//   sobe de nível (até o 10) com o que a vidência descobre: 100 por porta, 10 por armadilha, 10 por inimigo que o herói
//     não via, 1 por quadradinho que o herói não via (atrás de portas ou longe)
public class TalismaDaVidencia extends ArtefatoAchado {
    private static final int TAMANHO = 4;
    private static final int DISTANCIA_DO_AVISO = 3;

    public TalismaDaVidencia(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "talisma_da_videncia";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 100;
    }

    @Override
    protected int corDaBarra() {
        return 0x66DDFF;
    }

    @Override
    protected float aproveitamentoDeFora() {
        return 2F;
    }

    // Quem já foi avisado da armadilha perto (o aviso só volta depois de não ter mais nenhuma)
    private static final Set<UUID> APREENSIVOS = new HashSet<>();

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        carregar(pilha, (0.05F + nivel(pilha) * 0.005F) * energia(jogador));
        avisarDasArmadilhas(jogador);
    }

    // Armadilha escondida a até 3 quadradinhos, à vista: "Você se sente apreensivo." (uma vez, até ela sair de perto)
    public static boolean avisarDasArmadilhas(ServerPlayer jogador) {
        if (!Busca.armadilhaEscondidaPerto(jogador, DISTANCIA_DO_AVISO)) {
            APREENSIVOS.remove(jogador.getUUID());
            return false;
        }
        if (APREENSIVOS.add(jogador.getUUID())) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.talisma_da_videncia.apreensivo").withStyle(ChatFormatting.GOLD),
                    false);
        }
        return true;
    }

    // A busca achou segredos: 10 de carga por porta e 2 por armadilha
    public static void aoAcharSegredos(ServerPlayer jogador, int portas, int armadilhas) {
        ItemStack pilha = ArtefatoAchado.equipado(jogador, TalismaDaVidencia.class);
        if (pilha != null) {
            definirCargas(pilha, Math.min(100, cargas(pilha) + 10 * portas + 2 * armadilhas));
        }
    }

    public static float distanciaMaxima(ItemStack pilha) {
        return Math.min(5 + 2 * nivel(pilha), (cargas(pilha) - 3) / 1.08F);
    }

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (cargas(pilha) < 5) {
            avisar(jogador, "mensagem.pixeldungeon.talisma_da_videncia.sem_carga");
            return false;
        }
        float maxima = Math.max(2, distanciaMaxima(pilha));
        // Até onde o herói mira (pelo menos 2 quadradinhos, igual ao Shattered)
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getLookAngle();
        Vec3 fim = olho.add(olhar.scale(maxima * TAMANHO));
        BlockHitResult bloco = jogador.level().clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        float distancia = maxima;
        if (bloco.getType() != HitResult.Type.MISS) {
            distancia = (float) (bloco.getLocation().subtract(olho).horizontalDistance() / TAMANHO);
        }
        distancia = Math.max(2, Math.min(maxima, distancia));
        return ver(jogador, pilha, new Vec3(olhar.x, 0, olhar.z), distancia);
    }

    // A vidência numa direção (no chão), até a distância em quadradinhos
    public boolean ver(ServerPlayer jogador, ItemStack pilha, Vec3 direcao, float distancia) {
        float abertura = Math.round(200 * (float) Math.pow(0.92, distancia));
        Busca.Videncia vista = Busca.vidente(jogador, direcao, distancia, abertura);
        if (vista == null) {
            avisar(jogador, "mensagem.pixeldungeon.talisma_da_videncia.fora");
            return false;
        }
        int ganhou = vista.portas() * 100 + vista.armadilhas() * 10 + vista.novos();
        int turnos = 5 + 2 * nivel(pilha);
        double alcance = (distancia + 1) * TAMANHO;
        for (MobDoShattered inimigo : jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(alcance, 8, alcance))) {
            if (inimigo.aliado() || !vista.contem(inimigo.getX(), inimigo.getZ())) {
                continue;
            }
            if (!jogador.hasLineOfSight(inimigo)) {
                ganhou += 10;
            }
            inimigo.addEffect(new MobEffectInstance(MobEffects.GLOWING, Turno.ticks(turnos), 0, false, false));
        }
        // O custo: 3 + 1,08 por quadradinho (a parte quebrada sai da carga parcial)
        float custo = 3 + distancia * 1.08F;
        float sobra = cargas(pilha) + parcial(pilha) - custo;
        definirCargas(pilha, (int) Math.max(0, Math.floor(sobra)));
        definirParcial(pilha, sobra > 0 ? sobra - (float) Math.floor(sobra) : 0);
        definirExperiencia(pilha, experiencia(pilha) + ganhou);
        int precisa = 100 + 50 * nivel(pilha);
        if (experiencia(pilha) >= precisa && nivel(pilha) < nivelMaximo()) {
            definirExperiencia(pilha, experiencia(pilha) - precisa);
            subirNivel(jogador, pilha);
        }
        Furtividade.revelar(jogador);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }
}
