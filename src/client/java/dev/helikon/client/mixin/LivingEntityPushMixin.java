package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.AntiEntityPushAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Declines only the local player's ordinary entity-collision push response. */
@Mixin(LivingEntity.class)
abstract class LivingEntityPushMixin {
    @Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
    private void helikon$preventLocalEntityCollisionPush(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof LocalPlayer player && AntiEntityPushAccess.preventCollisionPush(player)) {
            callback.setReturnValue(false);
        }
    }
}
