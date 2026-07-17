package dev.helikon.client.module.render;

import net.minecraft.client.Minecraft;

/** Owns and restores the vanilla FOV option only while Zoom is active. */
public final class MinecraftZoomAccess {
    private Integer savedFov;

    public void tick(Zoom module) {
        Minecraft client = Minecraft.getInstance();
        if (module.isEnabled()) {
            if (savedFov == null) {
                savedFov = client.options.fov().get();
            }
            client.options.fov().set(module.fieldOfView());
        } else {
            reset();
        }
    }

    public void reset() {
        if (savedFov != null) {
            Minecraft.getInstance().options.fov().set(savedFov);
            savedFov = null;
        }
    }
}
