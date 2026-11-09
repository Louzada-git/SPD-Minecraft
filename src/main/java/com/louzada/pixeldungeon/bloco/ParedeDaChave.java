package com.louzada.pixeldungeon.bloco;

import com.louzada.pixeldungeon.Turno;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// A parede espectral da chave de crânio: aparece por 10 turnos e some sozinha.
// (Igual ao Shattered, bloqueia a passagem e a visão.)
public class ParedeDaChave extends Block {
    public static final int TURNOS = 10;

    public ParedeDaChave(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected void onPlace(BlockState estado, Level mundo, BlockPos posicao, BlockState antes, boolean movido) {
        if (!mundo.isClientSide() && !antes.is(this)) {
            mundo.scheduleTick(posicao, this, Turno.ticks(TURNOS));
        }
    }

    @Override
    protected void tick(BlockState estado, ServerLevel mundo, BlockPos posicao, RandomSource sorteio) {
        mundo.removeBlock(posicao, false);
    }
}
