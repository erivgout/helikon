package dev.helikon.client.module;

/** Receives a report after the registry isolates a faulty module. */
@FunctionalInterface
public interface ModuleFailureHandler {
    void onModuleFailure(Module module, String operation, RuntimeException exception);
}
