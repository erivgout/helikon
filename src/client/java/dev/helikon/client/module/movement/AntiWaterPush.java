package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Suppresses local velocity added by ordinary water currents. */
public final class AntiWaterPush extends Module {
    public AntiWaterPush() {
        super("anti_water_push", "AntiWaterPush", "Prevents local water currents from adding movement.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
    }

    /** Returns whether the local water-current velocity contribution should be omitted. */
    public boolean blocksWaterCurrent() {
        return isEnabled();
    }
}
