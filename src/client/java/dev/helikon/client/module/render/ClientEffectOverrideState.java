package dev.helikon.client.module.render;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tracks one locally injected client effect by object identity. A matching
 * server effect is never assumed to be Helikon's unless it is the exact
 * instance that this state requested from the platform adapter.
 */
public final class ClientEffectOverrideState<T> {
    private boolean active;
    private T original;
    private T override;

    /**
     * Returns the exact effect instance that should be installed. If a server
     * update replaced Helikon's instance, that current effect becomes the
     * state to restore when the local override is removed.
     */
    public T apply(T current, Supplier<? extends T> overrideFactory) {
        Objects.requireNonNull(overrideFactory, "overrideFactory");
        if (!active || current != override) {
            original = current;
            override = Objects.requireNonNull(overrideFactory.get(), "overrideFactory result");
            active = true;
        }
        return override;
    }

    /**
     * Clears ownership and reports whether the supplied current value is still
     * Helikon's exact override, plus the state that should then be restored.
     */
    public Restoration<T> restore(T current) {
        boolean removeOverride = active && current == override;
        T restore = removeOverride ? original : null;
        clear();
        return new Restoration<>(removeOverride, restore);
    }

    private void clear() {
        active = false;
        original = null;
        override = null;
    }

    public record Restoration<T>(boolean removeOverride, T original) {
    }
}
