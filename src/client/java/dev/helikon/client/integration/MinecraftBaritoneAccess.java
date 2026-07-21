package dev.helikon.client.integration;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.input.Input;

/** Direct in-process adapter for Helikon's embedded Baritone component. */
public final class MinecraftBaritoneAccess implements BaritoneAccess {
    private final TemporaryPauseProcess combatPause = new TemporaryPauseProcess("Helikon combat pause");
    private final TemporaryPauseProcess autoEatPause = new TemporaryPauseProcess("Helikon AutoEat pause");
    private boolean combatPauseRegistered;
    private boolean autoEatPauseRegistered;
    private int originalBlockBreakSpeed = -1;
    private int lastAppliedBlockBreakSpeed = -1;

    @Override
    public void apply(Options options) {
        Settings settings = BaritoneAPI.getSettings();
        settings.allowBreak.value = options.allowBreak();
        settings.allowPlace.value = options.allowPlace();
        settings.allowSprint.value = options.allowSprint();
        settings.allowParkour.value = options.allowParkour();
        settings.allowInventory.value = options.inventoryAutomationEnabled();
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
    public void setCombatPaused(boolean paused) {
        if (paused && !combatPauseRegistered) {
            primary().getPathingControlManager().registerProcess(combatPause);
            combatPauseRegistered = true;
        }
        combatPause.setPaused(paused);
    }

    @Override
    public void setAutoEatPaused(boolean paused) {
        if (paused && !autoEatPauseRegistered) {
            primary().getPathingControlManager().registerProcess(autoEatPause);
            autoEatPauseRegistered = true;
        }
        autoEatPause.setPaused(paused);
    }

    @Override
    public void setFastBreakDelay(boolean enabled, int delayTicks) {
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks must not be negative");
        }
        Settings.Setting<Integer> setting = BaritoneAPI.getSettings().blockBreakSpeed;
        if (enabled) {
            int requested = Math.max(1, delayTicks + 1);
            if (originalBlockBreakSpeed < 0) {
                originalBlockBreakSpeed = setting.value;
            }
            lastAppliedBlockBreakSpeed = requested;
            setting.value = requested;
            return;
        }
        if (originalBlockBreakSpeed >= 0 && setting.value == lastAppliedBlockBreakSpeed) {
            setting.value = originalBlockBreakSpeed;
        }
        originalBlockBreakSpeed = -1;
        lastAppliedBlockBreakSpeed = -1;
    }

    @Override
    public boolean isMovementForced() {
        IBaritone baritone = primary();
        if (!baritone.getPathingBehavior().isPathing()) {
            return false;
        }
        return baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_FORWARD)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_BACK)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_LEFT)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_RIGHT);
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

    /**
     * A temporary process preserves the current goal and composes correctly
     * with Baritone's own manual pause process.
     */
    private static final class TemporaryPauseProcess implements IBaritoneProcess {
        private final String name;
        private volatile boolean paused;

        private TemporaryPauseProcess(String name) {
            this.name = name;
        }

        private void setPaused(boolean paused) {
            this.paused = paused;
        }

        @Override
        public boolean isActive() {
            return paused;
        }

        @Override
        public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
            primary().getInputOverrideHandler().clearAllKeys();
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        @Override
        public boolean isTemporary() {
            return true;
        }

        @Override
        public void onLostControl() {
            // Combat pause owns no goal or path state.
        }

        @Override
        public double priority() {
            return 10.0D;
        }

        @Override
        public String displayName0() {
            return name;
        }
    }
}
