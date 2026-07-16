package dev.helikon.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Explicit module registry with lifecycle failure isolation. A callback failure
 * is logged, reported, and followed by a best-effort cleanup rather than
 * allowed to take down the rest of the client.
 */
public final class ModuleRegistry {
    private static final Logger LOGGER = Logger.getLogger(ModuleRegistry.class.getName());

    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final List<ModuleFailureHandler> failureHandlers = new ArrayList<>();

    public void register(Module module) {
        Module nonNullModule = Objects.requireNonNull(module, "module");
        if (modules.containsKey(nonNullModule.id())) {
            throw new IllegalArgumentException("A module is already registered with id '" + nonNullModule.id() + "'");
        }
        modules.put(nonNullModule.id(), nonNullModule);
    }

    public Optional<Module> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modules.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<Module> all() {
        return List.copyOf(modules.values());
    }

    public boolean setEnabled(Module module, boolean shouldEnable) {
        Objects.requireNonNull(module, "module");
        if (!modules.containsValue(module)) {
            throw new IllegalArgumentException("Module '" + module.id() + "' is not registered");
        }

        String operation = shouldEnable ? "enable" : "disable";
        try {
            if (shouldEnable) {
                module.enable();
            } else {
                module.disable();
            }
            return module.isEnabled() == shouldEnable;
        } catch (RuntimeException exception) {
            isolateFailure(module, operation, exception);
            return false;
        }
    }

    public boolean toggle(Module module) {
        return setEnabled(module, !module.isEnabled());
    }

    public void enableDefaultModules() {
        for (Module module : modules.values()) {
            if (module.defaultEnabled()) {
                setEnabled(module, true);
            }
        }
    }

    public void disableAll() {
        for (Module module : modules.values()) {
            setEnabled(module, false);
        }
    }

    /**
     * Runs periodic module work through the same failure isolation as lifecycle
     * transitions. A failed callback disables the registered module so its
     * normal cleanup can restore any client state it owns.
     */
    public boolean runGuarded(Module module, String operation, Runnable action) {
        Module nonNullModule = Objects.requireNonNull(module, "module");
        String nonBlankOperation = Objects.requireNonNull(operation, "operation");
        Runnable nonNullAction = Objects.requireNonNull(action, "action");
        if (nonBlankOperation.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }
        if (!modules.containsValue(nonNullModule)) {
            throw new IllegalArgumentException("Module '" + nonNullModule.id() + "' is not registered");
        }

        try {
            nonNullAction.run();
            return true;
        } catch (RuntimeException exception) {
            isolateFailure(nonNullModule, nonBlankOperation, exception);
            return false;
        }
    }

    public void addFailureHandler(ModuleFailureHandler handler) {
        failureHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    private void isolateFailure(Module module, String operation, RuntimeException exception) {
        LOGGER.log(Level.SEVERE, "Failed to " + operation + " module '" + module.id() + "'; disabling it", exception);

        if (module.isEnabled()) {
            try {
                module.disable();
            } catch (RuntimeException cleanupException) {
                LOGGER.log(Level.SEVERE, "Failed to clean up module '" + module.id() + "'", cleanupException);
                exception.addSuppressed(cleanupException);
            }
        }

        for (ModuleFailureHandler handler : List.copyOf(failureHandlers)) {
            try {
                handler.onModuleFailure(module, operation, exception);
            } catch (RuntimeException handlerException) {
                LOGGER.log(Level.WARNING, "Module failure handler threw an exception", handlerException);
            }
        }
    }
}
