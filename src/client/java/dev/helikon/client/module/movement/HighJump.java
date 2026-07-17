package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;
import java.util.OptionalDouble;

/** Raises the vertical velocity of one ordinary local ground jump. */
public final class HighJump extends Module {
    /** Minecraft-free facts sampled after normal movement for the current client tick. */
    public record Context(boolean screenOpen, boolean onGround, boolean jumpHeld, boolean onClimbable,
                          boolean inFluid, boolean passenger, boolean abilityFlying, boolean fallFlying,
                          double currentVerticalVelocity) {
        public Context {
            if (!Double.isFinite(currentVerticalVelocity)) {
                throw new IllegalArgumentException("currentVerticalVelocity must be finite");
            }
        }
    }

    private final NumberSetting jumpVelocity;
    private boolean observedGround;

    public HighJump() {
        super("high_jump", "HighJump", "Raises the local velocity of one ordinary ground jump.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        jumpVelocity = addSetting(new NumberSetting("jump_velocity", "Jump velocity",
                "Local upward velocity applied once after a normal ground jump.", 0.70D, 0.42D, 2.00D));
    }

    /** Returns a single upward velocity adjustment only for the first tick of an eligible normal jump. */
    public OptionalDouble verticalVelocity(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        if (!isEnabled() || current.screenOpen() || current.onClimbable() || current.inFluid()
                || current.passenger() || current.abilityFlying() || current.fallFlying()) {
            observedGround = false;
            return OptionalDouble.empty();
        }
        if (current.onGround()) {
            observedGround = true;
            return OptionalDouble.empty();
        }
        boolean boost = observedGround && current.jumpHeld() && current.currentVerticalVelocity() > 0.0D;
        observedGround = false;
        return boost ? OptionalDouble.of(Math.max(current.currentVerticalVelocity(), jumpVelocity.value()))
                : OptionalDouble.empty();
    }

    /** Clears a stale ground observation when player/world state becomes unavailable. */
    public void onContextLost() {
        observedGround = false;
    }

    @Override
    protected void onDisable() {
        observedGround = false;
    }
}
