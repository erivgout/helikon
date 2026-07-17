package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow bridge used only by the verified local-player water-current mixin. */
public final class AntiWaterPushAccess {
    private static volatile AntiWaterPush module;

    private AntiWaterPushAccess() {
    }

    public static void install(AntiWaterPush antiWaterPush) {
        module = Objects.requireNonNull(antiWaterPush, "antiWaterPush");
    }

    public static boolean blocksWaterCurrent() {
        AntiWaterPush current = module;
        return current != null && current.blocksWaterCurrent();
    }
}
