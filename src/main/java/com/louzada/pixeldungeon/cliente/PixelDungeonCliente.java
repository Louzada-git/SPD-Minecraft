package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.CaranguejoDeEsgoto;
import com.louzada.pixeldungeon.entidade.CobraDeEsgoto;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.GnollExplorador;
import com.louzada.pixeldungeon.entidade.RatoAlbino;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// Parte do mod que só roda no jogo do jogador (nunca no servidor): desenhos, telas...
@Mod(value = PixelDungeon.MODID, dist = Dist.CLIENT)
public class PixelDungeonCliente {

    public PixelDungeonCliente(IEventBus barramentoDoMod, ModContainer container) {
        barramentoDoMod.addListener(PixelDungeonCliente::aoRegistrarRenderizadores);
    }

    // Como cada inimigo aparece na tela.
    // Por enquanto são modelos e texturas do Minecraft; depois entram as texturas próprias.
    private static void aoRegistrarRenderizadores(EntityRenderersEvent.RegisterRenderers evento) {
        // Rato: modelo de gato, pequeno e cinza
        evento.registerEntityRenderer(ModEntidades.RATO_MARSUPIAL.get(),
                contexto -> new RenderizadorDeMob<RatoMarsupial, OcelotModel<RatoMarsupial>>(contexto,
                        new OcelotModel<RatoMarsupial>(contexto.bakeLayer(ModelLayers.OCELOT)), 0.4F,
                        RenderizadorDeMob.texturaDoMinecraft("cat/british_shorthair"), 0.75F));
        // Cobra: modelo de traça (comprido e com gomos), maior
        evento.registerEntityRenderer(ModEntidades.COBRA_DE_ESGOTO.get(),
                contexto -> new RenderizadorDeMob<CobraDeEsgoto, SilverfishModel<CobraDeEsgoto>>(contexto,
                        new SilverfishModel<CobraDeEsgoto>(contexto.bakeLayer(ModelLayers.SILVERFISH)), 0.3F,
                        RenderizadorDeMob.texturaDoMinecraft("silverfish"), 1.4F));
        // Gnoll: humanoide com a pele do zumbi do deserto
        evento.registerEntityRenderer(ModEntidades.GNOLL_EXPLORADOR.get(),
                contexto -> new RenderizadorDeMob<GnollExplorador, HumanoidModel<GnollExplorador>>(contexto,
                        new HumanoidModel<GnollExplorador>(contexto.bakeLayer(ModelLayers.ZOMBIE)), 0.5F,
                        RenderizadorDeMob.texturaDoMinecraft("zombie/husk"), 1.0F));
        // Caranguejo: modelo de aranha, baixinho
        evento.registerEntityRenderer(ModEntidades.CARANGUEJO_DE_ESGOTO.get(),
                contexto -> new RenderizadorDeMob<CaranguejoDeEsgoto, SpiderModel<CaranguejoDeEsgoto>>(contexto,
                        new SpiderModel<CaranguejoDeEsgoto>(contexto.bakeLayer(ModelLayers.SPIDER)), 0.8F,
                        RenderizadorDeMob.texturaDoMinecraft("spider/cave_spider"), 0.65F));
        // Rato albino: o mesmo gato pequeno, branco
        evento.registerEntityRenderer(ModEntidades.RATO_ALBINO.get(),
                contexto -> new RenderizadorDeMob<RatoAlbino, OcelotModel<RatoAlbino>>(contexto,
                        new OcelotModel<RatoAlbino>(contexto.bakeLayer(ModelLayers.OCELOT)), 0.4F,
                        RenderizadorDeMob.texturaDoMinecraft("cat/white"), 0.75F));
        // Enxame de moscas: modelo de endermite, voando
        evento.registerEntityRenderer(ModEntidades.ENXAME_DE_MOSCAS.get(),
                contexto -> new RenderizadorDeMob<EnxameDeMoscas, EndermiteModel<EnxameDeMoscas>>(contexto,
                        new EndermiteModel<EnxameDeMoscas>(contexto.bakeLayer(ModelLayers.ENDERMITE)), 0.3F,
                        RenderizadorDeMob.texturaDoMinecraft("endermite"), 1.3F));
        // Slime: o slime do Minecraft
        evento.registerEntityRenderer(ModEntidades.SLIME.get(), RenderizadorDeSlime::new);
        // Goo: um slime escuro e grande
        evento.registerEntityRenderer(ModEntidades.GOO.get(), RenderizadorDeGoo::new);
    }
}
