package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.WTapAccess;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports an initiated local attack to WTap before vanilla resets sprint, so the
 * module can observe the true sprint/forward state of the hit. It never changes
 * the attack itself.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class WTapAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void helikon$observeAttackForWTap(Player player, Entity target, CallbackInfo callback) {
        WTapAccess.observeAttack(player, target);
    }
}
