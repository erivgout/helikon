package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Selectively suppresses only the configured client-side impairment visuals. */
public final class AntiBlind extends Module {
    private final BooleanSetting blindness;
    private final BooleanSetting darkness;
    private final BooleanSetting nausea;
    private final BooleanSetting pumpkinOverlay;
    private final BooleanSetting powderSnowOverlay;

    public AntiBlind() {
        super("anti_blind", "AntiBlind", "Hides selected local impairment and overlay visuals.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        blindness = addSetting(new BooleanSetting("blindness", "Blindness", "Hide local Blindness fog.", true));
        darkness = addSetting(new BooleanSetting("darkness", "Darkness", "Hide local Darkness fog and lightmap dimming.", true));
        nausea = addSetting(new BooleanSetting("nausea", "Nausea", "Hide the local Nausea distortion overlay.", true));
        pumpkinOverlay = addSetting(new BooleanSetting("pumpkin_overlay", "Pumpkin overlay", "Hide the carved-pumpkin camera overlay.", true));
        powderSnowOverlay = addSetting(new BooleanSetting("powder_snow_overlay", "Powder snow overlay", "Hide the powder-snow camera overlay.", true));
    }

    public boolean hidesBlindness() { return isEnabled() && blindness.value(); }

    public boolean hidesDarkness() { return isEnabled() && darkness.value(); }

    public boolean hidesNausea() { return isEnabled() && nausea.value(); }

    public boolean hidesPumpkinOverlay() { return isEnabled() && pumpkinOverlay.value(); }

    public boolean hidesPowderSnowOverlay() { return isEnabled() && powderSnowOverlay.value(); }
}
