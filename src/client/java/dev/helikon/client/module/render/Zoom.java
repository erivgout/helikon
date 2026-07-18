package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/** Reversible option-based zoom that retains vanilla mouse aiming. */
public final class Zoom extends Module {
    private final IntegerSetting fieldOfView;

    public Zoom() {
        super("zoom", "Zoom", "Temporarily narrows the vanilla field of view while enabled.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        fieldOfView = addSetting(new IntegerSetting("field_of_view", "Field of view",
                "Zoomed vanilla FOV value.", 25, 10, 70));
    }

    public int fieldOfView() {
        return fieldOfView.value();
    }

    /** Applies the configured render FOV only while this module is enabled. */
    public float applyTo(float vanillaFov) {
        if (!Float.isFinite(vanillaFov) || vanillaFov <= 0.0F) {
            return vanillaFov;
        }
        return isEnabled() ? fieldOfView() : vanillaFov;
    }
}
