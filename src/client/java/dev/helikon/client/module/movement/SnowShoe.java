package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Prevents local downward motion while the player's feet are inside powder snow. */
public final class SnowShoe extends Module {
    private final NumberSetting riseSpeed;

    public SnowShoe() {
        super("snow_shoe", "SnowShoe", "Keeps the local player from sinking in loaded powder snow.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        riseSpeed = addSetting(new NumberSetting("rise_speed", "Rise speed",
                "Upward velocity while Jump is held in powder snow.", 0.12D, 0.02D, 0.3D));
    }

    public double verticalVelocity(boolean inPowderSnow, boolean jumping, double current) {
        if (!isEnabled() || !inPowderSnow) {
            return current;
        }
        return jumping ? Math.max(current, riseSpeed.value()) : Math.max(current, 0.0D);
    }
}
