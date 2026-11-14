package com.louzada.pixeldungeon.run;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;
import com.louzada.pixeldungeon.alquimia.ReceitasDaAlquimia;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.item.Acessorios;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAcessorio;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

// Só no teste do GitHub: os acessórios (o catalisador mágico no pote e no andar, o aprimoramento, e o que cada um faz).
public class TestesDosAcessorios {
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;

    public static void testar(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        testarNoPote(mundo, run, canto);
        testarNaMasmorra(mundo, run);
        testarNoHeroi(mundo, run);
        for (TipoDeAcessorio tipo : TipoDeAcessorio.values()) {
            Acessorios.definirDaRun(tipo, -1);
        }
    }

    private static boolean perto(float a, float b) {
        return Math.abs(a - b) < 0.001F;
    }

    private static ItemStack acessorio(TipoDeAcessorio tipo, int nivel) {
        ItemStack pilha = new ItemStack(ModItens.acessorio(tipo));
        NivelDoItem.definirNivel(pilha, nivel);
        return pilha;
    }

    // ---- No pote ----

    private static void testarNoPote(ServerLevel mundo, DadosDaRun run, BlockPos canto) {
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        MenuDaAlquimia menu = TestesDaAlquimia.menu(heroi, canto);
        EnergiaAlquimica.ganhar(heroi, 100);
        // O catalisador: 3 acessórios diferentes pra escolher, por 6 de energia, sempre os mesmos
        menu.getSlot(0).set(new ItemStack(ModItens.CATALISADOR_MAGICO.get()));
        Set<Object> opcoes = new HashSet<>();
        boolean tres = true;
        for (int i = 0; i < 3; i++) {
            tres &= menu.custo(i) == 6 && menu.exemplo(i).getItem() instanceof ItemAcessorio && NivelDoItem.nivel(menu.exemplo(i)) == 0;
            opcoes.add(menu.exemplo(i).getItem());
        }
        boolean mesmos = menu.exemplo(1).getItem() == menu.exemplo(1).getItem() && menu.exemplo(2).getItem() == menu.exemplo(2).getItem();
        Object escolhido = menu.exemplo(1).getItem();
        int energiaAntes = EnergiaAlquimica.energiaDe(heroi);
        boolean misturou = menu.misturar(heroi, 1);
        ItemStack saiu = menu.getSlot(MenuDaAlquimia.RESULTADO).getItem().copy();
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        boolean catalisador = tres && opcoes.size() == 3 && mesmos && misturou && saiu.getItem() == escolhido && menu.getSlot(0).getItem().isEmpty()
                && EnergiaAlquimica.energiaDe(heroi) == energiaAntes - 6;
        // Aprimorar: 6, 8 e 10 (o pedaço de papiro, 10, 15 e 20), até o nível 3
        menu.getSlot(0).set(acessorio(TipoDeAcessorio.CRANIO_DE_RATO, 0));
        boolean aprimorar = menu.custo(0) == 6 && menu.misturar(heroi, 0) && NivelDoItem.nivel(menu.getSlot(MenuDaAlquimia.RESULTADO).getItem()) == 1;
        menu.getSlot(MenuDaAlquimia.RESULTADO).set(ItemStack.EMPTY);
        menu.getSlot(0).set(acessorio(TipoDeAcessorio.CRANIO_DE_RATO, 2));
        aprimorar &= menu.custo(0) == 10;
        menu.getSlot(0).set(acessorio(TipoDeAcessorio.PEDACO_DE_PAPIRO, 1));
        aprimorar &= menu.custo(0) == 15;
        menu.getSlot(0).set(acessorio(TipoDeAcessorio.CRANIO_DE_RATO, 3));
        aprimorar &= menu.custo(0) < 0;
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.removed(heroi);
        heroi.getInventory().clearContent();
        boolean energia = ReceitasDaAlquimia.energia(heroi, acessorio(TipoDeAcessorio.TUFO_DE_FURAO, 2)) == 5;
        PixelDungeon.LOGGER.info("Teste dos acessórios no pote: o catalisador = {}, aprimorar = {}, energia = {}", catalisador, aprimorar, energia);
    }

    // ---- Na masmorra ----

    private static int raros(Random sorteio) {
        int albinos = 0;
        for (int i = 0; i < 3000; i++) {
            albinos += PopulacaoDoAndar.tipoAleatorio(1, sorteio) == ModEntidades.RATO_ALBINO.get() ? 1 : 0;
        }
        return albinos;
    }

