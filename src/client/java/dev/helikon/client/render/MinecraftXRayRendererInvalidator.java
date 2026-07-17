package dev.helikon.client.render;

import dev.helikon.client.module.render.XRay;
import net.minecraft.client.Minecraft;

/** Verified 26.2 bridge that rebuilds local compiled chunk geometry after XRay changes. */
public final class MinecraftXRayRendererInvalidator implements XRay.RendererInvalidator {
    @Override
    public void invalidateGeometry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        client.levelRenderer.invalidateCompiledGeometry(client.level, client.options,
                client.gameRenderer.mainCamera(), client.getBlockColors());
    }
}
