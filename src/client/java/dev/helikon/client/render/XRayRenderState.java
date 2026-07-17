package dev.helikon.client.render;

import java.util.Objects;
import java.util.Set;

/** Immutable Minecraft-free snapshot read by local chunk-compilation mixins. */
public record XRayRenderState(boolean active, Set<String> targetBlocks, float opacity) {
    public XRayRenderState {
        targetBlocks = Set.copyOf(Objects.requireNonNull(targetBlocks, "targetBlocks"));
        if (active && targetBlocks.isEmpty()) {
            throw new IllegalArgumentException("active XRay requires at least one target block");
        }
        if (!Float.isFinite(opacity) || opacity < 0.1F || opacity > 1.0F) {
            throw new IllegalArgumentException("opacity must be between 0.1 and 1.0");
        }
    }

    public static XRayRenderState disabled() {
        return new XRayRenderState(false, Set.of(), 1.0F);
    }

    /** Non-target blocks become absent only while an active local chunk is compiled. */
    public boolean hides(String blockId) {
        return active && !targetBlocks.contains(blockId);
    }
}
