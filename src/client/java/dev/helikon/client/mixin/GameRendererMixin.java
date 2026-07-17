package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters only the local visual activation emitted for a death-protection item. */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
abstract class GameRendererMixin {
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void helikon$hideDeathProtectionActivation(ItemStack itemStack, CallbackInfo callback) {
        if (RenderModuleAccess.shouldHideItemActivation(itemStack.has(DataComponents.DEATH_PROTECTION))) {
            callback.cancel();
        }
    }
}
