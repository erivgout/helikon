package dev.helikon.client.command;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/** Removes a module's keybind. */
public final class UnbindCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public UnbindCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "unbind";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "unbind <module>";
    }

    @Override
    public String description() {
        return "Removes a module's keybind.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + usage());
            return;
        }

        ModuleArguments.requireModule(registry, arguments.get(0), feedback).ifPresent(module -> {
            module.setKeybind(Keybind.unbound());
            feedback.info("Unbound '" + module.id() + "'.");
        });
    }
}
