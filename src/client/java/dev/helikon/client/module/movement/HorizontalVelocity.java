package dev.helikon.client.module.movement;

/** Immutable Minecraft-free horizontal velocity used by conservative movement policies. */
public record HorizontalVelocity(double x, double z) {
    public HorizontalVelocity {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("horizontal velocity must be finite");
        }
    }

    public double speed() {
        return Math.hypot(x, z);
    }

    public HorizontalVelocity scale(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
            throw new IllegalArgumentException("multiplier must be finite and non-negative");
        }
        return new HorizontalVelocity(x * multiplier, z * multiplier);
    }

    public HorizontalVelocity capped(double maximum) {
        if (!Double.isFinite(maximum) || maximum < 0.0D) {
            throw new IllegalArgumentException("maximum must be finite and non-negative");
        }
        double speed = speed();
        return speed > maximum && speed > 0.0D ? scale(maximum / speed) : this;
    }

    public HorizontalVelocity add(HorizontalVelocity other) {
        return new HorizontalVelocity(x + other.x, z + other.z);
    }
}
