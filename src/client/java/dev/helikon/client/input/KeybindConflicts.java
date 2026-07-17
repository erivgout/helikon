package dev.helikon.client.input;

import dev.helikon.client.module.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Finds exact module-bind collisions without deciding whether either module may run. */
public final class KeybindConflicts {
    private KeybindConflicts() {
    }

    /**
     * Returns other modules that have the exact same bound input, including
     * activation-independent collisions. An unbound module never conflicts.
     */
    public static List<Module> find(Module target, Collection<Module> modules) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modules, "modules");
        Keybind keybind = target.keybind();
        if (!keybind.isBound()) {
            return List.of();
        }
        List<Module> conflicts = new ArrayList<>();
        for (Module candidate : modules) {
            if (candidate != target && keybind.inputType() == candidate.keybind().inputType()
                    && keybind.keyCode() == candidate.keybind().keyCode()
                    && keybind.modifiers().equals(candidate.keybind().modifiers())) {
                conflicts.add(candidate);
            }
        }
        return List.copyOf(conflicts);
    }
}
