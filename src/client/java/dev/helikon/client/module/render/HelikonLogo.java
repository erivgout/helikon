package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.ColorSetting;

/** Original Helikon-branded HUD logo control. */
public final class HelikonLogo extends Module {
    private final ColorSetting color;

    public HelikonLogo() {
        super("helikon_logo", "HelikonLogo", "Shows an original Helikon wordmark in the HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        color = addSetting(new ColorSetting("color", "Color", "ARGB logo color.", 0xFF7DE7FF));
    }

    public int color() {
        return color.value();
    }
}
