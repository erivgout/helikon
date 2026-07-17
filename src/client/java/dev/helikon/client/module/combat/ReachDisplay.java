package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Enables a local readout of distance measured at Helikon's own ordinary attack requests. */
public final class ReachDisplay extends Module {
    public ReachDisplay() {
        super("reach_display", "ReachDisplay", "Shows measured local attack distance without claiming modified reach.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
    }
}
