package com.louzada.pixeldungeon.run;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.heroi.Aneis;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.GemaDeAnel;
import com.louzada.pixeldungeon.item.IdentidadesDaRun;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDeAnel;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: os anéis (gemas embaralhadas, pôr, o anel do poder, identificar com experiência e o amaldiçoado).
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDosAneis {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        long semente = run.semente();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.LADINO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // Cada gema esconde um tipo diferente nesta run
        Set<TipoDeAnel> tipos = new HashSet<>();
        for (GemaDeAnel gema : GemaDeAnel.values()) {
            tipos.add(IdentidadesDaRun.tipoDaGema(semente, gema));
        }
        ItemStack achado = SaqueDoAndar.anelAleatorio(new Random(4));
        PixelDungeon.LOGGER.info("Teste dos anéis: {} anéis, cada gema esconde um tipo diferente = {}, o achado vem sem identificar = {}",
                ModItens.ANEIS.size(), tipos.size() == GemaDeAnel.values().length, !NivelDoItem.identificado(achado));

        // Anel do poder +1 (bônus 2): clique direito põe na casinha; a intuição do ladrão +1 revela o tipo; +2 de força
        dados.definirNivel(2);
        Talentos.aprimorar(heroi, Talento.INTUICAO_DO_LADRAO);
        ItemStack poder = Identificacao.anel(semente, TipoDeAnel.PODER);
        NivelDoItem.definirNivel(poder, 1);
        poder.set(ModComponentes.IDENTIFICADO.get(), false);
        poder.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        poder.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 1.0F);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, poder);
        int forcaAntes = dados.forca();
        poder.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND);
        ItemStack posto = heroi.getItemBySlot(Aneis.PRIMEIRO);
        GemaDeAnel gemaDoPoder = ((ItemAnel) ModItens.ANEIS.get(0).get()).gema();
        if (posto.getItem() instanceof ItemAnel anel) {
            gemaDoPoder = anel.gema();
        }
        PixelDungeon.LOGGER.info("Teste do anel do poder: foi pra casinha = {}, conhece o tipo = {}, força {} -> {}", posto.getItem() instanceof ItemAnel,
                Identificacao.conhece(heroi, gemaDoPoder), forcaAntes, dados.forca());

        // Usando, se identifica depois de 1 nível de experiência (com a intuição do ladrão +1, metade)
        RegrasDoHeroi.ganharXp(heroi, RegrasDoHeroi.xpParaSubir(dados.nivel()) / 2 + 1);
        PixelDungeon.LOGGER.info("Teste da identificação do anel: identificou = {}", NivelDoItem.identificado(heroi.getItemBySlot(Aneis.PRIMEIRO)));

        // Amaldiçoado +0: bônus -2, e gruda no dedo
        ItemStack amaldicoado = Identificacao.anel(semente, TipoDeAnel.PRECISAO);
        amaldicoado.set(ModComponentes.MALDICAO.get(), Maldicao.ANEL_AMALDICOADO.id());
        amaldicoado.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        heroi.setItemSlot(Aneis.SEGUNDO, amaldicoado);
        Aneis.aoPor(heroi, heroi.getItemBySlot(Aneis.SEGUNDO));
        PixelDungeon.LOGGER.info("Teste do anel amaldiçoado: bônus {}, grudou = {}", dados.bonusDoAnel(TipoDeAnel.PRECISAO),
                NivelDoItem.grudada(heroi.getItemBySlot(Aneis.SEGUNDO)));

        heroi.setItemSlot(Aneis.PRIMEIRO, ItemStack.EMPTY);
        heroi.setItemSlot(Aneis.SEGUNDO, ItemStack.EMPTY);
        Aneis.atualizar(heroi, dados);
        heroi.getInventory().clearContent();
    }
}
