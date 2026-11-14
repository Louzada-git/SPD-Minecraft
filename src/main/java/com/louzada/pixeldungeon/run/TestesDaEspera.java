package com.louzada.pixeldungeon.run;

import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Espera;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Só no teste do GitHub: o botão de esperar. Uma Duelista com golpe paciente +2 espera 1 turno: fica parada, não usa itens
// nem ataca, e o próximo golpe de perto ganha o dano a mais. Sair do lugar desfaz, e ficar parado sem esperar não prepara.
public class TestesDaEspera {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000e5e5"), "TesteDaEspera"));
        heroi.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        heroi.removeAllEffects();
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        dados.definirNivel(3);
        Talentos.aprimorar(heroi, Talento.GOLPE_PACIENTE);
        Talentos.aprimorar(heroi, Talento.GOLPE_PACIENTE);
        dados.definir(Talentos.PACIENTE, false);

        // Ficar parado sem esperar não prepara o golpe
        for (int i = 0; i < 60; i++) {
            Talentos.aoTick(heroi, dados);
        }
        boolean paradoNaoPrepara = !dados.sim(Talentos.PACIENTE);

        // Esperar: parado, sem usar item nem atacar, e o golpe paciente fica pronto
        boolean comecou = Espera.esperar(heroi);
        boolean parado = heroi.hasEffect(ModEfeitos.ESPERANDO) && heroi.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0001
                && heroi.getAttributeValue(Attributes.JUMP_STRENGTH) <= 0.0001;
        boolean naoDuasVezes = !Espera.esperar(heroi);
        boolean semItem = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(heroi, InteractionHand.MAIN_HAND)).isCanceled();
        boolean semAtaque = NeoForge.EVENT_BUS.post(new AttackEntityEvent(heroi, heroi)).isCanceled();
        boolean pronto = dados.sim(Talentos.PACIENTE);
        float bonus = Talentos.bonusDoGolpe(heroi, dados, heroi, false, false, mundo.getGameTime());
        boolean gastou = !dados.sim(Talentos.PACIENTE);

        // Esperar de novo e sair do lugar: desfaz
        heroi.removeEffect(ModEfeitos.ESPERANDO);
        boolean depoisPodeUsar = !NeoForge.EVENT_BUS.post(new AttackEntityEvent(heroi, heroi)).isCanceled();
        Espera.esperar(heroi);
        heroi.setPos(heroi.getX() + 1, heroi.getY(), heroi.getZ());
        Talentos.aoTick(heroi, dados);
        boolean andarDesfaz = !dados.sim(Talentos.PACIENTE);
        heroi.removeAllEffects();

        PixelDungeon.LOGGER.info("Teste do esperar: começou = {}, parado = {}, não espera duas vezes = {}, não usa item = {}, não ataca = {}, "
                + "golpe paciente pronto = {}, bônus do golpe = {}, gastou = {}, depois pode atacar = {}, sair do lugar desfaz = {}, "
                + "ficar parado sem esperar não prepara = {}",
                comecou, parado, naoDuasVezes, semItem, semAtaque, pronto, (int) bonus, gastou, depoisPodeUsar, andarDesfaz, paradoNaoPrepara);
    }
}
