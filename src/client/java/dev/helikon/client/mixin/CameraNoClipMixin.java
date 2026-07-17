package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bypasses only Camera's local third-person visual collision-distance calculation. */
@Mixin(targets = "net.minecraft.client.Camera")
abstract class CameraNoClipMixin {
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void helikon$keepRequestedThirdPersonDistance(float requestedDistance,
                                                           CallbackInfoReturnable<Float> callback) {
        if (RenderModuleAccess.useUnclippedCameraDistance()) {
            callback.setReturnValue(requestedDistance);
        }
    }
}
