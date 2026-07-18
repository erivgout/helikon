package dev.helikon.client.module.combat;

import java.util.Objects;

/** Narrow bridge from the verified player-range mixin to the Minecraft-free Reach policy. */
public final class ReachAccess {
    private static volatile Reach reach;

    private ReachAccess() {
    }

    public static void install(Reach module) {
        reach = Objects.requireNonNull(module, "module");
    }

    public static double blockInteractionRange(double vanillaRange) {
        Reach module = reach;
        return module == null ? vanillaRange : module.blockInteractionRange(vanillaRange);
    }
}
