package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.OptionalDouble;

/** Holds the local player at the surface of ordinary water while enabled. */
public final class Jesus extends Module {
    private final NumberSetting catchDepth;

    public Jesus() {
        super("jesus", "Jesus", "Holds the local player steadily on the surface of ordinary water.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        catchDepth = addSetting(new NumberSetting("catch_depth", "Catch depth",
                "Maximum distance below the actual water surface that Jesus will stabilize.", 0.55D, 0.10D, 0.90D));
    }

    /** Returns the exact fluid surface height to hold, without applying recurring upward acceleration. */
    public OptionalDouble surfaceHeight(boolean hasWaterSurface, boolean sneaking, boolean jumping, boolean passenger,
                                        boolean abilityFlying, boolean fallFlying, double playerY,
                                        double waterSurfaceY, double currentVerticalVelocity) {
        if (!Double.isFinite(playerY) || !Double.isFinite(waterSurfaceY)
                || !Double.isFinite(currentVerticalVelocity)) {
            throw new IllegalArgumentException("Jesus surface facts must be finite");
        }
        if (!isEnabled() || !hasWaterSurface || sneaking || jumping || passenger || abilityFlying || fallFlying
                || currentVerticalVelocity > 0.30D
                || playerY < waterSurfaceY - catchDepth.value() || playerY > waterSurfaceY + 0.08D) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(waterSurfaceY);
    }
}
