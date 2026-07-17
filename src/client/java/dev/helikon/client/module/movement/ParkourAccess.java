package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow static bridge used only by the verified keyboard-input adapter. */
public final class ParkourAccess {
    private static volatile AutoParkour autoParkour;

    private ParkourAccess() {
    }

    public static void install(AutoParkour module) {
        autoParkour = Objects.requireNonNull(module, "module");
    }

    public static boolean shouldJump(ParkourContext context) {
        AutoParkour module = autoParkour;
        return module != null && module.shouldJump(context);
    }
}
