package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.StepAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies Step's local height to the local player only. LivingEntity overrides
 * Entity.maxUpStep with the STEP_HEIGHT attribute and never calls super, so the
 * injection must target this override.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityStepMixin {
    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void helikon$applyLocalStepHeight(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof LocalPlayer) {
            callback.setReturnValue(StepAccess.height(callback.getReturnValue()));
        }
    }
}
