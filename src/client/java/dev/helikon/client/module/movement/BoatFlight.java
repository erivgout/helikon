package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Velocity-steers only a locally driven ridden boat. */
public final class BoatFlight extends Module {
    /** One computed local boat velocity in blocks per tick. */
    public record Velocity(double x, double y, double z) {
    }

    private final NumberSetting speed;

    public BoatFlight() {
        super("boat_flight", "Boat Flight",
                "Steers a ridden boat with local horizontal and vertical velocity; servers may reject.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        speed = addSetting(new NumberSetting("speed", "Speed",
                "Local boat-flight speed in blocks per tick.", 0.6D, 0.1D, 2.0D));
    }

    /** Computes bounded boat velocity from already-observed ordinary inputs. */
    public Velocity velocity(HorizontalVelocity desiredDirection, boolean jump, boolean sneak) {
        if (desiredDirection == null) {
            throw new IllegalArgumentException("desiredDirection must not be null");
        }
        HorizontalVelocity direction = desiredDirection.speed() == 0.0D ? desiredDirection
                : desiredDirection.scale(1.0D / desiredDirection.speed());
        double vertical = (jump ? 1.0D : 0.0D) - (sneak ? 1.0D : 0.0D);
        return new Velocity(direction.x() * speed.value(), vertical * speed.value(), direction.z() * speed.value());
    }
}
