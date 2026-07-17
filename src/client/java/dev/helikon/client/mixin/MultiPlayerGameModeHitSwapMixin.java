package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.MinecraftHitSwapAccess;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selects HitSwap's configured local hotbar slot before vanilla synchronizes and sends an attack. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeHitSwapMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void helikon$selectHitSwapSlot(Player player, Entity target, CallbackInfo callback) {
        MinecraftHitSwapAccess.beforeAttack(player);
    }
}
