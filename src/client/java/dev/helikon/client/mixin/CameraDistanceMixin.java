package dev.helikon.client.mixin;

import dev.helikon.client.module.render.CameraModuleAccess;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the configured desired third-person distance while retaining Camera's normal collision clamp. */
@Mixin(Camera.class)
abstract class CameraDistanceMixin {
    @Shadow
    private float getMaxZoom(float desiredDistance) {
        throw new AssertionError();
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float helikon$cameraDistance(Camera camera, float vanillaDistance) {
        return getMaxZoom(CameraModuleAccess.desiredDistance(vanillaDistance));
    }
}
