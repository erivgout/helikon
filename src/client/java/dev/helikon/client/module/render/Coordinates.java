package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Configures a live local-position readout without retaining or transmitting location data. */
public final class Coordinates extends Module {
    private final BooleanSetting decimals;
    private final BooleanSetting dimension;

    public Coordinates() {
        super("coordinates", "Coordinates", "Shows the local player's current coordinates in the HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        decimals = addSetting(new BooleanSetting("decimals", "Decimals",
                "Show one decimal place instead of block coordinates.", false));
        dimension = addSetting(new BooleanSetting("dimension", "Dimension",
                "Show the current dimension below the coordinates.", true));
    }

    public boolean decimals() {
        return decimals.value();
    }

    public boolean showDimension() {
        return dimension.value();
    }
}
