package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Suppresses only the local death-protection item activation overlay. */
public final class AntiTotemAnimation extends Module {
    public AntiTotemAnimation() {
        super("anti_totem_animation", "AntiTotemAnimation",
                "Hides the local death-protection item activation overlay.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }

    /** Returns whether the supplied local item activation should be suppressed. */
    public boolean shouldSuppressItemActivation(boolean hasDeathProtection) {
        return isEnabled() && hasDeathProtection;
    }
}
