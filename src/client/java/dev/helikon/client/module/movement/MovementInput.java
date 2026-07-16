package dev.helikon.client.module.movement;

/**
 * Minecraft-free snapshot of the player movement keys after vanilla has
 * polled them for a client tick.
 */
public record MovementInput(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean shift,
        boolean sprint
) {
    public boolean isMoving() {
        return forward || backward || left || right;
    }

    /** Reproduces Minecraft 26.2 KeyboardInput's normalized side/forward vector. */
    public MovementVector movementVector() {
        float forwardImpulse = impulse(forward, backward);
        float sideImpulse = impulse(left, right);
        float lengthSquared = sideImpulse * sideImpulse + forwardImpulse * forwardImpulse;
        if (lengthSquared <= 0.0F) {
            return new MovementVector(0.0F, 0.0F);
        }
        float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
        return new MovementVector(sideImpulse * inverseLength, forwardImpulse * inverseLength);
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    /** Normalized local side (x) and forward (y) movement impulse. */
    public record MovementVector(float x, float y) {
    }
}
