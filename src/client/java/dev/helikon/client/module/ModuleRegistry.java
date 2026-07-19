package dev.helikon.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, Long> activationOrders = new LinkedHashMap<>();
    private final List<ModuleFailureHandler> failureHandlers = new ArrayList<>();
    private final List<ModuleStateChangeHandler> stateChangeHandlers = new ArrayList<>();
    private final Set<String> activeFailureEpisodes = ConcurrentHashMap.newKeySet();
    private long nextActivationOrder;
    private volatile ModuleTimingMetrics timingMetrics;

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
        boolean wasEnabled = module.isEnabled();
        if (shouldEnable && !module.isEnabled()) {
            activeFailureEpisodes.remove(module.id());
        }
        try {
            if (shouldEnable) {
                module.enable();
                if (!wasEnabled && module.isEnabled()) {
                    activationOrders.put(module.id(), ++nextActivationOrder);
                }
            } else {
                module.disable();
            }
        } catch (RuntimeException exception) {
            isolateFailure(module, operation, exception);
            notifyStateChange(module, wasEnabled);
            return false;
        }
        notifyStateChange(module, wasEnabled);
        return module.isEnabled() == shouldEnable;
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

    /** Disables every enabled module except one safety module that must remain armed. */
    public long disableAllExcept(Module retainedModule) {
        Module retained = Objects.requireNonNull(retainedModule, "retainedModule");
        if (!modules.containsValue(retained)) {
            throw new IllegalArgumentException("Module '" + retained.id() + "' is not registered");
        }
        long disabled = 0L;
        for (Module module : modules.values()) {
            if (module != retained && module.isEnabled() && setEnabled(module, false)) {
                disabled++;
            }
        }
        return disabled;
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

        ModuleTimingMetrics metrics = timingMetrics;
        boolean measure = metrics != null && metrics.isRecording();
        long startedAt = measure ? System.nanoTime() : 0L;
        try {
            nonNullAction.run();
            return true;
        } catch (RuntimeException exception) {
            isolateFailure(nonNullModule, nonBlankOperation, exception);
            return false;
        } finally {
            if (measure) {
                metrics.record(nonNullModule.id(), nonBlankOperation, Math.max(0L, System.nanoTime() - startedAt));
            }
        }
    }

    public void addFailureHandler(ModuleFailureHandler handler) {
        failureHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /** Observes successful state transitions so callers can persist them without polling every module. */
    public void addStateChangeHandler(ModuleStateChangeHandler handler) {
        stateChangeHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /** Installs optional local diagnostics; measurement remains inactive until its module enables recording. */
    public void setTimingMetrics(ModuleTimingMetrics timingMetrics) {
        this.timingMetrics = Objects.requireNonNull(timingMetrics, "timingMetrics");
    }

    /** Monotonic local order of the module's latest successful enable transition. */
    public long activationOrder(Module module) {
        Module nonNullModule = Objects.requireNonNull(module, "module");
        if (!modules.containsValue(nonNullModule)) {
            throw new IllegalArgumentException("Module '" + nonNullModule.id() + "' is not registered");
        }
        return activationOrders.getOrDefault(nonNullModule.id(), 0L);
    }

    private void isolateFailure(Module module, String operation, RuntimeException exception) {
        boolean firstReport = activeFailureEpisodes.add(module.id());
        if (firstReport) {
            LOGGER.log(Level.SEVERE, "Failed to " + operation + " module '" + module.id() + "'; disabling it",
                    exception);
        }

        if (module.isEnabled()) {
            try {
                module.disable();
            } catch (RuntimeException cleanupException) {
                LOGGER.log(Level.SEVERE, "Failed to clean up module '" + module.id() + "'", cleanupException);
                exception.addSuppressed(cleanupException);
            }
        }

        if (firstReport) {
            for (ModuleFailureHandler handler : List.copyOf(failureHandlers)) {
                try {
                    handler.onModuleFailure(module, operation, exception);
                } catch (RuntimeException handlerException) {
                    LOGGER.log(Level.WARNING, "Module failure handler threw an exception", handlerException);
                }
            }
        }
    }

    private void notifyStateChange(Module module, boolean previousState) {
        if (module.isEnabled() == previousState) {
            return;
        }
        for (ModuleStateChangeHandler handler : List.copyOf(stateChangeHandlers)) {
            try {
                handler.onStateChanged(module, module.isEnabled());
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Module state-change handler threw an exception", exception);
            }
        }
    }

    @FunctionalInterface
    public interface ModuleStateChangeHandler {
        void onStateChanged(Module module, boolean enabled);
    }
}
