package dev.helikon.client.mixin;

import dev.helikon.client.module.render.EntityEspRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selects only Helikon's current local EntityESP targets for Minecraft's native outline pass. */
@Mixin(Minecraft.class)
abstract class MinecraftEntityEspGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void helikon$showEntityEspTargets(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (EntityEspRenderAccess.shouldAppearGlowing(entity.getId())) {
            callback.setReturnValue(true);
        }
    }
}
