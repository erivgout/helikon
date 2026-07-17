package dev.helikon.client.hud;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Pure selection rules for the Active Modules HUD element. */
public final class ActiveModules {
    private ActiveModules() {
    }

    /**
     * Returns enabled module display names using the supplied presentation sort.
     */
    public static List<String> enabledNames(ModuleRegistry registry) {
        return enabledNames(registry, ActiveModulesLayout.Sort.REGISTRY, ignored -> 0);
    }

    public static List<String> enabledNames(
            ModuleRegistry registry,
            ActiveModulesLayout.Sort sort,
            ToIntFunction<String> width
    ) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(width, "width");
        List<String> names = registry.all().stream()
                .filter(Module::isEnabled)
                .map(Module::name)
                .toList();
        return switch (sort) {
            case REGISTRY -> names;
            case ALPHABETICAL -> names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            case WIDTH -> names.stream().sorted((left, right) -> {
                int comparison = Integer.compare(width.applyAsInt(right), width.applyAsInt(left));
                return comparison != 0 ? comparison : String.CASE_INSENSITIVE_ORDER.compare(left, right);
            }).toList();
        };
    }

    /** Produces a stable opaque rainbow hue from a caller-provided local tick count. */
    public static int rainbowColor(long tick) {
        return rainbowColor(tick, 0);
    }

    /** Produces a stable opaque rainbow hue for one line in the active-module list. */
    public static int rainbowColor(long tick, int lineIndex) {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("lineIndex must be non-negative");
        }
        float hue = Math.floorMod(tick + lineIndex * 28L, 360L) / 360.0F;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.78F, 1.0F);
    }
}
