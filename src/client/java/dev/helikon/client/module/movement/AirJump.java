package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;
import java.util.OptionalDouble;

/** Applies one local air jump for each fresh Jump-key press while airborne. */
public final class AirJump extends Module {
    public record Context(boolean screenOpen, boolean onGround, boolean jumpHeld, boolean inFluid,
                          boolean onClimbable, boolean passenger, boolean abilityFlying,
                          boolean fallFlying, double currentVerticalVelocity) {
        public Context {
            if (!Double.isFinite(currentVerticalVelocity)) {
                throw new IllegalArgumentException("currentVerticalVelocity must be finite");
            }
        }
    }

    private final NumberSetting jumpVelocity;
    private final BooleanSetting repeatWhileHeld;
    private final IntegerSetting repeatDelayTicks;
    private boolean jumpWasHeld;
    private int heldAirTicks;

    public AirJump() {
        super("air_jump", "Air Jump",
                "Lets each fresh Jump-key press apply a local jump while airborne.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        jumpVelocity = addSetting(new NumberSetting("jump_velocity", "Jump velocity",
                "Upward velocity applied by an airborne jump.", 0.42D, 0.10D, 1.50D));
        repeatWhileHeld = addSetting(new BooleanSetting("repeat_while_held", "Repeat while held",
                "Automatically perform another air jump while Space remains held.", true));
        repeatDelayTicks = addSetting(new IntegerSetting("repeat_delay_ticks", "Repeat delay",
                "Ticks between automatic held-Space air jumps.", 6, 2, 20,
                repeatWhileHeld::value));
    }

    public OptionalDouble verticalVelocity(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        boolean freshPress = current.jumpHeld() && !jumpWasHeld;
        jumpWasHeld = current.jumpHeld();
        if (!isEnabled() || current.screenOpen() || current.onGround()
                || current.inFluid() || current.onClimbable() || current.passenger()
                || current.abilityFlying() || current.fallFlying()) {
            heldAirTicks = 0;
            return OptionalDouble.empty();
        }
        if (!current.jumpHeld()) {
            heldAirTicks = 0;
            return OptionalDouble.empty();
        }
        heldAirTicks++;
        boolean repeat = repeatWhileHeld.value() && heldAirTicks >= repeatDelayTicks.value();
        if (!freshPress && !repeat) {
            return OptionalDouble.empty();
        }
        heldAirTicks = 0;
        return OptionalDouble.of(Math.max(current.currentVerticalVelocity(), jumpVelocity.value()));
    }

    public void onContextLost() {
        jumpWasHeld = false;
        heldAirTicks = 0;
    }

    @Override
    protected void onDisable() {
        jumpWasHeld = false;
        heldAirTicks = 0;
    }
}
