package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

/** Registry-only adapter that applies the pure conflict result. */
public final class MinecraftTooManyHaxAccess {
    private MinecraftTooManyHaxAccess() {
    }

    public static void tick(TooManyHax module, ModuleRegistry registry) {
        var enabledIds = registry.all().stream().filter(Module::isEnabled).map(Module::id).toList();
        for (String id : module.conflicts(enabledIds)) {
            registry.find(id).ifPresent(conflict -> registry.setEnabled(conflict, false));
        }
    }
}
