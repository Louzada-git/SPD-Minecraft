package com.louzada.pixeldungeon.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.bloco.ArmadilhaBloco;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.FantasmaAliado;
import com.louzada.pixeldungeon.entidade.FantasmaTriste;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// As armadilhas do Shattered no mundo (o "Trap" e o "pressCell" do Shattered). Quem pisa no quadradinho de uma armadilha:
//   - o herói ativa qualquer uma, até as escondidas ("Uma armadilha ... oculta ativa!")
//   - os inimigos (e os aliados) só ativam as que estão à vista, e desviam delas quando dá
//   - quem voa (o enxame, os fantasmas, o herói levitando) não ativa nada
//   - um item que cai em cima (arremessado, ou o saque de quem morreu ali) ativa qualquer uma
//   - com o tempo parado (ampulheta), a armadilha aparece mas só ativa quando o tempo voltar
// Ao ativar, a armadilha fica à vista e (quase todas) desarmada; o efeito de cada uma está no EfeitosDasArmadilhas.
// Cada armadilha ativa uma vez por chegada: quem fica parado em cima de uma agarradora não é agarrado de novo.
public class Armadilhas {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    // Quem estava em cima de cada armadilha na última olhada (pra só ativar quando alguém chega)
    private static final Map<Long, Set<UUID>> PRESENTES = new HashMap<>();
    // As pisadas com o tempo parado: a armadilha e quem pisou
    private static final Map<Long, UUID> ADIADAS = new HashMap<>();

    // ---- Onde ficam ----

    // O controle (o pedaço da coluna 0, linha 0) da armadilha do quadradinho (qx, qy): no chão, no canto do quadradinho
    public static BlockPos controle(BlockPos origem, int qx, int qy) {
        return new BlockPos(origem.getX() + qx * T, origem.getY(), origem.getZ() + qy * T);
    }

    // O meio do quadradinho, em cima do chão
    public static Vec3 centro(BlockPos controle) {
        return new Vec3(controle.getX() + T / 2.0, controle.getY() + 1, controle.getZ() + T / 2.0);
    }

