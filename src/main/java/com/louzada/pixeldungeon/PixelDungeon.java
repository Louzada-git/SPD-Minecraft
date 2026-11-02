package com.louzada.pixeldungeon;

import org.slf4j.Logger;

import com.louzada.pixeldungeon.registro.ModAbas;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.ComandosDaDungeon;
import com.louzada.pixeldungeon.run.EventosDaRun;
import com.mojang.logging.LogUtils;

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

        // Eventos do jogo (servidor ligando, jogador entrando, renascendo...)
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoIniciarServidor);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoEntrar);
        NeoForge.EVENT_BUS.addListener(EventosDaRun::aoRenascer);
        NeoForge.EVENT_BUS.addListener(ComandosDaDungeon::aoRegistrarComandos);
    }
}
