package dev.helikon.client.module.render;

import java.util.Objects;

/** Narrow bridge from the verified camera mixin to the Minecraft-free module setting. */
public final class CameraModuleAccess {
    private static volatile CameraDistance cameraDistance;

    private CameraModuleAccess() {
    }

    public static void install(CameraDistance module) {
        cameraDistance = Objects.requireNonNull(module, "module");
    }

    public static float desiredDistance(float vanillaDistance) {
        CameraDistance module = cameraDistance;
        return module == null || !module.isEnabled() ? vanillaDistance : module.distance();
    }
}
