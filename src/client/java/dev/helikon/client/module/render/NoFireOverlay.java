package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Suppresses only the local first-person fire screen effect. */
public final class NoFireOverlay extends Module {
    public NoFireOverlay() {
        super("no_fire_overlay", "NoFireOverlay", "Hides the local first-person fire overlay.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }

    /** Returns whether the fire screen-effect branch should be omitted locally. */
    public boolean hidesFireOverlay() {
        return isEnabled();
    }
}
