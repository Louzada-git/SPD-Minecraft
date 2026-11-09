package com.louzada.pixeldungeon.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.CaranguejoGrande;
import com.louzada.pixeldungeon.entidade.FantasmaAliado;
import com.louzada.pixeldungeon.entidade.FantasmaTriste;
import com.louzada.pixeldungeon.entidade.GnollTrapaceiro;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.RatoFetido;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EfeitosDeVarinha;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.artefato.RosaSeca;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: a missão do fantasma triste, os inimigos dela e a rosa seca.
// Tudo acontece numa plataforma de pedra longe da dungeon, na altura do andar 1 (a missão de teste é desse andar).
// O estado da missão da run é guardado antes e volta no fim (o teste não muda a run de verdade).
public class TestesDaMissaoDoFantasma {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos entrada = run.andar(1).entrada();
        BlockPos base = entrada.offset(2100, 0, 0);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 30; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 4; y++) {
                    mundo.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        heroi.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
        CompoundTag missaoDaRun = run.fantasma().copy();

        // ---- O fantasma aparece (o andar 4 é certo) com as duas recompensas ----
        DadosDaRun outraRun = new DadosDaRun();
        MissaoDoFantasma.talvezCriar(mundo, GeradorDeAndar.gerar(7, 4), base.offset(0, 0, 200), 4, outraRun);
        ItemStack arma = MissaoDoFantasma.arma(outraRun, mundo.registryAccess());
        ItemStack armadura = MissaoDoFantasma.armadura(outraRun, mundo.registryAccess());
        int tierDaArma = arma.getItem() instanceof ArmaDoShattered tipo ? tipo.tier() : 0;
        int tierDaArmadura = armadura.getItem() instanceof ArmaduraDoShattered tipo ? tipo.tier() : 0;
        PixelDungeon.LOGGER.info("Teste do fantasma triste: apareceu no andar 4 = {}, missão {}, arma do tier {} e armadura do tier {} (2 a 5 = {}), nível +{} nas duas = {}",
                MissaoDoFantasma.apareceu(outraRun), MissaoDoFantasma.tipo(outraRun), tierDaArma, tierDaArmadura,
                tierDaArma >= 2 && tierDaArma <= 5 && tierDaArmadura >= 2 && tierDaArmadura <= 5, NivelDoItem.nivel(arma),
                NivelDoItem.nivel(arma) == NivelDoItem.nivel(armadura));
        for (FantasmaTriste sobra : mundo.getEntitiesOfClass(FantasmaTriste.class, new AABB(base.offset(0, 0, 200)).inflate(400))) {
            sobra.discard();
        }

        // ---- A missão do caranguejo grande, do começo ao fim (nesta plataforma, como se fosse o andar 1) ----
        limpar(run.fantasma());
        run.fantasma().putBoolean("apareceu", true);
        run.fantasma().putInt("tipo", 3);
        run.fantasma().putInt("andar", Andares.andarPelaAltura(base.getY()));
        run.fantasma().put("arma", new ItemStack(ModItens.ESPADA_CURTA.get()).save(mundo.registryAccess()));
        run.fantasma().put("armadura", new ItemStack(ModItens.ARMADURA_DE_COURO.get()).save(mundo.registryAccess()));
        FantasmaTriste fantasma = ModEntidades.FANTASMA_TRISTE.get().create(mundo);
        fantasma.moveTo(base.getX() + 3.5, base.getY(), base.getZ() + 0.5);
        mundo.addFreshEntity(fantasma);
        boolean invulneravel = !fantasma.hurt(heroi.damageSources().playerAttack(heroi), 10);
        MissaoDoFantasma.conversar(heroi, fantasma);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), Andares.andarPelaAltura(base.getY()));
        List<CaranguejoGrande> nasceram = mundo.getEntitiesOfClass(CaranguejoGrande.class, PopulacaoDoAndar.caixaDoAndar(origem));
        CaranguejoGrande caranguejo = nasceram.isEmpty() ? ModEntidades.CARANGUEJO_GRANDE.get().create(mundo) : nasceram.get(0);
        if (nasceram.isEmpty()) {
            mundo.addFreshEntity(caranguejo);
        }
        caranguejo.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 2.5);
        caranguejo.setTarget(heroi);
        float vida = caranguejo.getHealth();
        caranguejo.invulnerableTime = 0;
        caranguejo.hurt(heroi.damageSources().playerAttack(heroi), 5);
        boolean bloqueouGolpe = caranguejo.getHealth() == vida;
        caranguejo.invulnerableTime = 0;
        caranguejo.hurt(EfeitosDeVarinha.fonte(heroi), 5);
        boolean bloqueouMagia = caranguejo.getHealth() == vida;
        caranguejo.setTarget(null);
        caranguejo.invulnerableTime = 0;
        caranguejo.hurt(heroi.damageSources().playerAttack(heroi), 5);
        boolean distraidoApanhou = caranguejo.getHealth() < vida;
        caranguejo.setHealth(1);
        caranguejo.setTarget(null);
        caranguejo.invulnerableTime = 0;
        caranguejo.hurt(heroi.damageSources().playerAttack(heroi), 5);
        int carnes = 0;
        for (ItemEntity noChao : mundo.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8))) {
            if (noChao.getItem().is(ModItens.CARNE_MISTERIOSA.get())) {
                carnes += noChao.getItem().getCount();
                noChao.discard();
            }
        }
        boolean cumprida = MissaoDoFantasma.cumprida(run);
        boolean escolheu = MissaoDoFantasma.escolherRecompensa(heroi, 1);
        boolean ganhouArmadura = heroi.getInventory().countItem(ModItens.ARMADURA_DE_COURO.get()) == 1;
        PixelDungeon.LOGGER.info("Teste da missão do fantasma: o fantasma é invulnerável = {}, a conversa deu a missão = {} e o caranguejo nasceu = {}, "
                + "bloqueou o golpe = {} e a magia = {}, distraído apanhou = {}, morto deixou {} carnes, cumprida = {}, "
                + "escolheu a armadura = {} (ganhou = {}), o fantasma foi embora = {}, completa = {}",
                invulneravel, MissaoDoFantasma.dada(run), !nasceram.isEmpty(), bloqueouGolpe, bloqueouMagia, distraidoApanhou, carnes, cumprida,
                escolheu, ganhouArmadura, fantasma.isRemoved(), MissaoDoFantasma.completa(run));
        heroi.getInventory().clearContent();

        // ---- O gnoll trapaceiro: joga dardos, cada acerto seguido é pior ----
        GnollTrapaceiro gnoll = ModEntidades.GNOLL_TRAPACEIRO.get().create(mundo);
        gnoll.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 12.5);
        mundo.addFreshEntity(gnoll);
        int acertos = 0;
        for (int i = 0; i < 20; i++) {
            if (gnoll.atirar(heroi)) {
                acertos++;
            }
        }
        boolean machucou = heroi.hasEffect(ModEfeitos.VENENO) || heroi.isOnFire();
        ItemStack dardos = gnoll.sortearSaque();
        PixelDungeon.LOGGER.info("Teste do gnoll trapaceiro: acertou {} de 20 dardos, sequência {}, envenenou ou pôs fogo = {}, deixou armas de arremesso = {} "
                + "({} de +{}, identificadas = {})",
                acertos, gnoll.sequencia(), machucou, dardos.getItem() instanceof ArmaDeArremesso, dardos.getCount(), NivelDoItem.nivel(dardos),
                NivelDoItem.identificado(dardos));
        gnoll.discard();
        heroi.removeAllEffects();
        heroi.clearFire();

        // ---- O rato fétido: muco cáustico e nuvem de fedor ----
        RatoFetido fetido = ModEntidades.RATO_FETIDO.get().create(mundo);
        fetido.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 2.5);
        mundo.addFreshEntity(fetido);
        for (int i = 0; i < 30 && !heroi.hasEffect(ModEfeitos.MUCO_CAUSTICO); i++) {
            fetido.aoAcertar(heroi, 1);
        }
        boolean muco = heroi.hasEffect(ModEfeitos.MUCO_CAUSTICO);
        fetido.setHealth(20);
        fetido.invulnerableTime = 0;
        fetido.hurt(heroi.damageSources().playerAttack(heroi), 3);
        int fedor = 0;
        for (NuvemDeGas nuvem : mundo.getEntitiesOfClass(NuvemDeGas.class, fetido.getBoundingBox().inflate(4))) {
            if (nuvem.tipo() == NuvemDeGas.Tipo.FEDOR) {
                fedor++;
                nuvem.discard();
            }
        }
        PixelDungeon.LOGGER.info("Teste do rato fétido: deixou muco cáustico = {}, apanhando soltou fedor = {}, é chefe (não corrompe) = {}", muco, fedor > 0,
                fetido.chefe());
        fetido.discard();
        heroi.removeAllEffects();

        // ---- A rosa seca e o fantasma aliado ----
        ItemStack rosa = new ItemStack(ModItens.ROSA_SECA.get());
        heroi.setItemInHand(InteractionHand.OFF_HAND, rosa);
        rosa = heroi.getOffhandItem();
        RosaSeca rosaItem = (RosaSeca) rosa.getItem();
        limpar(run.fantasma());
        boolean semMissao = !rosaItem.usar(heroi, rosa);
        run.fantasma().putBoolean("completa", true);
        int cheia = ArtefatoAchado.cargas(rosa);
        boolean chamou = rosaItem.usar(heroi, rosa);
        FantasmaAliado aliado = FantasmaAliado.doDono(heroi);
        float vidaDoAliado = aliado == null ? 0 : aliado.getMaxHealth();
        int danoMaximoSemArma = 0;
        ItemStack espada = SaqueDoAndar.armaDoTier(new Random(1), 2);
        boolean deuArma = false;
        boolean devolveu = false;
        boolean ordens = false;
        float curou = 0;
        if (aliado != null) {
            for (int i = 0; i < 200; i++) {
                danoMaximoSemArma = Math.max(danoMaximoSemArma, aliado.sortearDano());
            }
            heroi.getInventory().setItem(5, espada.copy());
            rosaItem.equipar(heroi, rosa, heroi.getInventory().getItem(5));
            aliado.receber(heroi, rosa);
            deuArma = aliado.arma().getItem() == espada.getItem();
            rosaItem.equipar(heroi, rosa, rosa);
            devolveu = RosaSeca.arma(rosa).isEmpty() && heroi.getInventory().countItem(espada.getItem()) == 1;
            // As ordens
            RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
            rato.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 8.5);
            mundo.addFreshEntity(rato);
            aliado.atacar(rato);
            boolean atacou = aliado.ordem() == FantasmaAliado.Ordem.ATACAR && aliado.getTarget() == rato;
            aliado.guardar(base.offset(0, 0, 5));
            boolean guardou = aliado.ordem() == FantasmaAliado.Ordem.GUARDAR;
            aliado.seguir();
            ordens = atacou && guardou && aliado.ordem() == FantasmaAliado.Ordem.SEGUIR;
            rato.discard();
            // A rosa cura o fantasma: a vida toda em 500 turnos (100 turnos = 8 de 40)
            aliado.setHealth(10);
            for (int i = 0; i < 100; i++) {
                rosaItem.aoTurno(heroi, rosa);
            }
            curou = aliado.getHealth() - 10;
        }
        // A pétala volta pra rosa
        heroi.getInventory().setItem(6, new ItemStack(ModItens.PETALA_SECA.get()));
        ItemStack petala = heroi.getInventory().getItem(6);
        petala.getItem().inventoryTick(petala, mundo, heroi, 6, false);
        PixelDungeon.LOGGER.info("Teste da rosa seca: sem a missão não funciona = {}, veio com {} de carga, chamou o fantasma = {} (vida {}, ficou {} de carga), "
                + "sem arma bate até {}, levou a arma = {}, devolveu = {}, obedeceu as ordens = {}, a rosa curou {} em 100 turnos, a pétala subiu a rosa pro nível {}",
                semMissao, cheia, chamou, vidaDoAliado, ArtefatoAchado.cargas(rosa), danoMaximoSemArma, deuArma, devolveu, ordens, curou,
                ArtefatoAchado.nivel(rosa));
        if (aliado != null) {
            aliado.discard();
        }

        // ---- As pétalas pelos andares: só com a missão completa e a rosa achada ----
        DadosDaRun semMissaoRun = new DadosDaRun();
        semMissaoRun.contarQueda(SaqueDoAndar.chaveDoArtefato(ModItens.ROSA_SECA.get()));
        int semPetalas = petalas(SaqueDoAndar.sortear(GeradorDeAndar.gerar(11, 4), 4, semMissaoRun));
        DadosDaRun comMissao = new DadosDaRun();
        comMissao.contarQueda(SaqueDoAndar.chaveDoArtefato(ModItens.ROSA_SECA.get()));
        comMissao.fantasma().putBoolean("completa", true);
        int comPetalas = petalas(SaqueDoAndar.sortear(GeradorDeAndar.gerar(11, 4), 4, comMissao));
        PixelDungeon.LOGGER.info("Teste das pétalas: sem a missão {} no andar 4, com a missão {}", semPetalas, comPetalas);

        // Tudo volta como era
        limpar(run.fantasma());
        for (String chave : missaoDaRun.getAllKeys()) {
            run.fantasma().put(chave, missaoDaRun.get(chave).copy());
        }
        run.mudouOFantasma();
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        for (Entity sobra : mundo.getEntitiesOfClass(Mob.class, new AABB(base).inflate(40))) {
            sobra.discard();
        }
    }

    private static void limpar(CompoundTag tag) {
        for (String chave : new ArrayList<>(tag.getAllKeys())) {
            tag.remove(chave);
        }
    }

    private static int petalas(List<SaqueDoAndar.Saque> saques) {
        int quantas = 0;
        for (SaqueDoAndar.Saque saque : saques) {
            for (ItemStack item : saque.itens()) {
                if (item.is(ModItens.PETALA_SECA.get())) {
                    quantas++;
                }
            }
        }
        return quantas;
    }
}
