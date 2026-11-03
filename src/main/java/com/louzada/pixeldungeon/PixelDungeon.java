package com.louzada.pixeldungeon;

import org.slf4j.Logger;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.EventosDeCombate;
import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.rede.ModRede;
import com.louzada.pixeldungeon.registro.ModAbas;
import com.louzada.pixeldungeon.registro.ModAnexos;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.registro.ModMateriais;
import com.louzada.pixeldungeon.run.ComandosDaDungeon;
import com.louzada.pixeldungeon.run.EventosDaRun;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// Classe principal do mod. O NeoForge cria ela quando o jogo carrega.
// O valor dentro de @Mod tem que ser igual ao mod_id do gradle.properties.
@Mod(PixelDungeon.MODID)
public class PixelDungeon {
    public static final String MODID = "pixeldungeon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PixelDungeon(IEventBus barramentoDoMod, ModContainer container) {
        // Registros: blocos, itens, abas do criativo...
        ModBlocos.BLOCOS.register(barramentoDoMod);
        ModItens.ITENS.register(barramentoDoMod);
        ModAbas.ABAS.register(barramentoDoMod);
        ModAnexos.ANEXOS.register(barramentoDoMod);
        ModEntidades.ENTIDADES.register(barramentoDoMod);
        ModMateriais.MATERIAIS.register(barramentoDoMod);
        ModEfeitos.EFEITOS.register(barramentoDoMod);
        ModComponentes.COMPONENTES.register(barramentoDoMod);
        barramentoDoMod.addListener(ModEntidades::aoCriarAtributos);

        // Pacotes de rede (menu de escolha de herói)
        barramentoDoMod.addListener(ModRede::aoRegistrar);

        // Eventos do jogo (servidor ligando, jogador entrando, morrendo, combate...)
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoIniciarServidor);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoEntrar);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoRenascer);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoMorrer);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoTickDoServidor);
        NeoForge.EVENT_BUS.addListener(ComandosDaDungeon::aoRegistrarComandos);
        NeoForge.EVENT_BUS.addListener(EventosDeCombate::aoReceberDano);
        NeoForge.EVENT_BUS.addListener(EventosDeCombate::aoMorrerAlguem);
        NeoForge.EVENT_BUS.addListener(EventosDeCombate::aoTomarDano);
        // O selo do Guerreiro olha o dano depois que o combate calculou (por isso prioridade baixa)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, Escudos::aoReceberDano);
        // Herói: blindagem, talentos, capa, cargas da Duelista, comida
        NeoForge.EVENT_BUS.addListener(GerenciadorDoHeroi::aoTickDoJogador);
        NeoForge.EVENT_BUS.addListener(GerenciadorDoHeroi::aoTerminarDeUsar);
        NeoForge.EVENT_BUS.addListener(NumerosFlutuantes::aoTickDoServidor);
        NeoForge.EVENT_BUS.addListener(NumerosFlutuantes::aoEntrarNoMundo);
        NeoForge.EVENT_BUS.addListener(PopulacaoDoAndar::aoTickDoServidor);
    }
}
