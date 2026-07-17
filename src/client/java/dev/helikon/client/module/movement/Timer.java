package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Supplies a bounded local client-timer multiplier; multiplayer remains server-authoritative. */
public final class Timer extends Module {
    private final NumberSetting tickMultiplier;

    public Timer() {
        super("timer", "Timer", "Applies a bounded local client tick multiplier with a multiplayer warning.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        tickMultiplier = addSetting(new NumberSetting("tick_multiplier", "Tick multiplier",
                "Local client rate multiplier; servers remain authoritative.", 1.0D, 0.50D, 1.25D));
    }

    public float multiplier() {
        return isEnabled() ? tickMultiplier.value().floatValue() : 1.0F;
    }
}
