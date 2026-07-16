package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

import java.util.Objects;

/** Requests vanilla sprinting only while its local movement conditions allow it. */
public final class AutoSprint extends Module {
    /** A normal player sprint-state operation for the thin Minecraft adapter. */
    public enum SprintAction {
        NONE,
        START,
        STOP
    }

    private static final int VANILLA_MINIMUM_SPRINT_FOOD = 7;

    private final BooleanSetting always;
    private final BooleanSetting forwardOnly;
    private final BooleanSetting hungerCheck;
    private final BooleanSetting collisionCheck;
    private boolean ownsSprint;
    private boolean releaseRequested;

    public AutoSprint() {
        super("auto_sprint", "AutoSprint", "Requests normal sprinting under local safety checks.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        always = addSetting(new BooleanSetting("always", "Always",
                "Request sprinting even before a movement key is held.", false));
        forwardOnly = addSetting(new BooleanSetting("forward_only", "Forward only",
                "Require the local forward key instead of allowing any movement direction.", true));
        hungerCheck = addSetting(new BooleanSetting("hunger_check", "Hunger check",
                "Require Minecraft's normal minimum food level for sprinting.", true));
        collisionCheck = addSetting(new BooleanSetting("collision_check", "Collision check",
                "Do not request sprinting while the local player has a horizontal collision.", true));
    }

    /**
     * Produces at most one reversible sprint-state request for a tick. The
     * module never claims an already-manual sprint, so disabling it does not
     * cancel that manual input.
     */
    public SprintAction update(SprintContext context) {
        SprintContext current = Objects.requireNonNull(context, "context");
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

    /** Clears state when no local player exists, avoiding cross-world carryover. */
    public void onPlayerUnavailable() {
        ownsSprint = false;
        releaseRequested = false;
    }

    private boolean shouldRequestSprint(SprintContext context) {
        if (!isEnabled()) {
            return false;
        }
        if (hungerCheck.value() && context.foodLevel() < VANILLA_MINIMUM_SPRINT_FOOD) {
            return false;
        }
        if (collisionCheck.value() && context.horizontalCollision()) {
            return false;
        }
        if (always.value()) {
            return true;
        }
        return forwardOnly.value() ? context.forward() : context.moving();
    }

    @Override
    protected void onDisable() {
        if (ownsSprint) {
            ownsSprint = false;
            releaseRequested = true;
        }
    }
}
