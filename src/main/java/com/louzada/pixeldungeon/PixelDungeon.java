package com.louzada.pixeldungeon;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// Classe principal do mod. O NeoForge cria ela quando o jogo carrega.
// O valor dentro de @Mod tem que ser igual ao mod_id do gradle.properties.
@Mod(PixelDungeon.MODID)
public class PixelDungeon {
    public static final String MODID = "pixeldungeon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PixelDungeon(IEventBus barramentoDoMod, ModContainer container) {
        LOGGER.info("Pixel Dungeon MC carregando");
    }
}
