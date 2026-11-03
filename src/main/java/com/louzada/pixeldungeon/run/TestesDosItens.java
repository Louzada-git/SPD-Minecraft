package com.louzada.pixeldungeon.run;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.dungeon.PlantasDoShattered;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.PesoDoEquipamento;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.EfeitosDePergaminho;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: poções, pergaminhos, identificação, maldições, sementes e plantas.
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDosItens {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        long semente = run.semente();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();

        // As cores das poções e as runas dos pergaminhos são embaralhadas pela semente da run
        PixelDungeon.LOGGER.info("Teste das identidades: a poção de força é a {}, o pergaminho de melhoria é o {}",
                Identificacao.pocao(semente, TipoDePocao.FORCA).getItem(), Identificacao.pergaminho(semente, TipoDePergaminho.MELHORIA).getItem());

        // Beber a poção de força: +1 de força e o tipo fica conhecido
        ItemStack forca = Identificacao.pocao(semente, TipoDePocao.FORCA);
        int forcaAntes = dados.forca();
        forca.getItem().finishUsingItem(forca, mundo, heroi);
        PixelDungeon.LOGGER.info("Teste da poção de força: força {} -> {}, conhece = {}", forcaAntes, dados.forca(),
                dados.conhece(TipoDePocao.FORCA.chaveDeConhecimento()));

        // Beber a poção de cura começa a curar
        ItemStack cura = Identificacao.pocao(semente, TipoDePocao.CURA);
        heroi.setHealth(5);
        cura.getItem().finishUsingItem(cura, mundo, heroi);
        PixelDungeon.LOGGER.info("Teste da poção de cura: curando = {}", heroi.hasEffect(ModEfeitos.CURANDO));

        // Gás paralisante: o rato dentro da nuvem fica paralisado
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            // No canto do quadradinho da entrada (a escada fica no meio dele)
            rato.moveTo(lugar.getX() + 1.5, lugar.getY(), lugar.getZ() + 1.5);
            mundo.addFreshEntity(rato);
            NuvemDeGas nuvem = NuvemDeGas.soltar(mundo, rato.position(), NuvemDeGas.Tipo.PARALISANTE, 4.0F, 5);
            nuvem.tick();
            PixelDungeon.LOGGER.info("Teste do gás paralisante: o rato ficou paralisado = {}", rato.hasEffect(ModEfeitos.PARALISADO));
            nuvem.discard();

            // Planta: a flor-estrela abençoa quem pisa nela
            BlockPos lugarDaPlanta = rato.blockPosition();
            mundo.removeBlock(lugarDaPlanta, false);
            boolean plantou = ItemSemente.plantar(mundo, lugarDaPlanta, TipoDePlanta.FLOR_ESTRELA);
            boolean ehPlanta = mundo.getBlockState(lugarDaPlanta).is(ModBlocos.PLANTAS.get(TipoDePlanta.FLOR_ESTRELA.ordinal()).get());
            PlantasDoShattered.ativar(mundo, lugarDaPlanta, TipoDePlanta.FLOR_ESTRELA, rato);
            PixelDungeon.LOGGER.info("Teste das plantas: plantou = {}, ativou = {}, a planta sumiu = {}", plantou && ehPlanta,
                    rato.hasEffect(ModEfeitos.ABENCOADO), mundo.getBlockState(lugarDaPlanta).isAir());
            rato.discard();
        }

        // Arma achada na dungeon: vem sem identificar e se identifica depois de 20 golpes
        ItemStack espada = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ESPADA.get()), new Random(3), true);
        boolean desconhecida = !NivelDoItem.identificado(espada);
        for (int golpe = 0; golpe < 20; golpe++) {
            NivelDoItem.aoUsar(heroi, espada);
        }
        PixelDungeon.LOGGER.info("Teste da identificação pelo uso: começou desconhecida = {}, depois de 20 golpes conhecida = {}", desconhecida,
                NivelDoItem.identificado(espada));

        // Pergaminho de identificação: escolhe a arma desconhecida e ela fica conhecida
        ItemStack outra = NivelDoItem.sortearDoChao(new ItemStack(ModItens.KATANA.get()), new Random(5), true);
        heroi.getInventory().setItem(5, outra);
        ItemStack identificacao = Identificacao.pergaminho(semente, TipoDePergaminho.IDENTIFICACAO);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, identificacao);
        identificacao.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND);
        EscolhaDeItem.escolheu(heroi, 5);
        PixelDungeon.LOGGER.info("Teste do pergaminho de identificação: arma identificada = {}, pergaminho gasto = {}",
                NivelDoItem.identificado(heroi.getInventory().getItem(5)), heroi.getMainHandItem().isEmpty());

        // Pergaminho de melhoria: +1 de nível
        ItemStack melhoria = Identificacao.pergaminho(semente, TipoDePergaminho.MELHORIA);
        int nivelAntes = NivelDoItem.nivel(heroi.getInventory().getItem(5));
        heroi.setItemInHand(InteractionHand.MAIN_HAND, melhoria);
        melhoria.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND);
        EscolhaDeItem.escolheu(heroi, 5);
        PixelDungeon.LOGGER.info("Teste do pergaminho de melhoria: nível {} -> {}", nivelAntes, NivelDoItem.nivel(heroi.getInventory().getItem(5)));

        // Remover maldição
        ItemStack amaldicoada = new ItemStack(ModItens.CLAVA.get());
        amaldicoada.set(ModComponentes.MALDICAO.get(), "polarizante");
        heroi.getInventory().setItem(6, amaldicoada);
        ItemStack removerMaldicao = Identificacao.pergaminho(semente, TipoDePergaminho.REMOVER_MALDICAO);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, removerMaldicao);
        removerMaldicao.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND);
        EscolhaDeItem.escolheu(heroi, 6);
        PixelDungeon.LOGGER.info("Teste do pergaminho de remover maldição: ainda amaldiçoada = {}", NivelDoItem.amaldicoado(heroi.getInventory().getItem(6)));

        // Maldição polarizante: ou não dá dano, ou dá 50% a mais
        ItemStack polarizante = new ItemStack(ModItens.ESPADA.get());
        polarizante.set(ModComponentes.MALDICAO.get(), "polarizante");
        RatoMarsupial alvo = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        boolean certo = true;
        if (alvo != null) {
            for (int golpe = 0; golpe < 10; golpe++) {
                float dano = Maldicao.aoGolpearCom(heroi, alvo, polarizante, 10);
                certo &= dano == 0 || dano == 15;
            }
        }
        PixelDungeon.LOGGER.info("Teste da maldição polarizante: dano 0 ou 15 = {}", certo);

        // Transmutação: a espada vira outra arma de tier 3, com o mesmo nível
        ItemStack paraTransmutar = new ItemStack(ModItens.ESPADA.get());
        NivelDoItem.definirNivel(paraTransmutar, 2);
        heroi.getInventory().setItem(7, paraTransmutar);
        PixelDungeon.LOGGER.info("Teste da transmutação: dá pra transmutar = {}", EfeitosDePergaminho.podeTransmutar(heroi, paraTransmutar));
        EfeitosDePergaminho.aplicarNoItem(heroi, TipoDePergaminho.TRANSMUTACAO, 7);
        ItemStack transmutada = heroi.getInventory().getItem(7);
        PixelDungeon.LOGGER.info("Teste da transmutação: virou outra arma = {}, nível = {}", !transmutada.is(ModItens.ESPADA.get()),
                NivelDoItem.nivel(transmutada));

        // Armaduras: uma de cada tier
        int[] armadurasPorTier = new int[6];
        for (var registrado : ModItens.ITENS.getEntries()) {
            if (registrado.get() instanceof ArmaduraDoShattered armadura) {
                armadurasPorTier[armadura.tier()]++;
            }
        }
        PixelDungeon.LOGGER.info("Teste das armaduras: por tier {}/{}/{}/{}/{}", armadurasPorTier[1], armadurasPorTier[2], armadurasPorTier[3],
                armadurasPorTier[4], armadurasPorTier[5]);

        // Armadura achada: vem sem identificar e se identifica depois de 20 golpes recebidos
        RatoMarsupial quemBate = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (quemBate != null) {
            quemBate.moveTo(lugar.getX() + 1.5, lugar.getY(), lugar.getZ() + 1.5);
            ItemStack malha = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ARMADURA_DE_MALHA.get()), new Random(9), true);
            malha.remove(ModComponentes.MALDICAO.get());
            heroi.setItemSlot(EquipmentSlot.CHEST, malha);
            boolean desconhecidaNoComeco = !NivelDoItem.identificado(heroi.getItemBySlot(EquipmentSlot.CHEST));
            Combatente comoCombatente = Combatente.de(heroi);
            Golpe golpeDoRato = Golpe.de(Combatente.de(quemBate), comoCombatente);
            for (int golpe = 0; golpe < 20; golpe++) {
                comoCombatente.aoSerAtingido(golpeDoRato, 3);
            }
            PixelDungeon.LOGGER.info("Teste da armadura: começou desconhecida = {}, depois de 20 golpes recebidos conhecida = {}", desconhecidaNoComeco,
                    NivelDoItem.identificado(heroi.getItemBySlot(EquipmentSlot.CHEST)));

            // O combate inteiro: o rato bate no herói 20 vezes (acerta ou erra, e a armadura segura um pouco)
            int acertos = 0;
            for (int golpe = 0; golpe < 20; golpe++) {
                if (Combate.resolver(Golpe.de(Combatente.de(quemBate), comoCombatente)) != Combate.ERROU) {
                    acertos++;
                }
            }
            PixelDungeon.LOGGER.info("Teste do combate: o rato acertou {} de 20 golpes no herói de malha", acertos);

            // Armadura amaldiçoada: descobre ao vestir e gruda no corpo
            ItemStack fedida = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ARMADURA_DE_COURO.get()), new Random(2), false);
            fedida.set(ModComponentes.MALDICAO.get(), "fedor");
            heroi.setItemSlot(EquipmentSlot.CHEST, fedida);
            NivelDoItem.aoEquipar(heroi, heroi.getItemBySlot(EquipmentSlot.CHEST));
            PixelDungeon.LOGGER.info("Teste da armadura amaldiçoada: descobriu = {}, grudou = {}",
                    NivelDoItem.maldicaoConhecida(heroi.getItemBySlot(EquipmentSlot.CHEST)),
                    EnchantmentHelper.has(heroi.getItemBySlot(EquipmentSlot.CHEST), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE));
            NivelDoItem.removerMaldicao(heroi.getItemBySlot(EquipmentSlot.CHEST));
            PixelDungeon.LOGGER.info("Teste da armadura amaldiçoada: soltou depois de remover a maldição = {}",
                    !EnchantmentHelper.has(heroi.getItemBySlot(EquipmentSlot.CHEST), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE));

            // Armadura de placas com 10 de força: pesada demais, o herói fica mais lento
            heroi.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItens.ARMADURA_DE_PLACAS.get()));
            dados.definirForca(10);
            double antes = heroi.getAttributeValue(Attributes.MOVEMENT_SPEED);
            PesoDoEquipamento.atualizar(heroi, dados);
            double depois = heroi.getAttributeValue(Attributes.MOVEMENT_SPEED);
            PixelDungeon.LOGGER.info("Teste do peso: placas com 10 de força deixam mais lento = {}", depois < antes);
            heroi.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            PesoDoEquipamento.atualizar(heroi, dados);
        }

        // O saque dos andares: 2 poções de força e 3 pergaminhos de melhoria no esgoto
        PixelDungeon.LOGGER.info("Teste do saque: poções de força = {}, pergaminhos de melhoria = {}", run.quedas(SaqueDoAndar.POCOES_DE_FORCA),
                run.quedas(SaqueDoAndar.PERGAMINHOS_DE_MELHORIA));

        // Sementes caem por peso, e dá pra sortear todas as plantas
        List<ItemStack> sementes = List.of(SaqueDoAndar.sementeAleatoria(new Random(1)), SaqueDoAndar.sementeAleatoria(new Random(2)));
        PixelDungeon.LOGGER.info("Teste das sementes: {} sementes sorteadas", sementes.stream().filter(pilha -> pilha.getItem() instanceof ItemSemente).count());

        heroi.getInventory().clearContent();
    }
}
