package com.louzada.pixeldungeon.cliente;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

// Desenha um inimigo usando um modelo qualquer (por enquanto, modelos do próprio Minecraft),
// com a textura e o tamanho que a gente escolher.
public class RenderizadorDeMob<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private ResourceLocation textura;
    private float escala;

    public RenderizadorDeMob(EntityRendererProvider.Context contexto, M modelo, float sombra, ResourceLocation textura, float escala) {
        super(contexto, modelo, sombra * escala);
        this.textura = textura;
        this.escala = escala;
    }

    @Override
    public ResourceLocation getTextureLocation(T entidade) {
        return textura;
    }

    @Override
    protected void scale(T entidade, PoseStack pose, float parcial) {
        pose.scale(escala, escala, escala);
    }

    // Atalho pra texturas do Minecraft: textura("silverfish") -> minecraft:textures/entity/silverfish.png
    public static ResourceLocation texturaDoMinecraft(String nome) {
        return ResourceLocation.withDefaultNamespace("textures/entity/" + nome + ".png");
    }
}
