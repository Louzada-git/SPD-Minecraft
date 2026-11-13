package com.louzada.pixeldungeon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.louzada.pixeldungeon.item.Acessorios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

// A vida que volta sozinha (a "regeneração" do Shattered) no Minecraft vem da fome. Marca quando a cura vem dela,
// pro cubo de sal (acessório) deixar essa cura mais fraca (ver Acessorios.aoCurar).
@Mixin(FoodData.class)
public class RegeneracaoDaFome {
    @Inject(method = "tick", at = @At("HEAD"))
    private void pixeldungeon$antes(Player jogador, CallbackInfo retorno) {
        Acessorios.marcarCuraDaFome(true);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void pixeldungeon$depois(Player jogador, CallbackInfo retorno) {
        Acessorios.marcarCuraDaFome(false);
    }
}
