package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.entidade.Goo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

// O Goo: o desenho do Shattered, que bombeia (e vai inchando) enquanto carrega o golpe forte
public class RenderizadorDeGoo extends RenderizadorDeSprite<Goo> {

    public RenderizadorDeGoo(EntityRendererProvider.Context contexto) {
        super(contexto, SpriteDoShattered.GOO, 0.9F);
    }

    @Override
    protected boolean usaEspecial(Goo goo) {
        return goo.carga() > 0;
    }

    // Cada etapa de bombear deixa ele um pouco maior
    @Override
    protected float esticarLargura(Goo goo) {
        return 1 + 0.08F * goo.carga();
    }

    @Override
    protected float esticarAltura(Goo goo) {
        return 1 + 0.15F * goo.carga();
    }
}
