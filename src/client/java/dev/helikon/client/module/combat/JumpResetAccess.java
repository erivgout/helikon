package dev.helikon.client.module.combat;

import java.util.Objects;

/**
 * Narrow bridge from verified client-side hit facts to JumpReset's pure policy.
 * It tracks the previous local hurt-time to derive a fresh-hit rising edge and a
 * per-call tick count, all on the client thread; it holds no Minecraft types.
 */
public final class JumpResetAccess {
    private static volatile JumpReset module;
    private static int previousHurtTime;
    private static long tick;

    private JumpResetAccess() {
    }

    public static void install(JumpReset jumpReset) {
        module = Objects.requireNonNull(jumpReset, "jumpReset");
    }

    /** Clears the tracked hurt edge when the local player is unavailable so a rejoin starts fresh. */
    public static void reset() {
        previousHurtTime = 0;
    }

    /**
     * Advances the hurt-edge tracker once per local input tick and returns whether a jump is requested.
     * {@code hurtTime} is Minecraft's local hurt-animation countdown, which rises on a fresh hit.
     */
    public static boolean shouldJump(boolean screenOpen, boolean onGround, boolean movingHorizontally, int hurtTime) {
        boolean freshHit = hurtTime > previousHurtTime;
        previousHurtTime = hurtTime;
        long now = tick++;
        JumpReset current = module;
        return current != null
                && current.shouldJumpReset(now, new JumpResetContext(screenOpen, onGround, freshHit, movingHorizontally));
    }
}
