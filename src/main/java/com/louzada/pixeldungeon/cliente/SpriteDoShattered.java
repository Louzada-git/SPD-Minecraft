package com.louzada.pixeldungeon.cliente;

import com.louzada.pixeldungeon.PixelDungeon;

import net.minecraft.resources.ResourceLocation;

// A folha de desenhos de um inimigo do Shattered (os arquivos "sprites/*.png" do jogo) e as animações dele.
// Os números são os mesmos das classes "*Sprite" do Shattered: o tamanho de cada quadro e, em cada animação,
// quantos quadros por segundo e quais quadros (contando da esquerda pra direita, linha por linha).
//   pixel: quantos blocos cada pixel do desenho ocupa no mundo
public record SpriteDoShattered(ResourceLocation textura, int larguraDaFolha, int alturaDaFolha, int largura, int altura, float pixel,
        Animacao parado, Animacao andando, Animacao atacando, Animacao morrendo, Animacao especial) {

    public record Animacao(float quadrosPorSegundo, boolean repete, int... quadros) {
        // O quadro da animação depois de "segundos" tocando
        public int quadro(float segundos) {
            int passo = (int) (segundos * quadrosPorSegundo);
            if (repete) {
                return quadros[Math.floorMod(passo, quadros.length)];
            }
            return quadros[Math.min(passo, quadros.length - 1)];
        }

        // Quanto tempo ela leva de ponta a ponta
        public float duracao() {
            return quadros.length / quadrosPorSegundo;
        }
    }

    private static Animacao anim(float fps, boolean repete, int... quadros) {
        return new Animacao(fps, repete, quadros);
    }

    private static ResourceLocation textura(String nome) {
        return ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "textures/entity/shattered/" + nome + ".png");
    }

    // RatSprite
    public static final SpriteDoShattered RATO = new SpriteDoShattered(textura("rat"), 256, 64, 16, 15, 1 / 16F,
            anim(2, true, 0, 0, 0, 1), anim(10, true, 6, 7, 8, 9, 10), anim(15, false, 2, 3, 4, 5, 0), anim(10, false, 11, 12, 13, 14), null);
    // AlbinoSprite (a segunda linha da folha do rato)
    public static final SpriteDoShattered RATO_ALBINO = new SpriteDoShattered(textura("rat"), 256, 64, 16, 15, 1 / 16F,
            anim(2, true, 16, 16, 16, 17), anim(10, true, 22, 23, 24, 25, 26), anim(15, false, 18, 19, 20, 21), anim(10, false, 27, 28, 29, 30),
            null);
    // SnakeSprite (a língua é rápida, mas o corpo sobe e desce devagar)
    public static final SpriteDoShattered COBRA = new SpriteDoShattered(textura("snake"), 256, 16, 12, 11, 1 / 16F,
            anim(10, true, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 1, 1),
            anim(8, true, 4, 5, 6, 7), anim(15, false, 8, 9, 10, 9, 0), anim(10, false, 11, 12, 13), null);
    // GnollSprite (do tamanho de uma pessoa)
    public static final SpriteDoShattered GNOLL = new SpriteDoShattered(textura("gnoll"), 256, 64, 12, 15, 0.125F,
            anim(2, true, 0, 0, 0, 1, 0, 0, 1, 1), anim(12, true, 4, 5, 6, 7), anim(12, false, 2, 3, 0), anim(12, false, 8, 9, 10), null);
    // CrabSprite
    public static final SpriteDoShattered CARANGUEJO = new SpriteDoShattered(textura("crab"), 256, 64, 16, 16, 1 / 16F,
            anim(5, true, 0, 1, 0, 2), anim(15, true, 3, 4, 5, 6), anim(12, false, 7, 8, 9), anim(12, false, 10, 11, 12, 13), null);
    // SwarmSprite
    public static final SpriteDoShattered ENXAME = new SpriteDoShattered(textura("swarm"), 256, 16, 16, 16, 1 / 16F,
            anim(15, true, 0, 1, 2, 3, 4, 5), anim(15, true, 0, 1, 2, 3, 4, 5), anim(20, false, 6, 7, 8, 9), anim(15, false, 10, 11, 12, 13, 14),
            null);
    // SlimeSprite
    public static final SpriteDoShattered SLIME = new SpriteDoShattered(textura("slime"), 128, 32, 14, 12, 1 / 12F,
            anim(3, true, 0, 1, 1, 0), anim(10, true, 0, 2, 3, 3, 2, 0), anim(15, false, 2, 3, 4, 6, 5), anim(10, false, 0, 5, 6, 7), null);
    // GooSprite (o especial é o "pump": ele bombeando antes do golpe carregado)
    public static final SpriteDoShattered GOO = new SpriteDoShattered(textura("goo"), 256, 16, 20, 14, 0.1F,
            anim(10, true, 2, 1, 0, 0, 1), anim(15, true, 3, 2, 1, 2), anim(10, false, 8, 9, 10), anim(10, false, 5, 6, 7),
            anim(20, true, 4, 3, 2, 1, 0));
    // EarthGuardianSprite (o guardião da terra, do tamanho de uma pessoa)
    public static final SpriteDoShattered GUARDIAO = new SpriteDoShattered(textura("guardian"), 256, 16, 12, 15, 0.125F,
            anim(2, true, 0, 0, 0, 0, 0, 1, 1), anim(15, true, 2, 3, 4, 5, 6, 7), anim(12, false, 8, 9, 10), anim(5, false, 11, 12, 13, 14, 15, 15),
            null);
    // MimicSprite (o mímico comum)
    public static final SpriteDoShattered MIMICO = new SpriteDoShattered(textura("mimic"), 256, 64, 16, 16, 1 / 16F,
            anim(5, true, 3, 3, 3, 4, 4), anim(10, true, 3, 4, 5, 6, 6, 5, 4), anim(10, false, 3, 7, 8, 9), anim(5, false, 10, 11, 12), null);
}
