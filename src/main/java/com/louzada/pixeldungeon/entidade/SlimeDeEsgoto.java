package com.louzada.pixeldungeon.entidade;

import java.util.Random;

import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Slime (andar 4): a membrana elástica deixa difícil passar de 6 de dano num golpe só.
// Igual ao Shattered, às vezes deixa uma arma de tier 2 que ele tinha engolido:
// a chance começa em 1 em 5 e cada arma que já caiu na run deixa a próxima 4x mais difícil.
public class SlimeDeEsgoto extends MobDoShattered {
    public static final Atributos ATRIBUTOS = new Atributos(20, 12, 5, 2, 5, 0, 0, 4, 9, 0.22);
    public static final Sons SONS = new Sons(SoundEvents.SLIME_SQUISH, SoundEvents.SLIME_HURT, SoundEvents.SLIME_DEATH);
    private static final String ARMA_DO_SLIME = "arma_do_slime";

    public SlimeDeEsgoto(EntityType<? extends SlimeDeEsgoto> tipo, Level mundo) {
        super(tipo, mundo, ATRIBUTOS, SONS);
    }

    // Igual ao Shattered: dano acima de 4 vai sendo "amortecido"
    @Override
    public float reduzirDanoRecebido(float dano) {
        if (dano >= 5) {
            return 4 + (float) (Math.sqrt(8 * (dano - 4) + 1) - 1) / 2;
        }
        return dano;
    }

    @Override
    public ItemStack sortearSaque() {
        if (!(level() instanceof ServerLevel mundo)) {
            return ItemStack.EMPTY;
        }
        DadosDaRun run = DadosDaRun.pegar(mundo.getServer());
        float chance = 0.2F * (float) Math.pow(0.25, run.quedas(ARMA_DO_SLIME));
        if (getRandom().nextFloat() < chance) {
            run.contarQueda(ARMA_DO_SLIME);
            return SaqueDoAndar.armaDoTier(new Random(getRandom().nextLong()), 2);
        }
        return ItemStack.EMPTY;
    }

    // Sangue verde (cor do Shattered)
    @Override
    public int corDoSangue() {
        return 0x88CC44;
    }
}
