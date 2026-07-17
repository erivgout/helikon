package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Optional;

/** Requests a normal local disconnect when an enabled, observed danger threshold is reached. */
public final class AutoLeave extends Module {
    public enum Danger {
        LOW_HEALTH,
        FALL_DISTANCE
    }

    public record Context(double health, double fallDistance) {
        public Context {
            if (!Double.isFinite(health) || health < 0.0D || !Double.isFinite(fallDistance) || fallDistance < 0.0D) {
                throw new IllegalArgumentException("auto-leave context is invalid");
            }
        }
    }

    private final BooleanSetting lowHealth;
    private final NumberSetting healthThreshold;
    private final BooleanSetting falling;
    private final NumberSetting fallThreshold;

    public AutoLeave() {
        super("auto_leave", "AutoLeave", "Disconnects from multiplayer when configured local danger thresholds are reached.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        lowHealth = addSetting(new BooleanSetting("low_health", "Low health",
                "Disconnect when observed health, including absorption, reaches the threshold.", true));
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Observed health at or below which to leave multiplayer.", 6.0D, 1.0D, 20.0D));
        falling = addSetting(new BooleanSetting("falling", "Dangerous fall",
                "Disconnect when observed fall distance reaches the threshold.", true));
        fallThreshold = addSetting(new NumberSetting("fall_threshold", "Fall threshold",
                "Observed fall distance at or above which to leave multiplayer.", 16.0D, 3.0D, 128.0D));
    }

    /** Returns the first configured local danger that requests a disconnect. */
    public Optional<Danger> danger(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("auto-leave context must not be null");
        }
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (lowHealth.value() && context.health() <= healthThreshold.value()) {
            return Optional.of(Danger.LOW_HEALTH);
        }
        if (falling.value() && context.fallDistance() >= fallThreshold.value()) {
            return Optional.of(Danger.FALL_DISTANCE);
        }
        return Optional.empty();
    }
}
