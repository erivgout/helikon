package dev.helikon.client.module.render;

import dev.helikon.client.render.ChamsTargets;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe bridge from Chams's local target scan to its narrowly scoped render mixins. */
public final class ChamsRenderAccess {
    private static final AtomicReference<ChamsTargets> TARGETS = new AtomicReference<>(ChamsTargets.empty());

    private ChamsRenderAccess() {
    }

    public static void install(ChamsTargets targets) {
        TARGETS.set(Objects.requireNonNull(targets, "targets"));
    }

    public static void clear() {
        TARGETS.set(ChamsTargets.empty());
    }

    public static boolean shouldRender(int entityId) {
        return TARGETS.get().contains(entityId);
    }

    public static OptionalInt colorFor(int entityId) {
        return TARGETS.get().colorFor(entityId);
    }
}
