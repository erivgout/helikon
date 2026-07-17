package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures one bounded, purely local aura around the local player. */
public final class LocalCosmetics extends Module {
    private final ColorSetting color;
    private final NumberSetting radius;
    private final NumberSetting segments;

    public LocalCosmetics() {
        super("local_cosmetics", "Local Cosmetics", "Draws a local aura around your player without remote assets.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        color = addSetting(new ColorSetting("color", "Aura color", "ARGB local aura line color.", 0xFF4DD0E1));
        radius = addSetting(new NumberSetting("radius", "Aura radius", "Local aura radius in blocks.",
                0.85D, 0.4D, 3.0D));
        segments = addSetting(new NumberSetting("segments", "Aura segments", "Bounded local aura line segments.",
                24.0D, 12.0D, 48.0D));
    }

    public int color() {
        return color.value();
    }

    public double radius() {
        return radius.value();
    }

    public int segments() {
        return (int) Math.round(segments.value());
    }
}
