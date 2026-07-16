package dev.helikon.client.panic;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.Objects;

/**
 * Executes a local panic reset through normal lifecycle boundaries. Future
 * modules restore gamma, timers, FOV, and other temporary state in onDisable.
 */
public final class PanicController {
    private final ModuleRegistry modules;
    private final PanicState state;
    private final Runnable closeHelikonScreen;
    private final Runnable clearTemporaryState;

    public PanicController(
            ModuleRegistry modules,
            PanicState state,
            Runnable closeHelikonScreen,
            Runnable clearTemporaryState
    ) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.state = Objects.requireNonNull(state, "state");
        this.closeHelikonScreen = Objects.requireNonNull(closeHelikonScreen, "closeHelikonScreen");
        this.clearTemporaryState = Objects.requireNonNull(clearTemporaryState, "clearTemporaryState");
    }

    /** Disables all modules, hides custom HUD for this session, and clears transient work. */
    public Result activate() {
        long enabledBefore = modules.all().stream().filter(Module::isEnabled).count();
        modules.disableAll();
        state.hideCustomHud();
        clearTemporaryState.run();
        closeHelikonScreen.run();
        return new Result(enabledBefore);
    }

    public void restoreCustomHud() {
        state.restoreCustomHud();
    }

    public record Result(long disabledModules) {
    }
}
