package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Resets ordinary accumulated fall distance without affecting flight, Elytra, or vehicles. */
public final class NoFall extends Module {
    public NoFall() {
        super("no_fall", "NoFall", "Prevents ordinary fall damage by resetting accumulated fall distance.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
    }

    public boolean shouldResetFall(double fallDistance, boolean onGround, boolean passenger,
                                   boolean flying, boolean fallFlying) {
        if (!Double.isFinite(fallDistance) || fallDistance < 0.0D) {
            throw new IllegalArgumentException("fallDistance must be finite and non-negative");
        }
        return isEnabled() && fallDistance > 0.0D && !onGround && !passenger && !flying && !fallFlying;
    }

    /** Covers a teleport that occurs after the ordinary per-tick fall check. */
    public boolean protectsTeleport(boolean passenger, boolean flying, boolean fallFlying) {
        return isEnabled() && !passenger && !flying && !fallFlying;
    }
}
