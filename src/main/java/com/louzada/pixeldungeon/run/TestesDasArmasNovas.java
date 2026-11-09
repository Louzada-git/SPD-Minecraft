package com.louzada.pixeldungeon.run;

import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.HeroiEmCombate;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as 16 armas que faltavam, com os números do Shattered e as habilidades da Duelista.
// Tudo acontece numa plataforma de pedra longe da dungeon. Um jogador de mentira faz as coisas e o log mostra o resultado.
public class TestesDasArmasNovas {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos base = run.andar(1).entrada().offset(1700, 0, 0);
        for (int x = -6; x <= 6; x++) {
            for (int z = -4; z <= 40; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        dados.definirForca(20);
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        heroi.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);

        // ---- Os números ----
        ArmaDoShattered lanca = ModItens.LANCA.get();
        ArmaDoShattered escudo = ModItens.ESCUDO_REDONDO.get();
        PixelDungeon.LOGGER.info("Teste dos números das armas novas: lança {}-{} (+1: {}-{}), alcance +{} blocos, escudo redondo defende {} (+1: {}), "
                + "machado grande pede {} de força, lâmina rúnica +1 até {}, mangual ataca de surpresa = {}", lanca.danoMinimo(0), lanca.danoMaximo(0),
                lanca.danoMinimo(1), lanca.danoMaximo(1), lanca.alcanceExtra(), escudo.defesa(0), escudo.defesa(1),
                ModItens.MACHADO_GRANDE.get().forcaNecessaria(0), ModItens.LAMINA_RUNICA.get().danoMaximo(1), ModItens.MANGUAL.get().atacaDeSurpresa());

        // ---- Posturas (não precisam de alvo) ----
        empunhar(heroi, dados, ModItens.BASTAO.get());
        HabilidadesDaDuelista.usar(heroi);
        boolean postura = heroi.hasEffect(ModEfeitos.POSTURA_DEFENSIVA) && heroi.getEffect(ModEfeitos.POSTURA_DEFENSIVA).getDuration() == 160;
        empunhar(heroi, dados, ModItens.CIMITARRA.get());
        double velocidade = heroi.getAttributeValue(Attributes.ATTACK_SPEED);
        HabilidadesDaDuelista.usar(heroi);
        double danca = heroi.getAttributeValue(Attributes.ATTACK_SPEED) / velocidade;
        PixelDungeon.LOGGER.info("Teste das posturas: postura defensiva por 4 turnos = {}, dança da espada deixa o ataque {}x mais rápido", postura,
                Math.round(danca * 100) / 100.0);
        heroi.removeAllEffects();

        // ---- Defender: o golpe é anulado; antes de bloquear, atacar não acaba a defesa; depois, acaba ----
        // (o jogador de mentira não leva dano, então o teste chama direto o que o evento de dano chama)
        empunhar(heroi, dados, ModItens.ESCUDO_REDONDO.get());
        HabilidadesDaDuelista.usar(heroi);
        Furtividade.revelar(heroi);
        boolean continuou = heroi.hasEffect(ModEfeitos.DEFENDENDO);
        boolean anulou = HabilidadesDaDuelista.defendeu(heroi);
        Furtividade.revelar(heroi);
        PixelDungeon.LOGGER.info("Teste do escudo: atacar antes de bloquear não acabou = {}, anulou o golpe = {}, a defesa acabou depois de atacar = {}",
                continuou, anulou, !heroi.hasEffect(ModEfeitos.DEFENDENDO));
        RatoMarsupial rato;
        float vida;

        // ---- Ceifar: o golpe vira sangramento ----
        rato = rato(mundo, base, 2);
        empunhar(heroi, dados, ModItens.FOICE.get());
        mirar(heroi, rato);
        vida = rato.getHealth();
        HabilidadesDaDuelista.usar(heroi);
        PixelDungeon.LOGGER.info("Teste da foice: sangrando = {}, a vida não mudou na hora = {}", rato.hasEffect(ModEfeitos.SANGRANDO), rato.getHealth() == vida);
        rato.discard();

        // ---- Espetar: perto demais não dá; no alcance, acerta ----
        rato = rato(mundo, base, 2);
        empunhar(heroi, dados, ModItens.LANCA.get());
        mirar(heroi, rato);
        HabilidadesDaDuelista.usar(heroi);
        boolean pertoNaoGastou = dados.inteiro(HabilidadesDaDuelista.CARGAS) == 8;
        rato.discard();
        rato = forte(rato(mundo, base, 6));
        mirar(heroi, rato);
        vida = rato.getHealth();
        double antes = rato.getZ();
        HabilidadesDaDuelista.usar(heroi);
        PixelDungeon.LOGGER.info("Teste da lança: perto demais não gastou carga = {}, de longe acertou = {}, empurrou = {}", pertoNaoGastou, rato.getHealth() < vida,
                rato.getZ() > antes + 2);
        rato.discard();
        soltar(heroi);

        // ---- Chicotear: todos os inimigos no alcance ----
        RatoMarsupial perto = forte(rato(mundo, base, 3));
        RatoMarsupial longe = forte(rato(mundo, base, 9));
        empunhar(heroi, dados, ModItens.CHICOTE.get());
        HabilidadesDaDuelista.usar(heroi);
        PixelDungeon.LOGGER.info("Teste do chicote: acertou os dois = {}", perto.getHealth() < 200 && longe.getHealth() < 200);
        perto.discard();
        longe.discard();
        soltar(heroi);

        // ---- Girar: 3 voltas (só a primeira gasta carga), a 4ª não dá; o próximo golpe leva tudo ----
        rato = forte(rato(mundo, base, 2));
        empunhar(heroi, dados, ModItens.MANGUAL.get());
        long agora = mundo.getGameTime();
        HabilidadesDaDuelista.usar(heroi);
        HabilidadesDaDuelista.usar(heroi);
        HabilidadesDaDuelista.usar(heroi);
        int cargas = dados.inteiro(HabilidadesDaDuelista.CARGAS);
        HabilidadesDaDuelista.usar(heroi);
        boolean quartaNao = dados.inteiro(HabilidadesDaDuelista.CARGAS) == cargas;
        mirar(heroi, rato);
        vida = rato.getHealth();
        golpeNormal(heroi, rato);
        PixelDungeon.LOGGER.info("Teste do mangual: 3 voltas gastaram {} carga, a 4ª não deu = {}, o golpe tirou {} de vida (pelo menos 24 a mais) = {}, parou de girar = {}",
                8 - cargas, quartaNao, Math.round(vida - rato.getHealth()), vida - rato.getHealth() >= 25, !HabilidadesDaDuelista.girando(dados, agora));
        rato.discard();

        // ---- Tiro carregado: o próximo golpe arremessa ----
        rato = forte(rato(mundo, base, 2));
        empunhar(heroi, dados, ModItens.BESTA.get());
        HabilidadesDaDuelista.usar(heroi);
        boolean carregou = heroi.hasEffect(ModEfeitos.CARREGADO);
        mirar(heroi, rato);
        antes = rato.getZ();
        golpeNormal(heroi, rato);
        PixelDungeon.LOGGER.info("Teste da besta: carregou = {}, o golpe arremessou o rato = {}, descarregou = {}", carregou, rato.getZ() > antes + 8,
                !heroi.hasEffect(ModEfeitos.CARREGADO));
        rato.discard();

        // ---- Corte rúnico: com +300% de poder, o encantamento flamejante sempre pega ----
        int pegouFogo = 0;
        for (int i = 0; i < 5; i++) {
            rato = forte(rato(mundo, base, 2));
            ItemStack lamina = empunhar(heroi, dados, ModItens.LAMINA_RUNICA.get());
            NivelDoItem.definirEncantamento(lamina, Encantamento.FLAMEJANTE);
            mirar(heroi, rato);
            HabilidadesDaDuelista.usar(heroi);
            if (rato.isOnFire()) {
                pegouFogo++;
            }
            rato.discard();
        }
        PixelDungeon.LOGGER.info("Teste da lâmina rúnica: pegou fogo {} de 5", pegouFogo);

        // ---- Retribuição: só com menos de metade da vida ----
        rato = forte(rato(mundo, base, 2));
        empunhar(heroi, dados, ModItens.MACHADO_GRANDE.get());
        mirar(heroi, rato);
        heroi.setHealth(heroi.getMaxHealth());
        HabilidadesDaDuelista.usar(heroi);
        boolean comVidaNao = rato.getHealth() == 200;
        heroi.setHealth(heroi.getMaxHealth() * 0.4F);
        HabilidadesDaDuelista.usar(heroi);
        PixelDungeon.LOGGER.info("Teste do machado grande: com a vida cheia não deu = {}, ferido acertou = {}, mangual faz surpresa = {}", comVidaNao,
                rato.getHealth() < 200, mangualSurpreende(heroi));
        rato.discard();

        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        heroi.setHealth(heroi.getMaxHealth());
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
    }