    // Os 16 blocos de chão viram a armadilha (escondida só se ela for das que se escondem)
    public static void colocar(ServerLevel mundo, BlockPos controle, TipoDeArmadilha tipo, boolean escondida) {
        ArmadilhaBloco bloco = ModBlocos.ARMADILHA.get();
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                mundo.setBlock(controle.offset(dx, 0, dz), bloco.pedaco(tipo, escondida && tipo.escondivel(), false, dx, dz), Block.UPDATE_CLIENTS);
            }
        }
        esquecer(controle);
        mundo.scheduleTick(controle, bloco, ArmadilhaBloco.INTERVALO);
    }

    @Nullable
    public static BlockState estado(ServerLevel mundo, BlockPos controle) {
        BlockState estado = mundo.getBlockState(controle);
        return estado.getBlock() instanceof ArmadilhaBloco ? estado : null;
    }

    // Tem uma armadilha escondida nesse quadradinho do andar?
    public static boolean escondidaEm(ServerLevel mundo, BlockPos origem, int qx, int qy) {
        BlockState estado = estado(mundo, controle(origem, qx, qy));
        return estado != null && estado.getValue(ArmadilhaBloco.ESCONDIDA);
    }

    // ---- Mudar ----

    private static void mudar(ServerLevel mundo, BlockPos controle, boolean escondida, boolean desarmada) {
        BlockState estado = estado(mundo, controle);
        if (estado == null) {
            return;
        }
        ArmadilhaBloco bloco = (ArmadilhaBloco) estado.getBlock();
        TipoDeArmadilha tipo = estado.getValue(ArmadilhaBloco.TIPO);
        for (int dx = 0; dx < T; dx++) {
            for (int dz = 0; dz < T; dz++) {
                BlockPos pedaco = controle.offset(dx, 0, dz);
                if (mundo.getBlockState(pedaco).getBlock() instanceof ArmadilhaBloco) {
                    mundo.setBlock(pedaco, bloco.pedaco(tipo, escondida, desarmada, dx, dz), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // Fica à vista (alguém achou procurando, pisou, leu o mapeamento...). Devolve true se estava escondida.
    public static boolean revelar(ServerLevel mundo, BlockPos controle) {
        BlockState estado = estado(mundo, controle);
        if (estado == null || !estado.getValue(ArmadilhaBloco.ESCONDIDA)) {
            return false;
        }
        mudar(mundo, controle, false, estado.getValue(ArmadilhaBloco.DESARMADA));
        return true;
    }

    // Não ativa mais (fica com o desenho preto)
    public static void desarmar(ServerLevel mundo, BlockPos controle) {
        mudar(mundo, controle, false, true);
        esquecer(controle);
    }

    public static void esquecer(BlockPos controle) {
        PRESENTES.remove(controle.asLong());
        ADIADAS.remove(controle.asLong());
    }

    // ---- Quem pisa ----

    // De 2 em 2 ticks, o controle olha quem está em cima do quadradinho; quem acabou de chegar pode ativar.
    // Na primeira olhada (o andar acabou de ser construído, ou o servidor ligou de novo), os itens que já estavam ali não ativam.
    public static void olhar(ServerLevel mundo, BlockPos controle) {
        BlockState estado = estado(mundo, controle);
        if (estado == null || estado.getValue(ArmadilhaBloco.DESARMADA)) {
            return;
        }
        long chave = controle.asLong();
        UUID adiada = ADIADAS.get(chave);
        if (adiada != null) {
            Entity quem = mundo.getEntity(adiada);
            if (!(quem instanceof LivingEntity vivo) || !vivo.hasEffect(ModEfeitos.TEMPO_PARADO)) {
                ADIADAS.remove(chave);
                acionar(mundo, controle, quem);
                return;
            }
        }
        boolean escondida = estado.getValue(ArmadilhaBloco.ESCONDIDA);
        Set<UUID> antes = PRESENTES.get(chave);
        Set<UUID> agora = new HashSet<>();
        List<Entity> pisaram = new ArrayList<>();
        AABB area = new AABB(controle.getX(), controle.getY() + 0.5, controle.getZ(), controle.getX() + T, controle.getY() + 2.5,
                controle.getZ() + T);
        for (Entity entidade : mundo.getEntities((Entity) null, area, e -> e instanceof LivingEntity || e instanceof ItemEntity)) {
            if (!emCima(entidade, controle)) {
                continue;
            }
            agora.add(entidade.getUUID());
            boolean chegou = antes == null ? !(entidade instanceof ItemEntity) : !antes.contains(entidade.getUUID());
            if (chegou && aciona(entidade, escondida)) {
                pisaram.add(entidade);
            }
        }
        PRESENTES.put(chave, agora);
        for (Entity quem : pisaram) {
            BlockState agoraEstado = estado(mundo, controle);
            if (agoraEstado == null || agoraEstado.getValue(ArmadilhaBloco.DESARMADA)) {
                break;
            }
            pisar(mundo, controle, quem);
        }
    }

    // Está em cima do quadradinho da armadilha (o meio dele, em pé no chão ou pulando)
    public static boolean emCima(Entity entidade, BlockPos controle) {
        double dx = entidade.getX() - controle.getX();
        double dz = entidade.getZ() - controle.getZ();
        double dy = entidade.getY() - controle.getY();
        return dx >= 0 && dx < T && dz >= 0 && dz < T && dy >= 0.5 && dy <= 2.5;
    }

    // Essa entidade ativa a armadilha ao pisar? (o "pressCell" forte ou fraco do Shattered)
    public static boolean aciona(Entity quem, boolean escondida) {
        if (quem instanceof ItemEntity) {
            return true;
        }
        if (!(quem instanceof LivingEntity criatura) || !criatura.isAlive() || criatura.isSpectator() || voa(criatura)) {
            return false;
        }
        return !escondida || criatura instanceof Player || criatura instanceof OvelhaMagica;
    }

    // Quem passa por cima das armadilhas sem encostar no chão
    public static boolean voa(LivingEntity criatura) {
        return criatura instanceof EnxameDeMoscas || criatura instanceof FantasmaTriste || criatura instanceof FantasmaAliado
                || criatura.hasEffect(ModEfeitos.LEVITANDO)
                || (criatura instanceof Player jogador && (jogador.isCreative() || jogador.getAbilities().flying));
    }

    // Alguém pisou (ou um item caiu em cima)
    public static void pisar(ServerLevel mundo, BlockPos controle, @Nullable Entity quem) {
        BlockState estado = estado(mundo, controle);
        if (estado == null || estado.getValue(ArmadilhaBloco.DESARMADA)) {
            return;
        }
        TipoDeArmadilha tipo = estado.getValue(ArmadilhaBloco.TIPO);
        // Com o tempo parado, a armadilha só aparece; ativa quando o tempo voltar
        if (quem instanceof LivingEntity vivo && vivo.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            revelar(mundo, controle);
            mundo.playSound(null, BlockPos.containing(centro(controle)), SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.8F);
            ADIADAS.put(controle.asLong(), quem.getUUID());
            return;
        }
        if (estado.getValue(ArmadilhaBloco.ESCONDIDA) && quem instanceof ServerPlayer jogador) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.armadilha_escondida", Component.translatable(tipo.chaveDoNome()))
                    .withStyle(ChatFormatting.GOLD), false);
        }
        acionar(mundo, controle, quem);
    }

    // Ativa (o "Trap.trigger" do Shattered): faz o barulho, desarma (se for das que desarmam), fica à vista e faz o efeito
    public static void acionar(ServerLevel mundo, BlockPos controle, @Nullable Entity quem) {
        BlockState estado = estado(mundo, controle);
        if (estado == null || estado.getValue(ArmadilhaBloco.DESARMADA)) {
            return;
        }
        TipoDeArmadilha tipo = estado.getValue(ArmadilhaBloco.TIPO);
        mundo.playSound(null, BlockPos.containing(centro(controle)), SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (tipo.desarmaAoAtivar()) {
            desarmar(mundo, controle);
        } else {
            revelar(mundo, controle);
        }
        EfeitosDasArmadilhas.ativar(mundo, controle, tipo, quem);
    }

    // Quem nasce em cima de uma armadilha armada (a ovelha, o inimigo invocado) ativa ela na hora, até se estiver escondida
    // (igual ao Shattered: numa sala cheia de armadilhas de invocação, uma ativa a outra)
    public static void acionarSeTiver(ServerLevel mundo, BlockPos controle, Entity quemNasceu) {
        BlockState estado = estado(mundo, controle);
        if (estado == null || estado.getValue(ArmadilhaBloco.DESARMADA)) {
            return;
        }
        PRESENTES.computeIfAbsent(controle.asLong(), chave -> new HashSet<>()).add(quemNasceu.getUUID());
        acionar(mundo, controle, quemNasceu);
    }

    // ---- Olhar de perto ----

    // Clique direito numa armadilha à vista: o nome e o que ela faz (e se já foi ativada)
    public static void descrever(Player jogador, BlockState estado) {
        TipoDeArmadilha tipo = estado.getValue(ArmadilhaBloco.TIPO);
        jogador.sendSystemMessage(Component.translatable(tipo.chaveDoTitulo()).withStyle(ChatFormatting.GOLD));
        if (estado.getValue(ArmadilhaBloco.DESARMADA)) {
            jogador.sendSystemMessage(Component.translatable("mensagem.pixeldungeon.armadilha_desarmada").withStyle(ChatFormatting.DARK_GRAY));
        }
        jogador.sendSystemMessage(Component.translatable(tipo.chaveDaDescricao()).withStyle(ChatFormatting.GRAY));
    }
}
