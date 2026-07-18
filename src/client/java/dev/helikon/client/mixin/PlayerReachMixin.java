package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.ReachAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends only the local player's verified 26.2 block-interaction range query. */
@Mixin(Player.class)
abstract class PlayerReachMixin {
    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void helikon$extendBlockInteractionRange(CallbackInfoReturnable<Double> callback) {
        if ((Object) this instanceof LocalPlayer) {
            callback.setReturnValue(ReachAccess.blockInteractionRange(callback.getReturnValue()));
        }
    }
}
