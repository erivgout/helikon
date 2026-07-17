package dev.helikon.client.render;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded, Minecraft-free accumulator for one Chams outline snapshot. Colors are
 * forced opaque because the vanilla outline pass treats a zero color as
 * "no outline".
 */
public final class ChamsTargetsBuilder {
    private static final int OPAQUE_MASK = 0xFF000000;

    private final int maximumEntities;
    private final Map<Integer, Integer> colors = new LinkedHashMap<>();

    public ChamsTargetsBuilder(int maximumEntities) {
        if (maximumEntities < 1) {
            throw new IllegalArgumentException("maximumEntities must be at least 1");
        }
        this.maximumEntities = maximumEntities;
    }

    /** Adds one target, returning false without mutation once the cap is reached. */
    public boolean offer(int entityId, int color) {
        if (colors.size() >= maximumEntities && !colors.containsKey(entityId)) {
            return false;
        }
        colors.put(entityId, OPAQUE_MASK | color);
        return true;
    }

    public ChamsTargets build() {
        return new ChamsTargets(colors);
    }
}
