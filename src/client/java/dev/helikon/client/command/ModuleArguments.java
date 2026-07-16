package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.Optional;

/** Shared argument handling for commands that take a module ID. */
final class ModuleArguments {
    private ModuleArguments() {
    }

    static Optional<Module> requireModule(ModuleRegistry registry, String moduleId, CommandFeedback feedback) {
        Optional<Module> module = registry.find(moduleId);
        if (module.isEmpty()) {
            feedback.error("Unknown module '" + moduleId + "'. Use " + CommandDispatcher.PREFIX + "modules to list modules.");
        }
        return module;
    }
}
