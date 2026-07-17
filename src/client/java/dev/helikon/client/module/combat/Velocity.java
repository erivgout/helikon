package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/**
 * Scales ordinary server-supplied motion for the local player. The Minecraft-specific packet
 * adapter supplies the received vector; this class owns only the tested scaling decision.
 */
public final class Velocity extends Module {
    private final NumberSetting horizontalPercent;
    private final NumberSetting verticalPercent;

    public Velocity() {
        super("velocity", "Velocity",
                "Scales received local-player knockback; the server may correct or reject the resulting motion.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        horizontalPercent = addSetting(new NumberSetting(
                "horizontal_percent",
                "Horizontal percent",
                "Percentage of received horizontal motion to retain.",
                0.0D,
                0.0D,
                200.0D
        ));
        verticalPercent = addSetting(new NumberSetting(
                "vertical_percent",
                "Vertical percent",
                "Percentage of received vertical motion to retain.",
                0.0D,
                0.0D,
                200.0D
        ));
    }

    /**
     * Returns the local motion that should replace a received entity-motion vector.
     * Disabled modules and non-finite inputs are deliberately passed through unchanged.
     */
    public Motion adjust(double x, double y, double z) {
        if (!isEnabled() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return new Motion(x, y, z);
        }
        double horizontalScale = horizontalPercent.value() / 100.0D;
        double verticalScale = verticalPercent.value() / 100.0D;
        return new Motion(x * horizontalScale, y * verticalScale, z * horizontalScale);
    }

    public double horizontalPercent() {
        return horizontalPercent.value();
    }

    public double verticalPercent() {
        return verticalPercent.value();
    }

    public record Motion(double x, double y, double z) {
    }
}
