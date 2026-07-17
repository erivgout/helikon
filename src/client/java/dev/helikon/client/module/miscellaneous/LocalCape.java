package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.ColorSetting;

/** Configures a procedural cape rendered for the local player only. */
public final class LocalCape extends Module {
    private final ColorSetting primaryColor;
    private final ColorSetting accentColor;

    public LocalCape() {
        super("local_cape", "Local Cape", "Renders a procedural cape for only your local player view.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        primaryColor = addSetting(new ColorSetting("primary_color", "Primary color",
                "ARGB cape color; alpha is ignored by Minecraft's solid cape layer.", 0xFF5E35B1));
        accentColor = addSetting(new ColorSetting("accent_color", "Accent color",
                "ARGB cape emblem color; alpha is ignored by Minecraft's solid cape layer.", 0xFFD1C4E9));
    }

    public int primaryColor() {
        return primaryColor.value();
    }

    public int accentColor() {
        return accentColor.value();
    }
}
