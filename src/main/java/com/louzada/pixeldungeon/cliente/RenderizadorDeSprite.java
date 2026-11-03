package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.cliente.SpriteDoShattered.Animacao;
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
import net.minecraft.world.entity.Mob;

// Desenha um inimigo com o desenho animado do Shattered: uma "placa" de pé que sempre vira pra câmera
// (girando só em volta do eixo de cima), igual aos personagens do jogo original.
//   - a animação muda sozinha: parado, andando, atacando (quando ele dá o golpe) e morrendo
//   - igual ao Shattered, os desenhos olham pra direita: quando o inimigo anda pra esquerda da tela, o desenho vira
//   - fica vermelho quando apanha, igual aos mobs do Minecraft
public class RenderizadorDeSprite<T extends Mob> extends EntityRenderer<T> {
    private SpriteDoShattered sprite;

    public RenderizadorDeSprite(EntityRendererProvider.Context contexto, SpriteDoShattered sprite, float sombra) {
        super(contexto);
        this.sprite = sprite;
        this.shadowRadius = sombra;
    }

    @Override
    public ResourceLocation getTextureLocation(T mob) {
        return sprite.textura();
    }

    // A animação especial (o Goo bombeando) está tocando?
    protected boolean usaEspecial(T mob) {
        return false;
    }

    // Quanto o desenho estica na largura e na altura (o Goo incha quando bombeia)
    protected float esticarLargura(T mob) {
        return 1.0F;
    }

    protected float esticarAltura(T mob) {
        return 1.0F;
    }

    @Override
    public void render(T mob, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        if (!mob.isInvisible()) {
            desenhar(mob, parcial, pose, buffers, luz);
        }
        super.render(mob, giro, parcial, pose, buffers, luz);
    }

    private void desenhar(T mob, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        int quadro = quadroAtual(mob, parcial);
        int colunas = sprite.larguraDaFolha() / sprite.largura();
        float u0 = (quadro % colunas) * sprite.largura() / (float) sprite.larguraDaFolha();
        float v0 = (quadro / colunas) * sprite.altura() / (float) sprite.alturaDaFolha();
        float u1 = u0 + sprite.largura() / (float) sprite.larguraDaFolha();
        float v1 = v0 + sprite.altura() / (float) sprite.alturaDaFolha();

        // Pra que lado da tela ele olha: o corpo dele contra a direita da câmera
        float giroDaCamera = entityRenderDispatcher.camera.getYRot();
        float corpo = Mth.rotLerp(parcial, mob.yBodyRotO, mob.yBodyRot) * Mth.DEG_TO_RAD;
        float camera = giroDaCamera * Mth.DEG_TO_RAD;
        float paraDireita = -Mth.sin(corpo) * -Mth.cos(camera) + Mth.cos(corpo) * -Mth.sin(camera);
        if (paraDireita < 0) {
            float troca = u0;
            u0 = u1;
            u1 = troca;
        }

        float largura = sprite.largura() * sprite.pixel() * esticarLargura(mob);
        float altura = sprite.altura() * sprite.pixel() * esticarAltura(mob);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - giroDaCamera));
        PoseStack.Pose ultimo = pose.last();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(sprite.textura()));
        int marca = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(mob.hurtTime > 0 || mob.deathTime > 0));
        vertice(vertices, ultimo, -largura / 2, 0, u0, v1, marca, luz);
        vertice(vertices, ultimo, largura / 2, 0, u1, v1, marca, luz);
        vertice(vertices, ultimo, largura / 2, altura, u1, v0, marca, luz);
        vertice(vertices, ultimo, -largura / 2, altura, u0, v0, marca, luz);
        pose.popPose();
    }

    private static void vertice(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v, int marca, int luz) {
        vertices.addVertex(pose, x, y, 0).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(marca).setLight(luz).setNormal(pose, 0, 1, 0);
    }

    // O quadro do desenho agora, pela animação que está tocando
    private int quadroAtual(T mob, float parcial) {
        if (mob.deathTime > 0 || !mob.isAlive()) {
            return sprite.morrendo().quadro((mob.deathTime + parcial) / 20.0F);
        }
        float golpe = mob.getAttackAnim(parcial);
        if (golpe > 0) {
            Animacao ataque = sprite.atacando();
            return ataque.quadro(golpe * ataque.duracao());
        }
        float segundos = (mob.tickCount + parcial) / 20.0F;
        if (sprite.especial() != null && usaEspecial(mob)) {
            return sprite.especial().quadro(segundos);
        }
        if (mob.walkAnimation.speed(parcial) > 0.1F) {
            return sprite.andando().quadro(segundos);
        }
        return sprite.parado().quadro(segundos);
    }
}
