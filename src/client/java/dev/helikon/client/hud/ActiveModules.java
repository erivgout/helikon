package dev.helikon.client.hud;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/** Pure selection rules for the Active Modules HUD element. */
public final class ActiveModules {
    private ActiveModules() {
    }

    /**
     * Returns enabled module display names in registry order. Sorting and
     * animation options belong to the later ActiveModules module settings.
     */
    public static List<String> enabledNames(ModuleRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return registry.all().stream()
                .filter(Module::isEnabled)
                .map(Module::name)
                .toList();
    }
}
