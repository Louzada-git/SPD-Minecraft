package com.louzada.pixeldungeon.cliente;

import java.util.UUID;

import com.louzada.pixeldungeon.entidade.ImagemEspelhada;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

// A imagem espelhada é desenhada igualzinha ao herói dono dela (com a pele e a arma dele)
public class RenderizadorDaImagem extends HumanoidMobRenderer<ImagemEspelhada, PlayerModel<ImagemEspelhada>> {

    public RenderizadorDaImagem(EntityRendererProvider.Context contexto) {
        super(contexto, new PlayerModel<>(contexto.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ImagemEspelhada imagem) {
        UUID dono = imagem.dono();
        if (dono == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        ClientPacketListener conexao = Minecraft.getInstance().getConnection();
        PlayerInfo info = conexao == null ? null : conexao.getPlayerInfo(dono);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(dono).texture();
    }
}
