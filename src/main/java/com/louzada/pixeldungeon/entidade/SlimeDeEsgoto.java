package com.louzada.pixeldungeon.entidade;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Slime (andar 4): a membrana elástica deixa difícil passar de 6 de dano num golpe só.
public class SlimeDeEsgoto extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(20, 12, 5, 2, 5, 0, 0, 4, 9, 0.22);

    public SlimeDeEsgoto(EntityType<? extends SlimeDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS);
    }

    // Igual ao Shattered: dano acima de 4 vai sendo "amortecido"
    @Override
    public float reduzirDanoRecebido(float dano) {
        if (dano >= 5) {
            return 4 + (float) (Math.sqrt(8 * (dano - 4) + 1) - 1) / 2;
        }
        return dano;
    }
}
