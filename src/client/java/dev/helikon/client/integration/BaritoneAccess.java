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
    }

    void apply(Options options);

    boolean execute(String command);

    void cancel();

    boolean isPathing();

    boolean hasPath();

    String goalDescription();
}
