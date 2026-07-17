package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

/** Conservative, capped local horizontal-velocity assistance; servers remain authoritative. */
public final class Speed extends Module {
    public enum Mode {
        VANILLA_ACCELERATION,
        STRAFE_ASSIST,
        MULTIPLIER
    }

    private final EnumSetting<Mode> mode;
    private final NumberSetting multiplier;
    private final NumberSetting acceleration;
    private final NumberSetting maximumSpeed;

    public Speed() {
        super("speed", "Speed", "Conservative local acceleration/strafe assistance with a multiplayer warning.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        mode = addSetting(new EnumSetting<>("mode", "Mode", "Choose a conservative local motion policy.",
                Mode.class, Mode.MULTIPLIER));
        multiplier = addSetting(new NumberSetting("multiplier", "Multiplier", "Capped horizontal multiplier.",
                3.0D, 1.0D, 3.0D));
        acceleration = addSetting(new NumberSetting("acceleration", "Acceleration", "Local directional acceleration.",
                0.08D, 0.0D, 0.08D));
        maximumSpeed = addSetting(new NumberSetting("maximum_speed", "Maximum speed", "Hard local horizontal speed cap.",
                0.90D, 0.05D, 0.90D));
    }

    public HorizontalVelocity adjust(HorizontalVelocity current, HorizontalVelocity desiredDirection, boolean moving) {
        if (current == null || desiredDirection == null) {
            throw new IllegalArgumentException("velocity inputs must not be null");
        }
        if (!isEnabled() || !moving) {
            return current;
        }
        HorizontalVelocity direction = desiredDirection.speed() == 0.0D ? desiredDirection
                : desiredDirection.scale(1.0D / desiredDirection.speed());
        HorizontalVelocity result = switch (mode.value()) {
            case VANILLA_ACCELERATION -> current.add(direction.scale(acceleration.value()));
            case STRAFE_ASSIST -> new HorizontalVelocity(
                    current.x() * 0.80D + direction.x() * acceleration.value() * 1.20D,
                    current.z() * 0.80D + direction.z() * acceleration.value() * 1.20D);
            case MULTIPLIER -> current.scale(multiplier.value());
        };
        return result.capped(maximumSpeed.value());
    }
}
