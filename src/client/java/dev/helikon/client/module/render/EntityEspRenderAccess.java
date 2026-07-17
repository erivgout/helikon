package dev.helikon.client.module.render;

import dev.helikon.client.render.EntityEspNativeOutlineTargets;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe bridge from EntityESP's local target scan to narrowly scoped render mixins. */
public final class EntityEspRenderAccess {
    private static final AtomicReference<EntityEspNativeOutlineTargets> TARGETS =
            new AtomicReference<>(EntityEspNativeOutlineTargets.empty());

    private EntityEspRenderAccess() {
    }

    public static void install(EntityEspNativeOutlineTargets targets) {
        TARGETS.set(Objects.requireNonNull(targets, "targets"));
    }

    public static void clear() {
        TARGETS.set(EntityEspNativeOutlineTargets.empty());
    }

    public static boolean shouldAppearGlowing(int entityId) {
        return TARGETS.get().contains(entityId);
    }

    public static OptionalInt shaderColorFor(int entityId) {
        return TARGETS.get().shaderColorFor(entityId);
    }

    static EntityEspNativeOutlineTargets targets() {
        return TARGETS.get();
    }
}
