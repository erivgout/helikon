package dev.helikon.client.command;

import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/** Resets every setting of a module to its default value. */
public final class ResetCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public ResetCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "reset <module>";
    }

    @Override
    public String description() {
        return "Resets a module's settings to defaults.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + usage());
            return;
        }

        ModuleArguments.requireModule(registry, arguments.get(0), feedback).ifPresent(module -> {
            module.resetSettings();
            feedback.info("Reset all settings of '" + module.id() + "' to defaults.");
        });
    }
}
