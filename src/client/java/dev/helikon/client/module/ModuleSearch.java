package dev.helikon.client.module;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Shared module search rules used by the ClickGUI and the {@code .search}
 * command: case-insensitive matching against module name, ID, and description.
 */
public final class ModuleSearch {
    private ModuleSearch() {
    }

    /** Modules matching the query, keeping the iteration order of the input. */
    public static List<Module> filter(Collection<Module> modules, String query) {
        String needle = normalize(query);
        return modules.stream()
                .filter(module -> matchesNormalized(module, needle))
                .toList();
    }

    /** Whether the module matches the query by name, ID, or description. */
    public static boolean matches(Module module, String query) {
        return matchesNormalized(module, normalize(query));
    }

    private static boolean matchesNormalized(Module module, String needle) {
        return module.name().toLowerCase(Locale.ROOT).contains(needle)
                || module.id().contains(needle)
                || module.description().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
