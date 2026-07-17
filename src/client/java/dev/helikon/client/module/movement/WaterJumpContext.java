package dev.helikon.client.module.movement;

/** Minecraft-free local facts for one ordinary jump request at a water exit. */
public record WaterJumpContext(
        boolean screenOpen,
        boolean inWater,
        boolean movingForward,
        boolean solidStepAhead,
        boolean headroomClear
) {
}
