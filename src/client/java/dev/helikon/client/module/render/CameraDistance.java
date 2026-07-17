package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Configures only this client's desired third-person camera distance. */
public final class CameraDistance extends Module {
    private final NumberSetting distance;

    public CameraDistance() {
        super("camera_distance", "CameraDistance", "Sets the local desired third-person camera distance.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        distance = addSetting(new NumberSetting("distance", "Distance",
                "Desired third-person camera distance before normal local collision clipping.", 4.0D, 1.0D, 32.0D));
    }

    public float distance() {
        return isEnabled() ? distance.value().floatValue() : 4.0F;
    }
}
