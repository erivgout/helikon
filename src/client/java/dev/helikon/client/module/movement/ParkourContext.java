package dev.helikon.client.module.movement;

/** Minecraft-free local facts used to decide whether one ordinary jump input is safe. */
public record ParkourContext(
        boolean screenOpen,
        boolean onGround,
        boolean movingForward,
        double horizontalSpeed,
        boolean ledgeAhead,
        boolean lavaAhead,
        int dropBlocks,
        boolean landingSupportsPlayer
) {
    public ParkourContext {
        if (!Double.isFinite(horizontalSpeed) || horizontalSpeed < 0.0D) {
            throw new IllegalArgumentException("horizontalSpeed must be finite and non-negative");
        }
        if (dropBlocks < 0) {
            throw new IllegalArgumentException("dropBlocks must be non-negative");
        }
    }
}
