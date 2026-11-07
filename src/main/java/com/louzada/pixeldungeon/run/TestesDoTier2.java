package com.louzada.pixeldungeon.run;

import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo.Magia;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.EfeitosDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: os talentos do tier 2 (os pontos de cada tier e alguns efeitos).
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDoTier2 {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        heroi.getInventory().clearContent();

        // Pontos: o tier 2 libera no nível 7 (1 ponto) e ganha 1 por nível até o 12 (6 pontos)
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        dados.definirNivel(6);
        Talentos.aprimorar(heroi, Talento.ESTOMAGO_DE_FERRO);
        boolean noNivel6 = dados.pontos(Talento.ESTOMAGO_DE_FERRO) > 0;
        dados.definirNivel(7);
        int livresNoNivel7 = dados.pontosLivres(2);
        Talentos.aprimorar(heroi, Talento.ESTOMAGO_DE_FERRO);
        boolean noNivel7 = dados.pontos(Talento.ESTOMAGO_DE_FERRO) > 0;
        dados.definirNivel(12);
        PixelDungeon.LOGGER.info("Teste do tier 2: no nível 6 aprimorou = {}, no nível 7 tinha {} ponto e aprimorou = {}, no nível 12 sobram {} do tier 2 e {} do tier 1",
                noNivel6, livresNoNivel7, noNivel7, dados.pontosLivres(2), dados.pontosLivres(1));

        // Guerreiro: estômago de ferro (comer leva 1 turno), tenacidade líquida (blindagem ao beber) e ímpeto letal
        PixelDungeon.LOGGER.info("Teste do estômago de ferro: a ração leva {} turno",
                TalentosDoTier2.turnosPraComer(heroi, new ItemStack(ModItens.RACAO.get()), 3));
        Talentos.aprimorar(heroi, Talento.TENACIDADE_LIQUIDA);
        Talentos.aprimorar(heroi, Talento.TENACIDADE_LIQUIDA);
        float blindagemAntes = Escudos.barreira(heroi);
        TalentosDoTier2.aoUsarPocao(heroi, TipoDePocao.CURA);
        float ganhou = Escudos.barreira(heroi) - blindagemAntes;
        PixelDungeon.LOGGER.info("Teste da tenacidade líquida: ganhou {} de blindagem, 10% da vida máxima = {}", ganhou,
                Math.round(ganhou) == Math.round(heroi.getMaxHealth() * 0.10F));
        Talentos.aprimorar(heroi, Talento.IMPETO_LETAL);
        Talentos.aprimorar(heroi, Talento.IMPETO_LETAL);
        heroi.attackStrengthTicker = 0;
        TalentosDoTier2.aoMatar(heroi, heroi.damageSources().playerAttack(heroi), false);
        PixelDungeon.LOGGER.info("Teste do ímpeto letal: ataque pronto = {}", heroi.attackStrengthTicker == TalentosDoTier2.ATAQUE_CARREGADO);

        // Mago: poder inscrito (+2 níveis nos próximos disparos; a melhoria dobra)
        dados.comecar(ClasseDoHeroi.MAGO, run.numeroDaRun());
        dados.definirNivel(7);
        Talentos.aprimorar(heroi, Talento.PODER_INSCRITO);
        TalentosDoTier2.aoLerPergaminho(heroi, TipoDePergaminho.IDENTIFICACAO, false);
        int depoisDeUm = dados.inteiro(TalentosDoTier2.INSCRITO_VEZES);
        TalentosDoTier2.aoLerPergaminho(heroi, TipoDePergaminho.MELHORIA, false);
        int depoisDaMelhoria = dados.inteiro(TalentosDoTier2.INSCRITO_VEZES);
        int bonus = TalentosDoTier2.niveisInscritos(heroi);
        PixelDungeon.LOGGER.info("Teste do poder inscrito: depois de um pergaminho = {}, depois da melhoria = {}, bônus do disparo = {}, sobraram {}",
                depoisDeUm, depoisDaMelhoria, bonus, dados.inteiro(TalentosDoTier2.INSCRITO_VEZES));

        // Ladino: passos silenciosos com +1 (não acorda a 3 quadradinhos ou mais)
        dados.comecar(ClasseDoHeroi.LADINO, run.numeroDaRun());
        dados.definirNivel(7);
        Talentos.aprimorar(heroi, Talento.PASSOS_SILENCIOSOS);
        PixelDungeon.LOGGER.info("Teste dos passos silenciosos: silencioso a 2 = {}, a 3 = {}", TalentosDoTier2.passosSilenciosos(heroi, 2.0),
                TalentosDoTier2.passosSilenciosos(heroi, 3.0));

        // Duelista: refeição focada +2 (1 carga) e agilidade líquida +1 (3x evasão)
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        dados.definirNivel(12);
        Talentos.aprimorar(heroi, Talento.REFEICAO_FOCADA);
        Talentos.aprimorar(heroi, Talento.REFEICAO_FOCADA);
        Talentos.aprimorar(heroi, Talento.AGILIDADE_LIQUIDA);
        dados.definir(HabilidadesDaDuelista.CARGAS, 0);
        TalentosDoTier2.aoComer(heroi, dados);
        int cargasDaArma = dados.inteiro(HabilidadesDaDuelista.CARGAS);
        TalentosDoTier2.aoUsarPocao(heroi, TipoDePocao.CURA);
        PixelDungeon.LOGGER.info("Teste da Duelista no tier 2: cargas depois de comer = {}, evasão 10 -> {}", cargasDaArma,
                TalentosDoTier2.esquiva(heroi, 10.0F));
        heroi.removeAllEffects();

        // Clérigo: recuperar inscrição repete o pergaminho de recarga (3 cargas), sentido divino e bênção nele mesmo
        dados.comecar(ClasseDoHeroi.CLERIGO, run.numeroDaRun());
        dados.definirNivel(12);
        for (Talento talento : new Talento[] { Talento.RECUPERAR_INSCRICAO, Talento.SENTIDO_DIVINO, Talento.BENCAO, Talento.RAIO_SOLAR }) {
            Talentos.aprimorar(heroi, talento);
        }
        boolean conhece = MagiasDoClerigo.conhecidas(dados::pontos).size() == 7;
        ItemStack tomo = new ItemStack(ModItens.TOMO_SAGRADO.get());
        heroi.setItemInHand(InteractionHand.OFF_HAND, tomo);
        heroi.getCooldowns().removeCooldown(tomo.getItem());
        EfeitosDePergaminho.ler(heroi, TipoDePergaminho.RECARGA);
        TalentosDoTier2.aoLerPergaminho(heroi, TipoDePergaminho.RECARGA, false);
        heroi.removeEffect(ModEfeitos.RECARREGANDO);
        MagiasDoClerigo.lancar(heroi, Magia.RECUPERAR_INSCRICAO);
        PixelDungeon.LOGGER.info("Teste da recuperar inscrição: conhece as 7 magias = {}, repetiu a recarga = {}, cargas do tomo = {}, ainda lembra = {}",
                conhece, heroi.hasEffect(ModEfeitos.RECARREGANDO), Cargas.cargas(tomo, Artefato.cargasMaximas(tomo)),
                MagiasDoClerigo.pergaminhoLembrado(dados, mundo.getGameTime()) != null);

        ItemStack outroTomo = new ItemStack(ModItens.TOMO_SAGRADO.get());
        heroi.setItemInHand(InteractionHand.OFF_HAND, outroTomo);
        MagiasDoClerigo.lancar(heroi, Magia.SENTIDO_DIVINO);
        float blindagemDoClerigo = Escudos.barreira(heroi);
        MagiasDoClerigo.lancar(heroi, Magia.BENCAO);
        PixelDungeon.LOGGER.info("Teste da bênção: sentido divino = {}, abençoado = {}, blindagem +{}, cargas do tomo = {}",
                heroi.hasEffect(ModEfeitos.SENTIDO_DIVINO), heroi.hasEffect(ModEfeitos.ABENCOADO), Escudos.barreira(heroi) - blindagemDoClerigo,
                Cargas.cargas(outroTomo, Artefato.cargasMaximas(outroTomo)));
        heroi.removeAllEffects();
        heroi.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        heroi.getInventory().clearContent();
    }
}
