package com.louzada.pixeldungeon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;

// No Minecraft, com 3 coxinhas de fome ou menos o jogador não consegue correr.
// No Shattered a fome não deixa ninguém mais lento, então aqui dá pra correr sempre.
@Mixin(LocalPlayer.class)
public class CorrerComFome {
    @Inject(method = "hasEnoughFoodToStartSprinting", at = @At("HEAD"), cancellable = true)
    private void pixeldungeon$correrSempre(CallbackInfoReturnable<Boolean> retorno) {
        retorno.setReturnValue(true);
    }
}
