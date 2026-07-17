package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.OptionalDouble;

/** Computes one bounded local vertical adjustment while the player is actually on a climbable block. */
public final class FastLadders extends Module {
    private final NumberSetting climbSpeed;

    public FastLadders() {
        super("fast_ladders", "FastLadders", "Applies a conservative local climb velocity on climbable blocks.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        climbSpeed = addSetting(new NumberSetting("climb_speed", "Climb speed",
                "Requested local upward velocity while moving forward on a climbable block.", 0.18D, 0.08D, 0.45D));
    }

    public OptionalDouble verticalVelocity(boolean onClimbable, boolean movingForward, boolean jumping,
                                           boolean sneaking, double currentVerticalVelocity) {
        if (!Double.isFinite(currentVerticalVelocity)) {
            throw new IllegalArgumentException("currentVerticalVelocity must be finite");
        }
        if (!isEnabled() || !onClimbable) {
            return OptionalDouble.empty();
        }
        double requested = climbSpeed.value();
        if (jumping) {
            return OptionalDouble.of(Math.max(currentVerticalVelocity, requested));
        }
        if (sneaking) {
            return OptionalDouble.of(Math.min(currentVerticalVelocity, -requested));
        }
        return movingForward ? OptionalDouble.of(Math.max(currentVerticalVelocity, requested)) : OptionalDouble.empty();
    }
}
