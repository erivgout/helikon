package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Enables the local TargetHUD readout; target facts remain session-only. */
public final class TargetHud extends Module {
    public TargetHud() {
        super("target_hud", "TargetHUD", "Shows locally observed name, health, armor, distance, item, and effects.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
    }
}
