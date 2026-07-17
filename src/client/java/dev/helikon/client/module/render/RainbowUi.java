package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.awt.Color;

/** Animated, local ClickGUI accent colors. */
public final class RainbowUi extends Module {
    private final NumberSetting speed;
    private final NumberSetting saturation;

    public RainbowUi() {
        super("rainbow_ui", "RainbowUI", "Animates the ClickGUI accent through the color spectrum.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        speed = addSetting(new NumberSetting("speed", "Speed", "Color cycles per minute.",
                3.0, 0.2, 20.0));
        saturation = addSetting(new NumberSetting("saturation", "Saturation", "Rainbow color saturation.",
                0.85, 0.1, 1.0));
    }

    public int accent(long nowMillis) {
        if (!isEnabled()) {
            return 0;
        }
        double cycles = nowMillis / 60_000.0 * speed.value();
        int rgb = Color.HSBtoRGB((float) (cycles - Math.floor(cycles)), saturation.value().floatValue(), 1.0F);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
