package dev.helikon.client.integration;

/**
 * Narrow boundary between Helikon controls and the separately licensed
 * embedded Baritone component.
 */
public interface BaritoneAccess {
    record Options(
            boolean active,
            boolean allowBreak,
            boolean allowPlace,
            boolean allowSprint,
            boolean allowParkour,
            boolean allowInventory,
            boolean hashCommands,
            boolean renderPath,
            boolean renderGoal
    ) {
        /**
         * Baritone's inventory behavior ticks independently of active pathing. Keep it
         * dormant whenever the Helikon module is off, including after panic.
         */
        public boolean inventoryAutomationEnabled() {
            return active && allowInventory;
        }
    }

    void apply(Options options);

    boolean execute(String command);

    void cancel();

    /** Temporarily yields Baritone's movement controls while local combat is active. */
    void setCombatPaused(boolean paused);

    /** Temporarily yields Baritone's movement controls while AutoEat owns the use key. */
    void setAutoEatPaused(boolean paused);

    /** Mirrors FastBreak's requested delay into Baritone without taking ownership when disabled. */
    void setFastBreakDelay(boolean enabled, int delayTicks);

    /** Whether Baritone is currently supplying horizontal movement input. */
    boolean isMovementForced();

    boolean isPathing();

    boolean hasPath();

    String goalDescription();
}
