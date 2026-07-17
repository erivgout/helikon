package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.FreecamAccess;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Redirects only the verified local mouse-turn invocation while a local-only freecam camera is detached. */
@Mixin(MouseHandler.class)
abstract class MouseFreecamMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void helikon$turnFreecamWhenDetached(LocalPlayer player, double deltaX, double deltaY) {
        FreecamAccess.turn(player, deltaX, deltaY);
    }
}
