package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.OptionalDouble;

/** Suppresses only the local upward movement caused by an active Levitation effect. */
public final class NoLevitation extends Module {
    public NoLevitation() {
        super("no_levitation", "NoLevitation", "Suppresses local upward velocity from Levitation.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
    }

    /**
     * Returns a replacement vertical velocity only for an upward-moving, non-riding local player.
     * Falling velocity is intentionally preserved so disabling the effect does not create a hover.
     */
    public OptionalDouble suppressedVerticalVelocity(boolean levitating, boolean passenger, double verticalVelocity) {
        if (!Double.isFinite(verticalVelocity)) {
            throw new IllegalArgumentException("verticalVelocity must be finite");
        }
        if (!isEnabled() || !levitating || passenger || verticalVelocity <= 0.0D) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(0.0D);
    }
}
