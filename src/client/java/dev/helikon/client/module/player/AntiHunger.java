package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/** Reduces ordinary exhaustion by stopping sprint below a configured food level. */
public final class AntiHunger extends Module {
    private final IntegerSetting foodThreshold;

    public AntiHunger() {
        super("anti_hunger", "AntiHunger",
                "Stops local sprinting below a configured food level to reduce ordinary exhaustion.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        foodThreshold = addSetting(new IntegerSetting("food_threshold", "Food threshold",
                "Stop sprinting when food is at or below this value.", 18, 1, 20));
    }

    public boolean shouldStopSprinting(int foodLevel, boolean sprinting) {
        return isEnabled() && sprinting && foodLevel <= foodThreshold.value();
    }
}
