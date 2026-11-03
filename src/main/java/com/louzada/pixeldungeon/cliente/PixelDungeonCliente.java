package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

// Parte do mod que só roda no jogo do jogador (nunca no servidor): desenhos, telas...
@Mod(value = PixelDungeon.MODID, dist = Dist.CLIENT)
public class PixelDungeonCliente {

    public PixelDungeonCliente(IEventBus barramentoDoMod, ModContainer container) {
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarRenderizadores);
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarCoresDosBlocos);
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarCoresDosItens);
        // Teclas (K talentos, V habilidade)
        barramentoDoMod.addListener(TeclasDoMod::aoRegistrar);
        NeoForge.EVENT_BUS.addListener(TeclasDoMod::aoTick);
        // A tela do jogo e o inventário iguais aos do Shattered
        barramentoDoMod.addListener(HudDoHeroi::aoRegistrar);
        NeoForge.EVENT_BUS.addListener(HudDoHeroi::aoDesenharCamada);
        NeoForge.EVENT_BUS.addListener(TelaDoInventario::aoAbrirTela);
        // Só no teste do GitHub: tira fotos da tela do jogo e do inventário
        if (FotosDeTeste.ligado()) {
            NeoForge.EVENT_BUS.addListener(FotosDeTeste::aoTick);
        }
    }

    // Como cada inimigo aparece na tela: com o desenho animado do Shattered (ver RenderizadorDeSprite)
    private static void aoRegistrarRenderizadores(EntityRenderersEvent.RegisterRenderers evento) {
        evento.registerEntityRenderer(ModEntidades.RATO_MARSUPIAL.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.RATO, 0.35F));
        evento.registerEntityRenderer(ModEntidades.RATO_ALBINO.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.RATO_ALBINO, 0.35F));
        evento.registerEntityRenderer(ModEntidades.COBRA_DE_ESGOTO.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.COBRA, 0.3F));
        evento.registerEntityRenderer(ModEntidades.GNOLL_EXPLORADOR.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.GNOLL, 0.45F));
        evento.registerEntityRenderer(ModEntidades.CARANGUEJO_DE_ESGOTO.get(),
                contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.CARANGUEJO, 0.45F));
        evento.registerEntityRenderer(ModEntidades.ENXAME_DE_MOSCAS.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.ENXAME, 0.25F));
        evento.registerEntityRenderer(ModEntidades.SLIME.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.SLIME, 0.5F));
        evento.registerEntityRenderer(ModEntidades.GOO.get(), RenderizadorDeGoo::new);
        evento.registerEntityRenderer(ModEntidades.MIMICO.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.MIMICO, 0.45F));
        // Míssil mágico: não tem modelo, é só o rastro de luz (as partículas saem dele mesmo)
        evento.registerEntityRenderer(ModEntidades.MISSEL_MAGICO.get(), NoopRenderer::new);
        // Luz guia do Clérigo: igual ao míssil, só o rastro de luz
        evento.registerEntityRenderer(ModEntidades.LUZ_GUIA.get(), NoopRenderer::new);
        // Flecha espiritual: o desenho da flecha do Shattered, brilhando
        evento.registerEntityRenderer(ModEntidades.FLECHA_ESPIRITUAL.get(), contexto -> new ThrownItemRenderer<>(contexto, 1.0F, true));
        // Gota de orvalho: o desenho da gota do Shattered, brilhando
        evento.registerEntityRenderer(ModEntidades.GOTA_DE_ORVALHO.get(), contexto -> new ThrownItemRenderer<>(contexto, 0.8F, true));
        // Poção e semente arremessadas: o desenho delas voando
        evento.registerEntityRenderer(ModEntidades.POCAO_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 1.0F, false));
        evento.registerEntityRenderer(ModEntidades.SEMENTE_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 0.8F, false));
        // Nuvem de gás: só a fumaça (as partículas saem dela mesma)
        evento.registerEntityRenderer(ModEntidades.NUVEM_DE_GAS.get(), NoopRenderer::new);
        // Imagem espelhada: igual ao herói dono dela
        evento.registerEntityRenderer(ModEntidades.IMAGEM_ESPELHADA.get(), RenderizadorDaImagem::new);
    }

    // A grama usa as texturas cinza da grama do Minecraft; aqui ela ganha a cor verde do esgoto
    private static final int COR_DA_GRAMA = 0xFF6E9B3D;

    private static void aoRegistrarCoresDosBlocos(RegisterColorHandlersEvent.Block evento) {
        evento.register((estado, mundo, posicao, camada) -> COR_DA_GRAMA, ModBlocos.CAPIM_ALTO.get(), ModBlocos.GRAMA_BAIXA.get());
    }

    private static void aoRegistrarCoresDosItens(RegisterColorHandlersEvent.Item evento) {
        evento.register((pilha, camada) -> COR_DA_GRAMA, ModItens.CAPIM_ALTO.get(), ModItens.GRAMA_BAIXA.get());
    }
}
