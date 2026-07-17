package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Keeps an actively used shield in its normal local first-person held pose. */
public final class NoShieldOverlay extends Module {
    public NoShieldOverlay() {
        super("no_shield_overlay", "NoShieldOverlay", "Hides the raised local first-person shield overlay.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }

    /** True only for a currently used vanilla shield while this local visual toggle is enabled. */
    public boolean hidesRaisedShield(boolean usingItem, boolean usingShield) {
        return isEnabled() && usingItem && usingShield;
    }
}
