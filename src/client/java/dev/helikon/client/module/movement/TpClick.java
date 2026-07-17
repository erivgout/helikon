package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindInputConsumer;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Optional;

/**
 * Relocates the local player to the block face it is looking at when the
 * module's bound key is pressed. The decision logic here is Minecraft-free and
 * unit-tested; a thin adapter performs the raycast and applies the local
 * position. The teleport is a client-side position change: Minecraft's ordinary
 * movement packet then reports the new position, so an authoritative server may
 * reject, correct, rubber-band, or kick.
 */
public final class TpClick extends Module implements KeybindInputConsumer {
    /** An immutable Minecraft-free feet destination for the local player. */
    public record Destination(double x, double y, double z) {
        public Destination {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("teleport destination must be finite");
            }
        }
    }

    private final NumberSetting maxDistance;
    private final BooleanSetting cancelVelocity;
    private boolean triggerKeyDown;

    public TpClick() {
        super("tp_click", "TpClick",
                "Teleports the local player to the block face it is looking at when the bound key is pressed.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        maxDistance = addSetting(new NumberSetting("max_distance", "Max distance",
                "Maximum block distance the teleport ray searches for a target.", 64.0D, 1.0D, 256.0D));
        cancelVelocity = addSetting(new BooleanSetting("cancel_velocity", "Cancel velocity",
                "Reset local velocity and fall distance after teleporting so no momentum is carried.", true));
    }

    /**
     * Tracks the trigger key across ticks and reports only the rising edge while
     * enabled and no screen is open. The physical key state is always recorded
     * so a key held across a screen (or across enabling) is not treated as a
     * fresh press until it is released and pressed again.
     */
    public boolean pollTrigger(boolean keyDown, boolean screenOpen) {
        boolean previous = triggerKeyDown;
        triggerKeyDown = keyDown;
        return isEnabled() && !screenOpen && keyDown && !previous;
    }

    /**
     * Computes the feet destination adjacent to a looked-at block face, or empty
     * when the module is disabled or the target is out of the configured range.
     */
    public Optional<Destination> destination(int hitX, int hitY, int hitZ,
                                             int faceX, int faceY, int faceZ, double distance) {
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("teleport distance must be finite");
        }
        if (Math.abs(faceX) + Math.abs(faceY) + Math.abs(faceZ) != 1) {
            throw new IllegalArgumentException("hit face must be one axis-aligned unit vector");
        }
        if (!isEnabled() || distance <= 0.0D || distance > maxDistance.value()) {
            return Optional.empty();
        }
        return Optional.of(new Destination(
                Math.addExact(hitX, faceX) + 0.5D,
                Math.addExact(hitY, faceY),
                Math.addExact(hitZ, faceZ) + 0.5D));
    }

    public double maxDistance() {
        return maxDistance.value();
    }

    public boolean cancelVelocity() {
        return cancelVelocity.value();
    }

    /** The bound key triggers a teleport instead of toggling the module; enable it through the GUI or a command. */
    @Override
    public boolean consumesKeybindInput() {
        return true;
    }

    @Override
    protected void onEnable() {
        // Require a fresh press before the first teleport, even if the key was already held.
        triggerKeyDown = true;
    }

    @Override
    protected void onDisable() {
        triggerKeyDown = false;
    }
}
