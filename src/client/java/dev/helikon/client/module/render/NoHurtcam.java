package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Removes the local camera rotation calculated from the active hurt animation. */
public final class NoHurtcam extends Module {
    public NoHurtcam() {
        super("no_hurtcam", "NoHurtcam", "Hides local camera shake caused by taking damage.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }

    /** True only while the renderer should suppress the normal hurt-camera transform. */
    public boolean hidesHurtCamera() {
        return isEnabled();
    }
}
