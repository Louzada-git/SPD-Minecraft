package com.louzada.pixeldungeon.run;

import java.util.Random;
import java.util.UUID;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.GlifosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.Glifo;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.SeloQuebrado;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Só no teste do GitHub: os glifos de armadura e o selo quebrado do Guerreiro.
// Um jogador de mentira faz as coisas e o log mostra o resultado (o build.yml confere).
public class TestesDosGlifos {

    public static void testar(MinecraftServer servidor) {
        ServerLevel mundo = servidor.overworld();
        DadosDaRun run = DadosDaRun.pegar(servidor);
        BlockPos lugar = run.andar(1).entrada();
        FakePlayer heroi = FakePlayerFactory.get(mundo, new GameProfile(UUID.fromString("5e7c1a3e-0000-4000-8000-00000000b0b0"), "TesteDoMod"));
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5, 0.0F, 0.0F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(heroi);
        dados.comecar(ClasseDoHeroi.GUERREIRO, run.numeroDaRun());
        heroi.getInventory().clearContent();
        heroi.removeAllEffects();

        // 15% das armaduras achadas vêm com glifo (e nunca junto com maldição)
        int comGlifo = 0;
        boolean juntoComMaldicao = false;
        Random sorteio = new Random(11);
        for (int i = 0; i < 400; i++) {
            ItemStack armadura = NivelDoItem.sortearDoChao(new ItemStack(ModItens.ARMADURA_DE_COURO.get()), sorteio, true);
            if (NivelDoItem.glifo(armadura) != null) {
                comGlifo++;
                juntoComMaldicao |= NivelDoItem.amaldicoado(armadura);
            }
        }
        PixelDungeon.LOGGER.info("Teste dos glifos: {} de 400 armaduras com glifo, perto de 15% = {}, junto com maldição = {}", comGlifo,
                comGlifo >= 40 && comGlifo <= 85, juntoComMaldicao);

        RatoMarsupial rato = ModEntidades.RATO_MARSUPIAL.get().create(mundo);
        if (rato != null) {
            rato.moveTo(heroi.getX() + 1.5, heroi.getY(), heroi.getZ());
            Combatente comoHeroi = Combatente.de(heroi);
            Golpe golpe = Golpe.de(Combatente.de(rato), comoHeroi);

            // Pedra: esquiva 0, mas o golpe perde dano
            vestir(heroi, Glifo.PEDRA, 0);
            float esquiva = comoHeroi.esquiva(golpe);
            float dano = comoHeroi.aoSerAtingido(golpe, 10);
            PixelDungeon.LOGGER.info("Teste do glifo de pedra: esquiva 0 = {}, dano 10 -> {} (menor = {})", esquiva == 0, dano, dano < 10);

            // Viscosidade +0: 1/6 do dano (arredondado pra cima) fica pra depois
            vestir(heroi, Glifo.VISCOSIDADE, 0);
            dados.definir(GlifosDoHeroi.DANO_ADIADO, 0.0F);
            float agora = comoHeroi.depoisDaArmadura(golpe, 10);
            PixelDungeon.LOGGER.info("Teste do glifo da viscosidade: 10 -> {}, adiado {}", agora, dados.decimal(GlifosDoHeroi.DANO_ADIADO));
            dados.definir(GlifosDoHeroi.DANO_ADIADO, 0.0F);
            heroi.removeEffect(ModEfeitos.DANO_ADIADO);

            // Espinhos: quem bate sangra (com nível alto pra quase sempre ativar)
            vestir(heroi, Glifo.ESPINHOS, 30);
            for (int i = 0; i < 20 && !rato.hasEffect(ModEfeitos.SANGRANDO); i++) {
                comoHeroi.aoSerAtingido(golpe, 1);
            }
            PixelDungeon.LOGGER.info("Teste do glifo de espinhos: o rato sangrou = {}", rato.hasEffect(ModEfeitos.SANGRANDO));
            rato.discard();
        }

        // Enxofre: o fogo se apaga
        vestir(heroi, Glifo.ENXOFRE, 0);
        heroi.igniteForSeconds(5);
        GlifosDoHeroi.aoTick(heroi, dados, false);
        PixelDungeon.LOGGER.info("Teste do glifo de enxofre: apagou o fogo = {}", !heroi.isOnFire());

        // Rapidez: longe dos inimigos, o herói fica mais rápido
        vestir(heroi, Glifo.RAPIDEZ, 0);
        // (longe da dungeon: os andares ficam nos 256 blocos a partir da origem da run)
        heroi.moveTo(lugar.getX() + 1500, lugar.getY(), lugar.getZ());
        double antes = heroi.getAttributeValue(Attributes.MOVEMENT_SPEED);
        GlifosDoHeroi.atualizarVelocidade(heroi);
        double depois = heroi.getAttributeValue(Attributes.MOVEMENT_SPEED);
        heroi.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        GlifosDoHeroi.atualizarVelocidade(heroi);
        PixelDungeon.LOGGER.info("Teste do glifo de rapidez: mais rápido sem inimigos perto = {}", depois > antes);
        heroi.moveTo(lugar.getX() - 1.5, lugar.getY(), lugar.getZ() + 0.5);

        // Potencial: carrega as varinhas da mochila
        ItemStack varinha = new ItemStack(ModItens.varinha(TipoDeVarinha.RELAMPAGO));
        Cargas.definir(varinha, mundo.getGameTime(), ItemVarinha.cargasMaximas(varinha), 0);
        heroi.getInventory().add(varinha);
        Glifo.carregarVarinhas(heroi, 1.0F);
        PixelDungeon.LOGGER.info("Teste do glifo do potencial: carga da varinha 0 -> {}", ItemVarinha.cargas(heroi.getInventory().getItem(0)));
        heroi.getInventory().clearContent();

        // Selo quebrado: guarda um aprimoramento e, com a transferência rúnica, leva o glifo pra outra armadura
        dados.definirNivel(7);
        Talentos.aprimorar(heroi, Talento.TRANSFERENCIA_RUNICA);
        ItemStack velha = new ItemStack(ModItens.ARMADURA_DE_COURO.get());
        velha.set(ModComponentes.SELO.get(), true);
        NivelDoItem.melhorar(heroi, velha, heroi.getRandom());
        NivelDoItem.definirGlifo(velha, Glifo.PEDRA);
        heroi.getInventory().setItem(0, velha);
        ItemStack nova = new ItemStack(ModItens.ARMADURA_DE_MALHA.get());
        heroi.setItemSlot(EquipmentSlot.CHEST, nova);
        SeloQuebrado.transferir(heroi);
        ItemStack vestida = heroi.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack tirada = heroi.getInventory().getItem(0);
        PixelDungeon.LOGGER.info("Teste do selo: a nova ficou com o selo = {}, +{}, glifo de pedra = {}; a velha voltou pra +{}, sem glifo = {}",
                SeloQuebrado.temSelo(vestida), NivelDoItem.nivel(vestida), NivelDoItem.glifo(vestida) == Glifo.PEDRA, NivelDoItem.nivel(tirada),
                NivelDoItem.glifo(tirada) == null);

        // Soltar o selo (vai pra mochila com o aprimoramento e o glifo) e afixar de novo pela tela de escolha
        heroi.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        heroi.getInventory().setItem(1, vestida);
        SeloQuebrado.soltarDaMao(heroi, heroi.getInventory().getItem(1));
        boolean seloNaMochila = false;
        for (ItemStack pilha : heroi.getInventory().items) {
            seloNaMochila |= pilha.getItem() instanceof SeloQuebrado && pilha.getOrDefault(ModComponentes.SELO_MELHORIA.get(), false)
                    && Glifo.peloId(pilha.get(ModComponentes.SELO_GLIFO.get())) == Glifo.PEDRA;
        }
        EscolhaDeItem.pedirArmaduraProSelo(heroi);
        EscolhaDeItem.escolheu(heroi, 0);
        ItemStack deNovo = heroi.getInventory().getItem(0);
        PixelDungeon.LOGGER.info("Teste do selo solto: foi pra mochila com o aprimoramento e o glifo = {}, afixado de novo +{} com glifo de pedra = {}",
                seloNaMochila, NivelDoItem.nivel(deNovo), SeloQuebrado.temSelo(deNovo) && NivelDoItem.glifo(deNovo) == Glifo.PEDRA);

        heroi.getInventory().clearContent();
        heroi.removeAllEffects();
    }

    // Veste uma armadura de couro com esse glifo e nível
    private static void vestir(FakePlayer heroi, Glifo glifo, int nivel) {
        ItemStack armadura = new ItemStack(ModItens.ARMADURA_DE_COURO.get());
        NivelDoItem.definirNivel(armadura, nivel);
        NivelDoItem.definirGlifo(armadura, glifo);
        heroi.setItemSlot(EquipmentSlot.CHEST, armadura);
    }
}
