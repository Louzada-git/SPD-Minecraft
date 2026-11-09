package com.louzada.pixeldungeon.run;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.bloco.PortaGigante;
import com.louzada.pixeldungeon.combate.EventosDeCombate;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.RatoAlbino;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.artefato.AmpulhetaDoTempo;
import com.louzada.pixeldungeon.item.artefato.BracadeiraDoLadrao;
import com.louzada.pixeldungeon.item.artefato.CaliceDeSangue;
import com.louzada.pixeldungeon.item.artefato.ChaveDeCranio;
import com.louzada.pixeldungeon.item.artefato.ChifreDaFartura;
import com.louzada.pixeldungeon.item.artefato.CorrentesEtereas;
import com.louzada.pixeldungeon.item.artefato.LivroInstavel;
import com.louzada.pixeldungeon.item.artefato.SandaliasDaNatureza;
import com.louzada.pixeldungeon.item.artefato.TalismaDaVidencia;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: os 9 artefatos que se acha pela dungeon.
// Tudo acontece numa plataforma de pedra longe da dungeon (o talismã, que só funciona dentro dela, vai pro andar 1).
// Um jogador de mentira usa cada artefato (na outra mão, a casinha de artefato) e o log mostra o resultado (o build.yml confere).
public class TestesDosArtefatos {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos entrada = run.andar(1).entrada();
        BlockPos base = entrada.offset(1900, 0, 0);
        for (int x = -8; x <= 8; x++) {
            for (int z = -6; z <= 30; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 6; y++) {
                    mundo.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        voltar(heroi, base);

        // ---- O saque: cada artefato só aparece uma vez por run; acabando, vem um anel ----
        DadosDaRun outraRun = new DadosDaRun();
        Set<String> vistos = new HashSet<>();
        Random sorteio = new Random(7);
        int quantos = ModItens.artefatos().size();
        for (int i = 0; i < quantos; i++) {
            ItemStack achado = SaqueDoAndar.artefatoAleatorio(sorteio, outraRun);
            if (achado.getItem() instanceof ArtefatoAchado artefato) {
                vistos.add(artefato.id());
            }
        }
        ItemStack depois = SaqueDoAndar.artefatoAleatorio(sorteio, outraRun);
        PixelDungeon.LOGGER.info("Teste do saque de artefatos: {} artefatos diferentes em {} sorteios, o seguinte virou {}", vistos.size(), quantos,
                depois.getItem() instanceof ArtefatoAchado ? "artefato" : "anel");

        // ---- Cálice de sangue ----
        ItemStack calice = equipar(heroi, ModItens.CALICE_DE_SANGUE.get());
        CaliceDeSangue caliceItem = (CaliceDeSangue) calice.getItem();
        caliceItem.furar(heroi, calice);
        int nivelDoCalice = ArtefatoAchado.nivel(calice);
        heroi.setHealth(4);
        for (int i = 0; i < 100; i++) {
            ArtefatoAchado.turnoDoHeroi(heroi);
        }
        float curou = heroi.getHealth() - 4;
        heroi.setHealth(heroi.getMaxHealth());
        PixelDungeon.LOGGER.info("Teste do cálice de sangue: subiu pro nível {}, dano no +0 {}-{} e no +10 {}-{}, a mais de cura em 100 turnos no +1: {}, desenho {}",
                nivelDoCalice, CaliceDeSangue.danoMinimo(0), CaliceDeSangue.danoMaximo(0), CaliceDeSangue.danoMinimo(10), CaliceDeSangue.danoMaximo(10),
                curou, caliceItem.estagio(calice));

        // ---- Chifre da fartura ----
        ItemStack chifre = equipar(heroi, ModItens.CHIFRE_DA_FARTURA.get());
        ChifreDaFartura chifreItem = (ChifreDaFartura) chifre.getItem();
        ArtefatoAchado.xpDoHeroi(heroi, 2.0F);
        int cargasDoChifre = ArtefatoAchado.cargas(chifre);
        heroi.getFoodData().setFoodLevel(5);
        boolean comeu = chifreItem.usar(heroi, chifre);
        int fome = heroi.getFoodData().getFoodLevel();
        ItemStack racao = new ItemStack(ModItens.RACAO.get());
        chifreItem.guardar(heroi, chifre, racao);
        PixelDungeon.LOGGER.info("Teste do chifre da fartura: 2 níveis de XP deram {} cargas, lanchou = {} (fome 5 virou {}), a ração subiu ele pro nível {} (sobrou ração = {})",
                cargasDoChifre, comeu, fome, ArtefatoAchado.nivel(chifre), !racao.isEmpty());
        heroi.getFoodData().setFoodLevel(20);
        heroi.removeAllEffects();

        // ---- Ampulheta do tempo ----
        ItemStack ampulheta = equipar(heroi, ModItens.AMPULHETA_DO_TEMPO.get());
        AmpulhetaDoTempo ampulhetaItem = (AmpulhetaDoTempo) ampulheta.getItem();
        int cheia = ArtefatoAchado.cargas(ampulheta);
        ampulhetaItem.usar(heroi, ampulheta);
        boolean estase = heroi.hasEffect(ModEfeitos.ESTASE);
        boolean invisivel = Furtividade.invisivel(heroi);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.VENENO, 200));
        boolean envenenou = heroi.hasEffect(ModEfeitos.VENENO);
        int depoisDaEstase = ArtefatoAchado.cargas(ampulheta);
        heroi.removeAllEffects();
        RatoMarsupial rato = rato(mundo, base, 12);
        ampulhetaItem.usarAgachado(heroi, ampulheta);
        boolean parou = heroi.hasEffect(ModEfeitos.TEMPO_PARADO) && rato.hasEffect(ModEfeitos.PARADO_NO_TEMPO);
        int depoisDeParar = ArtefatoAchado.cargas(ampulheta);
        Furtividade.revelar(heroi);
        boolean soltou = !heroi.hasEffect(ModEfeitos.TEMPO_PARADO) && !rato.hasEffect(ModEfeitos.PARADO_NO_TEMPO);
        PixelDungeon.LOGGER.info("Teste da ampulheta do tempo: veio com {} cargas, estase = {} (invisível = {}, o veneno pegou = {}) e ficou com {}, parou o tempo = {} e ficou com {}, atacar soltou = {}",
                cheia, estase, invisivel, envenenou, depoisDaEstase, parou, depoisDeParar, soltou);
        rato.discard();
        heroi.removeAllEffects();

