package com.louzada.pixeldungeon.run;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;
import com.louzada.pixeldungeon.alquimia.ReceitasDaAlquimia;
import com.louzada.pixeldungeon.bloco.PlantaDoShattered;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoEspecial;
import com.louzada.pixeldungeon.dungeon.geracao.ComodoSecreto;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.entidade.BombaAcesa;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.EfeitosDasBombas;
import com.louzada.pixeldungeon.item.EfeitosDeElixir;
import com.louzada.pixeldungeon.item.EfeitosDeInfusao;
import com.louzada.pixeldungeon.item.EfeitosDosFeiticos;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.EscolhaDeOpcao;
import com.louzada.pixeldungeon.item.IdentidadesDaRun;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemBomba;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.MetalLiquido;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.ResinaArcana;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.item.TipoDeElixir;
import com.louzada.pixeldungeon.item.TipoDeFeitico;
import com.louzada.pixeldungeon.item.TipoDeInfusao;
import com.louzada.pixeldungeon.item.TipoDePedra;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

// Só no teste do GitHub: a alquimia que no Shattered aparece na prisão (as bombas, o metal líquido e a resina,
// os compostos e os elixires e os feitiços; os acessórios ficam em TestesDosAcessorios). Usa as plataformas e o herói de mentira do TestesDaAlquimia.
public class TestesDaAlquimiaDaPrisao {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    public static void testar(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        testarBombasNoPote(mundo, run, canto);
        testarBombas(mundo, run);
        testarBarulhenta(mundo, run);
        testarBombasNoSaque(run);
        testarMetalEResina(mundo, run, canto);
        testarInfusoesNoPote(mundo, run, canto);
        testarInfusoes(mundo, run);
        testarElixires(mundo, run);
        testarFeiticosNoPote(mundo, run, canto);
        testarFeiticos(mundo, run);
        TestesDosAcessorios.testar(mundo, run, canto);
    }

    // Mistura a receita (entre as que aparecem) que dá esse item; devolve o que saiu (vazio se não tinha)
    static ItemStack misturarA(MenuDaAlquimia menu, FakePlayer heroi, Item item) {
        for (int i = 0; i < 3; i++) {
            if (menu.custo(i) >= 0 && menu.exemplo(i).is(item) && menu.misturar(heroi, i)) {
                ItemStack saiu = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().copy();
                menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
                return saiu;
            }
        }
        return ItemStack.EMPTY;
    }

    static int custoDe(MenuDaAlquimia menu, Item item) {
        for (int i = 0; i < 3; i++) {
            if (menu.custo(i) >= 0 && menu.exemplo(i).is(item)) {
                return menu.custo(i);
            }
        }
        return -1;
    }

    // ---- Bombas ----

    private static void testarBombasNoPote(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        MenuDaAlquimia menu = TestesDaAlquimia.menu(heroi, canto);
        EnergiaAlquimica.ganhar(heroi, 6);
        // Bomba + poção de gelo desconhecida: não serve; conhecida, vira a bomba de gelo (de graça)
        menu.getSlot(0).set(new ItemStack(ModItens.bomba(TipoDeBomba.COMUM)));
        menu.getSlot(1).set(Identificacao.pocao(run.semente(), TipoDePocao.GELO));
        boolean desconhecida = menu.custo(0) < 0;
        Identificacao.identificar(heroi, TipoDePocao.GELO);
        menu.getSlot(1).set(menu.getSlot(1).getItem().copy());
        boolean gelo = menu.custo(0) == 0 && menu.misturar(heroi, 0) && bomba(menu.getSlot(MenuDaAlquimia.RESULTADO).getItem()) == TipoDeBomba.GELO;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        // Bomba + bolha de gosma: bomba arcana, 6 de energia
        menu.getSlot(0).set(new ItemStack(ModItens.bomba(TipoDeBomba.COMUM)));
        menu.getSlot(1).set(new ItemStack(ModItens.BOLHA_DE_GOSMA.get()));
        boolean arcana = menu.custo(0) == 6 && menu.misturar(heroi, 0) && bomba(menu.getSlot(MenuDaAlquimia.RESULTADO).getItem()) == TipoDeBomba.ARCANA
                && EnergiaAlquimica.energiaDe(heroi) == 0;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.removed(heroi);
        PixelDungeon.LOGGER.info("Teste das bombas no pote: ingrediente desconhecido não serve = {}, bomba de gelo = {}, bomba arcana = {}",
                desconhecida, gelo, arcana);
    }

    private static TipoDeBomba bomba(ItemStack pilha) {
        return pilha.getItem() instanceof ItemBomba bomba ? bomba.tipo() : null;
    }

    private static Vec3 centro(BlockPos base, int qx, int qz) {
        return Armadilhas.centro(TestesDaAlquimia.quadradinho(base, qx, qz));
    }

