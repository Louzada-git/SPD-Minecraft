package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.entidade.Mimico;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

// O mímico: o modelo do baú do Minecraft, com a tampa abrindo e fechando como uma boca.
// Parado ele "mastiga" devagar; andando, mais rápido; mordendo, a tampa abre bem.
// (Escondido ele é um baú de verdade, então este modelo só aparece depois que ele se revela.)
public class ModeloDoMimico extends EntityModel<Mimico> {
    private ModelPart fundo;
    private ModelPart tampa;
    private ModelPart fechadura;

    // Recebe as peças do baú do Minecraft (ModelLayers.CHEST)
    public ModeloDoMimico(ModelPart raiz) {
        fundo = raiz.getChild("bottom");
        tampa = raiz.getChild("lid");
        fechadura = raiz.getChild("lock");
    }

    @Override
    public void setupAnim(Mimico mimico, float passos, float velocidadeDosPassos, float idade, float giroDaCabeca, float inclinacaoDaCabeca) {
        float rapidez = 0.25F + Math.min(1.0F, velocidadeDosPassos) * 0.5F;
        float mastigar = (Mth.sin(idade * rapidez) + 1.0F) * 0.5F * 0.3F;
        float mordida = Mth.sin(attackTime * Mth.PI) * 0.9F;
        float abertura = Math.max(mastigar, mordida);
        tampa.xRot = -(abertura * Mth.HALF_PI);
        fechadura.xRot = tampa.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int luz, int sobreposicao, int cor) {
        pose.pushPose();
        // O Minecraft desenha os mobs de cabeça pra baixo e o baú é desenhado de pé: aqui ele é desvirado,
        // virado de frente pra onde o mímico olha e centralizado
        pose.translate(0.0F, 1.501F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate(-0.5F, 0.0F, -0.5F);
        fundo.render(pose, vertices, luz, sobreposicao, cor);
        tampa.render(pose, vertices, luz, sobreposicao, cor);
        fechadura.render(pose, vertices, luz, sobreposicao, cor);
        pose.popPose();
    }
}