        // ---- Livro instável ----
        ItemStack livro = equipar(heroi, ModItens.LIVRO_INSTAVEL.get());
        LivroInstavel livroItem = (LivroInstavel) livro.getItem();
        int cargasDoLivro = ArtefatoAchado.cargas(livro);
        boolean nuncaProibido = true;
        int identificacoes = 0;
        Random leitura = new Random(3);
        for (int i = 0; i < 2000; i++) {
            TipoDePergaminho tipo = LivroInstavel.sortearPergaminho(net.minecraft.util.RandomSource.create(leitura.nextLong()));
            if (tipo == TipoDePergaminho.MELHORIA || tipo == TipoDePergaminho.TRANSMUTACAO) {
                nuncaProibido = false;
            }
            if (tipo == TipoDePergaminho.IDENTIFICACAO) {
                identificacoes++;
            }
        }
        LivroInstavel.embaralhar(livro, heroi.getRandom());
        int pedidos = LivroInstavel.pedidos(livro).size();
        TipoDePergaminho primeiro = LivroInstavel.aceitosAgora(livro).get(0);
        Identificacao.identificar(heroi, primeiro);
        ItemStack pergaminho = Identificacao.pergaminho(run.semente(), primeiro);
        livroItem.alimentar(heroi, livro, pergaminho);
        boolean leu = livroItem.usar(heroi, livro);
        PixelDungeon.LOGGER.info("Teste do livro instável: veio com {} cargas, nunca saiu melhoria nem transmutação = {}, identificação em {} de 2000, pede {} pergaminhos, "
                + "alimentado subiu pro nível {} e pede {}, leu = {} e ficou com {} cargas",
                cargasDoLivro, nuncaProibido, identificacoes, pedidos, ArtefatoAchado.nivel(livro), LivroInstavel.pedidos(livro).size(), leu,
                ArtefatoAchado.cargas(livro));
        for (ImagemEspelhada imagem : mundo.getEntitiesOfClass(ImagemEspelhada.class, new AABB(base).inflate(400))) {
            imagem.discard();
        }
        heroi.removeAllEffects();
        voltar(heroi, base);