    private static void testarBombas(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 3700);
        // Comum: o rato do lado leva dano; o de 2 quadradinhos não (a não ser pela outra bomba, que explode junto);
        // a poção do lado quebra
        RatoMarsupial perto = TestesDaAlquimia.rato(mundo, base, 2, 3);
        RatoMarsupial longe = TestesDaAlquimia.rato(mundo, base, 6, 3);
        Vec3 lugarDaPocao = centro(base, 3, 2);
        ItemEntity pocao = new ItemEntity(mundo, lugarDaPocao.x, lugarDaPocao.y, lugarDaPocao.z, Identificacao.pocao(run.semente(), TipoDePocao.CURA));
        mundo.addFreshEntity(pocao);
        BombaAcesa acesa = BombaAcesa.acender(mundo, centro(base, 3, 3), new ItemStack(ModItens.bomba(TipoDeBomba.COMUM)), null);
        acesa.explodir(mundo);
        boolean feriuPerto = perto.getHealth() < perto.getMaxHealth() || !perto.isAlive();
        boolean poupouLonge = longe.getHealth() == longe.getMaxHealth();
        boolean quebrou = pocao.isRemoved();
        // Uma bomba no chão do lado explode junto e pega o rato de longe
        Vec3 lugarDaOutra = centro(base, 5, 3);
        mundo.addFreshEntity(new ItemEntity(mundo, lugarDaOutra.x, lugarDaOutra.y, lugarDaOutra.z, new ItemStack(ModItens.bomba(TipoDeBomba.COMUM))));
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.COMUM, TestesDaAlquimia.quadradinho(base, 4, 3), null);
        boolean emCadeia = longe.getHealth() < longe.getMaxHealth() || !longe.isAlive();

        // Gelo: congela quem estiver a 2 quadradinhos
        RatoMarsupial congelado = TestesDaAlquimia.rato(mundo, base, 12, 3);
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.GELO, TestesDaAlquimia.quadradinho(base, 10, 3), null);
        boolean gelo = !congelado.isAlive() || congelado.hasEffect(ModEfeitos.CONGELADO);
        // Fogo: fogo a 2 quadradinhos
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.FOGO, TestesDaAlquimia.quadradinho(base, 17, 3), null);
        boolean fogo = false;
        BlockPos longeDoFogo = TestesDaAlquimia.quadradinho(base, 19, 3);
        for (BlockPos bloco : BlockPos.betweenClosed(longeDoFogo.offset(0, 1, 0), longeDoFogo.offset(T - 1, 1, T - 1))) {
            fogo |= mundo.getBlockState(bloco).is(BlockTags.FIRE);
        }
        // Lã: ovelhas em volta
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.LA, TestesDaAlquimia.quadradinho(base, 24, 3), null);
        int ovelhas = mundo.getEntitiesOfClass(OvelhaMagica.class, new AABB(centro(base, 24, 3), centro(base, 24, 3)).inflate(5 * T, 4, 5 * T)).size();
        // Fumaça: a nuvem de fumaça
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.FUMACA, TestesDaAlquimia.quadradinho(base, 30, 3), null);
        boolean fumaca = mundo.getEntitiesOfClass(NuvemDeGas.class, new AABB(centro(base, 30, 3), centro(base, 30, 3)).inflate(3))
                .stream().anyMatch(nuvem -> nuvem.tipo() == NuvemDeGas.Tipo.NEBLINA);
        // Clarão: paralisa
        RatoMarsupial paralisado = TestesDaAlquimia.rato(mundo, base, 37, 3);
        paralisado.setHealth(paralisado.getMaxHealth() + 100);
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.CLARAO, TestesDaAlquimia.quadradinho(base, 36, 3), null);
        boolean clarao = !paralisado.isAlive() || paralisado.hasEffect(ModEfeitos.PARALISADO);
        // Brotamento: não machuca nem quebra nada, e nascem plantas e grama alta
        RatoMarsupial poupado = TestesDaAlquimia.rato(mundo, base, 43, 3);
        Vec3 lugarDaSegunda = centro(base, 43, 2);
        ItemEntity segunda = new ItemEntity(mundo, lugarDaSegunda.x, lugarDaSegunda.y, lugarDaSegunda.z, Identificacao.pocao(run.semente(), TipoDePocao.CURA));
        mundo.addFreshEntity(segunda);
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.BROTAMENTO, TestesDaAlquimia.quadradinho(base, 43, 3), null);
        int plantas = 0;
        int capim = 0;
        BlockPos meio = TestesDaAlquimia.quadradinho(base, 43, 3);
        for (BlockPos bloco : BlockPos.betweenClosed(meio.offset(-3 * T, 1, -3 * T), meio.offset(4 * T, 1, 4 * T))) {
            plantas += mundo.getBlockState(bloco).getBlock() instanceof PlantaDoShattered ? 1 : 0;
            capim += mundo.getBlockState(bloco).is(ModBlocos.CAPIM_ALTO.get()) ? 1 : 0;
        }
        boolean brotamento = poupado.getHealth() == poupado.getMaxHealth() && !segunda.isRemoved() && plantas >= 2 && capim > 0;
        // Arcana: o dano ignora a armadura, e não quebra a poção
        // (numa plataforma separada, longe das plantas e das ovelhas das outras bombas)
        BlockPos separada = TestesDaAlquimia.plataforma(mundo, run, 5200);
        RatoMarsupial arcano = TestesDaAlquimia.rato(mundo, separada, 6, 3);
        float vidaAntes = arcano.getHealth();
        Vec3 lugarDaTerceira = centro(separada, 6, 2);
        ItemEntity terceira = new ItemEntity(mundo, lugarDaTerceira.x, lugarDaTerceira.y, lugarDaTerceira.z, Identificacao.pocao(run.semente(), TipoDePocao.CURA));
        mundo.addFreshEntity(terceira);
        EfeitosDasBombas.explodir(mundo, TipoDeBomba.ARCANA, TestesDaAlquimia.quadradinho(separada, 6, 3), null);
        boolean arcana = arcano.getHealth() < vidaAntes && !terceira.isRemoved();
        PixelDungeon.LOGGER.info("Bomba arcana: vida do rato {} -> {} (vivo = {}), poção quebrada = {}", vidaAntes, arcano.getHealth(), arcano.isAlive(),
                terceira.isRemoved());
        PixelDungeon.LOGGER.info("Teste das bombas: fere do lado = {}, não chega a 2 quadradinhos = {}, quebra a poção = {}, explode em cadeia = {}, "
                + "gelo = {}, fogo = {}, lã = {}, fumaça = {}, clarão = {}, brotamento = {}, arcana = {}", feriuPerto, poupouLonge, quebrou, emCadeia,
                gelo, fogo, ovelhas > 5, fumaca, clarao, brotamento, arcana);
        for (OvelhaMagica ovelha : mundo.getEntitiesOfClass(OvelhaMagica.class, new AABB(centro(base, 24, 3), centro(base, 24, 3)).inflate(6 * T, 4, 6 * T))) {
            ovelha.discard();
        }
    }

    private static void testarBarulhenta(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 3950);
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        // Clique direito numa bomba acesa: apaga o pavio e ela volta pro inventário
        BombaAcesa comum = BombaAcesa.acender(mundo, centro(base, 2, 2), new ItemStack(ModItens.bomba(TipoDeBomba.COMUM)), heroi);
        comum.interact(heroi, InteractionHand.MAIN_HAND);
        boolean apagou = comum.isRemoved() && heroi.getInventory().countItem(ModItens.bomba(TipoDeBomba.COMUM)) == 1;
        // A barulhenta: o pavio acaba e ela vira alarme (não explode sozinha nem dá pra pegar)
        BombaAcesa barulhenta = BombaAcesa.acender(mundo, centro(base, 8, 2), new ItemStack(ModItens.bomba(TipoDeBomba.BARULHO)), heroi);
        for (int i = 0; i <= BombaAcesa.PAVIO + 2 * 40; i++) {
            barulhenta.tick();
        }
        boolean disparou = barulhenta.disparada() && !barulhenta.isRemoved();
        barulhenta.interact(heroi, InteractionHand.MAIN_HAND);
        boolean naoPega = !barulhenta.isRemoved();
        // Alguém pisou: explode
        RatoMarsupial rato = TestesDaAlquimia.rato(mundo, base, 8, 2);
        barulhenta.tick();
        boolean explodiu = barulhenta.isRemoved() && (rato.getHealth() < rato.getMaxHealth() || !rato.isAlive());
        PixelDungeon.LOGGER.info("Teste da barulhenta: apaga o pavio = {}, vira alarme = {}, não dá pra pegar = {}, explode quando pisam = {}",
                apagou, disparou, naoPega, explodiu);
    }

    private static void testarBombasNoSaque(DadosDaRun run) {
        // O arsenal: 2 ou 3 prêmios diferentes entre bomba, arma, armadura e arma de arremesso
        boolean arsenal = false;
        boolean bombaNoArsenal = false;
        for (long semente = 1; semente <= 200 && !bombaNoArsenal; semente++) {
            for (int andar = 1; andar <= 4 && !bombaNoArsenal; andar++) {
                if (!ComodoEspecial.doAndar(semente, andar).contains(ComodoEspecial.Tipo.ARSENAL)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                List<PlantaDoAndar.PremioNoChao> bombas = TestesDaAlquimia.premiosDoTipo(planta, PlantaDoAndar.Premio.BOMBA);
                if (bombas.isEmpty()) {
                    continue;
                }
                int premios = bombas.size() + TestesDaAlquimia.premiosDoTipo(planta, PlantaDoAndar.Premio.ARMA).size()
                        + TestesDaAlquimia.premiosDoTipo(planta, PlantaDoAndar.Premio.ARMADURA).size()
                        + TestesDaAlquimia.premiosDoTipo(planta, PlantaDoAndar.Premio.ARREMESSO).size();
                List<SaqueDoAndar.Saque> saques = SaqueDoAndar.sortear(planta, andar, run);
                ItemStack bomba = TestesDaAlquimia.itemDoSaque(saques, bombas.get(0));
                bombaNoArsenal = bombas.size() == 1 && bomba(bomba) == TipoDeBomba.COMUM;
                arsenal = premios >= 2;
            }
        }
        // O arsenal secreto: uma bomba dupla
        boolean dupla = false;
        for (long semente = 1; semente <= 400 && !dupla; semente++) {
            for (int andar = 2; andar <= 4 && !dupla; andar++) {
                if (!ComodoSecreto.secretasDoAndar(semente, andar).contains(ComodoSecreto.Tipo.ARSENAL)) {
                    continue;
                }
                PlantaDoAndar planta = GeradorDeAndar.gerar(semente, andar);
                List<PlantaDoAndar.PremioNoChao> duplas = TestesDaAlquimia.premiosDoTipo(planta, PlantaDoAndar.Premio.BOMBA_DUPLA);
                if (!duplas.isEmpty()) {
                    ItemStack bomba = TestesDaAlquimia.itemDoSaque(SaqueDoAndar.sortear(planta, andar, run), duplas.get(0));
                    dupla = bomba(bomba) == TipoDeBomba.COMUM && bomba.getCount() == 2;
                }
            }
        }
        PixelDungeon.LOGGER.info("Teste das bombas no saque: o arsenal tem bomba = {}, 2 ou 3 prêmios = {}, bomba dupla no arsenal secreto = {}",
                bombaNoArsenal, arsenal, dupla);
    }

    // ---- Metal líquido e resina arcana ----

    private static void testarMetalEResina(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        MenuDaAlquimia menu = TestesDaAlquimia.menu(heroi, canto);
        EnergiaAlquimica.ganhar(heroi, 8);
        // 3 facas de arremesso (tier 1) sem maldição: 3 × 5 × (1 + 1) = 30 de metal líquido, por 3 de energia
        ItemStack facas = new ItemStack(ModItens.FACA_DE_ARREMESSO.get(), 3);
        facas.set(ModComponentes.IDENTIFICADO.get(), false);
        facas.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        menu.getSlot(0).set(facas.copy());
        boolean desconhecida = menu.custo(0) < 0;
        NivelDoItem.identificar(null, facas);
        menu.getSlot(0).set(facas.copy());
        int tier = ((ArmaDeArremesso) facas.getItem()).tier();
        int esperado = (int) Math.round(3 * 5.0 * (tier + 1));
        boolean metal = menu.custo(0) == 3 && menu.misturar(heroi, 0) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().is(ModItens.METAL_LIQUIDO.get())
                && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().getCount() == esperado;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        // Varinha de nível 0: 2 de resina, por 5 de energia
        ItemStack varinha = new ItemStack(ModItens.varinha(TipoDeVarinha.MISSEL_MAGICO));
        NivelDoItem.identificar(null, varinha);
        menu.getSlot(0).set(varinha.copy());
        boolean resina = menu.custo(0) == 5 && menu.misturar(heroi, 0) && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().is(ModItens.RESINA_ARCANA.get())
                && menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().getCount() == 2;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.removed(heroi);
        heroi.getInventory().clearContent();

        // Consertar: metade da durabilidade gasta, 10 de metal por arma inteira (tier 1)
        heroi.getInventory().add(new ItemStack(ModItens.METAL_LIQUIDO.get(), 20));
        ItemStack gastas = new ItemStack(ModItens.FACA_DE_ARREMESSO.get(), 3);
        ArmaDeArremesso.definirDurabilidade(gastas, 50);
        int usou = MetalLiquido.aplicar(heroi, gastas);
        boolean consertou = ArmaDeArremesso.durabilidade(gastas) >= ArmaDeArremesso.DURABILIDADE_MAXIMA - 0.5F && usou == 5
                && heroi.getInventory().countItem(ModItens.METAL_LIQUIDO.get()) == 15;
        // Inteira e completa: não gasta; faltando uma, completa o conjunto
        boolean inteira = MetalLiquido.aplicar(heroi, gastas) == 0;
        ItemStack duas = new ItemStack(ModItens.FACA_DE_ARREMESSO.get(), 2);
        boolean completou = MetalLiquido.aplicar(heroi, duas) == 10 && duas.getCount() == 3;
        // Resina: nível 0 gasta 1, nível 1 gasta 2; no 3 não dá mais
        heroi.getInventory().add(new ItemStack(ModItens.RESINA_ARCANA.get(), 3));
        ItemStack aprimorada = new ItemStack(ModItens.varinha(TipoDeVarinha.MISSEL_MAGICO));
        NivelDoItem.identificar(null, aprimorada);
        int cargasAntes = ItemVarinha.cargas(aprimorada);
        boolean subiu = ResinaArcana.aplicar(heroi, aprimorada) && NivelDoItem.nivel(aprimorada) == 1 && ItemVarinha.cargas(aprimorada) == cargasAntes + 1
                && ResinaArcana.aplicar(heroi, aprimorada) && NivelDoItem.nivel(aprimorada) == 2
                && heroi.getInventory().countItem(ModItens.RESINA_ARCANA.get()) == 0;
        heroi.getInventory().add(new ItemStack(ModItens.RESINA_ARCANA.get(), 9));
        NivelDoItem.definirNivel(aprimorada, 3);
        boolean limite = !ResinaArcana.aplicar(heroi, aprimorada);
        // A resina que veio da resina não volta: varinha +3 com 2 de resina dá 2 × (1 + 1) = 4
        boolean naoVolta = ResinaArcana.resinaDaVarinha(heroi, aprimorada) == 4;
        heroi.getInventory().clearContent();
        PixelDungeon.LOGGER.info("Teste do metal líquido e da resina: maldição desconhecida não serve = {}, metal das armas = {}, resina da varinha = {}, "
                + "conserta = {}, inteira não gasta = {}, completa o conjunto = {}, aprimora a varinha = {}, até o nível 3 = {}, a resina não volta = {}",
                desconhecida, metal, resina, consertou, inteira, completou, subiu, limite, naoVolta);
    }

    // ---- Compostos (infusões) e elixires ----

    private static void testarInfusoesNoPote(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        MenuDaAlquimia menu = TestesDaAlquimia.menu(heroi, canto);
        long semente = run.semente();
        EnergiaAlquimica.ganhar(heroi, 100);
        // Poção de gelo conhecida: a exótica e o composto nevado (8)
        Identificacao.identificar(heroi, TipoDePocao.GELO);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.GELO));
        boolean nevasca = custoDe(menu, ModItens.infusao(TipoDeInfusao.NEVASCA)) == 8
                && !misturarA(menu, heroi, ModItens.infusao(TipoDeInfusao.NEVASCA)).isEmpty();
        // Gás tóxico + bolha de gosma: o cáustico (1)
        Identificacao.identificar(heroi, TipoDePocao.GAS_TOXICO);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.GAS_TOXICO));
        menu.getSlot(1).set(new ItemStack(ModItens.BOLHA_DE_GOSMA.get()));
        boolean caustica = custoDe(menu, ModItens.infusao(TipoDeInfusao.CAUSTICA)) == 1
                && !misturarA(menu, heroi, ModItens.infusao(TipoDeInfusao.CAUSTICA)).isEmpty();
        // Poção desconhecida + semente: o instável (1)
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.PRESSA));
        menu.getSlot(1).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.values()[0])));
        boolean instavel = custoDe(menu, ModItens.infusao(TipoDeInfusao.INSTAVEL)) == 1
                && !misturarA(menu, heroi, ModItens.infusao(TipoDeInfusao.INSTAVEL)).isEmpty();
        menu.getSlot(1).set(ItemStack.EMPTY);
        // Nuvens tempestuosas (exótica) conhecida: 8 compostos aquáticos
        Identificacao.identificar(heroi, TipoDePocao.LEVITACAO);
        menu.getSlot(0).set(new ItemStack(ItemPocaoExotica.daCor(IdentidadesDaRun.corDoTipo(semente, TipoDePocao.LEVITACAO))));
        ItemStack aquaticas = misturarA(menu, heroi, ModItens.infusao(TipoDeInfusao.AQUATICA));
        boolean aquatica = aquaticas.getCount() == 8;
        // Poção de força conhecida: elixir do poder (16)
        Identificacao.identificar(heroi, TipoDePocao.FORCA);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.FORCA));
        boolean poder = custoDe(menu, ModItens.elixir(TipoDeElixir.PODER)) == 16 && !misturarA(menu, heroi, ModItens.elixir(TipoDeElixir.PODER)).isEmpty();
        // Cura + bolha de gosma: rejuvenescimento aquático (6)
        Identificacao.identificar(heroi, TipoDePocao.CURA);
        menu.getSlot(0).set(Identificacao.pocao(semente, TipoDePocao.CURA));
        menu.getSlot(1).set(new ItemStack(ModItens.BOLHA_DE_GOSMA.get()));
        boolean rejuvenescimento = custoDe(menu, ModItens.elixir(TipoDeElixir.REJUVENESCIMENTO_AQUATICO)) == 6;
        menu.removed(heroi);
        heroi.getInventory().clearContent();
        // Energizar: elixir 12, composto 12, 8 aquáticos 12
        boolean energia = ReceitasDaAlquimia.energia(heroi, new ItemStack(ModItens.elixir(TipoDeElixir.PODER))) == 12
                && ReceitasDaAlquimia.energia(heroi, new ItemStack(ModItens.infusao(TipoDeInfusao.NEVASCA))) == 12
                && ReceitasDaAlquimia.energia(heroi, new ItemStack(ModItens.infusao(TipoDeInfusao.AQUATICA), 8)) == 12;
        PixelDungeon.LOGGER.info("Teste dos compostos e elixires no pote: nevado = {}, cáustico = {}, instável = {}, 8 aquáticos = {}, poder = {}, "
                + "rejuvenescimento = {}, energia = {}", nevasca, caustica, instavel, aquatica, poder, rejuvenescimento, energia);
    }

    private static void testarInfusoes(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 4200);
        // Cáustico: muco em quem estiver a 3 quadradinhos
        RatoMarsupial comMuco = TestesDaAlquimia.rato(mundo, base, 5, 3);
        EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.CAUSTICA, centro(base, 2, 3), null);
        boolean caustica = comMuco.hasEffect(ModEfeitos.MUCO_CAUSTICO);
        // Nevado e eletrizado: as nuvens
        EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.NEVASCA, centro(base, 10, 3), null);
        boolean nevasca = !mundo.getEntitiesOfClass(NuvemDeGas.class, new AABB(centro(base, 10, 3), centro(base, 10, 3)).inflate(3))
                .stream().filter(nuvem -> nuvem.tipo() == NuvemDeGas.Tipo.CONGELANTE).toList().isEmpty();
        EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.CHOQUE, centro(base, 18, 3), null);
        boolean choque = !mundo.getEntitiesOfClass(NuvemDeGas.class, new AABB(centro(base, 18, 3), centro(base, 18, 3)).inflate(3))
                .stream().filter(nuvem -> nuvem.tipo() == NuvemDeGas.Tipo.ELETRICO).toList().isEmpty();
        // Infernal: fogo a 2 quadradinhos
        EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.INFERNAL, centro(base, 26, 3), null);
        boolean infernal = false;
        BlockPos longe = TestesDaAlquimia.quadradinho(base, 28, 3);
        for (BlockPos bloco : BlockPos.betweenClosed(longe.offset(0, 1, 0), longe.offset(T - 1, 1, T - 1))) {
            infernal |= mundo.getBlockState(bloco).is(BlockTags.FIRE);
        }
        // Aquático: empurra quem estiver perto
        RatoMarsupial empurrado = TestesDaAlquimia.rato(mundo, base, 35, 3);
        EfeitosDeInfusao.quebrar(mundo, TipoDeInfusao.AQUATICA, centro(base, 34, 3), null);
        boolean aquatica = empurrado.getDeltaMovement().horizontalDistance() > 0.5;
        // Instável: arremessado só sai poção que quebra; bebido, só poção de beber
        boolean instavel = true;
        for (int i = 0; i < 60; i++) {
            TipoDePocao quebra = EfeitosDeInfusao.sortear(mundo.getRandom(), true);
            TipoDePocao bebe = EfeitosDeInfusao.sortear(mundo.getRandom(), false);
            instavel &= quebra == TipoDePocao.GELO || quebra == TipoDePocao.CHAMA_LIQUIDA || quebra == TipoDePocao.GAS_TOXICO
                    || quebra == TipoDePocao.GAS_PARALISANTE || quebra == TipoDePocao.LEVITACAO || quebra == TipoDePocao.PUREZA;
            instavel &= bebe != TipoDePocao.GELO && bebe != TipoDePocao.CHAMA_LIQUIDA && bebe != TipoDePocao.GAS_TOXICO && bebe != TipoDePocao.GAS_PARALISANTE;
        }
        PixelDungeon.LOGGER.info("Teste dos compostos: cáustico = {}, nevado = {}, eletrizado = {}, infernal = {}, aquático = {}, instável = {}",
                caustica, nevasca, choque, infernal, aquatica, instavel);
    }

    private static void testarElixires(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 4450);
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        // Sangue de dragão e toque gélido: os golpes põem fogo e resfriam
        EfeitosDeElixir.beber(heroi, TipoDeElixir.SANGUE_DE_DRAGAO);
        EfeitosDeElixir.beber(heroi, TipoDeElixir.TOQUE_GELIDO);
        RatoMarsupial rato = TestesDaAlquimia.rato(mundo, base, 3, 3);
        for (int i = 0; i < 20; i++) {
            EfeitosDeElixir.aoAcertar(heroi, rato);
        }
        boolean golpes = rato.isOnFire() && rato.hasEffect(ModEfeitos.RESFRIADO);
        // Toque gélido: congelar não pega
        heroi.addEffect(new MobEffectInstance(ModEfeitos.CONGELADO, 2 * Turno.TICKS));
        boolean semFrio = heroi.hasEffect(ModEfeitos.FRIO_INTERIOR) && !heroi.hasEffect(ModEfeitos.CONGELADO);
        // Essência tóxica: solta gás tóxico e veneno não pega
        EfeitosDeElixir.beber(heroi, TipoDeElixir.ESSENCIA_TOXICA);
        heroi.moveTo(centro(base, 8, 3).x, centro(base, 8, 3).y, centro(base, 8, 3).z);
        EfeitosDeElixir.aoTurno(heroi);
        heroi.addEffect(new MobEffectInstance(ModEfeitos.VENENO, 5 * Turno.TICKS));
        boolean toxica = !mundo.getEntitiesOfClass(NuvemDeGas.class, heroi.getBoundingBox().inflate(2)).isEmpty() && !heroi.hasEffect(ModEfeitos.VENENO);
        // Poder: +1 de força e vida a mais, que some em 5 níveis
        int forca = dados.forcaBase();
        EfeitosDeElixir.beber(heroi, TipoDeElixir.PODER);
        boolean poder = dados.forcaBase() == forca + 1 && EfeitosDeElixir.bonusDeVida(dados) == 5;
        EfeitosDeElixir.aoSubirNivel(dados, 3);
        boolean diminui = EfeitosDeElixir.bonusDeVida(dados) == 2;
        EfeitosDeElixir.aoSubirNivel(dados, 2);
        boolean sumiu = EfeitosDeElixir.bonusDeVida(dados) == 0;
        // Armadura arcana: 5 + nível/2 (no nível 1, 5)
        EfeitosDeElixir.beber(heroi, TipoDeElixir.ARMADURA_ARCANA);
        MobEffectInstance arcana = heroi.getEffect(ModEfeitos.ARMADURA_ARCANA);
        boolean armadura = arcana != null && arcana.getAmplifier() == 4;
        // Queda suave e rejuvenescimento: os efeitos
        EfeitosDeElixir.beber(heroi, TipoDeElixir.QUEDA_SUAVE);
        EfeitosDeElixir.beber(heroi, TipoDeElixir.REJUVENESCIMENTO_AQUATICO);
        boolean outros = heroi.hasEffect(ModEfeitos.QUEDA_SUAVE) && heroi.hasEffect(ModEfeitos.REJUVENESCIMENTO);
        heroi.removeAllEffects();
        PixelDungeon.LOGGER.info("Teste dos elixires: fogo e frio nos golpes = {}, imune a frio = {}, essência tóxica = {}, poder = {}, a vida a mais diminui = {}, "
                + "some em 5 níveis = {}, armadura arcana = {}, queda suave e rejuvenescimento = {}", golpes, semFrio, toxica, poder, diminui, sumiu,
                armadura, outros);
    }
    // ---- Feitiços ----

    private static Item feitico(TipoDeFeitico tipo) {
        return ModItens.feitico(tipo);
    }

    private static void testarFeiticosNoPote(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        MenuDaAlquimia menu = TestesDaAlquimia.menu(heroi, canto);
        long semente = run.semente();
        EnergiaAlquimica.ganhar(heroi, 100);
        // Pergaminho desconhecido + pedra rúnica: o instável (1)
        menu.getSlot(0).set(Identificacao.pergaminho(semente, TipoDePergaminho.TERROR));
        menu.getSlot(1).set(new ItemStack(ModItens.pedra(TipoDePedra.INTUICAO)));
        boolean instavel = custoDe(menu, feitico(TipoDeFeitico.INSTAVEL)) == 1 && !misturarA(menu, heroi, feitico(TipoDeFeitico.INSTAVEL)).isEmpty();
        // Semente + pedra rúnica: 8 alquimizar (2)
        menu.getSlot(0).set(new ItemStack(ItemSemente.daPlanta(TipoDePlanta.values()[0])));
        menu.getSlot(1).set(new ItemStack(ModItens.pedra(TipoDePedra.INTUICAO)));
        boolean alquimizar = custoDe(menu, feitico(TipoDeFeitico.ALQUIMIZAR)) == 2
                && misturarA(menu, heroi, feitico(TipoDeFeitico.ALQUIMIZAR)).getCount() == 8;
        menu.getSlot(1).set(ItemStack.EMPTY);
        // Melhoria desconhecida não serve; conhecida, infusão mágica (12)
        menu.getSlot(0).set(Identificacao.pergaminho(semente, TipoDePergaminho.MELHORIA));
        boolean desconhecido = custoDe(menu, feitico(TipoDeFeitico.INFUSAO_MAGICA)) < 0;
        Identificacao.identificar(heroi, TipoDePergaminho.MELHORIA);
        menu.getSlot(0).set(menu.getSlot(0).getItem().copy());
        boolean infusao = desconhecido && custoDe(menu, feitico(TipoDeFeitico.INFUSAO_MAGICA)) == 12
                && !misturarA(menu, heroi, feitico(TipoDeFeitico.INFUSAO_MAGICA)).isEmpty();
        // Teletransporte: 6 mudanças de fase (10); transmutação: 12 reciclar (12); passagem: 5 sinalizadores (12)
        Identificacao.identificar(heroi, TipoDePergaminho.TELETRANSPORTE);
        menu.getSlot(0).set(Identificacao.pergaminho(semente, TipoDePergaminho.TELETRANSPORTE));
        boolean fase = custoDe(menu, feitico(TipoDeFeitico.MUDANCA_DE_FASE)) == 10
                && misturarA(menu, heroi, feitico(TipoDeFeitico.MUDANCA_DE_FASE)).getCount() == 6;
        Identificacao.identificar(heroi, TipoDePergaminho.TRANSMUTACAO);
        menu.getSlot(0).set(Identificacao.pergaminho(semente, TipoDePergaminho.TRANSMUTACAO));
        boolean reciclar = custoDe(menu, feitico(TipoDeFeitico.RECICLAR)) == 12
                && misturarA(menu, heroi, feitico(TipoDeFeitico.RECICLAR)).getCount() == 12;
        menu.getSlot(0).set(new ItemStack(ItemPergaminhoExotico.daRuna(IdentidadesDaRun.runaDoTipo(semente, TipoDePergaminho.TELETRANSPORTE))));
        boolean farol = custoDe(menu, feitico(TipoDeFeitico.FAROL_DE_RETORNO)) == 12
                && misturarA(menu, heroi, feitico(TipoDeFeitico.FAROL_DE_RETORNO)).getCount() == 5;
        // 9 de metal líquido não dá; 10 dão 8 agarrões (10)
        menu.getSlot(0).set(new ItemStack(ModItens.METAL_LIQUIDO.get(), 9));
        boolean poucoMetal = custoDe(menu, feitico(TipoDeFeitico.AGARRAO_TELECINETICO)) < 0;
        menu.getSlot(0).set(new ItemStack(ModItens.METAL_LIQUIDO.get(), 10));
        boolean agarrao = poucoMetal && custoDe(menu, feitico(TipoDeFeitico.AGARRAO_TELECINETICO)) == 10
                && misturarA(menu, heroi, feitico(TipoDeFeitico.AGARRAO_TELECINETICO)).getCount() == 8;
        menu.removed(heroi);
        heroi.getInventory().clearContent();
        // Energizar: 8 alquimizar dão 4, 6 mudanças de fase 12, a infusão mágica 12
        boolean energia = ReceitasDaAlquimia.energia(heroi, new ItemStack(feitico(TipoDeFeitico.ALQUIMIZAR), 8)) == 4
                && ReceitasDaAlquimia.energia(heroi, new ItemStack(feitico(TipoDeFeitico.MUDANCA_DE_FASE), 6)) == 12
                && ReceitasDaAlquimia.energia(heroi, new ItemStack(feitico(TipoDeFeitico.INFUSAO_MAGICA))) == 12;
        PixelDungeon.LOGGER.info("Teste dos feitiços no pote: instável = {}, alquimizar = {}, infusão mágica = {}, mudança de fase = {}, reciclar = {}, "
                + "sinalizador = {}, agarrão = {}, energia = {}", instavel, alquimizar, infusao, fase, reciclar, farol, agarrao, energia);
    }

    private static void testarFeiticos(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 4700);
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        Inventory inventario = heroi.getInventory();
        long semente = run.semente();

        // Instável: com inimigo à vista, só pergaminho de luta; sem, só de explorar
        Vec3 aqui = centro(base, 1, 3);
        heroi.moveTo(aqui.x, aqui.y, aqui.z);
        RatoMarsupial vigia = TestesDaAlquimia.rato(mundo, base, 4, 3);
        boolean instavel = true;
        for (int i = 0; i < 6; i++) {
            instavel &= EfeitosDosFeiticos.deLuta(EfeitosDosFeiticos.instavel(heroi));
        }
        vigia.discard();
        BlockPos longe = TestesDaAlquimia.plataforma(mundo, run, 4950);
        Vec3 sozinho = centro(longe, 1, 3);
        heroi.moveTo(sozinho.x, sozinho.y, sozinho.z);
        for (int i = 0; i < 6; i++) {
            instavel &= EfeitosDosFeiticos.deExplorar(EfeitosDosFeiticos.instavel(heroi));
        }
        heroi.moveTo(aqui.x, aqui.y, aqui.z);
        heroi.removeAllEffects();
        inventario.clearContent();

        // Alquimizar: a pilha escolhida vira energia, e só aí o feitiço é gasto
        Identificacao.identificar(heroi, TipoDePocao.CURA);
        inventario.setItem(0, new ItemStack(feitico(TipoDeFeitico.ALQUIMIZAR), 2));
        ItemStack curas = Identificacao.pocao(semente, TipoDePocao.CURA);
        curas.setCount(2);
        inventario.setItem(2, curas);
        int energiaAntes = EnergiaAlquimica.energiaDe(heroi);
        boolean naoGastouAinda = !EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.ALQUIMIZAR) && inventario.getItem(0).getCount() == 2;
        EscolhaDeItem.escolheu(heroi, 2);
        boolean alquimizar = naoGastouAinda && EnergiaAlquimica.energiaDe(heroi) == energiaAntes + 12 && inventario.getItem(2).isEmpty()
                && inventario.getItem(0).getCount() == 1;
        inventario.clearContent();

        // Infusão mágica: +1 sem perder o encantamento nem a maldição; sem nada pra aprimorar, não gasta
        inventario.setItem(0, new ItemStack(feitico(TipoDeFeitico.INFUSAO_MAGICA), 3));
        boolean semItem = !EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.INFUSAO_MAGICA) && inventario.getItem(0).getCount() == 3;
        ItemStack espada = new ItemStack(ModItens.ESPADA.get());
        NivelDoItem.definirEncantamento(espada, Encantamento.FLAMEJANTE);
        inventario.setItem(1, espada);
        ItemStack armadura = new ItemStack(ModItens.ARMADURA_DE_COURO.get());
        armadura.set(ModComponentes.MALDICAO.get(), Maldicao.IRRITANTE.id());
        inventario.setItem(2, armadura);
        EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.INFUSAO_MAGICA);
        EscolhaDeItem.escolheu(heroi, 1);
        EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.INFUSAO_MAGICA);
        EscolhaDeItem.escolheu(heroi, 2);
        boolean infusao = semItem && NivelDoItem.nivel(inventario.getItem(1)) == 1 && NivelDoItem.encantamento(inventario.getItem(1)) == Encantamento.FLAMEJANTE
                && NivelDoItem.nivel(inventario.getItem(2)) == 1 && NivelDoItem.amaldicoado(inventario.getItem(2)) && inventario.getItem(0).getCount() == 1;
        inventario.clearContent();

        // Reciclar: vira outro do mesmo tipo (a exótica continua exótica); equipamento não dá
        Identificacao.identificar(heroi, TipoDePocao.GELO);
        boolean reciclar = !EfeitosDosFeiticos.reciclavel(new ItemStack(ModItens.ESPADA.get()));
        ItemStack[] antes = { Identificacao.pocao(semente, TipoDePocao.GELO),
                new ItemStack(ItemPergaminhoExotico.daRuna(IdentidadesDaRun.runaDoTipo(semente, TipoDePergaminho.TERROR))),
                new ItemStack(ItemSemente.daPlanta(TipoDePlanta.values()[0])), new ItemStack(ModItens.pedra(TipoDePedra.INTUICAO)) };
        for (ItemStack velho : antes) {
            inventario.clearContent();
            inventario.setItem(0, velho.copy());
            reciclar &= EfeitosDosFeiticos.reciclar(heroi, inventario.getItem(0));
            // O velho sumiu e só sobrou o novo (diferente dele, do mesmo tipo)
            List<ItemStack> sobrou = new ArrayList<>();
            for (int i = 0; i < inventario.getContainerSize(); i++) {
                if (!inventario.getItem(i).isEmpty()) {
                    sobrou.add(inventario.getItem(i));
                }
            }
            reciclar &= sobrou.size() == 1 && sobrou.get(0).getItem() != velho.getItem() && sobrou.get(0).getItem().getClass() == velho.getItem().getClass();
        }
        inventario.clearContent();

        // Mudança de fase: o rato (no andar 1) vai pra longe e fica paralisado; sem alvo, não gasta
        BlockPos entrada = run.andar(1).entrada();
        RatoMarsupial fujao = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        fujao.moveTo(entrada.getX() + 0.5, entrada.getY(), entrada.getZ() + 0.5);
        mundo.addFreshEntity(fujao);
        boolean semAlvo = !EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.MUDANCA_DE_FASE);
        boolean fase = semAlvo && EfeitosDosFeiticos.mudancaDeFase(heroi, fujao) && fujao.position().distanceTo(Vec3.atBottomCenterOf(entrada)) > T
                && fujao.hasEffect(ModEfeitos.PARALISADO);
        fujao.discard();

        // Sinalizador: marcar não gasta; voltar gasta um e o sinalizador continua até o último
        inventario.setItem(0, new ItemStack(feitico(TipoDeFeitico.FAROL_DE_RETORNO), 2));
        boolean marcou = !EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.FAROL_DE_RETORNO) && EfeitosDosFeiticos.temFarol(heroi)
                && inventario.getItem(0).getCount() == 2;
        EfeitosDosFeiticos.lancar(heroi, TipoDeFeitico.FAROL_DE_RETORNO);
        EscolhaDeOpcao.escolheu(heroi, 1);
        boolean voltou = inventario.getItem(0).getCount() == 1 && EfeitosDosFeiticos.temFarol(heroi);
        boolean ultimo = EfeitosDosFeiticos.voltarAoFarol(heroi) && inventario.getItem(0).isEmpty() && !EfeitosDosFeiticos.temFarol(heroi);
        boolean farol = marcou && voltou && ultimo;
        inventario.clearContent();

        // Agarrão: as armas cravadas no inimigo, os itens do quadradinho; sem nada, não pega
        RatoMarsupial espetado = TestesDaAlquimia.rato(mundo, base, 8, 3);
        ArmaArremessada.espetar(espetado, new ItemStack(ModItens.FACA_DE_ARREMESSO.get()));
        ArmaArremessada.espetar(espetado, new ItemStack(ModItens.FACA_DE_ARREMESSO.get()));
        boolean agarrao = EfeitosDosFeiticos.agarrar(heroi, espetado, espetado.position()) == 2 && ArmaArremessada.espetados(espetado) == 0
                && inventario.countItem(ModItens.FACA_DE_ARREMESSO.get()) == 2;
        espetado.discard();
        Vec3 monte = centro(base, 14, 3);
        mundo.addFreshEntity(new ItemEntity(mundo, monte.x, monte.y + 0.2, monte.z, new ItemStack(ModItens.METAL_LIQUIDO.get(), 3)));
        mundo.addFreshEntity(new ItemEntity(mundo, monte.x + 1, monte.y + 0.2, monte.z, new ItemStack(ModItens.BOLHA_DE_GOSMA.get())));
        agarrao &= EfeitosDosFeiticos.agarrar(heroi, null, monte) == 2 && inventario.countItem(ModItens.METAL_LIQUIDO.get()) == 3
                && inventario.countItem(ModItens.BOLHA_DE_GOSMA.get()) == 1;
        agarrao &= EfeitosDosFeiticos.agarrar(heroi, null, centro(base, 20, 3)) == 0;
        inventario.clearContent();
        heroi.removeAllEffects();
        PixelDungeon.LOGGER.info("Teste dos feitiços: instável = {}, alquimizar = {}, infusão mágica = {}, reciclar = {}, mudança de fase = {}, "
                + "sinalizador = {}, agarrão = {}", instavel, alquimizar, infusao, reciclar, fase, farol, agarrao);
    }
}
