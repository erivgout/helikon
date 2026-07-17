package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Keeps the detached third-person camera at its requested local distance. */
public final class CameraNoClip extends Module {
    public CameraNoClip() {
        super("camera_no_clip", "CameraNoClip", "Prevents local third-person camera collision pull-in.",
                ModuleCategory.RENDER, false, Keybind.unbound());
    }

    /** True when the camera renderer should use its requested detached-camera distance without collision clipping. */
    public boolean usesUnclippedCameraDistance() {
        return isEnabled();
    }
}
