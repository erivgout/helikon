package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow static bridge used only by the verified keyboard-input adapter. */
public final class AntiAfkAccess {
    private static volatile AntiAfk antiAfk;

    private AntiAfkAccess() {
    }

    public static void install(AntiAfk module) {
        antiAfk = Objects.requireNonNull(module, "module");
    }

    public static AntiAfk.Action tick(AntiAfk.Context context) {
        AntiAfk module = antiAfk;
        return module == null ? AntiAfk.Action.NONE : module.tick(context);
    }
}
