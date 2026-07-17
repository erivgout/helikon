package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.OptionalDouble;

/** Caps ordinary local falling descent without changing fall state or packets. */
public final class Glide extends Module {
    private final NumberSetting descentSpeed;

    public Glide() {
        super("glide", "Glide", "Caps local falling descent while preserving normal opt-out states.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        descentSpeed = addSetting(new NumberSetting("descent_speed", "Descent speed",
                "Maximum local downward velocity while gliding; hold Sneak to descend normally.",
                0.08D, 0.01D, 0.40D));
    }

    /** Returns a capped downward velocity only for an eligible ordinary fall. */
    public OptionalDouble verticalVelocity(boolean screenOpen, boolean onGround, boolean inWater,
                                           boolean onClimbable, boolean sneaking, boolean passenger,
                                           boolean abilityFlying, boolean fallFlying, double currentVerticalVelocity) {
        if (!Double.isFinite(currentVerticalVelocity)) {
            throw new IllegalArgumentException("currentVerticalVelocity must be finite");
        }
        if (!isEnabled() || screenOpen || onGround || inWater || onClimbable || sneaking || passenger
                || abilityFlying || fallFlying || currentVerticalVelocity >= -descentSpeed.value()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(-descentSpeed.value());
    }
}
