package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.GlintColor;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Tints the locally rendered enchantment glint on item stacks. */
public final class RainbowEnchant extends Module {
    private final ColorSetting color;
    private final BooleanSetting rainbow;
    private final NumberSetting rainbowSpeed;

    public RainbowEnchant() {
        super("rainbow_enchant", "RainbowEnchant", "Customizes local item enchantment glint color.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        color = addSetting(new ColorSetting("color", "Color", "ARGB tint for local item enchantment glint.",
                0xFF8A5CFF));
        rainbow = addSetting(new BooleanSetting("rainbow", "Rainbow", "Cycle the local glint tint through hues.", false));
        rainbowSpeed = addSetting(new NumberSetting("rainbow_speed", "Rainbow speed",
                "Completed rainbow cycles per second.", 0.25D, 0.05D, 4.0D));
    }

    /** Returns the configured local glint tint for a supplied millisecond timestamp. */
    public int glintColor(long nowMillis) {
        return rainbow.value() ? GlintColor.rainbow(nowMillis, rainbowSpeed.value()) : color.value();
    }
}
