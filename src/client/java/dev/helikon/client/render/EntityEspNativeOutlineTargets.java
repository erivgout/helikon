package dev.helikon.client.render;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Immutable, Minecraft-free snapshot of entities selected for vanilla's
 * local entity-outline pass. Shader colors are intentionally optional: the
 * Glow mode retains Minecraft's normal team-derived color.
 */
public record EntityEspNativeOutlineTargets(Set<Integer> entityIds, Map<Integer, Integer> shaderColors) {
    public EntityEspNativeOutlineTargets {
        entityIds = Set.copyOf(Objects.requireNonNull(entityIds, "entityIds"));
        shaderColors = Map.copyOf(Objects.requireNonNull(shaderColors, "shaderColors"));
        if (!entityIds.containsAll(shaderColors.keySet())) {
            throw new IllegalArgumentException("Shader colors must belong to selected entity IDs");
        }
    }

    public static EntityEspNativeOutlineTargets empty() {
        return new EntityEspNativeOutlineTargets(Set.of(), Map.of());
    }

    public boolean contains(int entityId) {
        return entityIds.contains(entityId);
    }

    public OptionalInt shaderColorFor(int entityId) {
        Integer color = shaderColors.get(entityId);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }
}
