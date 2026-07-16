package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Lists registered modules grouped by category with their enabled state. */
public final class ModulesCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public ModulesCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "modules";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "modules";
    }

    @Override
    public String description() {
        return "Lists registered modules and their state.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (registry.all().isEmpty()) {
            feedback.info("No modules are registered.");
            return;
        }

        for (ModuleCategory category : ModuleCategory.values()) {
            String line = registry.all().stream()
                    .filter(module -> module.category() == category)
                    .map(ModulesCommand::describe)
                    .collect(Collectors.joining(", "));
            if (!line.isEmpty()) {
                feedback.info(category.displayName() + ": " + line);
            }
        }
    }

    private static String describe(Module module) {
        return module.id() + (module.isEnabled() ? " [on]" : " [off]");
    }
}
