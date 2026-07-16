package dev.helikon.client.command;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;

/** Assigns a module keybind, optionally with an activation mode. */
public final class BindCommand implements HelikonCommand {
    private final ModuleRegistry registry;
    private final KeyNameResolver keyNames;

    public BindCommand(ModuleRegistry registry, KeyNameResolver keyNames) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.keyNames = Objects.requireNonNull(keyNames, "keyNames");
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
        OptionalInt keyCode = keyNames.resolve(keyName);
        if (keyCode.isEmpty()) {
            feedback.error("Unknown key '" + keyName + "'. Examples: r, f6, right.shift.");
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

        Keybind keybind = new Keybind(keyCode.getAsInt(), activation);
        ModuleArguments.requireModule(registry, arguments.get(0), feedback).ifPresent(module -> {
            module.setKeybind(keybind);
            feedback.info("Bound '" + module.id() + "' to " + keyName + " ("
                    + activation.name().toLowerCase(Locale.ROOT) + ").");
        });
    }
}
