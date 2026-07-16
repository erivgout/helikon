package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/** Toggles a module by ID through the registry's failure-isolated path. */
public final class ToggleCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public ToggleCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "toggle";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "toggle <module>";
    }

    @Override
    public String description() {
        return "Enables or disables a module.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + usage());
            return;
        }

        ModuleArguments.requireModule(registry, arguments.get(0), feedback).ifPresent(module -> {
            boolean wantedEnabled = !module.isEnabled();
            if (registry.setEnabled(module, wantedEnabled)) {
                feedback.info((wantedEnabled ? "Enabled '" : "Disabled '") + module.id() + "'.");
            } else {
                feedback.error("Failed to " + (wantedEnabled ? "enable '" : "disable '") + module.id()
                        + "'; it was disabled after an error (see log).");
            }
        });
    }
}
