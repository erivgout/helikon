package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Configures bounded local movement while ordinary player collision is disabled. */
public final class NoClip extends Module {
    private final NumberSetting horizontalSpeed;
    private final NumberSetting verticalSpeed;

    public NoClip() {
        super("no_clip", "NoClip", "Attempts local movement through blocks with bounded directional velocity.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        horizontalSpeed = addSetting(new NumberSetting("horizontal_speed", "Horizontal speed",
                "Horizontal movement per tick.", 0.2D, 0.02D, 0.8D));
        verticalSpeed = addSetting(new NumberSetting("vertical_speed", "Vertical speed",
                "Jump/sneak movement per tick.", 0.2D, 0.02D, 0.8D));
    }

    public Motion motion(double yawDegrees, double forward, double sideways, boolean jump, boolean sneak) {
        double length = Math.hypot(forward, sideways);
        double normalizedForward = length > 1.0D ? forward / length : forward;
        double normalizedSideways = length > 1.0D ? sideways / length : sideways;
        double yaw = Math.toRadians(yawDegrees);
        double x = (-Math.sin(yaw) * normalizedForward + Math.cos(yaw) * normalizedSideways)
                * horizontalSpeed.value();
        double z = (Math.cos(yaw) * normalizedForward + Math.sin(yaw) * normalizedSideways)
                * horizontalSpeed.value();
        double y = ((jump ? 1.0D : 0.0D) - (sneak ? 1.0D : 0.0D)) * verticalSpeed.value();
        return new Motion(x, y, z);
    }

    public record Motion(double x, double y, double z) {
        public Motion {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("NoClip motion must be finite");
            }
        }
    }
}
