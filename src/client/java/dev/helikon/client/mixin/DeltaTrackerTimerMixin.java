package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.TimerModuleAccess;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies Timer's bounded local multiplier only to Minecraft's verified game-time delta calculation. */
@Mixin(DeltaTracker.Timer.class)
abstract class DeltaTrackerTimerMixin {
    @Redirect(method = "advanceGameTime", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float helikon$applyTimerMultiplier(FloatUnaryOperator provider, float vanillaMsPerTick) {
        return provider.apply(vanillaMsPerTick) / TimerModuleAccess.multiplier();
    }
}
