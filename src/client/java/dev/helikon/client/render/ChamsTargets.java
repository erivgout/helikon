package dev.helikon.client.render;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable, Minecraft-free snapshot of entities selected for the Chams
 * occlusion-visible outline pass, each mapped to its opaque render color.
 */
public record ChamsTargets(Map<Integer, Integer> colors) {
    public ChamsTargets {
        colors = Map.copyOf(Objects.requireNonNull(colors, "colors"));
    }

    public static ChamsTargets empty() {
        return new ChamsTargets(Map.of());
    }

    public boolean contains(int entityId) {
        return colors.containsKey(entityId);
    }

    public OptionalInt colorFor(int entityId) {
        Integer color = colors.get(entityId);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }
}
