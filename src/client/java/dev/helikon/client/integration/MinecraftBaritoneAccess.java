package dev.helikon.client.integration;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;

/** Direct in-process adapter for Helikon's embedded Baritone component. */
public final class MinecraftBaritoneAccess implements BaritoneAccess {
    @Override
    public void apply(Options options) {
        Settings settings = BaritoneAPI.getSettings();
        settings.allowBreak.value = options.allowBreak();
        settings.allowPlace.value = options.allowPlace();
        settings.allowSprint.value = options.allowSprint();
        settings.allowParkour.value = options.allowParkour();
        settings.allowInventory.value = options.allowInventory();
        settings.chatControl.value = options.active() && options.hashCommands();
        settings.chatControlAnyway.value = options.active() && options.hashCommands();
        settings.renderPath.value = options.active() && options.renderPath();
        settings.renderGoal.value = options.active() && options.renderGoal();
    }

    @Override
    public boolean execute(String command) {
        return primary().getCommandManager().execute(command);
    }

    @Override
    public void cancel() {
        if (!primary().getCommandManager().execute("cancel")) {
            primary().getPathingBehavior().cancelEverything();
        }
    }

    @Override
    public boolean isPathing() {
        return primary().getPathingBehavior().isPathing();
    }

    @Override
    public boolean hasPath() {
        return primary().getPathingBehavior().hasPath();
    }

    @Override
    public String goalDescription() {
        Object goal = primary().getPathingBehavior().getGoal();
        return goal == null ? "none" : goal.toString();
    }

    private static IBaritone primary() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }
}
