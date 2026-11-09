package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.Guarda;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// As guardas da varinha de proteção, com os desenhos do Shattered (o "WardSprite"): cada nível tem um desenho
// de tamanho diferente na mesma folha. As sentinelas (nível 4 a 6) flutuam um pouco, subindo e descendo.
// Igual ao Shattered, a guarda some aos poucos quando morre.
public class RenderizadorDaGuarda extends EntityRenderer<Guarda> {
    private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/entity/shattered/wards.png");
    private static final int LARGURA_DA_FOLHA = 64;
    private static final int ALTURA_DA_FOLHA = 16;
    private static final float PIXEL = 1 / 16F;
    // O retângulo de cada nível na folha (esquerda, cima, direita, baixo), iguais aos do Shattered
    private static final int[][] DESENHOS = {
            { 0, 0, 9, 10 }, { 10, 0, 21, 12 }, { 22, 0, 37, 16 }, { 38, 0, 44, 13 }, { 45, 0, 51, 15 }, { 52, 0, 60, 15 }
    };

    public RenderizadorDaGuarda(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.shadowRadius = 0.3F;
    }

    @Override
    public ResourceLocation getTextureLocation(Guarda guarda) {
        return TEXTURA;
    }

    @Override
    public void render(Guarda guarda, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        if (!guarda.isInvisible()) {
            desenhar(guarda, parcial, pose, buffers, luz);
        }
        super.render(guarda, giro, parcial, pose, buffers, luz);
    }

    private void desenhar(Guarda guarda, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        int[] desenho = DESENHOS[Mth.clamp(guarda.nivel(), 1, 6) - 1];
        float u0 = desenho[0] / (float) LARGURA_DA_FOLHA;
        float v0 = desenho[1] / (float) ALTURA_DA_FOLHA;
        float u1 = desenho[2] / (float) LARGURA_DA_FOLHA;
        float v1 = desenho[3] / (float) ALTURA_DA_FOLHA;
        float largura = (desenho[2] - desenho[0]) * PIXEL;
        float altura = (desenho[3] - desenho[1]) * PIXEL;
        float base = 0;
        if (guarda.nivel() >= 4) {
            base = 0.25F + 0.08F * Mth.sin((guarda.tickCount + parcial) / 10.0F);
        }
        int transparencia = 255;
        if (guarda.deathTime > 0) {
            transparencia = Math.max(0, 255 - (int) ((guarda.deathTime + parcial) * 12));
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityRenderDispatcher.camera.getYRot()));
        PoseStack.Pose ultimo = pose.last();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(TEXTURA));
        int marca = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(guarda.hurtTime > 0));
        vertice(vertices, ultimo, -largura / 2, base, u0, v1, marca, luz, transparencia);
        vertice(vertices, ultimo, largura / 2, base, u1, v1, marca, luz, transparencia);
        vertice(vertices, ultimo, largura / 2, base + altura, u1, v0, marca, luz, transparencia);
        vertice(vertices, ultimo, -largura / 2, base + altura, u0, v0, marca, luz, transparencia);
        pose.popPose();
    }

    private static void vertice(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v, int marca, int luz, int transparencia) {
        vertices.addVertex(pose, x, y, 0).setColor(255, 255, 255, transparencia).setUv(u, v).setOverlay(marca).setLight(luz).setNormal(pose, 0, 1, 0);
    }
}
