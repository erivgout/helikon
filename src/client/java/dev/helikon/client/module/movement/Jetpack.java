package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.OptionalDouble;

/**
 * Local jetpack: while the Jump key is held the player rises with a bounded
 * upward velocity; on release no motion is imposed, so the player falls
 * normally under vanilla gravity. Unlike Flight this never hovers or sustains
 * flight, and it only touches the vertical component of local velocity.
 */
public final class Jetpack extends Module {
    private final NumberSetting ascendSpeed;
    private final NumberSetting maxRise;

    public Jetpack() {
        super("jetpack", "Jetpack",
                "Hold Jump to rise with local upward velocity; release to fall normally. Servers may reject or correct it.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        ascendSpeed = addSetting(new NumberSetting("ascend_speed", "Ascend speed",
                "Upward velocity in blocks per tick applied while Jump is held.", 0.42D, 0.05D, 1.5D));
        maxRise = addSetting(new NumberSetting("max_rise_speed", "Max rise speed",
                "Upper bound on upward velocity in blocks per tick; the effective ascent is clamped to this.",
                0.6D, 0.05D, 2.0D));
    }

    /**
     * The vertical velocity to impose this tick, or empty to leave vanilla
     * motion untouched (letting the player fall normally). Present only while
     * enabled and the Jump key is held.
     */
    public OptionalDouble ascentVelocity(boolean jumpHeld) {
        if (!isEnabled() || !jumpHeld) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.min(ascendSpeed.value(), maxRise.value()));
    }
}
