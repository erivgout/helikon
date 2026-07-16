package dev.helikon.client.macro;

import java.util.Objects;
import java.util.Optional;

/** Client-tick macro scheduler that executes at most one configured action per tick. */
public final class MacroRunner {
    private Macro active;
    private int actionIndex;
    private int remainingDelayTicks;

    public synchronized boolean isRunning() {
        return active != null;
    }

    public synchronized Optional<String> activeName() {
        return active == null ? Optional.empty() : Optional.of(active.name());
    }

    /** Starts a nonempty macro when no other macro is active. */
    public synchronized void start(Macro macro, Optional<String> currentServerAddress) {
        Macro candidate = Objects.requireNonNull(macro, "macro");
        if (active != null) {
            throw new IllegalStateException("Macro '" + active.name() + "' is already running");
        }
        if (candidate.actions().isEmpty()) {
            throw new IllegalArgumentException("Macro '" + candidate.name() + "' has no actions");
        }
        if (candidate.isServerScoped() && !matchesCurrentServer(candidate, currentServerAddress)) {
            throw new IllegalArgumentException("Macro '" + candidate.name() + "' is restricted to " + candidate.serverAddress());
        }
        active = candidate;
        actionIndex = 0;
        remainingDelayTicks = 0;
    }

    /** Cancels the current run without changing its persisted definition. */
    public synchronized Optional<String> cancel() {
        if (active == null) {
            return Optional.empty();
        }
        String name = active.name();
        clear();
        return Optional.of(name);
    }

    /**
     * Cancels a server-scoped macro immediately after its connection changes,
     * without advancing any action or delay. Call this even while screens pause
     * normal execution.
     */
    public synchronized TickResult validateServerContext(Optional<String> currentServerAddress) {
        Objects.requireNonNull(currentServerAddress, "currentServerAddress");
        if (active != null && active.isServerScoped() && !matchesCurrentServer(active, currentServerAddress)) {
            return finish(TickStatus.CANCELLED_CONTEXT, "Server context changed");
        }
        return TickResult.idle();
    }

    /** Advances one client tick, pausing only through explicit delay actions. */
    public synchronized TickResult tick(Optional<String> currentServerAddress, MacroActionExecutor executor) {
        Objects.requireNonNull(currentServerAddress, "currentServerAddress");
        Objects.requireNonNull(executor, "executor");
        if (active == null) {
            return TickResult.idle();
        }
        TickResult contextResult = validateServerContext(currentServerAddress);
        if (contextResult.status() == TickStatus.CANCELLED_CONTEXT) {
            return contextResult;
        }
        if (remainingDelayTicks > 0) {
            remainingDelayTicks--;
            return TickResult.waiting(active.name(), remainingDelayTicks);
        }
        if (actionIndex >= active.actions().size()) {
            return finish(TickStatus.COMPLETED, "Completed");
        }

        MacroAction action = active.actions().get(actionIndex++);
        if (action.type() == MacroActionType.DELAY) {
            remainingDelayTicks = action.delayTicks();
            return TickResult.waiting(active.name(), remainingDelayTicks);
        }
        try {
            execute(action, executor);
        } catch (RuntimeException exception) {
            return finish(TickStatus.FAILED, exception.getMessage());
        }
        if (actionIndex >= active.actions().size()) {
            return finish(TickStatus.COMPLETED, "Completed");
        }
        return TickResult.executed(active.name(), action.type());
    }

    private static boolean matchesCurrentServer(Macro macro, Optional<String> currentServerAddress) {
        return currentServerAddress.map(Macro::normalizeServerAddress)
                .filter(macro.serverAddress()::equals)
                .isPresent();
    }

    private static void execute(MacroAction action, MacroActionExecutor executor) {
        switch (action.type()) {
            case LOCAL -> executor.executeLocalCommand(action.text());
            case CHAT -> executor.sendChat(action.text());
            case COMMAND -> executor.sendMinecraftCommand(action.text());
            case DELAY -> throw new IllegalStateException("Delay action cannot execute directly");
        }
    }

    private TickResult finish(TickStatus status, String message) {
        String name = active.name();
        clear();
        return new TickResult(status, name, Optional.empty(), 0, message == null ? "Unknown error" : message);
    }

    private void clear() {
        active = null;
        actionIndex = 0;
        remainingDelayTicks = 0;
    }

    public enum TickStatus {
        IDLE,
        WAITING,
        EXECUTED,
        COMPLETED,
        CANCELLED_CONTEXT,
        FAILED
    }

    /** A deterministic tick outcome used by the thin client notifier bridge. */
    public record TickResult(TickStatus status, String macroName, Optional<MacroActionType> actionType,
                             int remainingDelayTicks, String detail) {
        private static TickResult idle() {
            return new TickResult(TickStatus.IDLE, "", Optional.empty(), 0, "");
        }

        private static TickResult waiting(String macroName, int remainingDelayTicks) {
            return new TickResult(TickStatus.WAITING, macroName, Optional.empty(), remainingDelayTicks, "");
        }

        private static TickResult executed(String macroName, MacroActionType actionType) {
            return new TickResult(TickStatus.EXECUTED, macroName, Optional.of(actionType), 0, "");
        }
    }
}
