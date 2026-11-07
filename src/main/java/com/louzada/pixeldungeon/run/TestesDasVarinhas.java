package com.louzada.pixeldungeon.run;

import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.EfeitosDePergaminho;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: as varinhas (números, o chão, lançar num rato, identificar pelo uso, recarga e o cajado do Mago).
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDasVarinhas {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos entrada = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.MAGO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // As varinhas e os números do Shattered
        TipoDeVarinha relampago = TipoDeVarinha.RELAMPAGO;
        PixelDungeon.LOGGER.info("Teste das varinhas: {} varinhas, míssil mágico com {} cargas, relâmpago {}-{} no +0 e {}-{} no +1",
                ModItens.VARINHAS.size(), TipoDeVarinha.MISSEL_MAGICO.cargasMaximas(0, 0), relampago.danoMinimo(0, 1), relampago.danoMaximo(0, 1),
                relampago.danoMinimo(1, 1), relampago.danoMaximo(1, 1));

        // Achada na dungeon: sem identificar, 10 disparos pra identificar
        ItemStack achada = ItemVarinha.sortearDoChao(new ItemStack(ModItens.varinha(TipoDeVarinha.GEADA)), new Random(3));
        PixelDungeon.LOGGER.info("Teste do saque de varinha: identificada = {}, disparos pra identificar = {}", NivelDoItem.identificado(achada),
                Math.round(achada.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 0.0F)));

        // Relâmpago num rato dormindo: tira vida, gasta 1 das 2 cargas; com a intuição do estudioso +2, identifica no primeiro disparo
        dados.definirNivel(3);
        Talentos.aprimorar(heroi, Talento.INTUICAO_DO_ESTUDIOSO);
        Talentos.aprimorar(heroi, Talento.INTUICAO_DO_ESTUDIOSO);
        ItemStack varinha = new ItemStack(ModItens.varinha(relampago));
        varinha.set(ModComponentes.IDENTIFICADO.get(), false);
        varinha.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        varinha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), ItemVarinha.USOS_PARA_IDENTIFICAR);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, varinha);
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            rato.moveTo(entrada.getX() - 1.5, entrada.getY(), entrada.getZ() + 4.0);
            mundo.addFreshEntity(rato);
            rato.dormir();
            heroi.moveTo(entrada.getX() - 1.5, entrada.getY(), entrada.getZ() + 0.5, 0.0F, 0.0F);
            heroi.lookAt(EntityAnchorArgument.Anchor.EYES, rato.getEyePosition());
            float vidaAntes = rato.getHealth();
            heroi.getCooldowns().removeCooldown(varinha.getItem());
            varinha.getItem().use(mundo, heroi, InteractionHand.MAIN_HAND);
            boolean acertou = !rato.isAlive() || rato.getHealth() < vidaAntes;
            PixelDungeon.LOGGER.info("Teste da varinha do relâmpago: acertou o rato = {}, cargas {}/{}, identificou = {}", acertou, ItemVarinha.cargas(varinha),
                    ItemVarinha.cargasMaximas(varinha), NivelDoItem.identificado(varinha));
            rato.discard();
        }

        // Pergaminho de recarga enche as varinhas (a do relâmpago continua na mão)
        Cargas.definir(varinha, mundo.getGameTime(), ItemVarinha.cargasMaximas(varinha), 0);
        EfeitosDePergaminho.ler(heroi, TipoDePergaminho.RECARGA);
        PixelDungeon.LOGGER.info("Teste da recarga nas varinhas: cargas {}/{}", ItemVarinha.cargas(varinha), ItemVarinha.cargasMaximas(varinha));
        heroi.removeAllEffects();

        // Cajado do Mago: põe uma varinha do relâmpago +2 no lugar do míssil mágico; com a preservação de varinha, a antiga volta
        dados.definirNivel(7);
        Talentos.aprimorar(heroi, Talento.PRESERVACAO_DE_VARINHA);
        heroi.getInventory().clearContent();
        ItemStack cajado = new ItemStack(ModItens.CAJADO_DO_MAGO.get());
        ItemStack nova = new ItemStack(ModItens.varinha(relampago));
        NivelDoItem.definirNivel(nova, 2);
        CajadoDoMago.colocar(heroi, cajado, nova);
        boolean devolveu = false;
        for (ItemStack item : heroi.getInventory().items) {
            devolveu |= item.is(ModItens.varinha(TipoDeVarinha.MISSEL_MAGICO));
        }
        PixelDungeon.LOGGER.info("Teste do cajado: varinha = {}, nível {}, cargas {}/{}, devolveu a de míssil mágico = {}, gastou a nova = {}",
                CajadoDoMago.varinha(cajado).id(), NivelDoItem.nivel(cajado), CajadoDoMago.cargas(cajado), CajadoDoMago.cargasMaximas(cajado), devolveu,
                nova.isEmpty());
        heroi.getInventory().clearContent();
        heroi.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
}
