package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Produces bounded local mount velocity from the rider's current movement keys. */
public final class MountBypass extends Module {
    private final NumberSetting horizontalSpeed;
    private final NumberSetting verticalSpeed;
    private final BooleanSetting verticalControl;

    public MountBypass() {
        super("mount_bypass", "MountBypass", "Applies local directional movement to the currently ridden entity.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        horizontalSpeed = addSetting(new NumberSetting("horizontal_speed", "Horizontal speed",
                "Maximum mount horizontal velocity.", 0.35D, 0.05D, 1.0D));
        verticalSpeed = addSetting(new NumberSetting("vertical_speed", "Vertical speed",
                "Jump/sneak mount vertical velocity.", 0.2D, 0.02D, 0.6D));
        verticalControl = addSetting(new BooleanSetting("vertical_control", "Vertical control",
                "Use Jump and Sneak for vertical mount movement.", true));
    }

    public NoClip.Motion motion(double yaw, double forward, double sideways, boolean jump, boolean sneak) {
        double radians = Math.toRadians(yaw);
        double length = Math.max(1.0D, Math.hypot(forward, sideways));
        double x = (-Math.sin(radians) * forward / length + Math.cos(radians) * sideways / length)
                * horizontalSpeed.value();
        double z = (Math.cos(radians) * forward / length + Math.sin(radians) * sideways / length)
                * horizontalSpeed.value();
        double y = verticalControl.value()
                ? ((jump ? 1.0D : 0.0D) - (sneak ? 1.0D : 0.0D)) * verticalSpeed.value() : 0.0D;
        return new NoClip.Motion(x, y, z);
    }
}