    private static int contar(PlantaDoAndar planta, byte tipo) {
        int quantos = 0;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                quantos += planta.tipo(x, y) == tipo ? 1 : 0;
            }
        }
        return quantos;
    }

    private static int[] armadilhas(PlantaDoAndar planta) {
        int[] quantas = new int[2];
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.temArmadilha(x, y)) {
                    quantas[0]++;
                    quantas[1] += planta.armadilhaVisivel(x, y) ? 1 : 0;
                }
            }
        }
        return quantas;
    }

    private static void testarNaMasmorra(ServerLevel mundo, DadosDaRun run) {
        // Crânio de rato: o rato albino bem mais comum
        int semCranio = raros(new Random(5));
        Acessorios.definirDaRun(TipoDeAcessorio.CRANIO_DE_RATO, 3);
        int comCranio = raros(new Random(5));
        Acessorios.definirDaRun(TipoDeAcessorio.CRANIO_DE_RATO, -1);
        boolean cranio = comCranio > 2 * Math.max(1, semCranio);

        // Pedaço de papiro no nível 3: nenhuma maldição, e toda arma encantada
        Acessorios.definirDaRun(TipoDeAcessorio.PEDACO_DE_PAPIRO, 3);
        Random sorteio = new Random(7);
        boolean papiro = true;
        for (int i = 0; i < 200; i++) {
            ItemStack espada = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ESPADA.get()), sorteio, true);
            papiro &= !NivelDoItem.amaldicoado(espada) && NivelDoItem.encantamento(espada) != null;
        }
        Acessorios.definirDaRun(TipoDeAcessorio.PEDACO_DE_PAPIRO, -1);

        // Cristais exóticos no nível 3: 80% das poções e pergaminhos viram exóticos (a poção de força, nunca)
        Acessorios.definirDaRun(TipoDeAcessorio.CRISTAIS_EXOTICOS, 3);
        long semente = run.semente();
        int exoticos = 0;
        boolean forcaComum = true;
        for (int i = 0; i < 200; i++) {
            exoticos += Acessorios.talvezExotica(Identificacao.pocao(semente, TipoDePocao.CURA), sorteio, semente).getItem() instanceof ItemPocaoExotica ? 1 : 0;
            exoticos += Acessorios.talvezExotico(Identificacao.pergaminho(semente, TipoDePergaminho.TERROR), sorteio, semente).getItem() instanceof ItemPergaminhoExotico ? 1 : 0;
            forcaComum &= Acessorios.talvezExotica(Identificacao.pocao(semente, TipoDePocao.FORCA), sorteio, semente).getItem() instanceof ItemPocao;
        }
        Acessorios.definirDaRun(TipoDeAcessorio.CRISTAIS_EXOTICOS, -1);
        boolean cristais = exoticos > 260 && exoticos < 380 && forcaComum;

        // Moita de musgo e mecanismo: no nível 3, todos os andares que a semente deixou sem tema (mais ou menos metade) ganham o tema
        // (o musgo dá água ou grama; o mecanismo, armadilhas ou abismo)
        int semTema = 0;
        int comTema = 0;
        int armadilhasDeTema = 0;
        for (long outra = 1; outra <= 40; outra++) {
            if (TemaDoAndar.daSemente(outra, 3) != TemaDoAndar.Tema.NENHUM) {
                continue;
            }
            semTema++;
            TemaDoAndar.Tema doMusgo = TemaDoAndar.sortear(outra, 3, 3, -1).tema();
            comTema += doMusgo == TemaDoAndar.Tema.AGUA || doMusgo == TemaDoAndar.Tema.GRAMA ? 1 : 0;
            TemaDoAndar.Tema doMecanismo = TemaDoAndar.sortear(outra, 3, -1, 3).tema();
            armadilhasDeTema += doMecanismo == TemaDoAndar.Tema.ARMADILHAS || doMecanismo == TemaDoAndar.Tema.ABISMOS ? 1 : 0;
        }
        boolean temas = semTema >= 10 && semTema <= 30 && comTema == semTema && armadilhasDeTema == semTema
                && TemaDoAndar.sortear(1, 1, 3, 3).tema() == TemaDoAndar.Tema.NENHUM;
        // O andar de água tem bem mais água; o de armadilhas, 5x mais armadilhas (as a mais à vista)
        long fingida = 987654321L;
        TemaDoAndar.registrar(fingida, 3, new TemaDoAndar.Decisao(TemaDoAndar.Tema.NENHUM, -1));
        TemaDoAndar.registrar(fingida, 2, new TemaDoAndar.Decisao(TemaDoAndar.Tema.NENHUM, -1));
        int aguaNormal = contar(GeradorDeAndar.gerar(fingida, 3), PlantaDoAndar.AGUA);
        TemaDoAndar.registrar(fingida, 3, new TemaDoAndar.Decisao(TemaDoAndar.Tema.AGUA, -1));
        int aguaDoTema = contar(GeradorDeAndar.gerar(fingida, 3), PlantaDoAndar.AGUA);
        int[] armadilhasNormais = armadilhas(GeradorDeAndar.gerar(fingida, 2));
        TemaDoAndar.registrar(fingida, 2, new TemaDoAndar.Decisao(TemaDoAndar.Tema.ARMADILHAS, 3));
        int[] armadilhasDoTema = armadilhas(GeradorDeAndar.gerar(fingida, 2));
        temas &= aguaDoTema > aguaNormal && armadilhasDoTema[0] > armadilhasNormais[0] && armadilhasDoTema[1] > armadilhasNormais[1];

        // O catalisador cai num dos andares 1 a 4 (no 4, de certeza); com a luneta trincada, o andar tem itens a mais
        DadosDaRun novaRun = new DadosDaRun();
        novaRun.comecarNovaRun(semente);
        boolean catalisador = false;
        for (int andar = 1; andar <= 4; andar++) {
            for (SaqueDoAndar.Saque saque : SaqueDoAndar.sortear(GeradorDeAndar.gerar(semente, andar), andar, novaRun)) {
                catalisador |= saque.itens().stream().anyMatch(item -> item.is(ModItens.CATALISADOR_MAGICO.get()));
            }
        }
        catalisador &= novaRun.quedas(SaqueDoAndar.CATALISADOR) == 1;
        DadosDaRun semLuneta = new DadosDaRun();
        semLuneta.comecarNovaRun(semente);
        int itensSem = SaqueDoAndar.sortear(GeradorDeAndar.gerar(semente, 2), 2, semLuneta).size();
        Acessorios.definirDaRun(TipoDeAcessorio.LUNETA_TRINCADA, 3);
        DadosDaRun comLuneta = new DadosDaRun();
        comLuneta.comecarNovaRun(semente);
        int itensCom = SaqueDoAndar.sortear(GeradorDeAndar.gerar(semente, 2), 2, comLuneta).size();
        Acessorios.definirDaRun(TipoDeAcessorio.LUNETA_TRINCADA, -1);
        boolean luneta = itensCom > itensSem;

        PixelDungeon.LOGGER.info("Teste dos acessórios na masmorra: crânio de rato = {} ({} -> {} albinos), papiro = {}, cristais = {} ({} de 400), "
                + "musgo e mecanismo = {}, o catalisador cai = {}, luneta = {}", cranio, semCranio, comCranio, papiro, cristais, exoticos, temas, catalisador,
                luneta);
    }

    // ---- No herói ----

    private static void testarNoHeroi(ServerLevel mundo, DadosDaRun run) {
        BlockPos base = TestesDaAlquimia.plataforma(mundo, run, 5450);
        FakePlayer heroi = TestesDaAlquimia.heroi(mundo, run);
        Inventory inventario = heroi.getInventory();
        Vec3 aqui = Armadilhas.centro(TestesDaAlquimia.quadradinho(base, 1, 3));
        heroi.moveTo(aqui.x, aqui.y, aqui.z);

        // O nível que vale: o maior no inventário
        inventario.setItem(0, acessorio(TipoDeAcessorio.TREVO_DE_TREZE_FOLHAS, 1));
        inventario.setItem(1, acessorio(TipoDeAcessorio.TREVO_DE_TREZE_FOLHAS, 3));
        boolean nivel = Acessorios.nivel(heroi, TipoDeAcessorio.TREVO_DE_TREZE_FOLHAS) == 3 && Acessorios.nivel(heroi, TipoDeAcessorio.TUFO_DE_FURAO) == -1;

        // Trevo no nível 3: o dano sai sempre no mínimo ou no máximo
        RandomSource sorteio = heroi.getRandom();
        boolean trevo = true;
        Acessorios.marcarTrevo(3);
        for (int i = 0; i < 100; i++) {
            int dano = Acessorios.danoDoHeroi(sorteio, 2, 12);
            trevo &= dano == 2 || dano == 12;
        }
        Acessorios.marcarTrevo(-1);
        inventario.clearContent();

        // Olho de salamandra: o rato a 2 quadradinhos brilha, o de 8 não
        inventario.setItem(0, acessorio(TipoDeAcessorio.OLHO_DE_SALAMANDRA, 0));
        RatoMarsupial perto = TestesDaAlquimia.rato(mundo, base, 3, 3);
        RatoMarsupial longe = TestesDaAlquimia.rato(mundo, base, 9, 3);
        Acessorios.visaoMental(heroi);
        boolean olho = perto.hasEffect(MobEffects.GLOWING) && !longe.hasEffect(MobEffects.GLOWING);

        // Incensário caótico: solta um gás perto do inimigo
        inventario.setItem(1, acessorio(TipoDeAcessorio.INCENSARIO_CAOTICO, 0));
        boolean incensario = Acessorios.soltarGas(heroi, longe) != null && Acessorios.alvoDoIncensario(heroi) != null;
        perto.discard();
        longe.discard();
        inventario.clearContent();

        // Cubo de sal no nível 0: a fome cresce 80%, e a cura da fome 84%
        inventario.setItem(0, acessorio(TipoDeAcessorio.CUBO_DE_SAL, 0));
        heroi.getFoodData().setExhaustion(0);
        Acessorios.fome(heroi);
        heroi.getFoodData().addExhaustion(2.0F);
        Acessorios.fome(heroi);
        boolean fome = Math.abs(heroi.getFoodData().getExhaustionLevel() - 1.6F) < 0.01F;
        heroi.setHealth(10);
        Acessorios.marcarCuraDaFome(true);
        heroi.heal(1.0F);
        Acessorios.marcarCuraDaFome(false);
        boolean regeneracao = Math.abs(heroi.getHealth() - 10.84F) < 0.01F;
        heroi.heal(1.0F);
        regeneracao &= Math.abs(heroi.getHealth() - 11.84F) < 0.01F;
        inventario.clearContent();

        // Frasco de sangue no nível 3: a poção cura 50% a mais, mas no máximo 1 + 5% da vida por turno
        inventario.setItem(0, acessorio(TipoDeAcessorio.FRASCO_DE_SANGUE, 3));
        EfeitosNoTick.curar(heroi, 100);
        boolean frasco = heroi.getPersistentData().getInt("pixeldungeon_cura_restante") == 150
                && heroi.getPersistentData().getInt("pixeldungeon_cura_por_turno") == 1 + Math.round(0.05F * heroi.getMaxHealth());
        heroi.removeAllEffects();
        inventario.clearContent();

        // Fragmento do esquecimento: a espada não se identifica sozinha, fica pronta, e o fragmento identifica;
        // e com ela sem identificar na mão, os inimigos soltam itens 20% mais
        // (o fragmento fora da casinha 0, que é a da mão)
        inventario.setItem(9, acessorio(TipoDeAcessorio.FRAGMENTO_DO_ESQUECIMENTO, 0));
        ItemStack espada = new ItemStack(ModItens.ESPADA.get());
        espada.set(ModComponentes.IDENTIFICADO.get(), false);
        espada.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 2F);
        inventario.setItem(1, espada);
        for (int i = 0; i < 5; i++) {
            NivelDoItem.aoUsar(heroi, inventario.getItem(1));
        }
        boolean fragmento = !NivelDoItem.identificado(inventario.getItem(1)) && NivelDoItem.prontoPraIdentificar(inventario.getItem(1));
        heroi.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, inventario.getItem(1).copy());
        fragmento &= Math.abs(Acessorios.multiplicadorDeSaque(heroi) - 1.2F) < 0.01F;
        fragmento &= ItemAcessorio.identificar(heroi, inventario.getItem(1)) && NivelDoItem.identificado(inventario.getItem(1));
        inventario.clearContent();

        // Os números de cada um, nos níveis 0 e 3 (iguais aos do Shattered)
        boolean numeros = perto(Acessorios.multiplicadorDeEsquiva(0), 1.125F) && perto(Acessorios.multiplicadorDeEsquiva(3), 1.5F)
                && perto(Acessorios.renascerDeDia(3), 0.8F) && perto(Acessorios.renascerDeNoite(0), 1.25F) && perto(Acessorios.multiplicadorDeMimicos(3), 3)
                && perto(Acessorios.chanceDeDisparoBom(1), 0.5F) && Acessorios.turnosDoIncensario(2) == 100 && perto(Acessorios.chanceDaLuneta(0), 0.375F)
                && perto(Acessorios.chanceDePedraNaGrama(3), 0.8F) && Acessorios.alcanceDaVisaoMental(3) == 5;
        heroi.removeAllEffects();
        PixelDungeon.LOGGER.info("Teste dos acessórios no herói: o maior nível = {}, trevo = {}, olho = {}, incensário = {}, fome = {}, regeneração = {}, "
                + "frasco = {}, fragmento = {}, números = {}", nivel, trevo, olho, incensario, fome, regeneracao, frasco, fragmento, numeros);
    }
}
