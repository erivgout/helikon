package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Displays only the local player's observed Minecraft hunger saturation. */
public final class SaturationDisplay extends Module {
    public SaturationDisplay() {
        super("saturation_display", "Saturation Display", "Shows the local hunger saturation value in the HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }
}
