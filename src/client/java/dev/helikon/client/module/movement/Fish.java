package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Optional;

/** Applies bounded, input-driven local velocity assistance while swimming underwater. */
public final class Fish extends Module {
    public record Context(boolean inWater, boolean screenOpen, boolean passenger, boolean abilityFlying,
                          boolean fallFlying, boolean moving, boolean jumping, boolean sneaking,
                          HorizontalVelocity desiredDirection, HorizontalVelocity currentHorizontal,
                          double currentVertical) {
        public Context {
            if (desiredDirection == null || currentHorizontal == null || !Double.isFinite(currentVertical)) {
                throw new IllegalArgumentException("fish context is invalid");
            }
        }
    }

    public record Velocity(HorizontalVelocity horizontal, double vertical) {
        public Velocity {
            if (horizontal == null || !Double.isFinite(vertical)) {
                throw new IllegalArgumentException("fish velocity is invalid");
            }
        }
    }

    private final NumberSetting horizontalSpeed;
    private final NumberSetting verticalSpeed;

    public Fish() {
        super("fish", "Fish", "Applies bounded local underwater swimming velocity from normal movement input.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        horizontalSpeed = addSetting(new NumberSetting("horizontal_speed", "Horizontal speed",
                "Local underwater horizontal speed while directional input is held.", 0.15D, 0.05D, 0.60D));
        verticalSpeed = addSetting(new NumberSetting("vertical_speed", "Vertical speed",
                "Local underwater ascent or descent speed while Jump or Sneak is held.", 0.12D, 0.05D, 0.40D));
    }

    /** Produces a local velocity only for an eligible underwater input state. */
    public Optional<Velocity> velocity(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("fish context must not be null");
        }
        if (!isEnabled() || !context.inWater() || context.screenOpen() || context.passenger()
                || context.abilityFlying() || context.fallFlying()
                || (!context.moving() && !context.jumping() && !context.sneaking())) {
            return Optional.empty();
        }
        HorizontalVelocity horizontal = context.currentHorizontal();
        if (context.moving() && context.desiredDirection().speed() > 0.0D) {
            horizontal = context.desiredDirection().scale(horizontalSpeed.value() / context.desiredDirection().speed());
        }
        double vertical = context.currentVertical();
        if (context.jumping() != context.sneaking()) {
            vertical = context.jumping() ? verticalSpeed.value() : -verticalSpeed.value();
        }
        return Optional.of(new Velocity(horizontal, vertical));
    }
}
