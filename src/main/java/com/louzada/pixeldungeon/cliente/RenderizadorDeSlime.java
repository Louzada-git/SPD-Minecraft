package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.entidade.SlimeDeEsgoto;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;

// O slime tem a camada de fora transparente, igual ao slime do Minecraft
public class RenderizadorDeSlime extends RenderizadorDeMob<SlimeDeEsgoto, SlimeModel<SlimeDeEsgoto>> {
    public RenderizadorDeSlime(EntityRendererProvider.Context contexto) {
        super(contexto, new SlimeModel<SlimeDeEsgoto>(contexto.bakeLayer(ModelLayers.SLIME)), 0.5F,
                texturaDoMinecraft("slime/slime"), 2.0F);
        addLayer(new SlimeOuterLayer<>(this, contexto.getModelSet()));
    }
}
