package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppresses only GameRenderer's local hurt-camera transform, not the death-camera rotation. */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
abstract class HurtCameraMixin {
    @Redirect(
            method = "bobHurt",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;hurtTime:F")
    )
    private float helikon$hideHurtCamera(CameraEntityRenderState entityState) {
        return RenderModuleAccess.hideHurtCamera() ? 0.0F : entityState.hurtTime;
    }
}
