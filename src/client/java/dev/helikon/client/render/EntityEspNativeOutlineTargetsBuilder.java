package dev.helikon.client.render;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded, Minecraft-free accumulator for one native entity-outline snapshot.
 * Shader colors are forced opaque because the vanilla outline pass treats a
 * zero outline color as "no outline".
 */
public final class EntityEspNativeOutlineTargetsBuilder {
    private static final int OPAQUE_MASK = 0xFF000000;

    private final int maximumEntities;
    private final boolean shaderColors;
    private final Set<Integer> entityIds = new LinkedHashSet<>();
    private final Map<Integer, Integer> colors = new LinkedHashMap<>();

    public EntityEspNativeOutlineTargetsBuilder(int maximumEntities, boolean shaderColors) {
        if (maximumEntities < 1) {
            throw new IllegalArgumentException("maximumEntities must be at least 1");
        }
        this.maximumEntities = maximumEntities;
        this.shaderColors = shaderColors;
    }

    /** Adds one target, returning false without mutation once the cap is reached. */
    public boolean offer(int entityId, int shaderColor) {
        if (entityIds.size() >= maximumEntities && !entityIds.contains(entityId)) {
            return false;
        }
        entityIds.add(entityId);
        if (shaderColors) {
            colors.put(entityId, OPAQUE_MASK | shaderColor);
        }
        return true;
    }

    public EntityEspNativeOutlineTargets build() {
        return new EntityEspNativeOutlineTargets(entityIds, colors);
    }
}
