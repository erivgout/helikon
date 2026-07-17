package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow bridge from Minecraft's verified DeltaTracker timer calculation to the configured safe rate. */
public final class TimerModuleAccess {
    private static volatile Timer module;

    private TimerModuleAccess() {
    }

    public static void install(Timer timer) {
        module = Objects.requireNonNull(timer, "timer");
    }

    public static float multiplier() {
        Timer current = module;
        return current == null ? 1.0F : current.multiplier();
    }
}
