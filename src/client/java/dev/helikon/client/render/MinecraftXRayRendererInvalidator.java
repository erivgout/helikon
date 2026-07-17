package dev.helikon.client.render;

import dev.helikon.client.module.render.XRay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;

/** Marks the local 26.2 render-section range dirty after XRay changes. */
public final class MinecraftXRayRendererInvalidator implements XRay.RendererInvalidator {
    @Override
    public void invalidateGeometry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        SectionPos cameraSection = SectionPos.of(client.gameRenderer.mainCamera().position());
        int viewDistance = client.options.getEffectiveRenderDistance();
        client.level.setSectionRangeDirty(cameraSection.x() - viewDistance, client.level.getMinSectionY(),
                cameraSection.z() - viewDistance, cameraSection.x() + viewDistance,
                client.level.getMaxSectionY() - 1, cameraSection.z() + viewDistance);
    }
}
