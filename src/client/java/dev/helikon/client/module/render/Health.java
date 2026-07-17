package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Displays the local player's observed health as a compact readout near the crosshair. */
public final class Health extends Module {
    private final BooleanSetting showMax;
    private final BooleanSetting showAbsorption;
    private final BooleanSetting showDecimals;
    private final BooleanSetting colorByHealth;

    public Health() {
        super("health", "Health", "Shows the local player's health near the crosshair.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        showMax = addSetting(new BooleanSetting("show_max", "Show maximum",
                "Append the maximum health, for example 18/20.", true));
        showAbsorption = addSetting(new BooleanSetting("show_absorption", "Show absorption",
                "Append current absorption (yellow hearts) as +N when present.", true));
        showDecimals = addSetting(new BooleanSetting("show_decimals", "Show decimals",
                "Show one decimal place instead of rounded whole numbers.", false));
        colorByHealth = addSetting(new BooleanSetting("color_by_health", "Color by health",
                "Tint the readout from red to green by remaining health, overriding the HUD color.", true));
    }

    public boolean showMax() {
        return showMax.value();
    }

    public boolean showAbsorption() {
        return showAbsorption.value();
    }

    public boolean showDecimals() {
        return showDecimals.value();
    }

    public boolean colorByHealth() {
        return colorByHealth.value();
    }
}
