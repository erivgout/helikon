package dev.helikon.client.mixin;

import dev.helikon.client.module.player.PortalGuiAccess;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Changes only the verified portal screen-allowance query. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerPortalGuiMixin {
    @Redirect(method = "handlePortalTransitionEffect",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;isAllowedInPortal()Z"))
    private boolean helikon$allowCurrentPortalScreen(Screen screen) {
        return PortalGuiAccess.allows(screen.isAllowedInPortal());
    }
}
