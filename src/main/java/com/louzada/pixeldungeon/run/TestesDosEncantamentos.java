package com.louzada.pixeldungeon.run;

import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.EncantamentosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: os encantamentos de arma, as maldições novas e os aliados corrompidos.
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDosEncantamentos {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.DUELISTA, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // 10% das armas achadas vêm encantadas (e nunca junto com maldição)
        int encantadas = 0;
        boolean juntoComMaldicao = false;
        Random sorteio = new Random(12);
        for (int i = 0; i < 400; i++) {
            ItemStack espada = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ESPADA.get()), sorteio, true);
            if (NivelDoItem.encantamento(espada) != null) {
                encantadas++;
                juntoComMaldicao |= NivelDoItem.amaldicoado(espada);
            }
        }
        PixelDungeon.LOGGER.info("Teste dos encantamentos: {} de 400 armas encantadas, perto de 10% = {}, junto com maldição = {}", encantadas,
                encantadas >= 20 && encantadas <= 65, juntoComMaldicao);

        // Flamejante e congelante (com nível alto pra quase sempre ativar)
        RatoMarsupial rato = rato(mundo, heroi);
        for (int i = 0; i < 10 && !rato.isOnFire(); i++) {
            Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.FLAMEJANTE, 30), 1, 1.0F);
        }
        for (int i = 0; i < 10 && !rato.hasEffect(ModEfeitos.RESFRIADO); i++) {
            Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.CONGELANTE, 30), 1, 1.0F);
        }
        PixelDungeon.LOGGER.info("Teste do flamejante e do congelante: pegou fogo = {}, resfriado = {}", rato.isOnFire(),
                rato.hasEffect(ModEfeitos.RESFRIADO));

        // Vorpal: o golpe vira sangramento
        rato = rato(mundo, heroi);
        float vorpal = 1;
        for (int i = 0; i < 60 && vorpal > 0; i++) {
            vorpal = Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.VORPAL, 0), 1, 1.0F);
        }
        PixelDungeon.LOGGER.info("Teste do vorpal: o golpe virou sangramento = {}", vorpal == 0 && rato.hasEffect(ModEfeitos.SANGRANDO));

        // Mortal: o rato quase morto é executado
        rato = rato(mundo, heroi);
        float mortal = 0;
        for (int i = 0; i < 40 && mortal < rato.getHealth(); i++) {
            mortal = Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.MORTAL, 0), rato.getHealth() - 1, 1.0F);
        }
        PixelDungeon.LOGGER.info("Teste do mortal: executou = {}", mortal >= rato.getHealth());

        // Inercial: o dano que sobra fica guardado pro próximo golpe
        rato = rato(mundo, heroi);
        float vida = rato.getHealth();
        Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.INERCIAL, 0), vida + 12, 1.0F);
        float guardado = dados.decimal(Encantamento.DANO_CONSERVADO);
        float proximo = Encantamento.aoGolpearCom(heroi, rato(mundo, heroi), arma(Encantamento.INERCIAL, 0), 1, 1.0F);
        PixelDungeon.LOGGER.info("Teste do inercial: guardou {}, o próximo golpe de 1 deu {}", guardado, proximo);

        // De corrupção: o rato morto volta como aliado
        rato = rato(mundo, heroi);
        for (int i = 0; i < 30 && !rato.getPersistentData().hasUUID(Encantamento.CORROMPER); i++) {
            Encantamento.aoGolpearCom(heroi, rato, arma(Encantamento.DE_CORRUPCAO, 30), rato.getHealth() + 1, 1.0F);
        }
        boolean marcado = rato.getPersistentData().hasUUID(Encantamento.CORROMPER);
        EncantamentosDoHeroi.aoMorrer(rato);
        RatoMarsupial aliado = null;
        for (RatoMarsupial perto : mundo.getEntitiesOfClass(RatoMarsupial.class, new AABB(rato.blockPosition()).inflate(3))) {
            if (perto.aliado()) {
                aliado = perto;
            }
        }
        boolean naoPersegueHeroi = false;
        if (aliado != null) {
            aliado.setTarget(heroi);
            naoPersegueHeroi = aliado.getTarget() == null;
            aliado.discard();
        }
        PixelDungeon.LOGGER.info("Teste da corrupção: marcou = {}, voltou como aliado = {}, não persegue o herói = {}", marcado, aliado != null,
                naoPersegueHeroi);

        // Projetante: a arma alcança mais longe
        double alcanceAntes = heroi.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, arma(Encantamento.PROJETANTE, 0));
        EncantamentosDoHeroi.atualizar(heroi);
        double alcanceDepois = heroi.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        heroi.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        EncantamentosDoHeroi.atualizar(heroi);
        PixelDungeon.LOGGER.info("Teste do projetante: alcance {} -> {}", alcanceAntes, alcanceDepois);

        // Maldição amigável: encantado pelo rato, o herói não consegue machucá-lo
        rato = rato(mundo, heroi);
        ItemStack amigavel = new ItemStack(ModItens.ESPADA.get());
        amigavel.set(ModComponentes.MALDICAO.get(), Maldicao.AMIGAVEL.id());
        float amigo = 1;
        for (int i = 0; i < 120 && amigo > 0; i++) {
            amigo = Maldicao.aoGolpearCom(heroi, rato, amigavel, 5);
        }
        PixelDungeon.LOGGER.info("Teste da maldição amigável: o golpe não machucou = {}, o rato ficou encantado = {}", amigo == 0,
                rato.hasEffect(ModEfeitos.ENCANTADO));

        // Maldição explosiva: quando a pressão acaba, explode e começa de novo
        ItemStack explosiva = new ItemStack(ModItens.ESPADA.get());
        explosiva.set(ModComponentes.MALDICAO.get(), Maldicao.EXPLOSIVA.id());
        explosiva.set(ModComponentes.PRESSAO_EXPLOSIVA.get(), 0);
        Maldicao.aoGolpearCom(heroi, rato, explosiva, 5);
        PixelDungeon.LOGGER.info("Teste da maldição explosiva: explodiu e recomeçou = {}", explosiva.getOrDefault(ModComponentes.PRESSAO_EXPLOSIVA.get(), 0) >= 90);

        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
        dados.definir(Encantamento.DANO_CONSERVADO, 0F);
    }

    private static RatoMarsupial rato(ServerLevel mundo, FakePlayer heroi) {
        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        rato.moveTo(heroi.getX() + 1.5, heroi.getY(), heroi.getZ());
        return rato;
    }

    private static ItemStack arma(Encantamento encantamento, int nivel) {
        ItemStack espada = new ItemStack(ModItens.ESPADA.get());
        NivelDoItem.definirNivel(espada, nivel);
        NivelDoItem.definirEncantamento(espada, encantamento);
        return espada;
    }
}
