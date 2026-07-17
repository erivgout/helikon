package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

import java.util.Objects;

/** Requests normal sprinting while the local player is moving forward in water. */
public final class AutoSwim extends Module {
    public enum SprintAction {
        NONE,
        START,
        STOP
    }

    /** Minecraft-free local facts needed to decide whether vanilla swimming sprint should be requested. */
    public record Context(boolean screenOpen, boolean inWater, boolean forward, boolean passenger,
                          boolean abilityFlying, int foodLevel, boolean currentlySprinting) {
        public Context {
            if (foodLevel < 0 || foodLevel > 20) {
                throw new IllegalArgumentException("foodLevel must be between 0 and 20");
            }
        }
    }

    private final IntegerSetting minimumFood;
    private boolean ownsSprint;
    private boolean releaseRequested;

    public AutoSwim() {
        super("auto_swim", "AutoSwim", "Requests normal sprinting while moving forward in water.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        minimumFood = addSetting(new IntegerSetting("minimum_food", "Minimum food",
                "Minimum local food level required before requesting normal swim sprinting.", 7, 0, 20));
    }

    /** Produces at most one reversible normal sprint-state request for the current tick. */
    public SprintAction update(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        if (releaseRequested) {
            releaseRequested = false;
            return SprintAction.STOP;
        }
        if (!shouldRequestSprint(current)) {
            if (ownsSprint) {
                ownsSprint = false;
                return SprintAction.STOP;
            }
            return SprintAction.NONE;
        }
        if (!current.currentlySprinting()) {
            ownsSprint = true;
            return SprintAction.START;
        }
        return SprintAction.NONE;
    }

    /** Clears ownership without touching a player that no longer exists. */
    public void onPlayerUnavailable() {
        ownsSprint = false;
        releaseRequested = false;
    }

    @Override
    protected void onDisable() {
        if (ownsSprint) {
            ownsSprint = false;
            releaseRequested = true;
        }
    }

    private boolean shouldRequestSprint(Context context) {
        return isEnabled() && !context.screenOpen() && context.inWater() && context.forward()
                && !context.passenger() && !context.abilityFlying() && context.foodLevel() >= minimumFood.value();
    }
}