    // Põe a arma na mão com os atributos dela (o jogador de mentira não roda o tick que faz isso) e 8 cargas
    private static ItemStack empunhar(FakePlayer heroi, DadosDoHeroi dados, Item arma) {
        soltar(heroi);
        ItemStack pilha = new ItemStack(arma);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, pilha);
        pilha.forEachModifier(EquipmentSlot.MAINHAND, (atributo, modificador) -> {
            AttributeInstance instancia = heroi.getAttribute(atributo);
            if (instancia != null && !instancia.hasModifier(modificador.id())) {
                instancia.addTransientModifier(modificador);
            }
        });
        dados.definir(HabilidadesDaDuelista.CARGAS, 8);
        return pilha;
    }

    private static void soltar(FakePlayer heroi) {
        ItemStack naMao = heroi.getMainHandItem();
        naMao.forEachModifier(EquipmentSlot.MAINHAND, (atributo, modificador) -> {
            AttributeInstance instancia = heroi.getAttribute(atributo);
            if (instancia != null) {
                instancia.removeModifier(modificador.id());
            }
        });
        heroi.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    // Um golpe normal de perto (com a força toda, como se o herói tivesse esperado a recarga do ataque)
    private static void golpeNormal(FakePlayer heroi, RatoMarsupial rato) {
        rato.invulnerableTime = 0;
        rato.hurt(heroi.damageSources().playerAttack(heroi), (float) heroi.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private static boolean mangualSurpreende(FakePlayer heroi) {
        empunhar(heroi, RegrasDoHeroi.dados(heroi), ModItens.MANGUAL.get());
        boolean surpreende = new HeroiEmCombate(heroi).fazAtaqueSurpresa();
        soltar(heroi);
        return surpreende;
    }

    private static void mirar(FakePlayer heroi, RatoMarsupial rato) {
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getBoundingBox().getCenter());
    }

    private static RatoMarsupial rato(ServerLevel mundo, BlockPos base, double distancia) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        rato.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5 + distancia);
        mundo.addFreshEntity(rato);
        return rato;
    }

    // Um rato com 200 de vida (pra aguentar os golpes e dar pra medir)
    private static RatoMarsupial forte(RatoMarsupial rato) {
        AttributeInstance vida = rato.getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(200);
        }
        rato.setHealth(200);
        return rato;
    }
}
