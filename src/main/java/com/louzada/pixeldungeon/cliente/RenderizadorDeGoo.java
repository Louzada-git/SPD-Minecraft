package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.Goo;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;

// O Goo: um slime escuro, largo e baixo, que vai inchando enquanto bombeia o golpe carregado
public class RenderizadorDeGoo extends RenderizadorDeMob<Goo, SlimeModel<Goo>> {
    private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/entity/goo.png");
    // O modelo do slime tem meio bloco; assim ele fica do tamanho da caixa do Goo (1,8 x 1,4 blocos)
    private static final float LARGURA = 3.6F;
    private static final float ALTURA = 2.8F;

    public RenderizadorDeGoo(EntityRendererProvider.Context contexto) {
        super(contexto, new SlimeModel<Goo>(contexto.bakeLayer(ModelLayers.SLIME)), 0.25F, TEXTURA, LARGURA);
        addLayer(new SlimeOuterLayer<>(this, contexto.getModelSet()));
    }

    @Override
    protected void scale(Goo goo, PoseStack pose, float parcial) {
        // Cada etapa de bombear deixa ele um pouco maior (igual ao sprite do Shattered)
        float inchado = goo.carga();
        pose.scale(LARGURA * (1 + 0.08F * inchado), ALTURA * (1 + 0.15F * inchado), LARGURA * (1 + 0.08F * inchado));
    }
}
