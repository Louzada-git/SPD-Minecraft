package com.louzada.pixeldungeon.run;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.alquimia.GuiaDeAlquimia;
import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;
import com.louzada.pixeldungeon.alquimia.ReceitasDaAlquimia;
import com.louzada.pixeldungeon.bloco.ArbustoDeFruta;
import com.louzada.pixeldungeon.bloco.PoteDeAlquimia;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.Aneis;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.Alteracao;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.CorDePocao;
import com.louzada.pixeldungeon.item.CristalDeEnergia;
import com.louzada.pixeldungeon.item.EfeitosDasPedras;
import com.louzada.pixeldungeon.item.EfeitosDePergaminhoExotico;
import com.louzada.pixeldungeon.item.EfeitosDePocaoExotica;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.EscolhaDeOpcao;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.IdentidadesDaRun;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.PaginaDoGuia;
import com.louzada.pixeldungeon.item.RunaDePergaminho;
import com.louzada.pixeldungeon.item.TipoDeAnel;
import com.louzada.pixeldungeon.item.TipoDePedra;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDePocaoExotica;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.item.artefato.KitDoAlquimista;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

// Só no teste do GitHub: a alquimia. Um pote numa plataforma longe da dungeon e um herói de mentira mexendo na tela
// dele (do lado do servidor). O log mostra o resultado (o build.yml confere).
public class TestesDaAlquimia {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos canto = run.andar(1).entrada().offset(2600, 0, 0);
        for (int x = -4; x <= 5; x++) {
            for (int z = -4; z <= 5; z++) {
                mundo.setBlockAndUpdate(canto.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 3; y++) {
                    mundo.setBlockAndUpdate(canto.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        PoteDeAlquimia.colocar(mundo, canto);
        FakePlayer heroi = heroi(mundo, run);
        heroi.moveTo(canto.getX() - 2.5, canto.getY(), canto.getZ() + 0.5);
        testarEnergia(mundo, heroi, canto);
        testarSementes(mundo, heroi, canto);
        testarPedrasNoPote(heroi, canto, run);
        testarPedrasArremessadas(mundo, run);
        testarClarividencia(mundo, run);
        testarPedrasDeUsar(mundo, run);
        testarPocoesExoticas(mundo, run, canto);
        testarPergaminhosExoticos(mundo, run, canto);
        testarComidas(mundo, run, canto);
        testarLaboratorio(run);
        testarLaboratorioSecreto(run);
        testarPaginasDoGuia(mundo, run);
        testarKit(mundo, run);
        testarAntimagia(mundo, run);
        TestesDaAlquimiaDaPrisao.testar(mundo, run, canto);
    }

    // ---- O pote e a energia ----

    private static void testarEnergia(ServerLevel mundo, FakePlayer heroi, BlockPos canto) {
        boolean pote = mundo.getBlockState(canto.offset(1, 0, 1)).getBlock() instanceof PoteDeAlquimia
                && PoteDeAlquimia.cantoDe(canto.offset(1, 0, 1), mundo.getBlockState(canto.offset(1, 0, 1))).equals(canto);
        // Os cristais do chão viram energia na hora (e somem)
        ItemEntity cristais = SaqueDoAndar.soltar(mundo, heroi.getX(), heroi.getY(), heroi.getZ(), new ItemStack(ModItens.CRISTAL_DE_ENERGIA.get(), 5));
        CristalDeEnergia.aoEncostar(new ItemEntityPickupEvent.Pre(heroi, cristais));
        boolean dosCristais = EnergiaAlquimica.energiaDe(heroi) == 5 && cristais.isRemoved();
        // Energizar uma poção desconhecida dá 6 e identifica; a de força, se já é conhecida, dá 10
        long semente = Identificacao.sementeDaRun(heroi);
        MenuDaAlquimia menu = menu(heroi, canto);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.GELO));
        int mostrou = menu.energiaAoEnergizar();
        boolean energizou = menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_ENERGIZAR);
        boolean identificou = Identificacao.conhece(heroi, ((ItemPocao) Identificacao.pocao(semente, TipoDePocao.GELO).getItem()).cor());
        int depoisDaPocao = EnergiaAlquimica.energiaDe(heroi);
        Identificacao.identificar(heroi, TipoDePocao.FORCA);
        menu.getSlot(1).set(Identificacao.pocao(semente, TipoDePocao.FORCA));
        int daForca = menu.energiaAoEnergizar();
        // Uma semente não dá energia (o botão fica apagado)
        menu.getSlot(1).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.CEGUERVA)));
        int daSemente = menu.energiaAoEnergizar();
        menu.removed(heroi);
        PixelDungeon.LOGGER.info("Teste da energia alquímica: o pote tem 4 pedaços = {}, os cristais deram 5 = {}, a poção mostrou {} e deu {}, "
                + "identificou = {}, a de força conhecida dá {}, a semente dá {}, o que sobrou voltou pro inventário = {}", pote, dosCristais, mostrou,
                energizou ? depoisDaPocao - 5 : 0, identificou, daForca, daSemente, temNoInventario(heroi, ItemSemente.daPlanta(TipoDePlanta.CEGUERVA)));
    }

    // ---- Sementes em poção ----

    private static void testarSementes(ServerLevel mundo, FakePlayer heroi, BlockPos canto) {
        heroi.getInventory().clearContent();
        long semente = Identificacao.sementeDaRun(heroi);
        MenuDaAlquimia menu = menu(heroi, canto);
        // 2 sementes ainda não é receita
        menu.getSlot(0).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.CEGUERVA), 2));
        menu.getSlot(1).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.CEGUERVA)));
        boolean duasNao = menu.custo() < 0;
        // 3 cegervas: uma poção de invisibilidade, de graça, já identificada; o que sobrou da pilha continua lá
        menu.getSlot(2).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.CEGUERVA)));
        boolean receita = menu.custo() == 0 && menu.exemplo().is(ModItens.POCAO_ALEATORIA.get());
        int energia = EnergiaAlquimica.energiaDe(heroi);
        boolean misturou = menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_MISTURAR);
        ItemStack resultado = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem();
        boolean invisibilidade = Identificacao.tipoDaPocao(semente, resultado) == TipoDePocao.INVISIBILIDADE;
        boolean identificada = resultado.getItem() instanceof ItemPocao pocao && Identificacao.conhece(heroi, pocao.cor());
        boolean gastou = menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty();
        boolean deGraca = EnergiaAlquimica.energiaDe(heroi) == energia;
        // Sementes diferentes também dão uma poção (às vezes qualquer uma)
        int pocoes = 0;
        for (int i = 0; i < 20; i++) {
            menu.getSlot(0).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.FLOR_DE_FOGO)));
            menu.getSlot(1).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.MUDA_DE_GELO)));
            menu.getSlot(2).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.SOFRIMUSGO)));
            if (menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_MISTURAR) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().getItem() instanceof ItemPocao) {
                pocoes++;
            }
        }
        // A poção anterior, que ninguém pegou, foi pro inventário a cada mistura
        int noInventario = 0;
        for (ItemStack pilha : heroi.getInventory().items) {
            if (pilha.getItem() instanceof ItemPocao) {
                noInventario += pilha.getCount();
            }
        }
        menu.removed(heroi);
        PixelDungeon.LOGGER.info("Teste das sementes no pote: 2 sementes não é receita = {}, 3 sementes é receita de graça = {}, misturou = {}, "
                + "saiu invisibilidade = {}, identificada = {}, gastou 1 de cada = {}, não gastou energia = {}, 20 misturas diferentes deram {} poções, "
                + "{} foram pro inventário", duasNao, receita, misturou, invisibilidade, identificada, gastou, deGraca, pocoes, noInventario);
    }

    // ---- Pedras rúnicas ----

    private static void testarPedrasNoPote(FakePlayer heroi, BlockPos canto, DadosDaRun run) {
        heroi.getInventory().clearContent();
        long semente = Identificacao.sementeDaRun(heroi);
        // Um pergaminho que o herói ainda não conhece
        TipoDePergaminho desconhecido = null;
        for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
            if (!RegrasDoHeroi.dados(heroi).conhece(tipo.chaveDeConhecimento())) {
                desconhecido = tipo;
                break;
            }
        }
        MenuDaAlquimia menu = menu(heroi, canto);
        boolean previa = false;
        boolean duasPedras = false;
        boolean identificou = false;
        boolean gastou = false;
        if (desconhecido != null) {
            menu.getSlot(0).set(Identificacao.pergaminho(semente, desconhecido).copyWithCount(2));
            previa = menu.custo() == 0 && menu.exemplo().is(ModItens.PEDRA_ALEATORIA.get()) && menu.exemplo().getCount() == 2;
            menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_MISTURAR);
            ItemStack resultado = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem();
            duasPedras = resultado.is(ModItens.pedra(TipoDePedra.doPergaminho(desconhecido))) && resultado.getCount() == 2;
            identificou = RegrasDoHeroi.dados(heroi).conhece(desconhecido.chaveDeConhecimento());
            gastou = menu.getSlot(0).getItem().getCount() == 1;
            // Agora que é conhecido, o pote já mostra as pedras certas
            menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
            menu.getSlot(0).set(Identificacao.pergaminho(semente, desconhecido));
            previa = previa && menu.exemplo().is(ModItens.pedra(TipoDePedra.doPergaminho(desconhecido)));
        }
        menu.removed(heroi);
        int energiaDoChoque = ReceitasDaAlquimia.energia(heroi, new ItemStack(ModItens.pedra(TipoDePedra.CHOQUE), 2));
        int energiaDoEncantamento = ReceitasDaAlquimia.energia(heroi, new ItemStack(ModItens.pedra(TipoDePedra.ENCANTAMENTO)));
        // No chão: nunca as raras; e uma pedra de intuição a mais nos andares 1 a 4
        boolean nuncaRaras = true;
        Random sorteio = new Random(5);
        for (int i = 0; i < 300; i++) {
            ItemStack pedra = SaqueDoAndar.pedraAleatoria(sorteio);
            if (pedra.is(ModItens.pedra(TipoDePedra.ENCANTAMENTO)) || pedra.is(ModItens.pedra(TipoDePedra.ALTERACAO))) {
                nuncaRaras = false;
            }
        }
        PixelDungeon.LOGGER.info("Teste das pedras no pote: o pergaminho desconhecido mostra 2 pedras rúnicas e depois as certas = {}, deu 2 pedras = {}, "
                + "identificou o pergaminho = {}, gastou 1 = {}, energia de 2 de choque = {}, de 1 de encantamento = {}, no chão nunca as raras = {}, "
                + "pedra de intuição a mais nos andares 1 a 4 = {}", previa, duasPedras, identificou, gastou, energiaDoChoque, energiaDoEncantamento,
                nuncaRaras, run.quedas(SaqueDoAndar.PEDRA_DE_INTUICAO));
    }

    // Uma faixa de chão de pedra longe de tudo; cada pedra fica num pedaço dela (6 quadradinhos de largura)
    static BlockPos plataforma(ServerLevel mundo, DadosDaRun run) {
        return plataforma(mundo, run, 2700);
    }

    static BlockPos plataforma(ServerLevel mundo, DadosDaRun run, int desvio) {
        BlockPos perto = run.andar(1).entrada().offset(desvio, 0, 0);
        BlockPos base = new BlockPos(Math.floorDiv(perto.getX(), T) * T, perto.getY(), Math.floorDiv(perto.getZ(), T) * T);
        for (int x = 0; x < 8 * 6 * T; x++) {
            for (int z = 0; z < 7 * T; z++) {
                mundo.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 4; y++) {
                    mundo.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        return base;
    }

    // O bloco de chão do quadradinho (qx, qz) da plataforma
    static BlockPos quadradinho(BlockPos base, int qx, int qz) {
        return base.offset(qx * T, -1, qz * T);
    }

    static RatoMarsupial rato(ServerLevel mundo, BlockPos base, int qx, int qz) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        Vec3 meio = Armadilhas.centro(quadradinho(base, qx, qz));
        rato.moveTo(meio.x, meio.y, meio.z);
        mundo.addFreshEntity(rato);
        return rato;
    }

    private static void cair(ServerLevel mundo, TipoDePedra tipo, BlockPos base, int qx, int qz, Entity dono) {
        Vec3 meio = Armadilhas.centro(quadradinho(base, qx, qz));
        EfeitosDasPedras.cair(mundo, tipo, meio, meio, null, dono);
    }

    private static void testarPedrasArremessadas(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = plataforma(mundo, run);
        FakePlayer heroi = heroi(mundo, run);
        Vec3 longe = Armadilhas.centro(quadradinho(base, 47, 6));
        heroi.moveTo(longe.x, longe.y, longe.z);
        boolean noLugar = EfeitosDasPedras.quadradinhoEm(mundo, Armadilhas.centro(quadradinho(base, 2, 3))).equals(quadradinho(base, 2, 3));

        // Agressão: o rato marcado vira o alvo do outro rato
        RatoMarsupial marcado = rato(mundo, base, 1, 3);
        RatoMarsupial outro = rato(mundo, base, 3, 3);
        cair(mundo, TipoDePedra.AGRESSAO, base, 1, 3, heroi);
        boolean agressao = marcado.hasEffect(ModEfeitos.AGRESSAO) && EfeitosNoTick.alvoDaAgressao(outro) == marcado;

        // Medo e sono profundo
        RatoMarsupial medroso = rato(mundo, base, 8, 3);
        cair(mundo, TipoDePedra.MEDO, base, 8, 3, heroi);
        boolean medo = medroso.hasEffect(ModEfeitos.ATERRORIZADO);
        RatoMarsupial dorminhoco = rato(mundo, base, 10, 3);
        cair(mundo, TipoDePedra.SONO_PROFUNDO, base, 10, 3, heroi);
        boolean dormiu = dorminhoco.dormindo() && dorminhoco.hasEffect(ModEfeitos.SONO_MAGICO);
        Combate.aplicar(dorminhoco, 1, mundo.damageSources().generic());
        boolean acordou = !dorminhoco.dormindo() && !dorminhoco.hasEffect(ModEfeitos.SONO_MAGICO);

        // Explosão: fere o do meio e o do lado, não o de 2 quadradinhos; some com a semente e deixa a espada
        RatoMarsupial meio = rato(mundo, base, 14, 3);
        RatoMarsupial lado = rato(mundo, base, 15, 3);
        RatoMarsupial longeDaExplosao = rato(mundo, base, 16, 3);
        Vec3 centro = Armadilhas.centro(quadradinho(base, 14, 3));
        ItemEntity semente = SaqueDoAndar.soltar(mundo, centro.x + 0.5, centro.y, centro.z, new ItemStack(ItemSemente.daPlanta(TipoDePlanta.CEGUERVA)));
        ItemEntity espada = SaqueDoAndar.soltar(mundo, centro.x - 0.5, centro.y, centro.z, new ItemStack(ModItens.ESPADA.get()));
        cair(mundo, TipoDePedra.EXPLOSAO, base, 14, 3, heroi);
        boolean explodiu = feridoOuMorto(meio) && feridoOuMorto(lado) && !feridoOuMorto(longeDaExplosao);
        boolean itens = semente.isRemoved() && !espada.isRemoved();

        // Teletransporte: quem arremessou (aqui, um rato) vai pro quadradinho onde ela caiu
        RatoMarsupial viajante = rato(mundo, base, 20, 1);
        cair(mundo, TipoDePedra.TELETRANSPORTE, base, 23, 4, viajante);
        boolean teletransportou = EfeitosDasPedras.quadradinhoEm(mundo, viajante.position()).equals(quadradinho(base, 23, 4));

        // Rebanho: uma ovelha em cada quadradinho livre até 2 de distância (5x5)
        int ovelhas = EfeitosDasPedras.rebanho(mundo, quadradinho(base, 28, 3));

        // Choque: paralisa quem está até 2 quadradinhos dali (o de 3 não) e carrega a varinha de quem arremessou
        RatoMarsupial choque0 = rato(mundo, base, 38, 3);
        RatoMarsupial choque1 = rato(mundo, base, 39, 4);
        RatoMarsupial choque2 = rato(mundo, base, 40, 3);
        RatoMarsupial choque3 = rato(mundo, base, 41, 3);
        ItemStack varinha = new ItemStack(ModItens.varinha(TipoDeVarinha.MISSEL_MAGICO));
        Cargas.definir(varinha, mundo.getGameTime(), ItemVarinha.cargasMaximas(varinha), 0);
        heroi.getInventory().clearContent();
        heroi.getInventory().add(varinha);
        int atingidos = EfeitosDasPedras.chocar(mundo, quadradinho(base, 38, 3), heroi);
        boolean paralisou = choque0.hasEffect(ModEfeitos.PARALISADO) && choque1.hasEffect(ModEfeitos.PARALISADO) && choque2.hasEffect(ModEfeitos.PARALISADO)
                && !choque3.hasEffect(ModEfeitos.PARALISADO);
        boolean carregou = ItemVarinha.cargas(heroi.getInventory().getItem(0)) > 0;

        PixelDungeon.LOGGER.info("Teste das pedras arremessadas: quadradinho certo = {}, agressão = {}, medo = {}, sono profundo = {}, acordou ao apanhar = {}, "
                + "explosão feriu os 3x3 = {}, a semente sumiu e a espada não = {}, teletransporte = {}, {} ovelhas, choque atingiu {} e paralisou = {}, "
                + "carregou a varinha = {}", noLugar, agressao, medo, dormiu, acordou, explodiu, itens, teletransportou, ovelhas, atingidos, paralisou, carregou);
        for (Entity entidade : mundo.getEntitiesOfClass(Entity.class, new AABB(base).inflate(8 * 6 * T, 8, 8 * 6 * T))) {
            if (entidade instanceof RatoMarsupial || entidade instanceof OvelhaMagica || entidade instanceof ItemEntity) {
                entidade.discard();
            }
        }
    }

    private static boolean feridoOuMorto(LivingEntity criatura) {
        return !criatura.isAlive() || criatura.getHealth() < criatura.getMaxHealth();
    }

    // Clarividência num andar de verdade: esconde uma armadilha do andar e a pedra, caindo perto, revela
    private static void testarClarividencia(ServerLevel mundo, DadosDaRun run) {
        for (int andar = 2; andar <= 4; andar++) {
            PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
            BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
            for (int y = 0; y < planta.altura(); y++) {
                for (int x = 0; x < planta.largura(); x++) {
                    TipoDeArmadilha tipo = planta.armadilha(x, y);
                    if (tipo == null || !tipo.escondivel()) {
                        continue;
                    }
                    BlockPos controle = Armadilhas.controle(origem, x, y);
                    Armadilhas.colocar(mundo, controle, tipo, true);
                    boolean escondida = Armadilhas.escondidaEm(mundo, origem, x, y);
                    // Cai num quadradinho livre (sem armadilha) a 3 de distância, pra não ativar nada
                    int[][] lados = { { 3, 0 }, { -3, 0 }, { 0, 3 }, { 0, -3 } };
                    int[] lado = lados[0];
                    for (int[] tentativa : lados) {
                        if (planta.passavel(x + tentativa[0], y + tentativa[1]) && planta.armadilha(x + tentativa[0], y + tentativa[1]) == null) {
                            lado = tentativa;
                            break;
                        }
                    }
                    Vec3 perto = Armadilhas.centro(Armadilhas.controle(origem, x + lado[0], y + lado[1]));
                    EfeitosDasPedras.cair(mundo, TipoDePedra.CLARIVIDENCIA, perto, perto, null, null);
                    PixelDungeon.LOGGER.info("Teste da pedra da clarividência: a armadilha estava escondida = {}, foi revelada = {}", escondida,
                            !Armadilhas.escondidaEm(mundo, origem, x, y));
                    return;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste da pedra da clarividência: nenhuma armadilha que se esconde nos andares 2 a 4");
    }

    // As de usar num item, pelo caminho de verdade: a escolha do item (e a janelinha de opções) do lado do servidor
    private static void testarPedrasDeUsar(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = heroi(mundo, run);
        Inventory inventario = heroi.getInventory();
        long semente = Identificacao.sementeDaRun(heroi);

        // Encantamento: a espada amaldiçoada perde a maldição e ganha um encantamento; a armadura ganha um glifo
        inventario.setItem(0, new ItemStack(ModItens.pedra(TipoDePedra.ENCANTAMENTO), 2));
        ItemStack espada = new ItemStack(ModItens.ESPADA.get());
        espada.set(ModComponentes.MALDICAO.get(), Maldicao.IRRITANTE.id());
        inventario.setItem(1, espada);
        inventario.setItem(2, new ItemStack(ModItens.ARMADURA_DE_COURO.get()));
        EfeitosDasPedras.usar(heroi, TipoDePedra.ENCANTAMENTO);
        EscolhaDeItem.escolheu(heroi, 1);
        EfeitosDasPedras.usar(heroi, TipoDePedra.ENCANTAMENTO);
        EscolhaDeItem.escolheu(heroi, 2);
        boolean encantou = NivelDoItem.encantamento(inventario.getItem(1)) != null && !NivelDoItem.amaldicoado(inventario.getItem(1))
                && NivelDoItem.glifo(inventario.getItem(2)) != null && inventario.getItem(0).isEmpty();

        // Detecção mágica: descobre a maldição da arma sem identificar
        inventario.clearContent();
        inventario.setItem(0, new ItemStack(ModItens.pedra(TipoDePedra.DETECCAO_MAGICA)));
        ItemStack escondida = new ItemStack(ModItens.ESPADA.get());
        escondida.set(ModComponentes.MALDICAO.get(), Maldicao.IRRITANTE.id());
        escondida.set(ModComponentes.IDENTIFICADO.get(), false);
        escondida.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        inventario.setItem(1, escondida);
        EfeitosDasPedras.usar(heroi, TipoDePedra.DETECCAO_MAGICA);
        EscolhaDeItem.escolheu(heroi, 1);
        boolean detectou = NivelDoItem.maldicaoConhecida(inventario.getItem(1)) && !NivelDoItem.identificado(inventario.getItem(1))
                && inventario.getItem(0).isEmpty();

        // Alteração: a espada fica mais rápida (70% do dano), a armadura com mais evasão (menos defesa)
        inventario.clearContent();
        inventario.setItem(0, new ItemStack(ModItens.pedra(TipoDePedra.ALTERACAO), 2));
        inventario.setItem(1, new ItemStack(ModItens.ESPADA.get()));
        inventario.setItem(2, new ItemStack(ModItens.ARMADURA_DE_COURO.get()));
        EfeitosDasPedras.usar(heroi, TipoDePedra.ALTERACAO);
        EscolhaDeItem.escolheu(heroi, 1);
        EscolhaDeOpcao.escolheu(heroi, 0);
        EfeitosDasPedras.usar(heroi, TipoDePedra.ALTERACAO);
        EscolhaDeItem.escolheu(heroi, 2);
        EscolhaDeOpcao.escolheu(heroi, 0);
        ItemStack rapida = inventario.getItem(1);
        ItemStack evasiva = inventario.getItem(2);
        boolean arma = Alteracao.de(rapida) == Alteracao.VELOCIDADE && Alteracao.dano(rapida, 10) == 7
                && !java.util.Objects.equals(rapida.get(DataComponents.ATTRIBUTE_MODIFIERS), new ItemStack(ModItens.ESPADA.get()).get(DataComponents.ATTRIBUTE_MODIFIERS));
        ArmaduraDoShattered couro = ModItens.ARMADURA_DE_COURO.get();
        boolean armadura = Alteracao.de(evasiva) == Alteracao.EVASAO && couro.defesaMaxima(evasiva, 0) == couro.defesaMaxima(0) - 2
                && Alteracao.esquiva(evasiva) == 4;
        boolean gastouAsDuas = inventario.getItem(0).isEmpty();
        // Desistir na janelinha não gasta a pedra
        inventario.setItem(0, new ItemStack(ModItens.pedra(TipoDePedra.ALTERACAO)));
        EfeitosDasPedras.usar(heroi, TipoDePedra.ALTERACAO);
        EscolhaDeItem.escolheu(heroi, 1);
        EscolhaDeOpcao.escolheu(heroi, -1);
        boolean desistiu = inventario.getItem(0).getCount() == 1 && Alteracao.de(inventario.getItem(1)) == Alteracao.VELOCIDADE;

        // Intuição: o primeiro palpite (errado) não gasta a pedra; o segundo (certo) gasta e identifica
        inventario.clearContent();
        TipoDePocao desconhecida = null;
        for (TipoDePocao tipo : TipoDePocao.values()) {
            if (!RegrasDoHeroi.dados(heroi).conhece(tipo.chaveDeConhecimento())) {
                desconhecida = tipo;
                break;
            }
        }
        boolean errouSemGastar = false;
        boolean acertouGastando = false;
        if (desconhecida != null) {
            inventario.setItem(0, new ItemStack(ModItens.pedra(TipoDePedra.INTUICAO)));
            inventario.setItem(1, Identificacao.pocao(semente, desconhecida));
            RegrasDoHeroi.dados(heroi).definir(EfeitosDasPedras.INTUICAO_USADA, 0);
            List<Object> palpites = EfeitosDasPedras.tiposDesconhecidos(heroi, inventario.getItem(1));
            int certo = palpites.indexOf(desconhecida);
            int errado = certo == 0 ? 1 : 0;
            if (palpites.size() > 1) {
                EfeitosDasPedras.usar(heroi, TipoDePedra.INTUICAO);
                EscolhaDeItem.escolheu(heroi, 1);
                EscolhaDeOpcao.escolheu(heroi, errado);
                errouSemGastar = inventario.getItem(0).getCount() == 1 && !RegrasDoHeroi.dados(heroi).conhece(desconhecida.chaveDeConhecimento());
            }
            EfeitosDasPedras.usar(heroi, TipoDePedra.INTUICAO);
            EscolhaDeItem.escolheu(heroi, 1);
            EscolhaDeOpcao.escolheu(heroi, EfeitosDasPedras.tiposDesconhecidos(heroi, inventario.getItem(1)).indexOf(desconhecida));
            acertouGastando = inventario.getItem(0).isEmpty() && RegrasDoHeroi.dados(heroi).conhece(desconhecida.chaveDeConhecimento());
        }
        PixelDungeon.LOGGER.info("Teste das pedras de usar: encantamento = {}, detecção mágica = {}, alteração da arma = {}, da armadura = {}, "
                + "gastou as duas = {}, desistir não gasta = {}, intuição errou sem gastar = {}, acertou gastando = {}", encantou, detectou, arma, armadura,
                gastouAsDuas, desistiu, errouSemGastar, acertouGastando);
        inventario.clearContent();
    }

    // ---- Poções exóticas ----

    private static void testarPocoesExoticas(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        BlockPos base = plataforma(mundo, run, 2950);
        FakePlayer heroi = heroi(mundo, run);
        Inventory inventario = heroi.getInventory();
        long semente = Identificacao.sementeDaRun(heroi);
        Vec3 lugarDoHeroi = Armadilhas.centro(quadradinho(base, 2, 3));
        heroi.moveTo(lugarDoHeroi.x, lugarDoHeroi.y, lugarDoHeroi.z, -90.0F, 0.0F);

        // No pote: a poção + 4 de energia vira a exótica da mesma cor (sem energia, não dá)
        CorDePocao corDaCura = IdentidadesDaRun.corDoTipo(semente, TipoDePocao.CURA);
        MenuDaAlquimia menu = menu(heroi, canto);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.CURA));
        boolean semEnergia = menu.custo() == 4 && !menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_MISTURAR);
        EnergiaAlquimica.ganhar(heroi, 5);
        boolean misturou = menu.clickMenuButton(heroi, MenuDaAlquimia.BOTAO_MISTURAR);
        ItemStack exotica = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem();
        boolean daMesmaCor = exotica.getItem() instanceof ItemPocaoExotica pocao && pocao.cor() == corDaCura;
        boolean gastou4 = EnergiaAlquimica.energiaDe(heroi) == 1;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.removed(heroi);
        int energia = ReceitasDaAlquimia.energia(heroi, new ItemStack(ItemPocaoExotica.daCor(corDaCura)));
        PixelDungeon.LOGGER.info("Teste das poções exóticas no pote: sem energia não mistura = {}, misturou = {}, saiu a exótica da mesma cor = {}, "
                + "gastou 4 de energia = {}, energizar uma exótica dá {}", semEnergia, misturou, daMesmaCor, gastou4, energia);

        // Beber
        inventario.clearContent();
        beber(heroi, semente, TipoDePocao.CURA);
        boolean blindagem = Escudos.barreira(heroi) >= Math.round(0.6F * heroi.getMaxHealth() + 10);
        boolean identificou = Identificacao.conhece(heroi, corDaCura);
        beber(heroi, semente, TipoDePocao.PRESSA);
        boolean vigor = heroi.hasEffect(ModEfeitos.VIGOR);
        RatoMarsupial visto = rato(mundo, base, 10, 0);
        beber(heroi, semente, TipoDePocao.VISAO_MENTAL);
        boolean visao = heroi.hasEffect(ModEfeitos.VISAO_MAGICA) && visto.hasEffect(MobEffects.GLOWING);
        beber(heroi, semente, TipoDePocao.GAS_PARALISANTE);
        MobEffectInstance barro = heroi.getEffect(ModEfeitos.ARMADURA_DE_BARRO);
        boolean armaduraDeBarro = barro != null && barro.getAmplifier() == 2 + RegrasDoHeroi.dados(heroi).nivel() / 3 - 1;
        heroi.addEffect(new MobEffectInstance(ModEfeitos.VENENO, 10 * Turno.TICKS));
        beber(heroi, semente, TipoDePocao.PUREZA);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, 10 * Turno.TICKS));
        boolean limpeza = !heroi.hasEffect(ModEfeitos.VENENO) && heroi.hasEffect(ModEfeitos.PURIFICADO) && !heroi.hasEffect(ModEfeitos.ALEIJADO);
        // Bafo de dragão: o herói olha pro leste; o rato da frente queima, o de trás não
        RatoMarsupial naFrente = rato(mundo, base, 5, 3);
        RatoMarsupial atras = rato(mundo, base, 0, 3);
        beber(heroi, semente, TipoDePocao.CHAMA_LIQUIDA);
        boolean bafo = naFrente.isOnFire() && naFrente.hasEffect(ModEfeitos.ALEIJADO) && !atras.isOnFire();
        boolean gastouTodas = inventario.isEmpty();
        PixelDungeon.LOGGER.info("Teste das poções exóticas de beber: blindagem = {}, identificou = {}, vigor = {}, visão mágica = {}, "
                + "armadura de barro = {}, limpeza = {}, bafo de dragão = {}, gastou as poções = {}", blindagem, identificou, vigor, visao, armaduraDeBarro,
                limpeza, bafo, gastouTodas);

        // Maestria (já conhecida): a espada escolhida pede 2 de força a menos; a poção só se gasta na escolha
        inventario.clearContent();
        Identificacao.identificar(heroi, TipoDePocao.FORCA);
        CorDePocao corDaForca = IdentidadesDaRun.corDoTipo(semente, TipoDePocao.FORCA);
        inventario.setItem(0, new ItemStack(ItemPocaoExotica.daCor(corDaForca)));
        inventario.setItem(1, new ItemStack(ModItens.ESPADA.get()));
        EfeitosDePocaoExotica.beber(heroi, corDaForca);
        boolean esperou = inventario.getItem(0).getCount() == 1;
        EscolhaDeItem.escolheu(heroi, 1);
        ItemStack espada = inventario.getItem(1);
        boolean maestria = esperou && inventario.getItem(0).isEmpty()
                && ModItens.ESPADA.get().forcaNecessaria(espada) == ModItens.ESPADA.get().forcaNecessaria(0) - 2;
        // Inspiração divina: desconhecida, gasta na hora (conhecida, só na escolha); escolher o tier 1 dá 2 pontos a mais nele
        inventario.clearContent();
        CorDePocao corDaExperiencia = IdentidadesDaRun.corDoTipo(semente, TipoDePocao.EXPERIENCIA);
        boolean conhecia = Identificacao.conhece(heroi, corDaExperiencia);
        inventario.setItem(0, new ItemStack(ItemPocaoExotica.daCor(corDaExperiencia)));
        int antes = RegrasDoHeroi.dados(heroi).pontosLivres(1);
        EfeitosDePocaoExotica.beber(heroi, corDaExperiencia);
        boolean gastouNaHoraCerta = conhecia ? inventario.getItem(0).getCount() == 1 : inventario.getItem(0).isEmpty();
        EscolhaDeOpcao.escolheu(heroi, 0);
        boolean inspiracao = gastouNaHoraCerta && inventario.getItem(0).isEmpty() && RegrasDoHeroi.dados(heroi).pontosLivres(1) == antes + 2
                && EfeitosDePocaoExotica.tiersSemInspiracao(RegrasDoHeroi.dados(heroi)).size() == Talento.TIERS - 1;
        PixelDungeon.LOGGER.info("Teste das poções exóticas de escolher: maestria = {}, inspiração divina = {}", maestria, inspiracao);

        // Arremessar
        RatoMarsupial congelado = rato(mundo, base, 14, 3);
        RatoMarsupial doLado = rato(mundo, base, 15, 3);
        EfeitosDePocaoExotica.quebrar(mundo, Armadilhas.centro(quadradinho(base, 14, 3)), TipoDePocaoExotica.CONGELAMENTO_INSTANTANEO, corDaCura, heroi, null);
        boolean congelou = congelado.hasEffect(ModEfeitos.CONGELADO) && congelado.hasEffect(ModEfeitos.ENRAIZADO) && doLado.hasEffect(ModEfeitos.CONGELADO);
        Vec3 nuvens = Armadilhas.centro(quadradinho(base, 22, 3));
        EfeitosDePocaoExotica.quebrar(mundo, nuvens, TipoDePocaoExotica.GAS_CORROSIVO, corDaCura, heroi, null);
        EfeitosDePocaoExotica.quebrar(mundo, nuvens, TipoDePocaoExotica.NEBLINA, corDaCura, heroi, null);
        boolean corrosivo = false;
        boolean neblina = false;
        for (NuvemDeGas nuvem : mundo.getEntitiesOfClass(NuvemDeGas.class, AABB.ofSize(nuvens, 4, 4, 4))) {
            corrosivo |= nuvem.tipo() == NuvemDeGas.Tipo.CORROSIVO;
            neblina |= nuvem.tipo() == NuvemDeGas.Tipo.NEBLINA;
        }
        boolean espirrou = !EfeitosDePocaoExotica.quebrar(mundo, nuvens, TipoDePocaoExotica.BLINDAGEM, corDaCura, heroi, null);
        PixelDungeon.LOGGER.info("Teste das poções exóticas de arremessar: congelamento instantâneo = {}, gás corrosivo = {}, neblina = {}, "
                + "as boas só espirram = {}", congelou, corrosivo, neblina, espirrou);
        testarTempestade(mundo, run);
        inventario.clearContent();
        for (Entity entidade : mundo.getEntitiesOfClass(Entity.class, new AABB(base).inflate(8 * 6 * T, 8, 8 * 6 * T))) {
            if (entidade instanceof RatoMarsupial || entidade instanceof NuvemDeGas || entidade instanceof ItemEntity) {
                entidade.discard();
            }
        }
    }

    // Bebe a exótica da poção desse tipo (põe uma no inventário e bebe)
    private static void beber(FakePlayer heroi, long semente, TipoDePocao base) {
        CorDePocao cor = IdentidadesDaRun.corDoTipo(semente, base);
        heroi.getInventory().add(new ItemStack(ItemPocaoExotica.daCor(cor)));
        EfeitosDePocaoExotica.beber(heroi, cor);
    }

    // Nuvens tempestuosas num andar de verdade: o chão comum vira água
    private static void testarTempestade(ServerLevel mundo, DadosDaRun run) {
        for (int andar = 2; andar <= 4; andar++) {
            PlantaDoAndar planta = PopulacaoDoAndar.planta(run.semente(), andar);
            BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
            for (int y = 1; y < planta.altura() - 1; y++) {
                for (int x = 1; x < planta.largura() - 1; x++) {
                    if (planta.tipo(x, y) != PlantaDoAndar.CHAO || planta.armadilha(x, y) != null) {
                        continue;
                    }
                    BlockPos controle = Armadilhas.controle(origem, x, y);
                    if (!mundo.getBlockState(controle).is(ModBlocos.CHAO_ESGOTO.get())) {
                        continue;
                    }
                    EfeitosDePocaoExotica.tempestade(mundo, Armadilhas.centro(controle));
                    boolean agua = mundo.getBlockState(controle).is(Blocks.WATER) && planta.tipo(x, y) == PlantaDoAndar.AGUA;
                    boolean nuvem = !mundo.getEntitiesOfClass(NuvemDeGas.class, AABB.ofSize(Armadilhas.centro(controle), 4, 4, 4),
                            gas -> gas.tipo() == NuvemDeGas.Tipo.TEMPESTADE).isEmpty();
                    PixelDungeon.LOGGER.info("Teste das nuvens tempestuosas: o chão virou água = {}, a nuvem de chuva apareceu = {}", agua, nuvem);
                    return;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das nuvens tempestuosas: nenhum chão comum nos andares 2 a 4");
    }

    // ---- Pergaminhos exóticos ----

    private static void testarPergaminhosExoticos(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        BlockPos base = plataforma(mundo, run, 3200);
        FakePlayer heroi = heroi(mundo, run);
        Inventory inventario = heroi.getInventory();
        long semente = Identificacao.sementeDaRun(heroi);
        Vec3 lugarDoHeroi = Armadilhas.centro(quadradinho(base, 4, 3));
        heroi.moveTo(lugarDoHeroi.x, lugarDoHeroi.y, lugarDoHeroi.z, -90.0F, 0.0F);

        // No pote: o mesmo pergaminho serve pra 2 receitas (as pedras de graça e o exótico por 6)
        RunaDePergaminho runaDoTerror = IdentidadesDaRun.runaDoTipo(semente, TipoDePergaminho.TERROR);
        MenuDaAlquimia menu = menu(heroi, canto);
        menu.getSlot(0).set(Identificacao.pergaminho(semente, TipoDePergaminho.TERROR));
        boolean duasReceitas = menu.custo(0) == 0 && menu.custo(1) == 6 && menu.exemplo(1).getItem() instanceof ItemPergaminhoExotico exemplo
                && exemplo.runa() == runaDoTerror;
        EnergiaAlquimica.ganhar(heroi, 6);
        boolean misturou = menu.misturar(heroi, 1);
        ItemStack exotico = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem();
        boolean daMesmaRuna = exotico.getItem() instanceof ItemPergaminhoExotico pergaminho && pergaminho.runa() == runaDoTerror;
        boolean gastou6 = EnergiaAlquimica.energiaDe(heroi) == 0;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.removed(heroi);
        int energia = ReceitasDaAlquimia.energia(heroi, new ItemStack(ItemPergaminhoExotico.daRuna(runaDoTerror)));
        PixelDungeon.LOGGER.info("Teste dos pergaminhos exóticos no pote: o pergaminho mostra as 2 receitas = {}, misturou o exótico = {}, "
                + "da mesma runa = {}, gastou 6 de energia = {}, energizar um exótico dá {}", duasReceitas, misturou, daMesmaRuna, gastou6, energia);

        // Ler
        inventario.clearContent();
        ler(heroi, semente, TipoDePergaminho.IDENTIFICACAO);
        int conhecidos = 0;
        for (TipoDePocao tipo : TipoDePocao.values()) {
            conhecidos += RegrasDoHeroi.dados(heroi).conhece(tipo.chaveDeConhecimento()) ? 1 : 0;
        }
        for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
            conhecidos += RegrasDoHeroi.dados(heroi).conhece(tipo.chaveDeConhecimento()) ? 1 : 0;
        }
        for (TipoDeAnel tipo : TipoDeAnel.values()) {
            conhecidos += RegrasDoHeroi.dados(heroi).conhece(tipo.chaveDeConhecimento()) ? 1 : 0;
        }
        // (o próprio de identificação ficou conhecido ao ler; mais os 4 que a adivinhação descobriu)
        boolean adivinhacao = conhecidos >= 5;
        ler(heroi, semente, TipoDePergaminho.REMOVER_MALDICAO);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.SONO_MAGICO, 10 * Turno.TICKS));
        // (o herói de mentira nunca se machuca: a magia que não machuca é testada num rato imune)
        RatoMarsupial imune = rato(mundo, base, 30, 3);
        imune.addEffect(new MobEffectInstance(ModEfeitos.ANTIMAGIA, 10 * Turno.TICKS));
        Combate.aplicar(imune, 3, mundo.damageSources().magic());
        boolean antimagia = heroi.hasEffect(ModEfeitos.ANTIMAGIA) && !heroi.hasEffect(ModEfeitos.SONO_MAGICO) && imune.getHealth() == imune.getMaxHealth();
        imune.discard();
        heroi.removeEffect(ModEfeitos.ANTIMAGIA);
        ler(heroi, semente, TipoDePergaminho.IMAGEM_ESPELHADA);
        boolean prismatica = false;
        for (ImagemEspelhada imagem : mundo.getEntitiesOfClass(ImagemEspelhada.class, heroi.getBoundingBox().inflate(8))) {
            prismatica |= imagem.prismatica() && imagem.getMaxHealth() == 10 + (int) Math.floor(RegrasDoHeroi.dados(heroi).nivel() * 2.5F);
        }
        ler(heroi, semente, TipoDePergaminho.RECARGA);
        boolean energiaMistica = heroi.hasEffect(ModEfeitos.RECARGA_DE_ARTEFATO);
        ler(heroi, semente, TipoDePergaminho.MAPEAMENTO_MAGICO);
        boolean videncia = heroi.hasEffect(ModEfeitos.VIDENCIA);
        boolean passagem = EfeitosDePergaminhoExotico.andarDaPassagem(1) == 1 && EfeitosDePergaminhoExotico.andarDaPassagem(4) == 1
                && EfeitosDePergaminhoExotico.andarDaPassagem(5) == 1 && EfeitosDePergaminhoExotico.andarDaPassagem(9) == 6;
        // Desafio: os inimigos vêm atrás e o herói fica na arena
        RatoMarsupial chamado = rato(mundo, base, 14, 3);
        ler(heroi, semente, TipoDePergaminho.FURIA);
        boolean desafio = heroi.hasEffect(ModEfeitos.DESAFIO) && EfeitosDePergaminhoExotico.naArena(heroi);
        chamado.discard();
        // Pavor e explosão psiônica nos ratos à vista
        RatoMarsupial medroso = rato(mundo, base, 6, 3);
        ler(heroi, semente, TipoDePergaminho.TERROR);
        boolean pavor = medroso.hasEffect(ModEfeitos.PAVOR);
        medroso.discard();
        RatoMarsupial explodido = rato(mundo, base, 6, 3);
        ler(heroi, semente, TipoDePergaminho.RETRIBUICAO);
        boolean psionica = !explodido.isAlive() && heroi.hasEffect(ModEfeitos.CEGO) && heroi.hasEffect(ModEfeitos.FRACO);
        boolean gastouTodos = inventario.isEmpty();
        PixelDungeon.LOGGER.info("Teste dos pergaminhos exóticos de ler: adivinhação = {}, antimagia = {}, imagem prismática = {}, energia mística = {}, "
                + "vidência = {}, passagem = {}, desafio = {}, pavor = {}, explosão psiônica = {}, gastou os pergaminhos = {}", adivinhacao, antimagia,
                prismatica, energiaMistica, videncia, passagem, desafio, pavor, psionica, gastouTodos);

        // Encantamento (conhecido): escolhe a espada e o encantamento do meio (o incomum)
        heroi.setHealth(heroi.getMaxHealth());
        inventario.clearContent();
        Identificacao.identificar(heroi, TipoDePergaminho.MELHORIA);
        RunaDePergaminho runaDaMelhoria = IdentidadesDaRun.runaDoTipo(semente, TipoDePergaminho.MELHORIA);
        inventario.setItem(0, new ItemStack(ItemPergaminhoExotico.daRuna(runaDaMelhoria)));
        inventario.setItem(1, new ItemStack(ModItens.ESPADA.get()));
        EfeitosDePergaminhoExotico.ler(heroi, runaDaMelhoria);
        EscolhaDeItem.escolheu(heroi, 1);
        boolean esperou = inventario.getItem(0).getCount() == 1;
        EscolhaDeOpcao.escolheu(heroi, 1);
        Encantamento encanto = NivelDoItem.encantamento(inventario.getItem(1));
        boolean encantamento = esperou && inventario.getItem(0).isEmpty() && encanto != null && encanto.raridade() == Encantamento.Raridade.INCOMUM;
        // Metamorfose (conhecida): o primeiro talento vira um de outra classe
        Identificacao.identificar(heroi, TipoDePergaminho.TRANSMUTACAO);
        RunaDePergaminho runaDaTransmutacao = IdentidadesDaRun.runaDoTipo(semente, TipoDePergaminho.TRANSMUTACAO);
        inventario.setItem(0, new ItemStack(ItemPergaminhoExotico.daRuna(runaDaTransmutacao)));
        Talento antigo = RegrasDoHeroi.dados(heroi).talentos(1).get(0);
        EfeitosDePergaminhoExotico.ler(heroi, runaDaTransmutacao);
        EscolhaDeOpcao.escolheu(heroi, 0);
        EscolhaDeOpcao.escolheu(heroi, 0);
        Talento novo = RegrasDoHeroi.dados(heroi).talentos(1).get(0);
        boolean metamorfose = inventario.getItem(0).isEmpty() && novo != antigo && novo.classe() != antigo.classe() && novo.tier() == 1
                && !RegrasDoHeroi.dados(heroi).temTalento(antigo) && RegrasDoHeroi.dados(heroi).temTalento(novo);
        // Canto da sereia: o rato na mira vira aliado pra sempre; o outro fica encantado
        RatoMarsupial seduzido = rato(mundo, base, 6, 3);
        RatoMarsupial encantado = rato(mundo, base, 6, 4);
        EfeitosDePergaminhoExotico.cantar(heroi, seduzido);
        boolean sereia = seduzido.aliado() && seduzido.hasEffect(ModEfeitos.DOMINADO) && encantado.hasEffect(ModEfeitos.ENCANTADO) && !encantado.aliado();
        PixelDungeon.LOGGER.info("Teste dos pergaminhos exóticos de escolher: encantamento = {}, metamorfose = {}, canto da sereia = {}", encantamento,
                metamorfose, sereia);
        inventario.clearContent();
        for (Entity entidade : mundo.getEntitiesOfClass(Entity.class, new AABB(base).inflate(8 * 6 * T, 8, 8 * 6 * T))) {
            if (entidade instanceof RatoMarsupial || entidade instanceof ImagemEspelhada || entidade instanceof ItemEntity) {
                entidade.discard();
            }
        }
    }

    // Lê o exótico do pergaminho desse tipo (põe um no inventário e lê)
    private static void ler(FakePlayer heroi, long semente, TipoDePergaminho base) {
        RunaDePergaminho runa = IdentidadesDaRun.runaDoTipo(semente, base);
        heroi.getInventory().add(new ItemStack(ItemPergaminhoExotico.daRuna(runa)));
        EfeitosDePergaminhoExotico.ler(heroi, runa);
    }

    // ---- Comidas ----

    private static void testarComidas(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = heroi(mundo, run);
        MenuDaAlquimia menu = menu(heroi, canto);
        // 3 carnes misteriosas: as 3 receitas de ensopado (1 por 1, 2 por 2, 3 por 2)
        menu.getSlot(0).set(new ItemStack(ModItens.CARNE_MISTERIOSA.get(), 3));
        boolean tresReceitas = menu.custo(0) == 1 && menu.custo(1) == 2 && menu.custo(2) == 2 && menu.exemplo(2).getCount() == 3
                && menu.exemplo(2).is(ModItens.ENSOPADO.get());
        EnergiaAlquimica.ganhar(heroi, 2 + 6 + 2);
        boolean ensopado = menu.misturar(heroi, 2) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().getCount() == 3
                && menu.getSlot(0).getItem().isEmpty();
        // Salgado + ração + carne: torta de carne
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(ModItens.PASTEL.get()));
        menu.getSlot(1).set(new ItemStack(ModItens.RACAO.get()));
        menu.getSlot(2).set(new ItemStack(ModItens.ENSOPADO.get()));
        boolean torta = menu.custo(0) == 6 && menu.misturar(heroi, 0) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().is(ModItens.TORTA_DE_CARNE.get());
        // Frutinsossa + semente de grama-solar: fruta-solar
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(ModItens.FRUTA_SEM_GRACA.get()));
        menu.getSlot(1).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.GRAMA_SOLAR)));
        boolean fruta = menu.custo(0) == 2 && menu.misturar(heroi, 0)
                && FrutaSemGraca.pocao(menu.getSlot(MenuDaAlquimia.RESULTADO).getItem()) == TipoDePocao.CURA;
        boolean semEnergia = EnergiaAlquimica.energiaDe(heroi) == 0;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.removed(heroi);
        PixelDungeon.LOGGER.info("Teste das comidas no pote: 3 carnes mostram 3 receitas = {}, ensopado = {}, torta de carne = {}, fruta-solar = {}, "
                + "gastou a energia = {}", tresReceitas, ensopado, torta, fruta, semEnergia);

        // Comer: a fruta-solar cura (igual à poção), a crua não dá; a torta deixa bem alimentado
        heroi.getInventory().clearContent();
        ItemStack solar = FrutaSemGraca.cozida(TipoDePocao.CURA, 1);
        solar.finishUsingItem(mundo, heroi);
        boolean comeuFruta = heroi.hasEffect(ModEfeitos.CURANDO);
        boolean cruaNaoComida = !new ItemStack(ModItens.FRUTA_SEM_GRACA.get()).has(DataComponents.FOOD) && solar.isEmpty();
        new ItemStack(ModItens.TORTA_DE_CARNE.get()).finishUsingItem(mundo, heroi);
        boolean bemAlimentado = heroi.hasEffect(ModEfeitos.BEM_ALIMENTADO);
        boolean quebra = FrutaSemGraca.quebra(TipoDePocao.GAS_TOXICO) && FrutaSemGraca.quebra(TipoDePocao.PUREZA) && !FrutaSemGraca.quebra(TipoDePocao.CURA);
        // A carne misteriosa no fogo vira carne grelhada
        BlockPos base = plataforma(mundo, run, 3450);
        BlockPos fogo = base.offset(2, 0, 2);
        mundo.setBlockAndUpdate(fogo, Blocks.FIRE.defaultBlockState());
        ItemEntity carne = SaqueDoAndar.soltar(mundo, fogo.getX() + 0.5, fogo.getY(), fogo.getZ() + 0.5, new ItemStack(ModItens.CARNE_MISTERIOSA.get(), 2));
        carne.getItem().getItem().onEntityItemUpdate(carne.getItem(), carne);
        boolean grelhou = carne.getItem().is(ModItens.CARNE_GRELHADA.get()) && carne.getItem().getCount() == 2;
        carne.discard();
        mundo.setBlockAndUpdate(fogo, Blocks.AIR.defaultBlockState());
        // O arbusto de frutinsossa: quem pisa colhe uma fruta
        BlockPos arbusto = base.offset(6, 0, 6);
        mundo.setBlockAndUpdate(arbusto, ModBlocos.ARBUSTO_DE_FRUTA.get().defaultBlockState());
        boolean plantou = mundo.getBlockState(arbusto).is(ModBlocos.ARBUSTO_DE_FRUTA.get());
        ArbustoDeFruta.colher(mundo, arbusto);
        boolean colheu = plantou && mundo.getBlockState(arbusto).isAir()
                && !mundo.getEntitiesOfClass(ItemEntity.class, new AABB(arbusto).inflate(1), item -> item.getItem().is(ModItens.FRUTA_SEM_GRACA.get())).isEmpty();
        PixelDungeon.LOGGER.info("Teste das comidas: a fruta-solar cura = {}, a crua não é comida = {}, a torta deixa bem alimentado = {}, "
                + "as frutas certas quebram = {}, a carne no fogo grelhou = {}, o arbusto deu uma fruta = {}", comeuFruta, cruaNaoComida, bemAlimentado,
                quebra, grelhou, colheu);
        for (ItemEntity item : mundo.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(16))) {
            item.discard();
        }
    }

    // ---- O laboratório, o laboratório secreto e o guia de alquimia ----

    private static void testarLaboratorio(DadosDaRun run) {
        // Um laboratório por região, no andar 3 (metade das vezes) ou no 4
        boolean umPorRegiao = true;
        int noTres = 0;
        for (long semente = 1; semente <= 60; semente++) {
            int laboratorios = 0;
            for (int andar = 1; andar <= 4; andar++) {
                if (ComodoEspecial.doAndar(semente, andar).contains(ComodoEspecial.Tipo.LABORATORIO)) {
                    laboratorios++;
                    umPorRegiao &= andar >= 3;
                    noTres += andar == 3 ? 1 : 0;
                }
            }
            umPorRegiao &= laboratorios == 1;
        }
        boolean asVezesNoTres = noTres >= 10 && noTres <= 50;

        PlantaDoAndar planta = null;
        int andarDoLaboratorio = 0;
        for (long semente = 1; semente <= 60 && planta == null; semente++) {
            for (int andar = 3; andar <= 4 && planta == null; andar++) {
                if (ComodoEspecial.doAndar(semente, andar).contains(ComodoEspecial.Tipo.LABORATORIO)) {
                    PlantaDoAndar candidata = GeradorDeAndar.gerar(semente, andar);
                    if (!premiosDoTipo(candidata, PlantaDoAndar.Premio.PAGINA_DO_GUIA).isEmpty()) {
                        planta = candidata;
                        andarDoLaboratorio = andar;
                    }
                }
            }
        }
        if (planta == null) {
            PixelDungeon.LOGGER.info("Teste do laboratório: nenhuma semente com o laboratório");
            return;
        }
        int[] pote = acharPote(planta);
        PlantaDoAndar.Sala sala = planta.salaEm(pote[0], pote[1]);
        int[] porta = acharPorta(planta, sala, PlantaDoAndar.PORTA_TRANCADA);
        int direita = sala.x() + sala.largura() - 1;
        int baixo = sala.y() + sala.altura() - 1;
        boolean noCanto = sala.especial() && (pote[0] == sala.x() || pote[0] == direita) && (pote[1] == sala.y() || pote[1] == baixo);
        boolean doLadoOposto = porta != null && (porta[0] == sala.x() - 1 ? pote[0] == direita
                : porta[0] == direita + 1 ? pote[0] == sala.x() : porta[1] == sala.y() - 1 ? pote[1] == baixo : pote[1] == sala.y());
        boolean trancada = porta != null && planta.chavesDeFerro() >= 1;

        List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(planta, andarDoLaboratorio, run);
        boolean cristais = false;
        for (PlantaDoAndar.PremioNoChao premio : premiosDoTipo(planta, PlantaDoAndar.Premio.CRISTAIS_DO_LABORATORIO)) {
            ItemStack item = itemDoSaque(saques, premio);
            cristais = item.is(ModItens.CRISTAL_DE_ENERGIA.get()) && item.getCount() == 5;
        }
        List<PlantaDoAndar.PremioNoChao> premios = premiosDoTipo(planta, PlantaDoAndar.Premio.PREMIO_DO_LABORATORIO);
        boolean premiosCertos = premios.size() >= 1 && premios.size() <= 2;
        for (PlantaDoAndar.PremioNoChao premio : premios) {
            ItemStack item = itemDoSaque(saques, premio);
            // (o catalisador mágico, se ainda não caiu nesta run, é o primeiro prêmio)
            premiosCertos &= item.getItem() instanceof ItemPocao || item.getItem() instanceof ItemPedraRunica || item.is(ModItens.CATALISADOR_MAGICO.get());
        }
        List<PlantaDoAndar.PremioNoChao> paginas = premiosDoTipo(planta, PlantaDoAndar.Premio.PAGINA_DO_GUIA);
        boolean pagina = paginas.size() == 1 && itemDoSaque(saques, paginas.get(0)).is(ModItens.PAGINA_DO_GUIA.get());
        PixelDungeon.LOGGER.info("Teste do laboratório: um por região no andar 3 ou 4 = {}, às vezes no 3 = {}, o pote no canto = {}, "
                + "do lado oposto à porta = {}, porta trancada com chave = {}, 5 cristais = {}, os prêmios = {}, a página do guia = {}",
                umPorRegiao, asVezesNoTres, noCanto, doLadoOposto, trancada, cristais, premiosCertos, pagina);
    }

    private static void testarLaboratorioSecreto(DadosDaRun run) {
        for (long semente = 1; semente <= 400; semente++) {
            for (int andar = 2; andar <= 4; andar++) {
                if (!ComodoSecreto.secretasDoAndar(semente, andar).contains(ComodoSecreto.Tipo.LABORATORIO)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                List<PlantaDoAndar.PremioNoChao> montes = premiosDoTipo(planta, PlantaDoAndar.Premio.CRISTAIS_DO_LABORATORIO_SECRETO);
                if (montes.isEmpty()) {
                    continue;
                }
                int[] pote = acharPote(planta);
                PlantaDoAndar.Sala sala = planta.salaEm(pote[0], pote[1]);
                boolean noMeio = sala.especial() && Math.abs(pote[0] - (sala.x() + (sala.largura() - 1) / 2.0)) <= 0.5
                        && Math.abs(pote[1] - (sala.y() + (sala.altura() - 1) / 2.0)) <= 0.5;
                boolean escondida = acharPorta(planta, sala, PlantaDoAndar.PORTA_ESCONDIDA) != null;
                List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(planta, andar, run);
                boolean cristais = montes.size() == 2;
                for (PlantaDoAndar.PremioNoChao monte : montes) {
                    ItemStack item = itemDoSaque(saques, monte);
                    cristais &= item.is(ModItens.CRISTAL_DE_ENERGIA.get()) && item.getCount() >= 3 && item.getCount() <= 5;
                }
                List<PlantaDoAndar.PremioNoChao> pocoes = premiosDoTipo(planta, PlantaDoAndar.Premio.POCAO_DO_LABORATORIO_SECRETO);
                Set<TipoDePocao> tipos = EnumSet.noneOf(TipoDePocao.class);
                for (PlantaDoAndar.PremioNoChao pocao : pocoes) {
                    TipoDePocao tipo = Identificacao.tipoDaPocao(run.semente(), itemDoSaque(saques, pocao));
                    if (tipo != null) {
                        tipos.add(tipo);
                    }
                }
                boolean diferentes = pocoes.size() >= 2 && pocoes.size() <= 3 && tipos.size() == pocoes.size() && !tipos.contains(TipoDePocao.FORCA);
                PixelDungeon.LOGGER.info("Teste do laboratório secreto: o pote no meio = {}, porta escondida = {}, 2 montes de 3 a 5 cristais = {}, "
                        + "de 2 a 3 poções diferentes = {}", noMeio, escondida, cristais, diferentes);
                return;
            }
        }
        PixelDungeon.LOGGER.info("Teste do laboratório secreto: nenhuma semente com o laboratório secreto");
    }

    private static void testarPaginasDoGuia(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = heroi(mundo, run);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.definirPaginasDoGuia(0);
        ItemStack paginas = new ItemStack(ModItens.PAGINA_DO_GUIA.get(), GuiaDeAlquimia.PAGINAS.length + 1);
        int primeira = PaginaDoGuia.ler(heroi, paginas);
        boolean leuAPrimeira = primeira == 0 && paginas.getCount() == GuiaDeAlquimia.PAGINAS.length && GuiaDeAlquimia.conhece(dados.paginasDoGuia(), 0);
        for (int i = 1; i < GuiaDeAlquimia.PAGINAS.length; i++) {
            PaginaDoGuia.ler(heroi, paginas);
        }
        int todas = (1 << GuiaDeAlquimia.PAGINAS.length) - 1;
        boolean leuTodas = dados.paginasDoGuia() == todas && paginas.getCount() == 1;
        boolean sobrou = PaginaDoGuia.ler(heroi, paginas) == -1 && paginas.getCount() == 1;
        // Igual ao diário do Shattered, continuam de uma run pra outra (e são salvas com o jogador)
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        boolean continuam = dados.paginasDoGuia() == todas;
        DadosDoHeroi.Serializador serializador = new DadosDoHeroi.Serializador();
        CompoundTag salvo = serializador.write(dados, mundo.registryAccess());
        boolean salvas = serializador.read(heroi, salvo, mundo.registryAccess()).paginasDoGuia() == todas;
        dados.definirPaginasDoGuia(0);
        PixelDungeon.LOGGER.info("Teste das páginas do guia: leu a primeira = {}, leu todas = {}, a que sobrou não gasta = {}, "
                + "continuam na run nova = {}, são salvas = {}", leuAPrimeira, leuTodas, sobrou, continuam, salvas);
    }

    // ---- O kit do alquimista ----

    private static void testarKit(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = heroi(mundo, run);
        KitDoAlquimista tipo = ModItens.KIT_DO_ALQUIMISTA.get();
        heroi.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(tipo));
        ItemStack kit = heroi.getOffhandItem();
        // Equipado, ele esquenta: no nível 0, 100 turnos (o tempo do jogo não anda no teste, cada chamada é um turno)
        ArtefatoAchado.turnoDoHeroi(heroi);
        boolean esquentando = !KitDoAlquimista.pronto(kit) && !tipo.usar(heroi, kit);
        for (int i = 0; i < 100; i++) {
            ArtefatoAchado.turnoDoHeroi(heroi);
        }
        boolean pronto = KitDoAlquimista.pronto(kit);
        // Um nível de herói inteiro: 2 + nível do kit de energia
        ArtefatoAchado.xpDoHeroi(heroi, 1.0F);
        boolean guardou = KitDoAlquimista.energiaGuardada(kit) == 2;
        // Misturar sem pote: a poção exótica custa 4; sai 2 do kit e 2 do herói
        MenuDaAlquimia menu = new MenuDaAlquimia(1, heroi.getInventory(), ContainerLevelAccess.NULL, true);
        EnergiaAlquimica.ganhar(heroi, 3);
        menu.getSlot(0).set(Identificacao.pocao(run.semente(), TipoDePocao.CURA));
        boolean somou = menu.energiaTotal() == 5 && menu.energiaNoKit() == 2 && menu.custo(0) == 4;
        boolean misturou = menu.misturar(heroi, 0) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().getItem() instanceof ItemPocaoExotica
                && KitDoAlquimista.energiaGuardada(kit) == 0 && EnergiaAlquimica.energiaDe(heroi) == 1 && menu.stillValid(heroi);
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        // Energizar: 6 de energia do herói por nível
        EnergiaAlquimica.ganhar(heroi, 12);
        tipo.energizar(heroi, kit, 2);
        boolean energizou = ArtefatoAchado.nivel(kit) == 2 && EnergiaAlquimica.energiaDe(heroi) == 1;
        // Tirou e equipou de novo: esquenta outra vez, agora em (10 - 2)² = 64 turnos
        kit.set(ModComponentes.EQUIPADO_EM.get(), mundo.getGameTime() - 1000);
        ArtefatoAchado.turnoDoHeroi(heroi);
        boolean deNovo = KitDoAlquimista.aquecimento(kit) == 100F;
        for (int i = 0; i < 63; i++) {
            ArtefatoAchado.turnoDoHeroi(heroi);
        }
        boolean quase = !KitDoAlquimista.pronto(kit);
        ArtefatoAchado.turnoDoHeroi(heroi);
        boolean maisRapido = quase && KitDoAlquimista.pronto(kit);
        // Sem o kit nas mãos, a tela da alquimia sem pote fecha
        heroi.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        boolean fechou = !menu.stillValid(heroi);
        menu.removed(heroi);
        boolean noSaque = ModItens.artefatos().contains(ModItens.KIT_DO_ALQUIMISTA);
        PixelDungeon.LOGGER.info("Teste do kit do alquimista: esquenta ao equipar = {}, pronto em 100 turnos = {}, guardou energia com a experiência = {}, "
                + "a tela soma a energia do kit = {}, misturou gastando o kit primeiro = {}, energizou 2 níveis = {}, equipar de novo esquenta = {}, "
                + "no nível 2 esquenta em 64 turnos = {}, sem o kit a tela fecha = {}, aparece na dungeon = {}", esquentando, pronto, guardou, somou,
                misturou, energizou, deNovo, maisRapido, fechou, noSaque);
    }

    // ---- A antimagia nos itens mágicos ----

    private static void testarAntimagia(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = heroi(mundo, run);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.ANTIMAGIA, 20 * Turno.TICKS));
        // O pergaminho não é lido (nem gasto)
        ItemStack pergaminho = Identificacao.pergaminho(run.semente(), TipoDePergaminho.IDENTIFICACAO);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, pergaminho);
        boolean naoLeu = !pergaminho.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND).getResult().consumesAction() && pergaminho.getCount() == 1
                && !Identificacao.conhece(heroi, ((ItemPergaminho) pergaminho.getItem()).runa());
        // A varinha não dispara (nem gasta carga)
        ItemStack varinha = new ItemStack(ModItens.varinha(TipoDeVarinha.MISSEL_MAGICO));
        heroi.setItemInHand(InteractionHand.MAIN_HAND, varinha);
        int cargas = ItemVarinha.cargas(varinha);
        boolean naoDisparou = !varinha.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND).getResult().consumesAction()
                && ItemVarinha.cargas(varinha) == cargas;
        heroi.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // O anel não dá bônus
        ItemStack anel = Identificacao.anel(run.semente(), TipoDeAnel.PODER);
        heroi.setItemSlot(Aneis.PRIMEIRO, anel);
        Aneis.atualizar(heroi, RegrasDoHeroi.dados(heroi));
        boolean semAnel = RegrasDoHeroi.dados(heroi).bonusDoAnel(TipoDeAnel.PODER) == 0 && RegrasDoHeroi.dados(heroi).usaAnel(TipoDeAnel.PODER);
        heroi.setItemSlot(Aneis.PRIMEIRO, ItemStack.EMPTY);
        Aneis.atualizar(heroi, RegrasDoHeroi.dados(heroi));
        // O kit não guarda energia nem esquenta
        heroi.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItens.KIT_DO_ALQUIMISTA.get()));
        ItemStack kit = heroi.getOffhandItem();
        kit.set(ModComponentes.AQUECIMENTO.get(), 50F);
        kit.set(ModComponentes.EQUIPADO_EM.get(), mundo.getGameTime());
        ArtefatoAchado.turnoDoHeroi(heroi);
        ArtefatoAchado.xpDoHeroi(heroi, 1.0F);
        boolean kitParado = KitDoAlquimista.aquecimento(kit) == 50F && KitDoAlquimista.energiaGuardada(kit) == 0;
        // Passou a antimagia: volta a esquentar (sem começar de novo)
        heroi.removeEffect(ModEfeitos.ANTIMAGIA);
        ArtefatoAchado.turnoDoHeroi(heroi);
        boolean voltou = KitDoAlquimista.aquecimento(kit) == 49F;
        heroi.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        PixelDungeon.LOGGER.info("Teste da antimagia nos itens: o pergaminho não lê = {}, a varinha não dispara = {}, o anel não faz nada = {}, "
                + "o kit fica parado = {}, depois volta a esquentar = {}", naoLeu, naoDisparou, semAnel, kitParado, voltou);
    }

    static List<PlantaDoAndar.PremioNoChao> premiosDoTipo(PlantaDoAndar planta, PlantaDoAndar.Premio tipo) {
        List<PlantaDoAndar.PremioNoChao> lista = new ArrayList<>();
        for (PlantaDoAndar.PremioNoChao premio : planta.premios()) {
            if (premio.premio() == tipo) {
                lista.add(premio);
            }
        }
        return lista;
    }

    static ItemStack itemDoSaque(List<SaqueDoAndar.Saque> saques, PlantaDoAndar.PremioNoChao premio) {
        for (SaqueDoAndar.Saque saque : saques) {
            if (saque.x() == premio.x() && saque.y() == premio.y() && !saque.itens().isEmpty()) {
                return saque.itens().get(0);
            }
        }
        return ItemStack.EMPTY;
    }

    private static int[] acharPote(PlantaDoAndar planta) {
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.tipo(x, y) == PlantaDoAndar.POTE) {
                    return new int[] { x, y };
                }
            }
        }
        return new int[] { -1, -1 };
    }

    // A porta desse tipo na parede em volta da sala (null = não tem)
    private static int[] acharPorta(PlantaDoAndar planta, PlantaDoAndar.Sala sala, byte tipo) {
        for (int y = sala.y() - 1; y <= sala.y() + sala.altura(); y++) {
            for (int x = sala.x() - 1; x <= sala.x() + sala.largura(); x++) {
                boolean naParede = x == sala.x() - 1 || x == sala.x() + sala.largura() || y == sala.y() - 1 || y == sala.y() + sala.altura();
                if (naParede && planta.tipo(x, y) == tipo) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    // ---- Ajudantes ----

    static FakePlayer heroi(ServerLevel mundo, DadosDaRun run) {
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000a1c0"), "TesteDaAlquimia"));
        RegrasDoHeroi.dados(heroi).comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        return heroi;
    }

    static MenuDaAlquimia menu(FakePlayer heroi, BlockPos canto) {
        return new MenuDaAlquimia(1, heroi.getInventory(), ContainerLevelAccess.create(heroi.level(), canto));
    }

    private static boolean temNoInventario(FakePlayer heroi, net.minecraft.world.item.Item item) {
        for (ItemStack pilha : heroi.getInventory().items) {
            if (pilha.is(item)) {
                return true;
            }
        }
        return false;
    }
}