        // ---- Correntes etéreas ----
        ItemStack correntes = equipar(heroi, ModItens.CORRENTES_ETEREAS.get());
        CorrentesEtereas correntesItem = (CorrentesEtereas) correntes.getItem();
        int cincoCargas = ArtefatoAchado.cargas(correntes);
        rato = rato(mundo, base, 12);
        boolean puxou = correntesItem.puxarInimigo(heroi, correntes, rato);
        double pertoAgora = rato.position().distanceTo(heroi.position());
        int depoisDePuxar = ArtefatoAchado.cargas(correntes);
        rato.discard();
        ArtefatoAchado.xpDoHeroi(heroi, 1.0F);
        int comXp = ArtefatoAchado.cargas(correntes);
        // Uma parede lá na frente: as correntes puxam o herói até ela
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                mundo.setBlockAndUpdate(base.offset(x, y, 20), Blocks.STONE.defaultBlockState());
            }
        }
        // (o jogador de mentira não se mexe com o teletransporte: o teste confere onde ele pararia)
        Vec3 olho = heroi.getEyePosition();
        BlockHitResult parede = mundo.clip(new ClipContext(olho, new Vec3(base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 20.5),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, heroi));
        Vec3 pouso = correntesItem.puxarHeroi(heroi, correntes, parede);
        boolean foi = pouso != null;
        double andou = foi ? pouso.z - (base.getZ() + 0.5) : 0;
        PixelDungeon.LOGGER.info("Teste das correntes etéreas: vieram com {} cargas, puxou o rato = {} (ficou a {} blocos) e ficaram {}, a XP de 1 nível deixou {}, "
                + "puxou o herói até a parede = {} (andou {} blocos) e ficaram {}",
                cincoCargas, puxou, Math.round(pertoAgora * 10) / 10.0, depoisDePuxar, comXp, foi, Math.round(andou * 10) / 10.0, ArtefatoAchado.cargas(correntes));
        voltar(heroi, base);

        // ---- Braçadeira do ladrão ----
        ItemStack bracadeira = equipar(heroi, ModItens.BRACADEIRA_DO_LADRAO.get());
        BracadeiraDoLadrao bracadeiraItem = (BracadeiraDoLadrao) bracadeira.getItem();
        ArtefatoAchado.xpDoHeroi(heroi, 1.0F);
        int cargasDaBracadeira = ArtefatoAchado.cargas(bracadeira);
        RatoAlbino albino = ModEntidades.RATO_ALBINO.get().create(mundo);
        albino.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 2.5);
        mundo.addFreshEntity(albino);
        bracadeiraItem.roubar(heroi, bracadeira, albino);
        boolean roubouCarne = heroi.getInventory().countItem(ModItens.CARNE_MISTERIOSA.get()) > 0;
        boolean desorientado = albino.hasEffect(ModEfeitos.CEGO) && albino.hasEffect(ModEfeitos.ALEIJADO);
        EventosDeCombate.darRecompensa(albino, heroi);
        int carnesNoChao = 0;
        for (ItemEntity noChao : mundo.getEntitiesOfClass(ItemEntity.class, albino.getBoundingBox().inflate(3))) {
            if (noChao.getItem().is(ModItens.CARNE_MISTERIOSA.get())) {
                carnesNoChao++;
                noChao.discard();
            }
        }
        PixelDungeon.LOGGER.info("Teste da braçadeira do ladrão: 1 nível de XP deu {} cargas, roubou a carne do rato albino = {}, ele ficou cego e aleijado = {}, "
                + "marcado = {}, ao morrer deixou {} carnes, sobrou {} carga e {} de experiência",
                cargasDaBracadeira, roubouCarne, desorientado, BracadeiraDoLadrao.foiRoubado(albino), carnesNoChao, ArtefatoAchado.cargas(bracadeira),
                ArtefatoAchado.experiencia(bracadeira));
        albino.discard();
        heroi.getInventory().clearContent();

        // ---- Sandálias da natureza ----
        ItemStack sandalias = equipar(heroi, ModItens.SANDALIAS_DA_NATUREZA.get());
        SandaliasDaNatureza sandaliasItem = (SandaliasDaNatureza) sandalias.getItem();
        for (int i = 0; i < 10; i++) {
            sandaliasItem.aoPisotear(heroi, sandalias);
        }
        int pisoteando = ArtefatoAchado.cargas(sandalias);
        for (TipoDePlanta tipo : List.of(TipoDePlanta.MAGI_REAL, TipoDePlanta.TERRAIZ, TipoDePlanta.FLOR_ESTRELA)) {
            sandaliasItem.alimentar(heroi, sandalias, new ItemStack(ItemSemente.daPlanta(tipo)));
        }
        boolean repetida = sandaliasItem.aceita(sandalias, new ItemStack(ItemSemente.daPlanta(TipoDePlanta.FLOR_ESTRELA)));
        ArtefatoAchado.definirCargas(sandalias, 100);
        RatoMarsupial noLugar = rato(mundo, base, 0);
        boolean enraizou = sandaliasItem.enraizar(heroi, sandalias, heroi.blockPosition());
        boolean abencoado = noLugar.hasEffect(ModEfeitos.ABENCOADO);
        noLugar.discard();
        PixelDungeon.LOGGER.info("Teste das sandálias da natureza: 10 pisoteadas deram {} de carga, 3 sementes subiram pro nível {} ({}), aceita a mesma de novo = {}, "
                + "enraizou a flor-estrela = {} (o rato no lugar ficou abençoado = {}) e ficou com {}",
                pisoteando, ArtefatoAchado.nivel(sandalias), sandalias.getHoverName().getString(), repetida, enraizou, abencoado,
                ArtefatoAchado.cargas(sandalias));
        heroi.removeAllEffects();

        // ---- Chave de crânio ----
        ItemStack chave = new ItemStack(ModItens.CHAVE_DE_CRANIO.get());
        heroi.setItemInHand(InteractionHand.MAIN_HAND, chave);
        heroi.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ChaveDeCranio chaveItem = (ChaveDeCranio) chave.getItem();
        int cargasDaChave = ArtefatoAchado.cargas(chave);
        BlockPos controle = base.offset(-2, 0, 8);
        PortaGigante porta = ModBlocos.PORTA_GIGANTE.get();
        for (int coluna = 0; coluna < PortaGigante.LARGURA; coluna++) {
            for (int linha = 0; linha < PortaGigante.ALTURA; linha++) {
                mundo.setBlockAndUpdate(controle.relative(PortaGigante.lado(Direction.Axis.Z), coluna).above(linha),
                        porta.pedaco(Direction.Axis.Z, coluna, linha, true));
            }
        }
        boolean destrancou = chaveItem.usarNaPorta(heroi, chave, controle) && !mundo.getBlockState(controle).getValue(PortaGigante.TRANCADA);
        int depoisDeDestrancar = ArtefatoAchado.cargas(chave);
        boolean trancou = chaveItem.usarNaPorta(heroi, chave, controle);
        BlockState trancada = mundo.getBlockState(controle);
        boolean doHeroi = trancada.getValue(PortaGigante.TRANCADA) && trancada.getValue(PortaGigante.DO_HEROI);
        int depoisDeTrancar = ArtefatoAchado.cargas(chave);
        boolean abriuDeGraca = chaveItem.usarNaPorta(heroi, chave, controle) && !mundo.getBlockState(controle).getValue(PortaGigante.TRANCADA)
                && ArtefatoAchado.cargas(chave) == depoisDeTrancar;
        ArtefatoAchado.definirCargas(chave, 2);
        rato = rato(mundo, base, -4);
        boolean levantou = chaveItem.levantarParedes(heroi, chave, 0, -1);
        int paredes = 0;
        for (BlockPos aqui : BlockPos.betweenClosed(base.offset(-8, 0, -6), base.offset(8, 6, -1))) {
            if (mundo.getBlockState(aqui).is(ModBlocos.PAREDE_DA_CHAVE.get())) {
                paredes++;
            }
        }
        boolean ratoEmpurrado = rato.getZ() < base.getZ() - 4.5;
        PixelDungeon.LOGGER.info("Teste da chave de crânio: veio com {} cargas, destrancou a porta trancada = {} (ficou {}), trancou a porta = {} (do herói = {}, ficou {}), "
                + "abriu a dela de graça = {}, levantou paredes = {} ({} blocos de parede, o rato foi empurrado = {})",
                cargasDaChave, destrancou, depoisDeDestrancar, trancou, doHeroi, depoisDeTrancar, abriuDeGraca, levantou, paredes, ratoEmpurrado);
        rato.discard();
        heroi.getInventory().clearContent();

        // ---- Talismã da vidência (dentro do andar 1) ----
        ItemStack talisma = equipar(heroi, ModItens.TALISMA_DA_VIDENCIA.get());
        TalismaDaVidencia talismaItem = (TalismaDaVidencia) talisma.getItem();
        boolean semCarga = !talismaItem.usar(heroi, talisma);
        ArtefatoAchado.definirCargas(talisma, 100);
        heroi.moveTo(entrada.getX() + 0.5, entrada.getY(), entrada.getZ() + 0.5, 0.0F, 0.0F);
        RatoMarsupial vigiado = rato(mundo, entrada, 1);
        boolean viu = talismaItem.ver(heroi, talisma, new Vec3(0, 0, 1), 7);
        int depoisDeVer = ArtefatoAchado.cargas(talisma);
        boolean brilhou = vigiado.hasEffect(MobEffects.GLOWING);
        vigiado.discard();
        ArtefatoAchado.definirCargas(talisma, 50);
        TalismaDaVidencia.aoAcharPortas(heroi, 2);
        PixelDungeon.LOGGER.info("Teste do talismã da vidência: sem carga não vê = {}, viu 7 quadradinhos = {} (100 virou {}), o rato brilhou = {}, achar 2 portas deu {}",
                semCarga, viu, depoisDeVer, brilhou, ArtefatoAchado.cargas(talisma) - 50);

        voltar(heroi, base);
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        for (Entity sobra : mundo.getEntitiesOfClass(MobDoShattered.class, new AABB(base).inflate(40))) {
            sobra.discard();
        }
    }

    // Põe o artefato na outra mão (a casinha de artefato) e devolve a pilha que está lá
    private static ItemStack equipar(FakePlayer heroi, net.minecraft.world.item.Item artefato) {
        ItemStack pilha = new ItemStack(artefato);
        heroi.setItemInHand(InteractionHand.OFF_HAND, pilha);
        return heroi.getOffhandItem();
    }

    private static void voltar(FakePlayer heroi, BlockPos base) {
        heroi.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
        heroi.setDeltaMovement(Vec3.ZERO);
    }

    private static RatoMarsupial rato(ServerLevel mundo, BlockPos base, double distancia) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        rato.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5 + distancia);
        mundo.addFreshEntity(rato);
        return rato;
    }
}
