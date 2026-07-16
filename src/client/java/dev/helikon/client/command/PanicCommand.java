package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Disables every enabled module through the registry so each module's
 * {@code onDisable} cleanup runs. Configuration is preserved.
 */
public final class PanicCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public PanicCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "panic";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "panic";
    }

    @Override
    public String description() {
        return "Disables all modules immediately.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        long enabledBefore = registry.all().stream().filter(Module::isEnabled).count();
        registry.disableAll();
        feedback.info("Panic: disabled " + enabledBefore + " module(s). Configuration is unchanged.");
    }
}
