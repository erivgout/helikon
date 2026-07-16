package dev.helikon.client.module.movement;

/** Minecraft-free facts used to decide whether AutoSprint may request sprinting. */
public record SprintContext(
        boolean forward,
        boolean moving,
        int foodLevel,
        boolean horizontalCollision,
        boolean currentlySprinting
) {
    public SprintContext {
        if (foodLevel < 0 || foodLevel > 20) {
            throw new IllegalArgumentException("foodLevel must be between 0 and 20");
        }
    }
}
