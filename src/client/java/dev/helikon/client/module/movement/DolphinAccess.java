package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow bridge from the verified keyboard-input adapter to Dolphin's pure policy. */
public final class DolphinAccess {
    private static volatile Dolphin module;

    private DolphinAccess() {
    }

    public static void install(Dolphin dolphin) {
        module = Objects.requireNonNull(dolphin, "dolphin");
    }

    public static boolean shouldJump(DolphinContext context) {
        Dolphin dolphin = module;
        return dolphin != null && dolphin.shouldJump(context);
    }
}
