package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Supplies a bounded local stepping height to the normal entity collision path. */
public final class Step extends Module {
    private final NumberSetting stepHeight;

    public Step() {
        super("step", "Step", "Raises local normal collision step height without packet bypasses.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        stepHeight = addSetting(new NumberSetting("step_height", "Step height",
                "Maximum local collision step height in blocks.", 1.0D, 0.6D, 3.0D));
    }

    public float stepHeight(float vanillaHeight) {
        if (!Float.isFinite(vanillaHeight) || vanillaHeight < 0.0F) {
            throw new IllegalArgumentException("vanillaHeight must be finite and non-negative");
        }
        return isEnabled() ? (float) Math.max(vanillaHeight, stepHeight.value()) : vanillaHeight;
    }
}
