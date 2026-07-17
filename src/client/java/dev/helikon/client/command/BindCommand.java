package dev.helikon.client.command;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindConflicts;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/** Assigns a module keybind, optionally with an activation mode. */
public final class BindCommand implements HelikonCommand {
    private final ModuleRegistry registry;
    private final KeyNameResolver keyNames;
    private final Predicate<Keybind> reservedKeybinds;

    public BindCommand(ModuleRegistry registry, KeyNameResolver keyNames, Predicate<Keybind> reservedKeybinds) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.keyNames = Objects.requireNonNull(keyNames, "keyNames");
        this.reservedKeybinds = Objects.requireNonNull(reservedKeybinds, "reservedKeybinds");
    }

    @Override
    public String name() {
        return "bind";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "bind <module> <key> [toggle|hold|press_once]";
    }

    @Override
    public String description() {
        return "Binds a key that controls a module.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() < 2 || arguments.size() > 3) {
            feedback.error("Usage: " + usage());
            return;
        }

        String keyName = arguments.get(1).toLowerCase(Locale.ROOT);
        KeybindArgument input = KeybindArgument.parse(keyName, keyNames).orElse(null);
        if (input == null) {
            feedback.error("Unknown key '" + keyName + "'. Examples: r, ctrl+r, mouse1, alt+mouse1.");
            return;
        }
        if (reservedKeybinds.test(keybind(input, Keybind.Activation.TOGGLE))) {
            feedback.error("Key '" + keyName + "' opens the Helikon GUI, so it cannot activate a module. "
                    + "Pick another key or rebind the GUI key in Controls.");
            return;
        }

        final Keybind.Activation activation;
        if (arguments.size() == 3) {
            try {
                activation = Keybind.Activation.valueOf(arguments.get(2).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                feedback.error("Unknown activation '" + arguments.get(2) + "'. Use toggle, hold, or press_once.");
                return;
            }
        } else {
            activation = Keybind.Activation.TOGGLE;
        }

        Keybind keybind = keybind(input, activation);
        ModuleArguments.requireModule(registry, arguments.get(0), feedback).ifPresent(module -> {
            module.setKeybind(keybind);
            feedback.info("Bound '" + module.id() + "' to " + keyName + " ("
                    + activation.name().toLowerCase(Locale.ROOT) + ").");
            List<String> conflicts = KeybindConflicts.find(module, registry.all()).stream()
                    .map(conflict -> conflict.id())
                    .toList();
            if (!conflicts.isEmpty()) {
                feedback.info("Warning: this bind is also used by " + String.join(", ", conflicts) + ".");
            }
        });
    }

    private static Keybind keybind(KeybindArgument input, Keybind.Activation activation) {
        return new Keybind(input.inputType(), input.code(), input.modifiers(), activation);
    }
}
