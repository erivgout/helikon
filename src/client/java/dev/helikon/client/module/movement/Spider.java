package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.OptionalDouble;

/** Applies bounded local upward velocity while the player moves into a wall. */
public final class Spider extends Module {
    private final NumberSetting climbSpeed;

    public Spider() {
        super("spider", "Spider", "Climbs local horizontal collisions while movement input is held.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        climbSpeed = addSetting(new NumberSetting("climb_speed", "Climb speed",
                "Local upward velocity while moving into a wall.", 0.20D, 0.08D, 0.50D));
    }

    public OptionalDouble verticalVelocity(boolean screenOpen, boolean horizontalCollision, boolean moving,
                                           boolean onClimbable, boolean sneaking, boolean passenger,
                                           boolean abilityFlying, boolean fallFlying, double currentVerticalVelocity) {
        if (!Double.isFinite(currentVerticalVelocity)) {
            throw new IllegalArgumentException("currentVerticalVelocity must be finite");
        }
        if (!isEnabled() || screenOpen || !horizontalCollision || !moving || onClimbable || sneaking
                || passenger || abilityFlying || fallFlying) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(currentVerticalVelocity, climbSpeed.value()));
    }
}
