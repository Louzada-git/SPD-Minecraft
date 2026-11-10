package com.louzada.pixeldungeon.cliente;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.registro.ModMenus;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

// Parte do mod que só roda no jogo do jogador (nunca no servidor): desenhos, telas...
@Mod(value = PixelDungeon.MODID, dist = Dist.CLIENT)
public class PixelDungeonCliente {

    public PixelDungeonCliente(IEventBus barramentoDoMod, ModContainer container) {
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarRenderizadores);
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarTelas);
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarCoresDosBlocos);
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarCoresDosItens);
        // O desenho dos artefatos que mudam (o cálice enchendo, o chifre, as sandálias virando botas)
        barramentoDoMod.addListener(PixelDungeonCliente::aoPrepararOCliente);
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
    // As telas com casinhas (o pote de alquimia)
    private static void aoRegistrarTelas(RegisterMenuScreensEvent evento) {
        evento.register(ModMenus.ALQUIMIA.get(), TelaDaAlquimia::new);
    }

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
        evento.registerEntityRenderer(ModEntidades.RAIO_SOLAR.get(), NoopRenderer::new);
        // Flecha espiritual: o desenho da flecha do Shattered, brilhando
        evento.registerEntityRenderer(ModEntidades.FLECHA_ESPIRITUAL.get(), contexto -> new ThrownItemRenderer<>(contexto, 1.0F, true));
        // Gota de orvalho: o desenho da gota do Shattered, brilhando
        evento.registerEntityRenderer(ModEntidades.GOTA_DE_ORVALHO.get(), contexto -> new ThrownItemRenderer<>(contexto, 0.8F, true));
        // Poção e semente arremessadas: o desenho delas voando
        evento.registerEntityRenderer(ModEntidades.POCAO_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 1.0F, false));
        evento.registerEntityRenderer(ModEntidades.ARMA_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 1.0F, false));
        evento.registerEntityRenderer(ModEntidades.SEMENTE_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 0.8F, false));
        evento.registerEntityRenderer(ModEntidades.PEDRA_ARREMESSADA.get(), contexto -> new ThrownItemRenderer<>(contexto, 0.8F, false));
        // Nuvem de gás: só a fumaça (as partículas saem dela mesma)
        evento.registerEntityRenderer(ModEntidades.NUVEM_DE_GAS.get(), NoopRenderer::new);
        // Imagem espelhada: igual ao herói dono dela
        evento.registerEntityRenderer(ModEntidades.IMAGEM_ESPELHADA.get(), RenderizadorDaImagem::new);
        // Os aliados das varinhas: o guardião da terra e as guardas (cada nível de guarda tem um desenho)
        evento.registerEntityRenderer(ModEntidades.GUARDIAO_DA_TERRA.get(),
                contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.GUARDIAO, 0.45F));
        evento.registerEntityRenderer(ModEntidades.GUARDA.get(), RenderizadorDaGuarda::new);
        // A missão do fantasma: o fantasma triste, os inimigos dela e o fantasma da rosa seca
        evento.registerEntityRenderer(ModEntidades.FANTASMA_TRISTE.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.FANTASMA, 0.0F));
        evento.registerEntityRenderer(ModEntidades.FANTASMA_ALIADO.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.FANTASMA, 0.0F));
        evento.registerEntityRenderer(ModEntidades.OVELHA_MAGICA.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.OVELHA, 0.5F));
        evento.registerEntityRenderer(ModEntidades.RATO_FETIDO.get(), contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.RATO_FETIDO, 0.35F));
        evento.registerEntityRenderer(ModEntidades.GNOLL_TRAPACEIRO.get(),
                contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.GNOLL_TRAPACEIRO, 0.45F));
        evento.registerEntityRenderer(ModEntidades.CARANGUEJO_GRANDE.get(),
                contexto -> new RenderizadorDeSprite<>(contexto, SpriteDoShattered.CARANGUEJO_GRANDE, 0.5F));
    }

    private static void aoPrepararOCliente(FMLClientSetupEvent evento) {
        evento.enqueueWork(() -> {
            ResourceLocation estagio = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "estagio");
            for (var artefato : ModItens.artefatos()) {
                ItemProperties.register(artefato.get(), estagio,
                        (pilha, mundo, quem, semente) -> pilha.getItem() instanceof ArtefatoAchado achado ? achado.estagio(pilha) : 0);
            }
        });
    }

    // A grama usa as texturas cinza da grama do Minecraft; aqui ela ganha a cor verde do esgoto
    private static final int COR_DA_GRAMA = 0xFF6E9B3D;

    private static void aoRegistrarCoresDosBlocos(RegisterColorHandlersEvent.Block evento) {
        evento.register((estado, mundo, posicao, camada) -> COR_DA_GRAMA, ModBlocos.CAPIM_ALTO.get(), ModBlocos.GRAMA_BAIXA.get());
    }

    private static void aoRegistrarCoresDosItens(RegisterColorHandlersEvent.Item evento) {
        evento.register((pilha, camada) -> COR_DA_GRAMA, ModItens.CAPIM_ALTO.get(), ModItens.GRAMA_BAIXA.get());
        // A fruta-sem-graça cozida fica com a cor do brilho dela no Shattered
        evento.register((pilha, camada) -> corDaFruta(FrutaSemGraca.pocao(pilha)), ModItens.FRUTA_SEM_GRACA.get());
    }

    // O brilho da fruta cozida no Shattered (clareado, pra a fruta não ficar escura demais)
    private static int corDaFruta(@Nullable TipoDePocao tipo) {
        if (tipo == null) {
            return 0xFFFFFFFF;
        }
        int cor = switch (tipo) {
            case CURA -> 0x2EE62E;
            case FORCA -> 0xCC0022;
            case GAS_PARALISANTE -> 0x67583D;
            case INVISIBILIDADE -> 0xD9D9D9;
            case CHAMA_LIQUIDA -> 0xFF7F00;
            case GELO -> 0x66B3FF;
            case VISAO_MENTAL -> 0x919999;
            case GAS_TOXICO -> 0xA15CE5;
            case LEVITACAO -> 0x1B5F79;
            case PUREZA -> 0xC152AA;
            case EXPERIENCIA -> 0x404040;
            case PRESSA -> 0xCCBB00;
        };
        int r = (((cor >> 16) & 0xFF) + 255) / 2;
        int g = (((cor >> 8) & 0xFF) + 255) / 2;
        int b = ((cor & 0xFF) + 255) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
