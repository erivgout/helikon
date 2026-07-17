package dev.helikon.client.event;

import java.util.Objects;

/** Minecraft-free local-player facts sampled once at a client-tick boundary. */
public record PlayerStateSnapshot(
        boolean alive,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int selectedSlot,
        long inventoryFingerprint
) {
    public PlayerStateSnapshot {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Player position and rotation values must be finite");
        }
        if (selectedSlot < 0) {
            throw new IllegalArgumentException("selectedSlot must not be negative");
        }
    }
}
