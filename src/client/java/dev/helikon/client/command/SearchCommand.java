package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleSearch;

import java.util.List;
import java.util.Objects;

/** Searches modules by name, ID, and description using the shared GUI rules. */
public final class SearchCommand implements HelikonCommand {
    private static final int MAX_RESULT_LINES = 8;

    private final ModuleRegistry registry;

    public SearchCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "search";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "search <text>";
    }

    @Override
    public String description() {
        return "Finds modules by name, ID, or description.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }

        String query = String.join(" ", arguments);
        List<Module> matches = ModuleSearch.filter(registry.all(), query);
        if (matches.isEmpty()) {
            feedback.info("No modules match '" + query + "'.");
            return;
        }

        feedback.info(matches.size() + " module(s) match '" + query + "':");
        for (Module module : matches.subList(0, Math.min(matches.size(), MAX_RESULT_LINES))) {
            feedback.info(module.id() + " — " + module.name() + " (" + module.category().displayName() + ")");
        }
        if (matches.size() > MAX_RESULT_LINES) {
            feedback.info("…and " + (matches.size() - MAX_RESULT_LINES) + " more.");
        }
    }
}
