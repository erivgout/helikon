package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppresses only the verified local first-person fire screen-effect branch. */
@Mixin(targets = "net.minecraft.client.renderer.ScreenEffectRenderer")
abstract class ScreenEffectRendererMixin {
    @Redirect(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z")
    )
    private boolean helikon$hideFireOverlay(LocalPlayer player) {
        return !RenderModuleAccess.hideFireOverlay() && player.isOnFire();
    }
}
