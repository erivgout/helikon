package dev.helikon.client.module.movement;

import java.util.Objects;

/** Narrow bridge from the entity collision hook to Step's Minecraft-free height policy. */
public final class StepAccess {
    private static volatile Step module;

    private StepAccess() {
    }

    public static void install(Step step) {
        module = Objects.requireNonNull(step, "step");
    }

    public static float height(float vanillaHeight) {
        Step current = module;
        return current == null ? vanillaHeight : current.stepHeight(vanillaHeight);
    }
}
