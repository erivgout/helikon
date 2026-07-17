package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.NoSlowAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies only configured local use/sneak movement-speed exemptions to the verified 26.2 methods. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerNoSlowMixin {
    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void helikon$ignoreConfiguredUseSlowdown(CallbackInfoReturnable<Float> callback) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && NoSlowAccess.ignoreUse(player.getUseItem().getUseAnimation())) {
            callback.setReturnValue(1.0F);
        }
    }

    @Inject(method = "modifyInput", at = @At("RETURN"), cancellable = true)
    private void helikon$restoreConfiguredSneakInput(Vec2 original, CallbackInfoReturnable<Vec2> callback) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!NoSlowAccess.ignoreSneak(player)) {
            return;
        }
        double sneakSpeed = player.getAttributeValue(Attributes.SNEAKING_SPEED);
        if (Double.isFinite(sneakSpeed) && sneakSpeed > 0.0D) {
            callback.setReturnValue(callback.getReturnValue().scale((float) (1.0D / sneakSpeed)));
        }
    }
}
