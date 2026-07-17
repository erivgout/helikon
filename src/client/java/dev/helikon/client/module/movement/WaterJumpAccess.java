package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow bridge from verified client-side water-edge facts to WaterJump's pure policy. */
public final class WaterJumpAccess {
    private static volatile WaterJump module;

    private WaterJumpAccess() {
    }

    public static void install(WaterJump waterJump) {
        module = Objects.requireNonNull(waterJump, "waterJump");
    }

    public static boolean shouldJump(WaterJumpContext context) {
        WaterJump current = module;
        return current != null && current.shouldJump(context);
    }
}
