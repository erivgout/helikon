package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Arms only already-permitted Minecraft flight as a conservative fall safety measure. */
public final class NoFall extends Module {
    public record Action(boolean setFlying, boolean flying) {
        private static final Action NONE = new Action(false, false);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting activationDistance;
    private boolean ownedFlying;

    public NoFall() {
        super("no_fall", "NoFall", "Arms only environment-permitted flight near a configured fall distance.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        activationDistance = addSetting(new NumberSetting("activation_distance", "Activation distance",
                "Fall distance at which allowed flight is armed locally.", 6.0D, 2.0D, 40.0D));
    }

    public boolean shouldArmAllowedFlight(double fallDistance, boolean mayFly, boolean onGround) {
        if (!Double.isFinite(fallDistance) || fallDistance < 0.0D) {
            throw new IllegalArgumentException("fallDistance must be finite and non-negative");
        }
        return isEnabled() && mayFly && !onGround && fallDistance >= activationDistance.value();
    }

    /** Arms and restores only flight state owned by this local safety policy. */
    public Action update(double fallDistance, boolean mayFly, boolean onGround, boolean flying, boolean activationAllowed) {
        if (!Double.isFinite(fallDistance) || fallDistance < 0.0D) {
            throw new IllegalArgumentException("fallDistance must be finite and non-negative");
        }
        if (activationAllowed && shouldArmAllowedFlight(fallDistance, mayFly, onGround) && !flying) {
            ownedFlying = true;
            return new Action(true, true);
        }
        if (ownedFlying && (!isEnabled() || onGround || !mayFly)) {
            boolean restore = mayFly && flying;
            ownedFlying = false;
            return restore ? new Action(true, false) : Action.none();
        }
        return Action.none();
    }

    @Override
    protected void onDisable() {
        // The next tick restores only flight that this module armed.
    }

    /** Drops local ownership when the player/world it belonged to is gone. */
    public void onContextLost() {
        ownedFlying = false;
    }
}
