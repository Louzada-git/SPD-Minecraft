package com.louzada.pixeldungeon.run;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as armas de arremesso (números, durabilidade, juntar o conjunto, cravar no inimigo e um arremesso de verdade).
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDasArmasDeArremesso {
    // O arremesso de verdade: o rato que leva a pedra (conferido depois, no fim do teste do Goo)
    private static RatoMarsupial ratoDoArremesso = null;
    private static float vidaDoRatoAntes = 0;

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos entrada = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        RegrasDoHeroi.dados(heroi).comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // As 15 armas, 3 de cada tier, e os números da pedra (2-5 de dano, 9 de força)
        int armas = 0;
        int[] porTier = new int[6];
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaDeArremesso arma) {
                armas++;
                porTier[arma.tier()]++;
            }
        }
        ArmaDeArremesso pedra = ModItens.PEDRA_DE_ARREMESSO.get();
        PixelDungeon.LOGGER.info("Teste das armas de arremesso: {} armas, por tier {}/{}/{}/{}/{}, pedra {}-{} com força {}", armas, porTier[1], porTier[2],
                porTier[3], porTier[4], porTier[5], pedra.danoMinimo(0), pedra.danoMaximo(0), pedra.forcaNecessaria(0));

        // O Guerreiro começa com 3 pedras; a pedra aguenta 5 acertos (9 com projéteis duráveis +2)
        int pedrasNoKit = 0;
        for (ItemStack item : ClasseDoHeroi.GUERREIRO.kitInicial()) {
            if (item.is(pedra)) {
                pedrasNoKit += item.getCount();
            }
        }
        PixelDungeon.LOGGER.info("Teste da durabilidade: o Guerreiro começa com {} pedras, a pedra gasta {} por acerto, com projéteis duráveis +2 gasta {}",
                pedrasNoKit, String.format(Locale.ROOT, "%.1f", pedra.gastoPorUso(0, 0)), String.format(Locale.ROOT, "%.1f", pedra.gastoPorUso(0, 2)));

        // Juntar o que voltou: 2 inteiras + 1 com 80 = 3 com 80; 1 com 30 + 1 com 50 = 1 com 80
        ItemStack inteiras = new ItemStack(pedra, 2);
        ItemStack gasta = new ItemStack(pedra, 1);
        ArmaDeArremesso.definirDurabilidade(gasta, 80);
        ArmaDeArremesso.juntar(inteiras, gasta);
        ItemStack quase = new ItemStack(pedra, 1);
        ArmaDeArremesso.definirDurabilidade(quase, 30);
        ItemStack metade = new ItemStack(pedra, 1);
        ArmaDeArremesso.definirDurabilidade(metade, 50);
        ArmaDeArremesso.juntar(quase, metade);
        PixelDungeon.LOGGER.info("Teste de juntar: 2 inteiras + 1 com 80 = {} com {}, 1 com 30 + 1 com 50 = {} com {}", inteiras.getCount(),
                Math.round(ArmaDeArremesso.durabilidade(inteiras)), quase.getCount(), Math.round(ArmaDeArremesso.durabilidade(quase)));

        // Achadas na dungeon: em 3, sem identificar, se identificam com 10 acertos
        ItemStack achada = SaqueDoAndar.arremessoAleatorio(new Random(5), 1);
        PixelDungeon.LOGGER.info("Teste do saque de arremesso: quantidade {}, identificada = {}, acertos pra identificar = {}", achada.getCount(),
                NivelDoItem.identificado(achada), Math.round(achada.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 0.0F)));

        // A faca cravada no rato cai quando ele morre
        Item faca = ModItens.FACA_DE_ARREMESSO.get();
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            rato.moveTo(entrada.getX() - 1.5, entrada.getY(), entrada.getZ() - 1.5);
            mundo.addFreshEntity(rato);
            ArmaArremessada.espetar(rato, new ItemStack(faca));
            int cravadas = ArmaArremessada.espetados(rato);
            rato.hurt(mundo.damageSources().genericKill(), 1000);
            var noChao = mundo.getEntitiesOfClass(ItemEntity.class, rato.getBoundingBox().inflate(3), item -> item.getItem().is(faca));
            PixelDungeon.LOGGER.info("Teste da faca cravada: cravadas = {}, caiu quando o rato morreu = {}", cravadas, !noChao.isEmpty());
            noChao.forEach(ItemEntity::discard);
        }

        // Um arremesso de verdade: a pedra voa até um rato dormindo (que não desvia) e cai no chão perto dele
        RatoMarsupial alvo = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (alvo == null) {
            return;
        }
        alvo.moveTo(entrada.getX() - 1.5, entrada.getY(), entrada.getZ() + 4.0);
        mundo.addFreshEntity(alvo);
        alvo.dormir();
        mundo.setChunkForced(alvo.chunkPosition().x, alvo.chunkPosition().z, true);
        heroi.moveTo(entrada.getX() - 1.5, entrada.getY(), entrada.getZ() + 0.5, 0.0F, 0.0F);
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, alvo.getEyePosition());
        ItemStack pedras = new ItemStack(pedra, 3);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, pedras);
        pedra.arremessar(heroi, pedras);
        ratoDoArremesso = alvo;
        vidaDoRatoAntes = alvo.getHealth();
        PixelDungeon.LOGGER.info("Teste do arremesso: ficaram {} pedras na mão", pedras.getCount());
        heroi.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    // Uns segundos depois (no fim do teste do Goo): a pedra acertou o rato e caiu no chão
    public static void verificar(MinecraftServer servidor) {
        if (ratoDoArremesso == null) {
            return;
        }
        ServerLevel mundo = servidor.overworld();
        boolean acertou = !ratoDoArremesso.isAlive() || ratoDoArremesso.getHealth() < vidaDoRatoAntes;
        var noChao = mundo.getEntitiesOfClass(ItemEntity.class, ratoDoArremesso.getBoundingBox().inflate(8),
                item -> item.getItem().is(ModItens.PEDRA_DE_ARREMESSO.get()));
        PixelDungeon.LOGGER.info("Teste do arremesso: acertou o rato = {}, a pedra caiu no chão = {}", acertou, !noChao.isEmpty());
        noChao.forEach(ItemEntity::discard);
        mundo.setChunkForced(ratoDoArremesso.chunkPosition().x, ratoDoArremesso.chunkPosition().z, false);
        ratoDoArremesso.discard();
        ratoDoArremesso = null;
    }
}
