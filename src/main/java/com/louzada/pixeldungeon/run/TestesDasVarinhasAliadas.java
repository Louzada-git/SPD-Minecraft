package com.louzada.pixeldungeon.run;

import java.util.List;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.Guarda;
import com.louzada.pixeldungeon.entidade.GuardiaoDaTerra;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ArmaduraDePedra;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.EfeitosDeVarinha;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as varinhas de terra viva, corrupção e proteção, os aliados delas e os efeitos novos.
// Tudo acontece numa plataforma de pedra longe da dungeon (sem outros inimigos por perto).
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDasVarinhasAliadas {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos entrada = run.andar(1).entrada();
        BlockPos base = entrada.offset(1500, 0, 0);
        for (int x = -6; x <= 6; x++) {
            for (int z = -4; z <= 30; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.MAGO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        heroi.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);

        // ---- Terra viva ----
        RatoMarsupial rato = rato(mundo, base, 8);
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getBoundingBox().getCenter());
        EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.TERRA_VIVA, 0, 1);
        int primeiro = ArmaduraDePedra.pedra(heroi);
        ArmaduraDePedra.somar(heroi, 0, 30);
        int maximo = ArmaduraDePedra.pedra(heroi);
        float sobrou = ArmaduraDePedra.segurar(heroi, 6);
        PixelDungeon.LOGGER.info("Teste da terra viva: o disparo virou {} de pedra, efeito = {}, no máximo {}, golpe de 6 virou {} e a pedra ficou {}",
                primeiro, heroi.hasEffect(ModEfeitos.ARMADURA_DE_PEDRA), maximo, sobrou, ArmaduraDePedra.pedra(heroi));
        rato.discard();

        // Com pedra bastante, o próximo disparo cria o guardião; sem inimigos, ele volta a ser pedra
        rato = rato(mundo, base, 8);
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getBoundingBox().getCenter());
        int pedraAntes = ArmaduraDePedra.pedra(heroi);
        EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.TERRA_VIVA, 0, 1);
        GuardiaoDaTerra guardiao = EfeitosDeVarinha.guardiaoDe(heroi);
        boolean nasceu = guardiao != null && guardiao.aliado();
        float vidaDoGuardiao = guardiao == null ? 0 : guardiao.getHealth();
        int pedraDepois = ArmaduraDePedra.pedra(heroi);
        rato.discard();
        boolean voltou = guardiao != null && guardiao.conferirInimigos();
        PixelDungeon.LOGGER.info("Teste do guardião da terra: nasceu aliado = {}, vida {} (a pedra era {}), a pedra foi pra ele = {}, voltou sem inimigos = {}, virou {} de pedra",
                nasceu, vidaDoGuardiao, pedraAntes, pedraDepois == 0, voltou && guardiao.isRemoved(), ArmaduraDePedra.pedra(heroi));
        ArmaduraDePedra.tirar(heroi);

        // ---- Efeitos novos ----
        rato = rato(mundo, base, 8);
        rato.addEffect(new MobEffectInstance(ModEfeitos.FEITICO, 200));
        float feitico = Combate.pelosEfeitos(rato);
        double velocidade = rato.getAttributeValue(Attributes.MOVEMENT_SPEED);
        rato.addEffect(new MobEffectInstance(ModEfeitos.LENTO, 200));
        double lento = rato.getAttributeValue(Attributes.MOVEMENT_SPEED) / velocidade;
        rato.addEffect(new MobEffectInstance(ModEfeitos.CONDENADO, 200));
        float vida = rato.getHealth();
        rato.invulnerableTime = 0;
        rato.hurt(EfeitosDeVarinha.fonte(heroi), 3);
        float perdeu = vida - rato.getHealth();
        PixelDungeon.LOGGER.info("Teste dos efeitos novos: feitiço deixa {} da precisão, lento deixa {} da velocidade, condenado fez 3 de dano virar {}",
                feitico, Math.round(lento * 100) / 100.0, Math.round(perdeu * 100) / 100.0);
        rato.discard();

        // ---- Corrupção ----
        // Com a vida cheia, o rato resiste e fica com um efeito ruim
        rato = rato(mundo, base, 8);
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getBoundingBox().getCenter());
        float resistenciaCheio = EfeitosDeVarinha.resistenciaACorrupcao(rato);
        EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.CORRUPCAO, 0, 1);
        boolean efeitoRuim = temAlgum(rato, ModEfeitos.FRACO, ModEfeitos.VULNERAVEL, ModEfeitos.ALEIJADO, ModEfeitos.CEGO, ModEfeitos.ATERRORIZADO,
                ModEfeitos.FURIOSO, ModEfeitos.LENTO, ModEfeitos.FEITICO, ModEfeitos.PARALISADO);
        // Quase morto, ele é corrompido, e o herói ganha o XP dele
        rato.setHealth(1);
        float resistenciaFerido = EfeitosDeVarinha.resistenciaACorrupcao(rato);
        int xpAntes = dados.xp();
        EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.CORRUPCAO, 0, 1);
        PixelDungeon.LOGGER.info("Teste da varinha de corrupção: resistência {} com a vida cheia, ficou com efeito ruim = {}; ferido {}, corrompido = {}, ganhou XP = {}",
                Math.round(resistenciaCheio * 100) / 100.0, efeitoRuim, Math.round(resistenciaFerido * 100) / 100.0, rato.aliado(), dados.xp() > xpAntes);
        rato.discard();

        // O chefe não pode ser corrompido: depois de pegar todos os efeitos ruins, fica condenado
        Goo goo = ModEntidades.GOO.get().create(mundo);
        boolean condenado = false;
        if (goo != null) {
            goo.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 8.5);
            mundo.addFreshEntity(goo);
            for (int i = 0; i < 20 && !condenado; i++) {
                heroi.lookAt(EntityAnchorArgument.Anchor.EYES, goo.getBoundingBox().getCenter());
                EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.CORRUPCAO, 0, 1);
                condenado = goo.hasEffect(ModEfeitos.CONDENADO);
            }
            PixelDungeon.LOGGER.info("Teste da corrupção no chefe: ficou condenado = {}, virou aliado = {}", condenado, goo.aliado());
            goo.discard();
        }

        // ---- Proteção ----
        heroi.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(base.getX() + 0.5, base.getY(), base.getZ() + 12.5));
        boolean semVarinha = EfeitosDeVarinha.podeLancar(heroi, TipoDeVarinha.PROTECAO);
        heroi.getInventory().add(new ItemStack(ModItens.varinha(TipoDeVarinha.PROTECAO)));
        boolean comVarinha = EfeitosDeVarinha.podeLancar(heroi, TipoDeVarinha.PROTECAO);
        EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.PROTECAO, 0, 1);
        List<Guarda> guardas = mundo.getEntitiesOfClass(Guarda.class, new AABB(base).inflate(40));
        Guarda guarda = guardas.isEmpty() ? null : guardas.get(0);
        int nivelAoNascer = guarda == null ? 0 : guarda.nivel();
        int nivelMelhorada = 0;
        boolean cheia = true;
        if (guarda != null) {
            heroi.lookAt(EntityAnchorArgument.Anchor.EYES, guarda.getBoundingBox().getCenter());
            EfeitosDeVarinha.lancar(heroi, TipoDeVarinha.PROTECAO, 0, 1);
            nivelMelhorada = guarda.nivel();
            heroi.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(base.getX() + 3.5, base.getY(), base.getZ() + 20.5));
            cheia = !EfeitosDeVarinha.podeLancar(heroi, TipoDeVarinha.PROTECAO);
        }
        PixelDungeon.LOGGER.info("Teste da varinha de proteção: sem varinha na mochila pode = {}, com ela pode = {}, a guarda nasceu nível {}, melhorou pra {}, energia {}/{}, sem energia pra outra = {}",
                semVarinha, comVarinha, nivelAoNascer, nivelMelhorada, EfeitosDeVarinha.energiaDasGuardas(heroi), EfeitosDeVarinha.energiaMaxima(heroi), cheia);

        // A guarda dispara sozinha num rato à vista; melhorada até sentinela, ganha vida
        if (guarda != null) {
            rato = rato(mundo, base, 4);
            float vidaDoRato = rato.getHealth();
            guarda.agir();
            boolean acertou = !rato.isAlive() || rato.getHealth() < vidaDoRato;
            int disparos = guarda.disparos();
            guarda.melhorar(0);
            guarda.melhorar(0);
            PixelDungeon.LOGGER.info("Teste da guarda: disparou no rato = {}, disparos {}, virou sentinela nível {} com vida {}/{}", acertou, disparos, guarda.nivel(),
                    guarda.getHealth(), guarda.getMaxHealth());
            rato.discard();
            guarda.sumir();
        }

        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        dados.comecar(ClasseDoHeroi.MAGO, run.numeroDaRun());
    }

    private static RatoMarsupial rato(ServerLevel mundo, BlockPos base, double distancia) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        rato.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5 + distancia);
        mundo.addFreshEntity(rato);
        return rato;
    }

    @SafeVarargs
    private static boolean temAlgum(MobDoShattered criatura, Holder<MobEffect>... efeitos) {
        for (Holder<MobEffect> efeito : efeitos) {
            if (criatura.hasEffect(efeito)) {
                return true;
            }
        }
        return false;
    }
}
